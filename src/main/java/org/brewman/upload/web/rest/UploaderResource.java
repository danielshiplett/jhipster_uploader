package org.brewman.upload.web.rest;

import java.io.IOException;
import java.util.Optional;

import javax.inject.Inject;

import org.brewman.upload.domain.Upload;
import org.brewman.upload.service.UploadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Specifically designed to handle the ng-flow/flow.js upload library.
 * 
 * @author danielshiplett
 */
@RestController
@RequestMapping("/api")
public class UploaderResource {
    private final static Logger LOG = LoggerFactory
            .getLogger(UploaderResource.class);

    @Inject
    private UploadService uploadService;

    /**
     * This is pretty much stock from the ng-flow requirements to handle upload
     * of a single file chunk.
     * 
     * @param flowChunkNumber
     * @param flowChunkSize
     * @param flowCurrentChunkSize
     * @param flowFilename
     * @param flowIdentifier
     * @param flowRelativePath
     * @param flowTotalChunks
     * @param flowTotalSize
     * @param file
     * 
     * @return
     * 
     * @throws Exception
     */
    @RequestMapping(value = "/uploader", method = RequestMethod.POST)
    public ResponseEntity<?> saveChunk(
            @RequestParam("flowChunkNumber") int flowChunkNumber,
            @RequestParam("flowChunkSize") int flowChunkSize,
            @RequestParam("flowCurrentChunkSize") int flowCurrentChunkSize,
            @RequestParam("flowFilename") String flowFilename,
            @RequestParam("flowIdentifier") String flowIdentifier,
            @RequestParam("flowRelativePath") String flowRelativePath,
            @RequestParam("flowTotalChunks") int flowTotalChunks,
            @RequestParam("flowTotalSize") long flowTotalSize,
            @RequestParam("file") MultipartFile file) throws Exception {
        LOG.info("/rest/upload: POST: {} -- {} -- {}", flowIdentifier,
                flowChunkNumber, flowTotalSize);

        LOG.trace("flowChunkNumber: {}", flowChunkNumber);
        LOG.trace("flowChunkSize: {}", flowChunkSize);
        LOG.trace("flowCurrentChunkSize: {}", flowCurrentChunkSize);
        LOG.trace("flowFilename: {}", flowFilename);
        LOG.trace("flowIdentifier: {}", flowIdentifier);
        LOG.trace("flowRelativePath: {}", flowRelativePath);
        LOG.trace("flowTotalChunks: {}", flowTotalChunks);
        LOG.trace("flowTotalSize: {}", flowTotalSize);

        try {
            Upload u = uploadService.saveChunk(flowIdentifier, flowChunkNumber,
                    flowChunkSize, flowCurrentChunkSize, flowFilename,
                    flowTotalChunks, flowTotalSize, file);
            LOG.debug("upload: {}", u);
        } catch (IOException e) {
            LOG.error(e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>(flowIdentifier, HttpStatus.OK);
    }

    /**
     * This is pretty much stock from the ng-flow requirements to handle the
     * test of a single file chunk.
     * 
     * @param flowChunkNumber
     * @param flowChunkSize
     * @param flowCurrentChunkSize
     * @param flowFilename
     * @param flowIdentifier
     * @param flowRelativePath
     * @param flowTotalChunks
     * @param flowTotalSize
     * @param file
     * 
     * @return
     * 
     * @throws Exception
     */
    @RequestMapping(value = "/uploader", method = RequestMethod.GET)
    public ResponseEntity<?> testChunk(
            @RequestParam("flowChunkNumber") int flowChunkNumber,
            @RequestParam("flowChunkSize") int flowChunkSize,
            @RequestParam("flowCurrentChunkSize") int flowCurrentChunkSize,
            @RequestParam("flowFilename") String flowFilename,
            @RequestParam("flowIdentifier") String flowIdentifier,
            @RequestParam("flowRelativePath") String flowRelativePath,
            @RequestParam("flowTotalChunks") int flowTotalChunks,
            @RequestParam("flowTotalSize") long flowTotalSize) throws Exception {
        LOG.trace("flowChunkNumber: {}", flowChunkNumber);
        LOG.trace("flowChunkSize: {}", flowChunkSize);
        LOG.trace("flowCurrentChunkSize: {}", flowCurrentChunkSize);
        LOG.trace("flowFilename: {}", flowFilename);
        LOG.trace("flowIdentifier: {}", flowIdentifier);
        LOG.trace("flowRelativePath: {}", flowRelativePath);
        LOG.trace("flowTotalChunks: {}", flowTotalChunks);
        LOG.trace("flowTotalSize: {}", flowTotalSize);

        boolean b = false;

        try {
            b = uploadService.testChunk(flowIdentifier, flowChunkNumber,
                    flowChunkSize, flowCurrentChunkSize, flowFilename,
                    flowTotalChunks, flowTotalSize);
        } catch (IOException e) {
            LOG.error(e.getMessage());
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        /**
         * According to the docs, we return 200 when the chunk has already been
         * completed, a permanent error when we want to stop the upload, or
         * anything else when we want to allow the chunk to be uploaded.
         * 
         * NOTE that even though the doc says anything else, apparenlty a 202
         * isn't going to work for the false case here. So change it to 502 and
         * it seems to work.
         */
        if (b) {
            return new ResponseEntity<>(b, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(b, HttpStatus.BAD_GATEWAY);
        }
    }

    /**
     * Had to override the default ng-flow unique identifier function with a
     * call to here. We will generate a new identifier for each file. This
     * prevents multiple users from uploading a file of the same name and having
     * a collision.
     * 
     * @param user
     *            the current user
     * 
     * @return a new unique identifier
     */
    @RequestMapping(value = "/uploader/getUniqueIdentifier", method = RequestMethod.GET)
    public String getUniqueIdentifier() {
        return uploadService.getAvailableIdentifier();
    }

    /**
     * Get the specified upload.
     * 
     * @param flowIdentifier
     * 
     * @return
     */
    @RequestMapping(value = "/uploader/upload/{flowIdentifier}", method = RequestMethod.GET)
    public ResponseEntity<?> getUpload(@PathVariable String flowIdentifier) {
        return Optional
                .ofNullable(uploadService.getUpload(flowIdentifier))
                .map(upload -> new ResponseEntity<Upload>(upload, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));
    }
}
