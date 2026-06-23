package com.aivle.backend.service;

import com.aivle.backend.entity.Book;
import com.aivle.backend.exception.BookNotFoundException;
import com.aivle.backend.repository.BookRepository;
import com.aivle.backend.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookService {

    private final BookRepository bookRepository;
    private final ReviewRepository reviewRepository;
    private final S3ImageService s3ImageService;

    public List<Book> getBooks() {
        return bookRepository.findAll();
    }

    public Book getBook(Long id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new BookNotFoundException(id));
    }

    @Transactional
    public Book createBook(Book book, String username) {
        book.setCreatedBy(username);

        if (book.getCoverImageUrl() != null) {
            String s3Url = s3ImageService.uploadBase64Image(book.getCoverImageUrl());
            book.setCoverImageUrl(s3Url);
        }

        return bookRepository.save(book);
    }

    @Transactional
    public Book updateBook(Long id, Book newData, String username) {
        Book book = getBook(id);

        checkOwner(book, username);

        if (newData.getTitle() != null) {
            book.setTitle(newData.getTitle());
        }

        if (newData.getAuthor() != null) {
            book.setAuthor(newData.getAuthor());
        }

        if (newData.getContent() != null) {
            book.setContent(newData.getContent());
        }

        if (newData.getCoverImageUrl() != null) {
            String s3Url = s3ImageService.uploadBase64Image(newData.getCoverImageUrl());
            book.setCoverImageUrl(s3Url);
        }

        if (newData.getTags() != null) {
            book.setTags(newData.getTags());
        }

        book.setUpdatedAt(LocalDateTime.now());

        return bookRepository.save(book);
    }

    @Transactional
    public void deleteBook(Long id, String username) {
        Book book = getBook(id);

        checkOwner(book, username);

        reviewRepository.deleteByBookId(id);
        bookRepository.deleteById(id);
    }

    @Transactional
    public Book updateCover(Long id, String url, String username) {
        Book book = getBook(id);

        checkOwner(book, username);

        String s3Url = s3ImageService.uploadBase64Image(url);
        book.setCoverImageUrl(s3Url);

        book.setUpdatedAt(LocalDateTime.now());

        return bookRepository.save(book);
    }

    @Transactional
    public Book likeBook(Long id, Integer likes) {
        Book book = getBook(id);

        if (likes != null) {
            book.setLikes(likes);
        } else {
            book.setLikes(book.getLikes() + 1);
        }

        return bookRepository.save(book);
    }

    public List<Book> getMyBooks(String username) {
        return bookRepository.findByCreatedBy(username);
    }

    private void checkOwner(Book book, String username) {
        if (book.getCreatedBy() == null || !book.getCreatedBy().equals(username)) {
            throw new AccessDeniedException("권한이 없습니다.");
        }
    }
}