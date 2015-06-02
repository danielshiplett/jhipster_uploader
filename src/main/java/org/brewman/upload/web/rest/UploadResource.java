package org.brewman.upload.web.rest;

import com.codahale.metrics.annotation.Timed;
import org.brewman.upload.domain.Upload;
import org.brewman.upload.repository.UploadRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

/**
 * REST controller for managing Upload.
 */
@RestController
@RequestMapping("/api")
public class UploadResource {

    private final Logger log = LoggerFactory.getLogger(UploadResource.class);

    @Inject
    private UploadRepository uploadRepository;

    /**
     * POST  /uploads -> Create a new upload.
     */
    @RequestMapping(value = "/uploads",
            method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<Void> create(@RequestBody Upload upload) throws URISyntaxException {
        log.debug("REST request to save Upload : {}", upload);
        if (upload.getId() != null) {
            return ResponseEntity.badRequest().header("Failure", "A new upload cannot already have an ID").build();
        }
        uploadRepository.save(upload);
        return ResponseEntity.created(new URI("/api/uploads/" + upload.getId())).build();
    }

    /**
     * PUT  /uploads -> Updates an existing upload.
     */
    @RequestMapping(value = "/uploads",
        method = RequestMethod.PUT,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<Void> update(@RequestBody Upload upload) throws URISyntaxException {
        log.debug("REST request to update Upload : {}", upload);
        if (upload.getId() == null) {
            return create(upload);
        }
        uploadRepository.save(upload);
        return ResponseEntity.ok().build();
    }

    /**
     * GET  /uploads -> get all the uploads.
     */
    @RequestMapping(value = "/uploads",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public List<Upload> getAll() {
        log.debug("REST request to get all Uploads");
        return uploadRepository.findAll();
    }

    /**
     * GET  /uploads/:id -> get the "id" upload.
     */
    @RequestMapping(value = "/uploads/{id}",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<Upload> get(@PathVariable String id) {
        log.debug("REST request to get Upload : {}", id);
        return Optional.ofNullable(uploadRepository.findOne(id))
            .map(upload -> new ResponseEntity<>(
                upload,
                HttpStatus.OK))
            .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    /**
     * DELETE  /uploads/:id -> delete the "id" upload.
     */
    @RequestMapping(value = "/uploads/{id}",
            method = RequestMethod.DELETE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public void delete(@PathVariable String id) {
        log.debug("REST request to delete Upload : {}", id);
        uploadRepository.delete(id);
    }
}
