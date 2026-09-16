package com.aimeetingknowledge.platform.aiservice;

import com.aimeetingknowledge.platform.aiservice.dto.IndexTranscriptRequest;
import com.aimeetingknowledge.platform.aiservice.dto.IndexTranscriptResponse;
import com.aimeetingknowledge.platform.aiservice.dto.MeetingAnalysisRequest;
import com.aimeetingknowledge.platform.aiservice.dto.MeetingAnalysisResponse;
import com.aimeetingknowledge.platform.aiservice.dto.TranscriptionResponse;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.File;

@Component
public class RestClientAiServiceClient implements AiServiceClient {

    private final RestClient restClient;

    public RestClientAiServiceClient(AiServiceProperties properties, RestClient.Builder restClientBuilder) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.connectTimeoutMs());
        requestFactory.setReadTimeout(properties.readTimeoutMs());

        this.restClient = restClientBuilder
                .baseUrl(properties.url())
                .requestFactory(requestFactory)
                .build();
    }

    public RestClientAiServiceClient(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public TranscriptionResponse transcribeAudio(String filePath) {
        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            throw new AiServiceException("Audio file not found for transcription.");
        }

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new FileSystemResource(file));

        try {
            TranscriptionResponse response = restClient.post()
                    .uri("/transcribe")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(TranscriptionResponse.class);

            if (response == null) {
                throw new AiServiceException("Received empty response from AI service.");
            }
            return response;
        } catch (RestClientException exc) {
            throw new AiServiceException("Failed to call AI transcription service.", exc);
        }
    }

    @Override
    public MeetingAnalysisResponse analyzeMeeting(String text) {
        try {
            MeetingAnalysisResponse response = restClient.post()
                    .uri("/analyze")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new MeetingAnalysisRequest(text))
                    .retrieve()
                    .body(MeetingAnalysisResponse.class);

            if (response == null) {
                throw new AiServiceException("Received empty response from AI analysis service.");
            }
            return response;
        } catch (RestClientException exc) {
            throw new AiServiceException("Failed to call AI meeting analysis service.", exc);
        }
    }

    @Override
    public IndexTranscriptResponse indexTranscript(IndexTranscriptRequest request) {
        try {
            IndexTranscriptResponse response = restClient.post()
                    .uri("/index-transcript")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(IndexTranscriptResponse.class);

            if (response == null) {
                throw new AiServiceException("Received empty response from AI transcript indexing service.");
            }
            return response;
        } catch (RestClientException exc) {
            throw new AiServiceException("Failed to call AI transcript indexing service.", exc);
        }
    }
}
