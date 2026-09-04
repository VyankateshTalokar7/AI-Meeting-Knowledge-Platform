package com.aimeetingknowledge.platform.meeting.audio;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AudioStorageServiceTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void generatesSafeStoredFilenameForUnsafeOriginalFilename() throws Exception {
        AudioStorageService service = storageService(100L);
        MockMultipartFile file = new MockMultipartFile(
                "file", "../../../../etc/recording.mp3", "audio/mpeg", new byte[]{1, 2, 3}
        );

        var stored = service.store(file);

        assertThat(stored.originalFilename()).isEqualTo("recording.mp3");
        assertThat(stored.storedFilename()).matches("[0-9a-f-]{36}\\.mp3");
        assertThat(Path.of(stored.storagePath()).normalize()).startsWith(temporaryDirectory.toAbsolutePath().normalize());
        assertThat(Files.exists(Path.of(stored.storagePath()))).isTrue();
    }

    @Test
    void rejectsUnsupportedAudioType() {
        AudioStorageService service = storageService(100L);
        MockMultipartFile file = new MockMultipartFile("file", "notes.txt", "text/plain", new byte[]{1});

        assertThatThrownBy(() -> service.store(file)).isInstanceOf(UnsupportedAudioTypeException.class);
    }

    @Test
    void rejectsOversizedAudio() {
        AudioStorageService service = storageService(3L);
        MockMultipartFile file = new MockMultipartFile("file", "recording.mp3", "audio/mpeg", new byte[]{1, 2, 3, 4});

        assertThatThrownBy(() -> service.store(file)).isInstanceOf(AudioFileTooLargeException.class);
    }

    private AudioStorageService storageService(long maxFileSizeBytes) {
        return new AudioStorageService(new StorageProperties(
                temporaryDirectory.toString(),
                maxFileSizeBytes,
                List.of("audio/mpeg", "audio/wav", "audio/x-wav", "audio/mp4", "audio/webm")
        ));
    }
}
