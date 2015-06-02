package org.brewman.upload.service.impl;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileExistsException;
import org.brewman.upload.domain.Upload;
import org.brewman.upload.repository.UploadRepository;
import org.brewman.upload.service.UploadService;
import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.codahale.metrics.annotation.Timed;

/**
 * A very simple implementation of the UploadService.
 * 
 * This version will just store the chunks and the file locally. Nothing else is
 * done in terms of tracking who uploaded it or making it available for
 * download.
 * 
 * @author danielshiplett
 */
@Service
public class UploadServiceImpl implements UploadService, InitializingBean {

    private static final Logger LOG = LoggerFactory
            .getLogger(UploadServiceImpl.class);

    @Inject
    private UploadRepository uploadRepository;

    private String baseLocation = null;

    @Override
    public void afterPropertiesSet() throws Exception {
        /**
         * Our only property must be set.
         */
        if (baseLocation == null) {
            return;
        }

        /**
         * Create the base location and all parent paths.
         */
        this.createBaseLocation();
    }

    /**
     * Get the base location for the file storage.
     * 
     * @return the base location
     */
    public String getBaseLocation() {
        return baseLocation;
    }

    /**
     * Set the base location for the file storage.
     * 
     * @param baseLocation
     *            the base location
     * 
     * @return this
     */
    public UploadServiceImpl withBaseLocation(String baseLocation) {
        this.baseLocation = baseLocation;
        return this;
    }

    /**
     * If the base location doesn't exist on disk, then go ahead and create it
     * creating any parent directories as needed.
     * 
     * @throws IOException
     */
    private synchronized void createBaseLocation() throws IOException {
        createDirectory(baseLocation);
    }

    /**
     * Create the named directory and any parent directories.
     * 
     * @param directoryPath
     *            the directory path to create
     * 
     * @throws IOException
     */
    private synchronized void createDirectory(String directoryPath)
            throws IOException {
        File dir = new File(directoryPath);

        if (!dir.exists()) {
            try {
                dir.mkdirs();
                return;
            } catch (SecurityException e) {
                LOG.error(e.getMessage(), e);
                throw new IOException(e);
            }
        } else {
            if (!dir.isDirectory()) {
                /**
                 * It exists but is a file?
                 */
                throw new IOException(String.format(
                        "File name exists and is not a directory: %s",
                        directoryPath));
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.brewman.upload.service.UploadService#getUpload(java.lang.String)
     */
    @Override
    public Upload getUpload(String flowIdentifier) {
        UUID uuid = UUID.fromString(flowIdentifier);
        return uploadRepository.findOne(uuid.toString());
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.brewman.upload.service.UploadService#saveChunk(java.lang.String,
     * int, int, int, java.lang.String, int, long,
     * org.springframework.web.multipart.MultipartFile)
     */
    @Override
    public Upload saveChunk(String flowIdentifier, int flowChunkNumber,
            int flowChunkSize, int flowCurrentChunkSize, String flowFilename,
            int flowTotalChunks, long flowTotalSize, MultipartFile file)
            throws IOException {
        /*
         * Get the Upload to see if we have already started this upload. We will
         * attach the next chuck if there is one or create a new entry
         * otherwise.
         */
        Upload upload = this.getUpload(flowIdentifier);

        /*
         * Force the creation of the upload record to be handled in the
         * testChunk function.
         */
        if (upload == null) {
            throw new IOException("Upload record does not exist.");
        }

        /*
         * Test that the file hasn't changed since the last chunk test.
         */
        testFlowUnchanged(upload, flowFilename, flowTotalChunks, flowTotalSize);

        /*
         * Check if the chunk has already been uploaded. It seems that this is
         * normal case when you pause/resume. So just return the Upload as it
         * currently stands.
         */
        if (upload.getChunks().get(flowChunkNumber - 1)) {
            LOG.warn("chunk already uploaded: {}", flowChunkNumber - 1);
            return upload;
        }

        /*
         * Add this chunk to the chunk directory and set the chunk list element
         * to true.
         */
        try {
            upload = saveChunkToDisk(upload, flowChunkNumber, file);
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
            throw e;
        }

        /*
         * Check to see if the upload is complete. If all the chunks are in then
         * it is and we should mark it as such.
         */
        if (uploadComplete(upload)) {
            LOG.trace("flow complete");
            upload.setUploadComplete(true);
            upload = uploadRepository.save(upload);
        }

        /*
         * If the upload is complete, then go ahead and process the file.
         */
        if (upload.getUploadComplete() == true) {
            /*
             * TODO: Concatenate all the chunks.
             */
            upload = mergeChunks(upload);

            /*
             * Calculate and set the MD5SUM.
             */
            upload.setMd5sum(calculateMd5Sum(flowIdentifier));

            /*
             * Record the completed time.
             */
            upload.setCompletedAt(new LocalDate());

            upload = uploadRepository.save(upload);

            LOG.trace("upload: {}", upload);
        }

        LOG.debug("upload: {}", upload);

        return upload;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.brewman.upload.service.UploadService#testChunk(java.lang.String,
     * int, int, int, java.lang.String, int, long)
     */
    @Override
    public boolean testChunk(String flowIdentifier, int flowChunkNumber,
            int flowChunkSize, int flowCurrentChunkSize, String flowFilename,
            int flowTotalChunks, long flowTotalSize) throws IOException {
        /*
         * Confirm that the flowIdentifier is an UUID and use it from here on
         * out.
         */
        UUID uuid = UUID.fromString(flowIdentifier);

        /*
         * Get the current UUID to see if we have already started this upload.
         * Will attach the next chuck if there is one or create a new entry
         * otherwise.
         */
        Upload upload = uploadRepository.findOne(uuid.toString());

        /*
         * Check that the total size isn't too large!
         * 
         * Cap it at 4GB for now.
         */
        long maxSize = 4l * 1024l * 1024l * 1024l;
        if (flowTotalSize > maxSize) {
            LOG.error("flowTotalSize: {} -- ", flowTotalSize, maxSize);
            throw new IOException("file to large");
        }

        if (upload == null) {
            /*
             * Entry didn't exist. Check to make sure we haven't already created
             * a file with the same UUID (highly unlikely).
             */
            if (testFileExists(uuid)) {
                throw new FileExistsException("file already exists");
            }

            /*
             * Create the new Upload entry and leave it as incomplete.
             */
            upload = new Upload();

            upload.setId(uuid.toString());
            upload.setOriginalName(flowFilename);
            upload.setUploadedAt(new LocalDate());
            upload.setUploadComplete(false);
            upload.setTotalChunks(flowTotalChunks);
            upload.setTotalSize(flowTotalSize);

            /*
             * Create the chunks list with all false.
             */
            List<Boolean> chunks = new ArrayList<Boolean>();

            for (int i = 0; i < flowTotalChunks; i++) {
                chunks.add(new Boolean(false));
            }

            upload.setChunks(chunks);

            /*
             * Create a directory to store the chunks in.
             */
            createDirectory(getChunkDirectoryPath(upload));

            upload = uploadRepository.save(upload);
        }

        /*
         * Test that the file hasn't changed since the last chunk test.
         */
        testFlowUnchanged(upload, flowFilename, flowTotalChunks, flowTotalSize);

        /*
         * Check if the chunk has already been uploaded.
         */
        if (upload.getChunks().get(flowChunkNumber - 1)) {
            LOG.warn("chunk already uploaded: {}", flowChunkNumber - 1);
            return true;
        }

        /*
         * We're all good. Let them send the chunk.
         */
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.brewman.upload.service.UploadService#getAvailableIdentifier()
     */
    @Override
    public String getAvailableIdentifier() {
        return getAvailableUUID().toString();
    }

    /**
     * Get's the next available (and random) UUID to assign as this flow's
     * unique identifier.
     * 
     * @return
     */
    private UUID getAvailableUUID() {
        UUID uuid = null;

        while (uuid == null) {
            uuid = UUID.randomUUID();
            LOG.debug("trying uuid: {}", uuid);

            if (testFileExists(uuid)) {
                LOG.debug("uuid exists, trying again");
                uuid = null;
            }
        }

        return uuid;
    }

    /**
     * Does this file already exist on the filesystem?
     * 
     * @param uuid
     *            the file UUID (name)
     * 
     * @return
     */
    private boolean testFileExists(UUID uuid) {
        File f = new File(baseLocation + uuid.toString());

        return f.exists();
    }

    /**
     * Check that the flow hasn't changed at all. The first testChunk call gets
     * to set all the flow fields. After that, the owner, file name, total chunk
     * count, and total file size cannot change.
     * 
     * @param upload
     * @param user
     * @param flowFilename
     * @param flowTotalChunks
     * @param flowTotalSize
     * 
     * @throws IOException
     */
    private void testFlowUnchanged(Upload upload, String flowFilename,
            int flowTotalChunks, long flowTotalSize) throws IOException {

        if (!upload.getOriginalName().equals(flowFilename)) {
            throw new IOException("flowFilename change");
        }

        if (upload.getTotalChunks() != flowTotalChunks) {
            throw new IOException("flowTotalChunks change");
        }

        if (upload.getTotalSize() != flowTotalSize) {
            throw new IOException("flowTotalSize change");
        }
    }

    /**
     * Calculate the MD5SUM of the give file.
     * 
     * @param flowIdentifier
     * 
     * @return
     * 
     * @throws IOException
     */
    @Timed
    private String calculateMd5Sum(String flowIdentifier) throws IOException {
        String md5 = null;

        /*
         * Generate an MD5 sum. This should be safe for large files.
         */
        FileInputStream fis = new FileInputStream(getFile(flowIdentifier));
        md5 = DigestUtils.md5Hex(fis);
        fis.close();

        return md5;
    }

    /**
     * Check the chunks list to see if they are all true. If they are, then all
     * the chunks are uploaded.
     * 
     * @param u
     * 
     * @return
     */
    private synchronized boolean uploadComplete(Upload u) {
        for (Boolean b : u.getChunks()) {
            if (b.booleanValue() == false) {
                return false;
            }
        }

        return true;
    }

    /**
     * Write the new chunk to disk and update the Upload record. Don't
     * synchronize here because the file write may take some time. Only
     * synchronize on the update of the Upload.
     * 
     * @param u
     * @param flowChunkNumber
     * @param file
     * 
     * @return
     * 
     * @throws IOException
     */
    private Upload saveChunkToDisk(Upload u, int flowChunkNumber,
            MultipartFile file) throws IOException {
        File f = getChunkFile(u, flowChunkNumber);

        FileOutputStream output = new FileOutputStream(f, true);

        try {
            output.write(file.getBytes());
        } finally {
            output.close();
        }

        return updateUploadForChunk(u, flowChunkNumber);
    }

    /**
     * Force the update of the Upload through here so that we can avoid
     * optimistic update exceptions.
     * 
     * @param u
     * @param flowChunkNumber
     * 
     * @return
     */
    private synchronized Upload updateUploadForChunk(Upload u,
            int flowChunkNumber) {
        u = uploadRepository.findOne(u.getId());
        u.getChunks().set(flowChunkNumber - 1, new Boolean(true));
        u = uploadRepository.save(u);

        return u;
    }

    /**
     * Now that the chunks have all been uploaded, we need to merge them back
     * into the original file.
     * 
     * @param upload
     * 
     * @return
     * 
     * @throws IOException
     */
    @Timed
    private synchronized Upload mergeChunks(Upload upload) throws IOException {
        upload = uploadRepository.findOne(upload.getId());

        FileOutputStream flowFile = new FileOutputStream(getFileLocation(upload
                .getId().toString()));

        for (int i = 1; i <= upload.getTotalChunks(); i++) {
            byte[] chunkBytes;

            try {
                chunkBytes = getChunkBytes(upload, i);
                flowFile.write(chunkBytes);

                File chunkFile = getChunkFile(upload, i);
                chunkFile.delete();
            } catch (IOException e) {
                LOG.error(e.getMessage());
            }
        }

        flowFile.close();

        File chunkDir = new File(getChunkDirectoryPath(upload));
        chunkDir.delete();

        upload = uploadRepository.save(upload);

        return upload;
    }

    /**
     * Get the full path for this flow.
     * 
     * @param flowIdentifier
     * 
     * @return
     */
    private String getFileLocation(String flowIdentifier) {
        return baseLocation + File.separator + flowIdentifier;
    }

    /**
     * Get a File for this flow.
     * 
     * @param flowIdentifier
     * 
     * @return
     */
    private File getFile(String flowIdentifier) {
        return new File(getFileLocation(flowIdentifier));
    }

    /**
     * Get a File for this chunk of the flow.
     * 
     * @param u
     * @param flowChunkNumber
     * 
     * @return
     */
    private File getChunkFile(Upload u, int flowChunkNumber) {
        return new File(getChunkDirectoryPath(u) + flowChunkNumber);
    }

    /**
     * Get the bytes from the chunk of the flow.
     * 
     * @param u
     * @param flowChunkNumber
     * 
     * @return
     * 
     * @throws IOException
     */
    private byte[] getChunkBytes(Upload u, int flowChunkNumber)
            throws IOException {
        byte[] rtn = null;
        FileInputStream fis = null;
        BufferedInputStream bis = null;
        File f = getChunkFile(u, flowChunkNumber);

        if (f.exists()) {
            try {
                fis = new FileInputStream(f);
                bis = new BufferedInputStream(fis);
                rtn = new byte[(int) f.length()];
                bis.read(rtn);
            } catch (IOException e) {
                LOG.error(e.getMessage(), e);
                return null;
            } finally {
                bis.close();
            }
        }

        return rtn;
    }

    /**
     * Given the Upload, get the path that the chunks should be placed in.
     * 
     * @param u
     * 
     * @return
     */
    private String getChunkDirectoryPath(Upload u) {
        return baseLocation + File.separator + u.getId().toString() + ".chunk"
                + File.separator;
    }
}
