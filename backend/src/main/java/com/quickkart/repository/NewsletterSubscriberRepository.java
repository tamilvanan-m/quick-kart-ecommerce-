package com.quickkart.repository;

import com.quickkart.entity.NewsletterSubscriber;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NewsletterSubscriberRepository extends JpaRepository<NewsletterSubscriber, Long> {
    boolean existsByEmailIgnoreCase(String email);
}
