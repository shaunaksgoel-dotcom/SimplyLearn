package com.example.simplylearn.service;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class OpenAIService {
    private final WebClient webClient;

    public OpenAIService(@Value("${openai.api.key}") String apiKey) {
        this

                .webClient = WebClient.builder().baseUrl("https://api.openai.com/v1").defaultHeader("Authorization", new String[] { "Bearer " + apiKey }).defaultHeader("Content-Type", new String[] { "application/json" }).build();
    }

    public String createSummary(String inputText) {
        String prompt = "Summarize the following study materials clearly and concisely.\nFocus on key concepts and important takeaways.\n\nMaterials:\n" + inputText;
        return runChat(prompt);
    }

    public String createPodcastScript(String inputText) {
        String prompt = "You are an educational podcast writer.\n\nCreate a spoken, TWO-SPEAKER podcast script based on the study material below.\n\nSTRICT FORMAT RULES (you MUST follow these exactly):\n- Every spoken line MUST start with either \"A:\" or \"B:\"\n- Speakers A and B should alternate naturally, like a real conversation\n- Write in clear, natural spoken English\n- Use normal punctuation (commas, periods, question marks)\n- Each line should be one speaker talking (no long paragraphs)\n- When transitioning to a NEW MAJOR TOPIC or SECTION, insert the token [[SECTION_BREAK]] on its own line\n- Do NOT insert [[SECTION_BREAK]] inside a sentence\n- Do NOT include stage directions (no \"intro music\", \"host\", \"your name\", etc.)\n- Do NOT label sections or topics\n- Do NOT explain that breaks exist\n- Do NOT mention speaker names beyond \"A:\" and \"B:\"\n- Do NOT include anything except the spoken dialogue and [[SECTION_BREAK]]\n\nSTYLE & TONE:\n- Calm, professional, and engaging\n- Designed for a student listener\n- Smooth explanations, not robotic\n- Sounds like two people teaching together, not reading notes\n\nIMPORTANT:\n- Speaker A and Speaker B should both contribute meaningfully\n- Use short-to-medium length spoken lines (natural speech)\n- Avoid repeating the same idea between speakers unless it adds clarity\n\nStudy material:\n\n\n\n" + inputText;
        return runChat(prompt);
    }

    public String createSlideshowOutline(String inputText) {
        String prompt = "You are a teacher creating a slideshow for students.\n\nConvert the study material below into a slideshow outline.\n\nSTRICT RULES:\n- Each slide must have a short, clear title\n- Bullet points must be short and student-friendly\n- No paragraphs\n- No extra commentary\n- Do not repeat ideas across slides\n\nLENIENT RULES:\n- Have around 4 bullets per slide (strictly between 3 and 5)\n- Create enough slides to cover all key concepts (aim for 5slides, but more or fewer is acceptable)\n- For each slide, also provide a short visual description suitable for an illustration.\n\nREQUIRED FORMAT (VERY IMPORTANT FOLLOW EXACTLY):\n\nSlide 1: Title here\n- Bullet 1\n- Bullet 2\n- Bullet 3\n- Bullet 4\nImage: short description of an illustration\n\nSlide 2: Title here\n- Bullet 1\n- Bullet 2\n- Bullet 3\n- Bullet 4\nImage: short description of an illustration\n\nStudy material:\n\n\n\n" + inputText;
        return runChat(prompt);
    }
    public String createVideoOutline(String inputText){
        String prompt = """
                You are a professional educational video scriptwriter and storyboard creator.
                
                Take the study material provided below and create a clear, engaging video concept for students.
                
                Generate a JSON array named "scenes". Each element must have:
                {
                "sceneNumber: integer (starting from 1),
                "script": "a few sentences of narration text that clearly explains the concept in this segment. Use clear and engaging language for students."
                "illustrationIdea": "A short, vivid, detailed description of the visual or image that best represents this scene. MAKE SURE IMAGE IS KID FRIENDLY, and not too complex. Avoid prompts that will give weird and wonky images. Stick to simply, kid friendly descriptions. Make sure there are no faces in the image or people. Make sure images are simple."
                }
                Rules:
                - Make between 5 and 15 scenes, depending on the length of the material.
                - Use concise, student‑friendly language.
                - Do not include introductions or closing statements—stick to the content.
                - Use JSON only. No commentary, no markdown, no extra text.
                
                Example output:
                {
                  "scenes": [
                    {
                      "sceneNumber": 1,
                      "script": "[Script text for scene 1]",
                      "illustrationIdea": "[Illustration idea for scene 1]"
                    },
                    {
                      "sceneNumber": 2,
                      "script": "[Script text for scene 2]",
                      "illustrationIdea": "[Illustration idea for scene 2]"
                    },
                    {
                      "sceneNumber": 3,
                      "script": "[Script text for scene 3]",
                      "illustrationIdea": "[Illustration idea for scene 3]"
                    }
                  ]
                } 
                Study material:
                """ + inputText;
        return runChat(prompt);
    }
    public String createQuiz(String inputText){
        String prompt = """
You are a helpful AI that creates educational quizzes.

Generate multiple-choice quiz strictly based on the study material below.
Amount of questions should depend on the length of the material, but should always be between 5 and 20 questions.
MUST BE IN JSON FORMAT!!!
Each question must:
- Have the exact JSON keys: "question", "A", "B", "C", "D", and "correctAnswer".
- "correctAnswer" must contain the correct choice letter only ("A", "B", "C", or "D").
- Contain no explanations, chat text, or commentary — only pure JSON.
- JSON must be valid, well-formatted, and start with { and end with }.

Expected output JSON structure:
{
  "quiz": [
    {
      "question": "Question text",
      "A": "Choice A text",
      "B": "Choice B text",
      "C": "Choice C text",
      "D": "Choice D text",
      "correctAnswer": "C"
    },
    ...
  ]
}

Study material:
""" + inputText;        return runChat(prompt);
    }

    private String runChat(String prompt) {
        Map<String, Object> body = Map.of("model", "gpt-4o-mini", "messages",

                List.of(
                        Map.of("role", "user", "content", prompt)), "temperature",

                Double.valueOf(0.7D), "max_tokens",
                Integer.valueOf(1500));
        Map response = (Map)((WebClient.RequestBodySpec)this.webClient.post().uri("/chat/completions", new Object[0])).bodyValue(body).retrieve().bodyToMono(Map.class).block();
        List<Map> choices = (List)response.get("choices");
        Map firstChoice = choices.get(0);
        Map message = (Map)firstChoice.get("message");
        return message.get("content").toString();
    }
}

