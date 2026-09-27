package com.cfs.BMS.repository;

import com.cfs.BMS.entity.Movie;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovieRepository extends JpaRepository<Movie, Long> {

  Page<Movie> findByGenreIgnoreCase(String genre, Pageable pageable);
  Page<Movie> findByLanguageIgnoreCase(String language, Pageable pageable);
  Page<Movie> findByTitleContainingIgnoreCase(String title, Pageable pageable);

}
