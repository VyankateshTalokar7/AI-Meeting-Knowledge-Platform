package com.aimeetingknowledge.platform.aiservice;

import com.aimeetingknowledge.platform.aiservice.dto.MeetingAnalysisResponse;
import com.aimeetingknowledge.platform.aiservice.dto.TranscriptionResponse;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AiServiceClientTest {

    @TempDir
    Path tempDir;

    private MockRestServiceServer mockServer;
    private RestClientAiServiceClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost:8000");
        mockServer = MockRestServiceServer.bindTo(builder).build();
        client = new RestClientAiServiceClient(builder.build());
    }

    @Test
    void successfulTranscriptionCallParsesResponse() throws IOException {
        Path audioFile = tempDir.resolve("sample.wav");
        Files.write(audioFile, new byte[]{1, 2, 3});

        String mockJsonResponse = """
                {
                  "filename": "sample.wav",
                  "text": "Hello world",
                  "language": "en",
                  "segments": [
                    {"id": 0, "start": 0.0, "end": 1.5, "text": "Hello world"}
                  ]
                }
                """;

        mockServer.expect(requestTo("http://localhost:8000/transcribe"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.CONTENT_TYPE, Matchers.startsWith(MediaType.MULTIPART_FORM_DATA_VALUE)))
                .andExpect(MockRestRequestMatchers.content().string(Matchers.containsString("name=\"file\"")))
                .andRespond(withSuccess(mockJsonResponse, MediaType.APPLICATION_JSON));

        TranscriptionResponse response = client.transcribeAudio(audioFile.toString());

        assertThat(response.text()).isEqualTo("Hello world");
        assertThat(response.language()).isEqualTo("en");
        assertThat(response.segments()).hasSize(1);
        mockServer.verify();
    }

    @Test
    void throwsAiServiceExceptionWhenServerReturnsError() throws IOException {
        Path audioFile = tempDir.resolve("sample.wav");
        Files.write(audioFile, new byte[]{1, 2, 3});

        mockServer.expect(requestTo("http://localhost:8000/transcribe"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withServerError());

        assertThatThrownBy(() -> client.transcribeAudio(audioFile.toString()))
                .isInstanceOf(AiServiceException.class)
                .hasMessageContaining("Failed to call AI transcription service");
        mockServer.verify();
    }

    @Test
    void throwsAiServiceExceptionWhenFileDoesNotExist() {
        assertThatThrownBy(() -> client.transcribeAudio(tempDir.resolve("non-existent.wav").toString()))
                .isInstanceOf(AiServiceException.class)
                .hasMessageContaining("Audio file not found");
    }

    @Test
    void successfulAnalyzeCallParsesResponse() {
        String mockJsonResponse = """
                {
                  "summary": "Meeting executive summary.",
                  "key_topics": ["Topic 1", "Topic 2"],
                  "decisions": ["Decision 1"],
                  "action_items": [
                    {
                      "task": "Do something",
                      "assignee": "Alice",
                      "due_date": "2026-09-30"
                    }
                  ]
                }
                """;

        mockServer.expect(requestTo("http://localhost:8000/analyze"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.CONTENT_TYPE, Matchers.startsWith(MediaType.APPLICATION_JSON_VALUE)))
                .andRespond(withSuccess(mockJsonResponse, MediaType.APPLICATION_JSON));

        MeetingAnalysisResponse response = client.analyzeMeeting("Meeting transcript text.");

        assertThat(response.summary()).isEqualTo("Meeting executive summary.");
        assertThat(response.keyTopics()).containsExactly("Topic 1", "Topic 2");
        assertThat(response.decisions()).containsExactly("Decision 1");
        assertThat(response.actionItems()).hasSize(1);
        assertThat(response.actionItems().get(0).task()).isEqualTo("Do something");
        assertThat(response.actionItems().get(0).assignee()).isEqualTo("Alice");
        assertThat(response.actionItems().get(0).dueDate()).isEqualTo("2026-09-30");
        mockServer.verify();
    }

    @Test
    void throwsAiServiceExceptionWhenAnalyzeFails() {
        mockServer.expect(requestTo("http://localhost:8000/analyze"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withServerError());

        assertThatThrownBy(() -> client.analyzeMeeting("Transcript text."))
                .isInstanceOf(AiServiceException.class)
                .hasMessageContaining("Failed to call AI meeting analysis service");
        mockServer.verify();
    }
}
