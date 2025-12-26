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

@Service
public class PollyService {

    private static final int MAX_CHARS = 2500;

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
                .region(Region.US_EAST_1) // REQUIRED for generative voices
                .build();
    }
    /**
     * Single-speaker narration (used for VIDEO)
     * One randomized generative voice for the entire narration
     */
    public void synthesizeSingleSpeakerPodcast(String script, Path outputPath) throws Exception {

        // 🎲 Pick ONE voice for the whole video
        List<VoiceId> videoVoices = List.of(
                VoiceId.MATTHEW,
                VoiceId.DANIELLE
        );

        VoiceId chosenVoice =
                videoVoices.get(new Random().nextInt(videoVoices.size()));

        try (OutputStream outputStream = Files.newOutputStream(outputPath)) {

            for (String raw : script.split("\\r?\\n")) {

                String line = raw.trim();
                if (line.isEmpty()) continue;

                if (line.equals("[[SECTION_BREAK]]")) {
                    writePause(outputStream, 900);
                    continue;
                }

                String ssml = buildSsml(line);

                SynthesizeSpeechRequest request = SynthesizeSpeechRequest.builder()
                        .engine(Engine.GENERATIVE)
                        .voiceId(chosenVoice)
                        .outputFormat(OutputFormat.MP3)
                        .textType(TextType.SSML)
                        .text(ssml)
                        .build();

                try (ResponseInputStream<SynthesizeSpeechResponse> audio =
                             polly.synthesizeSpeech(request)) {

                    audio.transferTo(outputStream);
                }

                // Natural pause between lines
                writePause(outputStream, 300);
            }
        }
    }


    @PreDestroy
    public void close() {
        polly.close();
    }

    // =========================================================
    // 🎧 PODCAST (UNCHANGED, TWO SPEAKERS)
    // =========================================================

    /**
     * Called by ConversionService for PODCAST
     */
    public void synthesizePodcastToMp3(String script, Path outputPath) throws Exception {

        List<PodcastLine> lines = parseScript(script);

        // 🎲 Pick TWO voices ONCE per podcast
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

                SynthesizeSpeechRequest request = SynthesizeSpeechRequest.builder()
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

                // Natural conversational pause
                writePause(outputStream, 250);
            }
        }
    }

    // =========================================================
    // 🎬 VIDEO (NEW — ONE SPEAKER, RANDOMIZED ONCE)
    // =========================================================

    /**
     * Called by ConversionService for VIDEO
     * One generative speaker per entire video
     */
    public void synthesizeVideoNarrationToMp3(String script, Path outputPath) throws Exception {

        // 🎲 Pick ONE voice per video
        VoiceId voice = VIDEO_GENERATIVE_VOICES.get(
                random.nextInt(VIDEO_GENERATIVE_VOICES.size())
        );

        List<String> chunks = splitIntoChunks(script);

        try (OutputStream outputStream = Files.newOutputStream(outputPath)) {

            for (String chunk : chunks) {

                String ssml = buildSsml(chunk);

                SynthesizeSpeechRequest request = SynthesizeSpeechRequest.builder()
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

                // Slight pause between narration chunks
                writePause(outputStream, 400);
            }
        }
    }

    // =========================================================
    // SCRIPT PARSING (PODCAST ONLY)
    // =========================================================

    private List<PodcastLine> parseScript(String script) {

        List<PodcastLine> result = new ArrayList<>();

        for (String raw : script.split("\\r?\\n")) {

            String line = raw.trim();
            if (line.isEmpty()) continue;

            if (line.equals("[[SECTION_BREAK]]")) {
                result.add(PodcastLine.sectionBreak());
                continue;
            }

            if (line.startsWith("A:")) {
                result.add(new PodcastLine("A", line.substring(2).trim()));
            } else if (line.startsWith("B:")) {
                result.add(new PodcastLine("B", line.substring(2).trim()));
            }
        }

        return result;
    }

    // =========================================================
    // TEXT CHUNKING (VIDEO)
    // =========================================================

    private List<String> splitIntoChunks(String text) {

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

        SynthesizeSpeechRequest request = SynthesizeSpeechRequest.builder()
                .engine(Engine.GENERATIVE)
                .voiceId(VoiceId.MATTHEW) // safe generative voice
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
