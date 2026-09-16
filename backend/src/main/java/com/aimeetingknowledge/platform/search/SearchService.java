package com.aimeetingknowledge.platform.search;

import com.aimeetingknowledge.platform.aiservice.AiServiceClient;
import com.aimeetingknowledge.platform.aiservice.dto.SearchAiServiceRequest;
import com.aimeetingknowledge.platform.aiservice.dto.SearchAiServiceResponse;
import com.aimeetingknowledge.platform.meeting.Meeting;
import com.aimeetingknowledge.platform.meeting.MeetingRepository;
import com.aimeetingknowledge.platform.search.dto.SearchApiRequest;
import com.aimeetingknowledge.platform.search.dto.SearchApiResponse;
import com.aimeetingknowledge.platform.user.User;
import com.aimeetingknowledge.platform.user.UserNotFoundException;
import com.aimeetingknowledge.platform.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SearchService {

    private final UserRepository userRepository;
    private final MeetingRepository meetingRepository;
    private final AiServiceClient aiServiceClient;

    public SearchService(
            UserRepository userRepository,
            MeetingRepository meetingRepository,
            AiServiceClient aiServiceClient
    ) {
        this.userRepository = userRepository;
        this.meetingRepository = meetingRepository;
        this.aiServiceClient = aiServiceClient;
    }

    @Transactional(readOnly = true)
    public SearchApiResponse search(SearchApiRequest request, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(UserNotFoundException::new);

        List<Long> ownedMeetingIds = meetingRepository.findAllByUserOrderByMeetingDateDesc(user).stream()
                .map(Meeting::getId)
                .toList();

        if (ownedMeetingIds.isEmpty()) {
            return new SearchApiResponse("No meeting knowledge is available because you have no meetings.", List.of());
        }

        int effectiveTopK = (request.topK() != null) ? request.topK() : 5;

        SearchAiServiceRequest aiRequest = new SearchAiServiceRequest(
                request.query(),
                ownedMeetingIds,
                effectiveTopK
        );

        SearchAiServiceResponse aiResponse = aiServiceClient.search(aiRequest);

        return new SearchApiResponse(
                aiResponse.answer(),
                aiResponse.references() != null ? aiResponse.references() : List.of()
        );
    }
}
