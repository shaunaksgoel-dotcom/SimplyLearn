package com.example.simplylearn.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

import static reactor.netty.http.HttpConnectionLiveness.log;

@Service
public class OpenAIService {

    private final WebClient webClient;

    public OpenAIService(@Value("${openai.api.key}") String apiKey) {

        this.webClient = WebClient.builder()
                .baseUrl("https://api.openai.com/v1")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    // ======================
    // TEXT SUMMARY
    // ======================
    public String createSummary(String inputText) {

        String prompt = """
                Summarize the following study materials clearly and concisely.
                Focus on key concepts and important takeaways.

                Materials:
                """ + inputText;

        return runChat(prompt);
    }

    // ======================
    // PODCAST SCRIPT (TWO SPEAKERS)
    // ======================
    public String createPodcastScript(String inputText) {

        String prompt = """
                You are an educational podcast writer.
                
                Create a spoken, TWO-SPEAKER podcast script based on the study material below.
                
                STRICT FORMAT RULES (you MUST follow these exactly):
                - Every spoken line MUST start with either "A:" or "B:"
                - Speakers A and B should alternate naturally, like a real conversation
                - Write in clear, natural spoken English
                - Use normal punctuation (commas, periods, question marks)
                - Each line should be one speaker talking (no long paragraphs)
                - When transitioning to a NEW MAJOR TOPIC or SECTION, insert the token [[SECTION_BREAK]] on its own line
                - Do NOT insert [[SECTION_BREAK]] inside a sentence
                - Do NOT include stage directions (no "intro music", "host", "your name", etc.)
                - Do NOT label sections or topics
                - Do NOT explain that breaks exist
                - Do NOT mention speaker names beyond "A:" and "B:"
                - Do NOT include anything except the spoken dialogue and [[SECTION_BREAK]]
                
                STYLE & TONE:
                - Calm, professional, and engaging
                - Designed for a student listener
                - Smooth explanations, not robotic
                - Sounds like two people teaching together, not reading notes
                
                IMPORTANT:
                - Speaker A and Speaker B should both contribute meaningfully
                - Use short-to-medium length spoken lines (natural speech)
                - Avoid repeating the same idea between speakers unless it adds clarity
                
                Study material:
                
                
                
                """ + inputText;

        return runChat(prompt);
    }

    // ======================
    // SLIDESHOW OUTLINE
    // ======================
    public String createSlideshowOutline(String inputText) {

        String prompt = """
                You are a teacher creating a slideshow for students.
                
                Convert the study material below into a slideshow outline.
                
                STRICT RULES:
                - Each slide must have a short, clear title
                - Bullet points must be short and student-friendly
                - No paragraphs
                - No extra commentary
                - Do not repeat ideas across slides
                
                LENIENT RULES:
                - Have around 4 bullets per slide (strictly between 3 and 5)
                - Create enough slides to cover all key concepts (aim for 5–10 slides, but more or fewer is acceptable)
                - For each slide, also provide a short visual description suitable for an illustration.
                
                REQUIRED FORMAT (VERY IMPORTANT — FOLLOW EXACTLY):
                
                Slide 1: Title here
                - Bullet 1
                - Bullet 2
                - Bullet 3
                - Bullet 4
                Image: short description of an illustration
                
                Slide 2: Title here
                - Bullet 1
                - Bullet 2
                - Bullet 3
                - Bullet 4
                Image: short description of an illustration
                
                Study material:
                
                
                
                """ + inputText;

        return runChat(prompt);
    }

    // ======================
    // 🎬 VIDEO SCRIPT (NEW)
    // ======================
    public String createVideoScript(String inputText) {
    System.out.println("The prompt is created!");
    log.info("MyNameIsShaunak");
        log.error("MyNameIsShaunak");
        String prompt = """
                You are an educational video script writer.
                
                Convert the study material below into a video narration script.
                
                STRICT RULES:
                - One speaker only
                - Write in clear, natural spoken English
                - No stage directions
                - No titles or labels
                - Do NOT mention visuals explicitly
                - Insert [[SCENE_BREAK]] on its own line when the visual scene should change
                - Do NOT insert [[SCENE_BREAK]] inside a sentence
                - Each scene should explain ONE clear idea
                
                STYLE & TONE:
                - Calm, confident, educational
                - Designed for students
                - Smooth flow, not robotic
                - Sounds like a teacher explaining concepts clearly
                
                Study material:
                
                
                
                """ + inputText;

        return runChat(prompt);
    }

    // ======================
    // CORE CHAT METHOD
    // ======================
    private String runChat(String prompt) {

        Map<String, Object> body = Map.of(
                "model", "gpt-4o-mini",
                "messages", List.of(
                        Map.of(
                                "role", "user",
                                "content", prompt
                        )
                ),
                "temperature", 0.7,
                "max_tokens", 1500
        );

        Map response = webClient.post()
                .uri("/chat/completions")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        List choices = (List) response.get("choices");
        Map firstChoice = (Map) choices.get(0);
        Map message = (Map) firstChoice.get("message");

        return message.get("content").toString();
    }
}
