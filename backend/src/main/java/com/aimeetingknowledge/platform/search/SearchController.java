package com.aimeetingknowledge.platform.search;

import com.aimeetingknowledge.platform.search.dto.SearchApiRequest;
import com.aimeetingknowledge.platform.search.dto.SearchApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @PostMapping
    public SearchApiResponse search(
            @Valid @RequestBody SearchApiRequest request,
            Authentication authentication
    ) {
        return searchService.search(request, authentication.getName());
    }
}
