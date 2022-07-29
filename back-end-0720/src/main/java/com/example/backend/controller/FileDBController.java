package com.example.backend.controller;

import com.example.backend.message.ResponseFileReview;
import com.example.backend.message.ResponseMessage;
import com.example.backend.model.FileDB;
import com.example.backend.paging.ReviewCriteria;
import com.example.backend.service.FileDBService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@CrossOrigin("http://localhost:8080")
@RequestMapping("/api")
public class FileDBController {

    @Autowired
    private FileDBService fileDBService;

    Logger logger = LoggerFactory.getLogger(this.getClass());

    @PostMapping("/upload")
    public ResponseEntity<ResponseMessage> uploadFile(
            @RequestParam("id") Long id,
            @RequestParam("title") String title,
            @RequestParam("content") String content,
            @RequestParam("writer") String writer,
            @RequestParam("starNum") long starNum,
            @RequestParam("file") MultipartFile file
    ) {
        String message = "";

        logger.info("id {} : ", id );
        logger.info("title {} : ", title);
        logger.info("content {} : ", content);
        logger.info("file {} : ", file);
        logger.info("writer {} : ", writer);
        logger.info("starNum {} : ", starNum);

        try {
            fileDBService.store(id, title, content, writer,starNum, file);

            message = "Uploaded the file successfully: " + file.getOriginalFilename();
            return ResponseEntity.status(HttpStatus.OK).body(new ResponseMessage(message));
        } catch (Exception e) {
            message = "Could not upload the file: " + file.getOriginalFilename() + "!";
            return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body(new ResponseMessage(message));
        }
    }

    @GetMapping("/files")
    public ResponseEntity<List<ResponseFileReview>> getListFiles(ReviewCriteria criteria) {
        logger.info("criteria title{} : ", criteria.getSearchTitle());
        logger.info("criteria page{} : ", criteria.getPage());
        logger.info("criteria size{} : ", criteria.getSize());

        List<ResponseFileReview> files = fileDBService.findByTitleContaining(criteria).map(dbFile -> {
            String fileDownloadUri = ServletUriComponentsBuilder
                    .fromCurrentContextPath()
                    .path("/api/files/")
                    .path(String.valueOf(dbFile.getReviewId()))
                    .toUriString();
            String fileDownloadUri2 = ServletUriComponentsBuilder
                    .fromCurrentContextPath()
                    .path("/api/files2/")
                    .path(String.valueOf(dbFile.getReviewId()))
                    .toUriString();

            return new ResponseFileReview(
                    dbFile.getId(),
                    dbFile.getReviewId(),
                    dbFile.getName(),
                    dbFile.getTitle(),
                    dbFile.getContent(),
                    dbFile.getWriter(),
                    dbFile.getStarNum(),
                    dbFile.getInsertTime(),
                    criteria.getPage(),
                    criteria.getTotalItems(),
                    criteria.getTotalPages(),
                    fileDownloadUri,
                    fileDownloadUri2,
                    dbFile.getType(),
                    dbFile.getData().length);
        }).collect(Collectors.toList());

        return ResponseEntity.status(HttpStatus.OK).body(files);
    }

    @GetMapping("/reviews")
    public ResponseEntity<List<ResponseFileReview>> getReviews(ReviewCriteria criteria) {

        List<ResponseFileReview> files = fileDBService.findByProduct(criteria).map(dbFile -> {
            String fileDownloadUri = ServletUriComponentsBuilder
                    .fromCurrentContextPath()
                    .path("/api/files/")
                    .path(String.valueOf(dbFile.getReviewId()))
                    .toUriString();
            String fileDownloadUri2 = ServletUriComponentsBuilder
                    .fromCurrentContextPath()
                    .path("/api/files2/")
                    .path(String.valueOf(dbFile.getReviewId()))
                    .toUriString();
        logger.info("review {}", criteria);
            return new ResponseFileReview(
                    dbFile.getId(),
                    dbFile.getReviewId(),
                    dbFile.getName(),
                    dbFile.getTitle(),
                    dbFile.getContent(),
                    dbFile.getWriter(),
                    dbFile.getStarNum(),
                    dbFile.getInsertTime(),
                    criteria.getPage(),
                    criteria.getTotalItems(),
                    criteria.getTotalPages(),
                    fileDownloadUri,
                    fileDownloadUri2,
                    dbFile.getType(),
                    dbFile.getData().length);
        }).collect(Collectors.toList());

        return ResponseEntity.status(HttpStatus.OK).body(files);
    }

    @GetMapping("/files/{id}")
    public ResponseEntity<byte[]> getFile(@PathVariable long id) {
        FileDB fileDB = fileDBService.getFile(id);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileDB.getName() + "\"")
                .body(fileDB.getData());
    }
    @GetMapping("/files2/{id}")
    public ResponseEntity<byte[]> getFile2(@PathVariable long id) {
        FileDB fileDB = fileDBService.getFile(id);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileDB.getName() + "\"")
                .body(fileDB.getData2());
    }

    @PutMapping("/files/deletion/{id}")
    public ResponseEntity<HttpStatus> deleteBoard(
            @PathVariable("id") Long reviewId
    ){
            logger.info("id", reviewId);
        try {
            fileDBService.deleteById(reviewId);
            return new ResponseEntity<HttpStatus>(HttpStatus.OK);
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            return new ResponseEntity<HttpStatus>(HttpStatus.BAD_REQUEST);
        }
    }
}
