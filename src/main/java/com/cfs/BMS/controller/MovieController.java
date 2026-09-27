package com.cfs.BMS.controller;


import com.cfs.BMS.entity.Movie;
import com.cfs.BMS.service.MovieService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/movies", "/api/v1/movies"})
@RequiredArgsConstructor
@Tag(name = "Movie Management", description = "Endpoints for browsing and searching movies")
public class MovieController {

    private final MovieService movieService;

    @GetMapping
    @Operation(summary = "Get all movies (paginated)")
    public ResponseEntity<Page<Movie>> getAllMovies(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(movieService.getAllMovies(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get movie by ID")
    public ResponseEntity<Movie> getMovieById(@PathVariable Long id) {
        return ResponseEntity.ok(movieService.getMovieById(id));
    }

    @GetMapping("/search")
    @Operation(summary = "Search movies by title")
    public ResponseEntity<Page<Movie>> searchMovies(@RequestParam String title,
                                                   @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(movieService.searchByTitle(title, pageable));
    }

    // FIX: was duplicated @GetMapping("/genre/{...}") twice -> ambiguous mapping, app failed startup.
    @GetMapping("/genre/{genre}")
    @Operation(summary = "Filter movies by genre")
    public ResponseEntity<Page<Movie>> getByGenre(@PathVariable String genre,
                                                 @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(movieService.getByGenre(genre, pageable));
    }

    @GetMapping("/language/{language}")
    @Operation(summary = "Filter movies by language")
    public ResponseEntity<Page<Movie>> getByLanguage(@PathVariable String language,
                                                    @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(movieService.getByLanguage(language, pageable));
    }
}
