package org.brewman.upload.repository;

import org.brewman.upload.domain.Upload;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Spring Data MongoDB repository for the Upload entity.
 */
public interface UploadRepository extends MongoRepository<Upload,String> {

}
