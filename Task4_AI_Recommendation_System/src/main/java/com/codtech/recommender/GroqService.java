package com.codtech.recommender;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Component
public class GroqService {

    @Value("${groq.api.key}")
    private String apiKey;

    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL = "llama-3.3-70b-versatile";

    public String getExplanation(String userSkills, String jobTitle, String requiredSkills) throws Exception {
        String prompt = "A fresher developer has these skill ratings out of 5: " + userSkills +
                ". They have been recommended for the role: " + jobTitle +
                " which requires: " + requiredSkills +
                ". In 3-4 sentences explain why this job suits them and what skills they should improve. Be specific and encouraging.";

        String requestBody = "{\n" +
                "  \"model\": \"" + MODEL + "\",\n" +
                "  \"messages\": [\n" +
                "    {\n" +
                "      \"role\": \"user\",\n" +
                "      \"content\": \"" + prompt.replace("\"", "'") + "\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"max_tokens\": 200\n" +
                "}";

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GROQ_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        String responseBody = response.body();

      if (response.statusCode() != 200) {
            return "API Error: " + responseBody;
        }

        int contentStart = responseBody.indexOf("\"content\":\"") + 11;
        if (contentStart <= 11) {
            return "Parse error: " + responseBody.substring(0, Math.min(200, responseBody.length()));
        }

        StringBuilder result = new StringBuilder();
        int i = contentStart;
        while (i < responseBody.length()) {
            char c = responseBody.charAt(i);
            if (c == '\\' && i + 1 < responseBody.length()) {
                char next = responseBody.charAt(i + 1);
                if (next == 'n') { result.append(" "); i += 2; continue; }
                if (next == '"') { result.append("\""); i += 2; continue; }
                if (next == '\\') { result.append("\\"); i += 2; continue; }
            }
            if (c == '"') break;
            result.append(c);
            i++;
        }
        return result.toString();
    }
}