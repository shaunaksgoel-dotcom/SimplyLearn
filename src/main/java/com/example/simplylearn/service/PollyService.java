package com.example.simplylearn.service;

import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.polly.PollyClient;
import software.amazon.awssdk.services.polly.model.*;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static reactor.netty.http.HttpConnectionLiveness.log;

@Service
public class PollyService {

    // 🔐 SAFE limit for generative Polly (keep buffer)
    private static final int MAX_CHARS = 1200;

    // ======================
    // 🎙️ PODCAST VOICES (2 speakers)
    // ======================
    private static final List<VoiceId> PODCAST_GENERATIVE_VOICES = List.of(
            VoiceId.MATTHEW,
            VoiceId.RUTH,
            VoiceId.JOANNA
    );

    // ======================
    // 🎬 VIDEO VOICES (1 speaker per video)
    // ======================
    private static final List<VoiceId> VIDEO_GENERATIVE_VOICES = List.of(
            VoiceId.MATTHEW,
            VoiceId.DANIELLE
    );

    private final Random random = new Random();
    private final PollyClient polly;

    public PollyService() {
        this.polly = PollyClient.builder()
                .region(Region.US_EAST_1)
                .build();
    }

    // =========================================================
    // 🎬 VIDEO — SINGLE SPEAKER, CHUNKED (FIXED)
    // =========================================================
    public void synthesizeSingleSpeakerPodcast(
            String script,
            Path outputPath
    ) throws Exception {

        // 🎲 Pick ONE voice for entire video
        VoiceId voice = VIDEO_GENERATIVE_VOICES.get(
                random.nextInt(VIDEO_GENERATIVE_VOICES.size())
        );

        try (OutputStream outputStream = Files.newOutputStream(outputPath)) {

            for (String raw : script.split("\\r?\\n")) {

                String line = raw.trim();
                if (line.isEmpty()) continue;

                if (line.equals("[[SECTION_BREAK]]")) {
                    writePause(outputStream, 900);
                    continue;
                }

                // ✅ CHUNK LONG LINES SAFELY
                for (String chunk : splitIntoChunks(line)) {

                    String ssml = buildSsml(chunk);

                    SynthesizeSpeechRequest request =
                            SynthesizeSpeechRequest.builder()
                                    .engine(Engine.GENERATIVE)
                                    .voiceId(voice)
                                    .outputFormat(OutputFormat.MP3)
                                    .textType(TextType.SSML)
                                    .text(ssml)
                                    .build();

                    try (ResponseInputStream<SynthesizeSpeechResponse> audio =
                                 polly.synthesizeSpeech(request)) {

                        audio.transferTo(outputStream);
                    }

                    writePause(outputStream, 300);
                }
            }
        }
    }

    @PreDestroy
    public void close() {
        polly.close();
    }

    // =========================================================
    // 🎧 PODCAST (UNCHANGED)
    // =========================================================
    public void synthesizePodcastToMp3(String script, Path outputPath) throws Exception {

        List<PodcastLine> lines = parseScript(script);

        List<VoiceId> shuffled = new ArrayList<>(PODCAST_GENERATIVE_VOICES);
        Collections.shuffle(shuffled);

        VoiceId speakerA = shuffled.get(0);
        VoiceId speakerB = shuffled.get(1);

        try (OutputStream outputStream = Files.newOutputStream(outputPath)) {

            for (PodcastLine line : lines) {

                if (line.isSectionBreak()) {
                    writePause(outputStream, 800);
                    continue;
                }

                VoiceId voice =
                        line.speaker().equals("A") ? speakerA : speakerB;

                String ssml = buildSsml(line.text());

                SynthesizeSpeechRequest request =
                        SynthesizeSpeechRequest.builder()
                                .engine(Engine.GENERATIVE)
                                .voiceId(voice)
                                .outputFormat(OutputFormat.MP3)
                                .textType(TextType.SSML)
                                .text(ssml)
                                .build();

                try (ResponseInputStream<SynthesizeSpeechResponse> audio =
                             polly.synthesizeSpeech(request)) {

                    audio.transferTo(outputStream);
                }

                writePause(outputStream, 250);
            }
        }
    }

    // =========================================================
    // PODCAST SCRIPT PARSER
    // =========================================================
    private List<PodcastLine> parseScript(String script) {

        List<PodcastLine> result = new ArrayList<>();

        for (String raw : script.split("\\r?\\n")) {

            String line = raw.trim();
            if (line.isEmpty()) continue;

            if (line.equals("[[SECTION_BREAK]]")) {
                result.add(PodcastLine.sectionBreak());
            } else if (line.startsWith("A:")) {
                result.add(new PodcastLine("A", line.substring(2).trim()));
            } else if (line.startsWith("B:")) {
                result.add(new PodcastLine("B", line.substring(2).trim()));
            }
        }

        return result;
    }

    // =========================================================
    // ✂️ CHUNKING (CRITICAL)
    // =========================================================
    private List<String> splitIntoChunks(String text) {
System.out.println("Splitting text into chunks!");
log.info("lol");
log.error("lol");
        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String sentence : text.split("(?<=[.!?])\\s+")) {

            if (current.length() + sentence.length() > MAX_CHARS) {
                chunks.add(current.toString().trim());
                current.setLength(0);
            }

            current.append(sentence).append(" ");
        }

        if (!current.isEmpty()) {
            chunks.add(current.toString().trim());
        }

        return chunks;
    }

    // =========================================================
    // SSML
    // =========================================================
    private String buildSsml(String text) {
        return """
                <speak>
                    <prosody rate="medium">
                        %s
                    </prosody>
                </speak>
                """.formatted(escapeXml(text));
    }

    private String escapeXml(String text) {
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    // =========================================================
    // PAUSES
    // =========================================================
    private void writePause(OutputStream outputStream, int millis) throws Exception {

        String ssml = "<speak><break time=\"" + millis + "ms\"/></speak>";

        SynthesizeSpeechRequest request =
                SynthesizeSpeechRequest.builder()
                        .engine(Engine.GENERATIVE)
                        .voiceId(VoiceId.MATTHEW)
                        .outputFormat(OutputFormat.MP3)
                        .textType(TextType.SSML)
                        .text(ssml)
                        .build();

        try (ResponseInputStream<SynthesizeSpeechResponse> audio =
                     polly.synthesizeSpeech(request)) {

            audio.transferTo(outputStream);
        }
    }

    // =========================================================
    // INTERNAL MODEL
    // =========================================================
    private record PodcastLine(String speaker, String text) {
        static PodcastLine sectionBreak() {
            return new PodcastLine(null, null);
        }
        boolean isSectionBreak() {
            return speaker == null;
        }
    }
}
