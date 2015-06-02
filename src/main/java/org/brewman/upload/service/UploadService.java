package org.brewman.upload.service;

import java.io.IOException;

import org.brewman.upload.domain.Upload;
import org.springframework.web.multipart.MultipartFile;

/**
 * Interface for all file upload operations.
 * 
 * @author danielshiplett
 */
public interface UploadService {

    /**
     * Get the Upload object for the specified flowIdentifier.
     * 
     * @param flowIdentifier
     *            the unique flow identifier for the uploaded object.
     * 
     * @return
     */
    Upload getUpload(String flowIdentifier);

    /**
     * Save a single chunk to a file. The file doesn't not already have to
     * exist.
     * 
     * This function pretty much follows the flow-ng/flow.js required API so we
     * can pretty much pass this straight through from the REST layer.
     * 
     * @param flowIdentifier
     *            the unique identifier of the file
     * @param flowChunkNumber
     *            from flow-ng
     * @param flowChunkSize
     *            from flow-ng
     * @param flowCurrentChunkSize
     *            from flow-ng
     * @param flowFilename
     *            from flow-ng
     * @param flowTotalChunks
     *            from flow-ng
     * @param flowTotalSize
     *            from flow-ng
     * @param file
     *            the Multipart HTTP chunk (ie. the data)
     * 
     * @return The Upload object for this upload. It will already be persisted
     *         in the DB.
     * 
     * @throws IOException
     */
    Upload saveChunk(String flowIdentifier, int flowChunkNumber,
            int flowChunkSize, int flowCurrentChunkSize, String flowFilename,
            int flowTotalChunks, long flowTotalSize, MultipartFile file)
            throws IOException;

    /**
     * Save a single chunk to a file. The file doesn't not already have to
     * exist.
     * 
     * This function pretty much follows the flow-ng/flow.js required API so we
     * can pretty much pass this straight through from the REST layer.
     * 
     * @param flowIdentifier
     *            the unique identifier of the file
     * @param flowChunkNumber
     *            from flow-ng
     * @param flowChunkSize
     *            from flow-ng
     * @param flowCurrentChunkSize
     *            from flow-ng
     * @param flowFilename
     *            from flow-ng
     * @param flowTotalChunks
     *            from flow-ng
     * @param flowTotalSize
     *            from flow-ng
     * 
     * @return true if the chunk already exists or false if it does not and we
     *         want to allow it.
     * 
     * @throws IOException
     *             if we don't want to allow the file upload
     */
    boolean testChunk(String flowIdentifier, int flowChunkNumber,
            int flowChunkSize, int flowCurrentChunkSize, String flowFilename,
            int flowTotalChunks, long flowTotalSize) throws IOException;

    /**
     * Get another available upload identifier. Needed by flow-ng to get a
     * unique identifier for the upload. We will also use it as the filename on
     * disk. This will also check that the identifier is truly unique.
     * 
     * @return
     */
    String getAvailableIdentifier();
}
