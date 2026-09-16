package com.aimeetingknowledge.platform.search;

import com.aimeetingknowledge.platform.aiservice.AiServiceClient;
import com.aimeetingknowledge.platform.aiservice.AiServiceException;
import com.aimeetingknowledge.platform.aiservice.dto.SearchAiServiceRequest;
import com.aimeetingknowledge.platform.aiservice.dto.SearchAiServiceResponse;
import com.aimeetingknowledge.platform.aiservice.dto.SearchReferenceDto;
import com.aimeetingknowledge.platform.meeting.Meeting;
import com.aimeetingknowledge.platform.meeting.MeetingRepository;
import com.aimeetingknowledge.platform.search.dto.SearchApiRequest;
import com.aimeetingknowledge.platform.search.dto.SearchApiResponse;
import com.aimeetingknowledge.platform.user.User;
import com.aimeetingknowledge.platform.user.UserNotFoundException;
import com.aimeetingknowledge.platform.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private MeetingRepository meetingRepository;
    @Mock private AiServiceClient aiServiceClient;

    @InjectMocks private SearchService searchService;

    @Test
    void userWithMeetingsDerivesIdsAndCallsAiService() {
        User user = new User("Alice", "alice@example.com", "hash");
        Meeting m1 = new Meeting(user, "Meeting 1", null, Instant.now());
        Meeting m2 = new Meeting(user, "Meeting 2", null, Instant.now());

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(meetingRepository.findAllByUserOrderByMeetingDateDesc(user)).thenReturn(List.of(m1, m2));

        SearchAiServiceResponse aiResponse = new SearchAiServiceResponse(
                "RAG answer",
                List.of(new SearchReferenceDto(1L, 10L, 0, 0.0, 5.0))
        );
        when(aiServiceClient.search(any())).thenReturn(aiResponse);

        SearchApiRequest apiRequest = new SearchApiRequest("What decision was made?", 5);
        SearchApiResponse response = searchService.search(apiRequest, "alice@example.com");

        assertThat(response.answer()).isEqualTo("RAG answer");
        assertThat(response.references()).hasSize(1);

        ArgumentCaptor<SearchAiServiceRequest> captor = ArgumentCaptor.forClass(SearchAiServiceRequest.class);
        verify(aiServiceClient).search(captor.capture());
        SearchAiServiceRequest sentRequest = captor.getValue();
        assertThat(sentRequest.query()).isEqualTo("What decision was made?");
        assertThat(sentRequest.topK()).isEqualTo(5);
        // meeting_ids derived exclusively from owned meetings
        assertThat(sentRequest.meetingIds()).containsExactly(m1.getId(), m2.getId());
    }

    @Test
    void userWithNoMeetingsReturnsCleanResponseWithoutCallingAiService() {
        User user = new User("Bob", "bob@example.com", "hash");

        when(userRepository.findByEmail("bob@example.com")).thenReturn(Optional.of(user));
        when(meetingRepository.findAllByUserOrderByMeetingDateDesc(user)).thenReturn(List.of());

        SearchApiRequest apiRequest = new SearchApiRequest("Any topics?", 5);
        SearchApiResponse response = searchService.search(apiRequest, "bob@example.com");

        assertThat(response.answer()).contains("no meetings");
        assertThat(response.references()).isEmpty();
        verify(aiServiceClient, never()).search(any());
    }

    @Test
    void unknownUserThrowsUserNotFoundException() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        SearchApiRequest apiRequest = new SearchApiRequest("Test", 5);
        assertThatThrownBy(() -> searchService.search(apiRequest, "unknown@example.com"))
                .isInstanceOf(UserNotFoundException.class);
    }
}
