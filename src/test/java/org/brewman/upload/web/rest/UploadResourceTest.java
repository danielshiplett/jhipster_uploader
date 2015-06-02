package org.brewman.upload.web.rest;

import org.brewman.upload.Application;
import org.brewman.upload.domain.Upload;
import org.brewman.upload.repository.UploadRepository;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.hamcrest.Matchers.hasItem;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import org.joda.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test class for the UploadResource REST controller.
 *
 * @see UploadResource
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
@WebAppConfiguration
@IntegrationTest
public class UploadResourceTest {

    private static final String DEFAULT_ORIGINAL_NAME = "SAMPLE_TEXT";
    private static final String UPDATED_ORIGINAL_NAME = "UPDATED_TEXT";

    private static final LocalDate DEFAULT_UPLOADED_AT = new LocalDate(0L);
    private static final LocalDate UPDATED_UPLOADED_AT = new LocalDate();
    private static final String DEFAULT_MD5SUM = "SAMPLE_TEXT";
    private static final String UPDATED_MD5SUM = "UPDATED_TEXT";

    private static final Boolean DEFAULT_UPLOAD_COMPLETE = false;
    private static final Boolean UPDATED_UPLOAD_COMPLETE = true;

    private static final Integer DEFAULT_TOTAL_CHUNKS = 0;
    private static final Integer UPDATED_TOTAL_CHUNKS = 1;

    private static final Long DEFAULT_TOTAL_SIZE = 0L;
    private static final Long UPDATED_TOTAL_SIZE = 1L;

    private static final LocalDate DEFAULT_COMPLETED_AT = new LocalDate(0L);
    private static final LocalDate UPDATED_COMPLETED_AT = new LocalDate();

    @Inject
    private UploadRepository uploadRepository;

    private MockMvc restUploadMockMvc;

    private Upload upload;

    @PostConstruct
    public void setup() {
        MockitoAnnotations.initMocks(this);
        UploadResource uploadResource = new UploadResource();
        ReflectionTestUtils.setField(uploadResource, "uploadRepository", uploadRepository);
        this.restUploadMockMvc = MockMvcBuilders.standaloneSetup(uploadResource).build();
    }

    @Before
    public void initTest() {
        uploadRepository.deleteAll();
        upload = new Upload();
        upload.setOriginalName(DEFAULT_ORIGINAL_NAME);
        upload.setUploadedAt(DEFAULT_UPLOADED_AT);
        upload.setMd5sum(DEFAULT_MD5SUM);
        upload.setUploadComplete(DEFAULT_UPLOAD_COMPLETE);
        upload.setTotalChunks(DEFAULT_TOTAL_CHUNKS);
        upload.setTotalSize(DEFAULT_TOTAL_SIZE);
        upload.setCompletedAt(DEFAULT_COMPLETED_AT);
    }

    @Test
    public void createUpload() throws Exception {
        int databaseSizeBeforeCreate = uploadRepository.findAll().size();

        // Create the Upload
        restUploadMockMvc.perform(post("/api/uploads")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(upload)))
                .andExpect(status().isCreated());

        // Validate the Upload in the database
        List<Upload> uploads = uploadRepository.findAll();
        assertThat(uploads).hasSize(databaseSizeBeforeCreate + 1);
        Upload testUpload = uploads.get(uploads.size() - 1);
        assertThat(testUpload.getOriginalName()).isEqualTo(DEFAULT_ORIGINAL_NAME);
        assertThat(testUpload.getUploadedAt()).isEqualTo(DEFAULT_UPLOADED_AT);
        assertThat(testUpload.getMd5sum()).isEqualTo(DEFAULT_MD5SUM);
        assertThat(testUpload.getUploadComplete()).isEqualTo(DEFAULT_UPLOAD_COMPLETE);
        assertThat(testUpload.getTotalChunks()).isEqualTo(DEFAULT_TOTAL_CHUNKS);
        assertThat(testUpload.getTotalSize()).isEqualTo(DEFAULT_TOTAL_SIZE);
        assertThat(testUpload.getCompletedAt()).isEqualTo(DEFAULT_COMPLETED_AT);
    }

    @Test
    public void getAllUploads() throws Exception {
        // Initialize the database
        uploadRepository.save(upload);

        // Get all the uploads
        restUploadMockMvc.perform(get("/api/uploads"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.[*].id").value(hasItem(upload.getId())))
                .andExpect(jsonPath("$.[*].originalName").value(hasItem(DEFAULT_ORIGINAL_NAME.toString())))
                .andExpect(jsonPath("$.[*].uploadedAt").value(hasItem(DEFAULT_UPLOADED_AT.toString())))
                .andExpect(jsonPath("$.[*].md5sum").value(hasItem(DEFAULT_MD5SUM.toString())))
                .andExpect(jsonPath("$.[*].uploadComplete").value(hasItem(DEFAULT_UPLOAD_COMPLETE.booleanValue())))
                .andExpect(jsonPath("$.[*].totalChunks").value(hasItem(DEFAULT_TOTAL_CHUNKS)))
                .andExpect(jsonPath("$.[*].totalSize").value(hasItem(DEFAULT_TOTAL_SIZE.intValue())))
                .andExpect(jsonPath("$.[*].completedAt").value(hasItem(DEFAULT_COMPLETED_AT.toString())));
    }

    @Test
    public void getUpload() throws Exception {
        // Initialize the database
        uploadRepository.save(upload);

        // Get the upload
        restUploadMockMvc.perform(get("/api/uploads/{id}", upload.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value(upload.getId()))
            .andExpect(jsonPath("$.originalName").value(DEFAULT_ORIGINAL_NAME.toString()))
            .andExpect(jsonPath("$.uploadedAt").value(DEFAULT_UPLOADED_AT.toString()))
            .andExpect(jsonPath("$.md5sum").value(DEFAULT_MD5SUM.toString()))
            .andExpect(jsonPath("$.uploadComplete").value(DEFAULT_UPLOAD_COMPLETE.booleanValue()))
            .andExpect(jsonPath("$.totalChunks").value(DEFAULT_TOTAL_CHUNKS))
            .andExpect(jsonPath("$.totalSize").value(DEFAULT_TOTAL_SIZE.intValue()))
            .andExpect(jsonPath("$.completedAt").value(DEFAULT_COMPLETED_AT.toString()));
    }

    @Test
    public void getNonExistingUpload() throws Exception {
        // Get the upload
        restUploadMockMvc.perform(get("/api/uploads/{id}", Long.MAX_VALUE))
                .andExpect(status().isNotFound());
    }

    @Test
    public void updateUpload() throws Exception {
        // Initialize the database
        uploadRepository.save(upload);

		int databaseSizeBeforeUpdate = uploadRepository.findAll().size();

        // Update the upload
        upload.setOriginalName(UPDATED_ORIGINAL_NAME);
        upload.setUploadedAt(UPDATED_UPLOADED_AT);
        upload.setMd5sum(UPDATED_MD5SUM);
        upload.setUploadComplete(UPDATED_UPLOAD_COMPLETE);
        upload.setTotalChunks(UPDATED_TOTAL_CHUNKS);
        upload.setTotalSize(UPDATED_TOTAL_SIZE);
        upload.setCompletedAt(UPDATED_COMPLETED_AT);
        restUploadMockMvc.perform(put("/api/uploads")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(upload)))
                .andExpect(status().isOk());

        // Validate the Upload in the database
        List<Upload> uploads = uploadRepository.findAll();
        assertThat(uploads).hasSize(databaseSizeBeforeUpdate);
        Upload testUpload = uploads.get(uploads.size() - 1);
        assertThat(testUpload.getOriginalName()).isEqualTo(UPDATED_ORIGINAL_NAME);
        assertThat(testUpload.getUploadedAt()).isEqualTo(UPDATED_UPLOADED_AT);
        assertThat(testUpload.getMd5sum()).isEqualTo(UPDATED_MD5SUM);
        assertThat(testUpload.getUploadComplete()).isEqualTo(UPDATED_UPLOAD_COMPLETE);
        assertThat(testUpload.getTotalChunks()).isEqualTo(UPDATED_TOTAL_CHUNKS);
        assertThat(testUpload.getTotalSize()).isEqualTo(UPDATED_TOTAL_SIZE);
        assertThat(testUpload.getCompletedAt()).isEqualTo(UPDATED_COMPLETED_AT);
    }

    @Test
    public void deleteUpload() throws Exception {
        // Initialize the database
        uploadRepository.save(upload);

		int databaseSizeBeforeDelete = uploadRepository.findAll().size();

        // Get the upload
        restUploadMockMvc.perform(delete("/api/uploads/{id}", upload.getId())
                .accept(TestUtil.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk());

        // Validate the database is empty
        List<Upload> uploads = uploadRepository.findAll();
        assertThat(uploads).hasSize(databaseSizeBeforeDelete - 1);
    }
}
