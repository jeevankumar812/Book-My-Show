package com.cfs.BMS.service;


import com.cfs.BMS.entity.Movie;
import com.cfs.BMS.exception.NotFoundException;
import com.cfs.BMS.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MovieService {

    private final MovieRepository movieRepository;

    @Transactional
    public Movie addMovie(Movie movie) {
        return movieRepository.save(movie);
    }

    /** Back-compat alias for old typo. */
    @Transactional
    public Movie addMove(Movie movie) {
        return addMovie(movie);
    }

    public Page<Movie> getAllMovies(Pageable pageable) {
        return movieRepository.findAll(pageable);
    }

    public Movie getMovieById(Long id) {
        return movieRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Movie not found with id: " + id));
    }

    public Page<Movie> searchByTitle(String title, Pageable pageable) {
        return movieRepository.findByTitleContainingIgnoreCase(title, pageable);
    }

    public Page<Movie> getByGenre(String genre, Pageable pageable) {
        return movieRepository.findByGenreIgnoreCase(genre, pageable);
    }

    public Page<Movie> getByLanguage(String language, Pageable pageable) {
        return movieRepository.findByLanguageIgnoreCase(language, pageable);
    }
}
