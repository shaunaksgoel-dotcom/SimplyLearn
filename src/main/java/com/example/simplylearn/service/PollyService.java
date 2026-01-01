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
            VoiceId.DANIELLE,
            VoiceId.JOANNA
    );


    private final Random random = new Random();
    private final PollyClient polly;

    public PollyService() {
        this.polly = PollyClient.builder()
                .region(Region.US_EAST_1) // REQUIRED for generative voices
                .build();
    }

    @PreDestroy
    public void close() {
        polly.close();
    }

    public void synthesizePodcastToMp3(String script, Path outputPath) throws Exception {
        List<PodcastLine> lines = parseScript(script);
        List<VoiceId> shuffled = new ArrayList<>(PODCAST_GENERATIVE_VOICES);
        Collections.shuffle(shuffled);
        VoiceId speakerA = shuffled.get(0);
        VoiceId speakerB = shuffled.get(1);
        OutputStream outputStream = Files.newOutputStream(outputPath, new java.nio.file.OpenOption[0]);
        try {
            for (PodcastLine line : lines) {
                if (line.isSectionBreak()) {
                    writePause(outputStream, 800);
                    continue;
                }
                VoiceId voice = line.speaker().equals("A") ? speakerA : speakerB;
                String ssml = buildSsml(line.text());
                SynthesizeSpeechRequest request = (SynthesizeSpeechRequest)SynthesizeSpeechRequest.builder().engine(Engine.GENERATIVE).voiceId(voice).outputFormat(OutputFormat.MP3).textType(TextType.SSML).text(ssml).build();
                ResponseInputStream<SynthesizeSpeechResponse> audio = this.polly.synthesizeSpeech(request);
                try {
                    audio.transferTo(outputStream);
                    if (audio != null)
                        audio.close();
                } catch (Throwable throwable) {
                    if (audio != null)
                        try {
                            audio.close();
                        } catch (Throwable throwable1) {
                            throwable.addSuppressed(throwable1);
                        }
                    throw throwable;
                }
                writePause(outputStream, 250);
            }
            if (outputStream != null)
                outputStream.close();
        } catch (Throwable throwable) {
            if (outputStream != null)
                try {
                    outputStream.close();
                } catch (Throwable throwable1) {
                    throwable.addSuppressed(throwable1);
                }
            throw throwable;
        }
    }

    private List<PodcastLine> parseScript(String script) {
        List<PodcastLine> result = new ArrayList<>();
        for (String raw : script.split("\\r?\\n")) {
            String line = raw.trim();
            if (!line.isEmpty())
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

    private String buildSsml(String text) {
        return "<speak>\n    <prosody rate=\"medium\">\n        %s\n    </prosody>\n</speak>\n"

                .formatted(new Object[] { escapeXml(text) });
    }

    private String escapeXml(String text) {
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private void writePause(OutputStream outputStream, int millis) throws Exception {
        String ssml = "<speak><break time=\"" + millis + "ms\"/></speak>";
        SynthesizeSpeechRequest request = (SynthesizeSpeechRequest)SynthesizeSpeechRequest.builder().engine(Engine.GENERATIVE).voiceId(VoiceId.JOANNA).outputFormat(OutputFormat.MP3).textType(TextType.SSML).text(ssml).build();
        ResponseInputStream<SynthesizeSpeechResponse> audio = this.polly.synthesizeSpeech(request);
        try {
            audio.transferTo(outputStream);
            if (audio != null)
                audio.close();
        } catch (Throwable throwable) {
            if (audio != null)
                try {
                    audio.close();
                } catch (Throwable throwable1) {
                    throwable.addSuppressed(throwable1);
                }
            throw throwable;
        }
    }



    private record PodcastLine(String speaker, String text) {

        static PodcastLine sectionBreak() {
            return new PodcastLine(null, null);
        }

        boolean isSectionBreak() {
            return speaker == null;
        }
    }
}
