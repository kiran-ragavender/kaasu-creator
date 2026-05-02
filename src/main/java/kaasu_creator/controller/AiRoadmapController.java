package kaasu_creator.controller;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Controller
public class AiRoadmapController {

    @Value("${openai.api.key:}")
    private String openAiApiKey;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public AiRoadmapController() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(30000);
        this.restTemplate = new RestTemplate(factory);
        this.objectMapper = new ObjectMapper();
    }

    @GetMapping("/ai-roadmap")
    public String showAiRoadmap() {
        return "ai-roadmap";
    }

    @PostMapping("/generate-roadmap")
    public String generateRoadmap(
            @RequestParam BigDecimal monthlyIncome,
            @RequestParam BigDecimal monthlyExpenses,
            @RequestParam(required = false) BigDecimal savingsGoal,
            @RequestParam(required = false) String extraNotes,
            Authentication authentication,
            Model model) {

        if (openAiApiKey == null || openAiApiKey.isBlank()) {
            model.addAttribute("error", "AI service is not configured. Please contact support.");
            return "ai-roadmap";
        }

        if (monthlyIncome.compareTo(BigDecimal.ZERO) <= 0) {
            model.addAttribute("error", "Monthly income must be greater than 0.");
            return "ai-roadmap";
        }
        if (monthlyExpenses.compareTo(BigDecimal.ZERO) < 0) {
            model.addAttribute("error", "Monthly expenses cannot be negative.");
            return "ai-roadmap";
        }

        try {
            Map<String, Object> financialData = new HashMap<>();
            financialData.put("monthlyIncome", monthlyIncome);
            financialData.put("monthlyExpenses", monthlyExpenses);
            financialData.put("savingsGoal", savingsGoal != null ? savingsGoal : "Not specified");
            financialData.put("extraNotes", extraNotes != null && !extraNotes.isBlank() ? extraNotes : "None provided");

            String roadmap = generateAiRoadmap(financialData);

            model.addAttribute("monthlyIncome", monthlyIncome);
            model.addAttribute("monthlyExpenses", monthlyExpenses);
            model.addAttribute("savingsGoal", savingsGoal);
            model.addAttribute("extraNotes", extraNotes);
            model.addAttribute("roadmap", roadmap);
            model.addAttribute("generated", true);

        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().value() == 401) {
                model.addAttribute("error", "AI API key is invalid or unauthorized. Please check your configuration.");
            } else if (e.getStatusCode().value() == 429) {
                model.addAttribute("error", "AI service rate limit reached. Please try again in a moment.");
            } else {
                model.addAttribute("error", "AI service returned an error. Please try again later.");
            }
        } catch (Exception e) {
            model.addAttribute("error", "Could not generate roadmap. Please try again later.");
        }

        return "ai-roadmap";
    }

    private String generateAiRoadmap(Map<String, Object> financialData) throws Exception {
        String systemMessage = "You are Kaasu-chan, a friendly and encouraging finance assistant inside a budgeting app. " +
            "Create short, practical, numbered step-by-step financial roadmaps. " +
            "Be clear, supportive, and realistic. Keep responses under 250 words. " +
            "Do not give legal, tax, or investment advice. Focus on budgeting, saving, and spending control.";

        String userMessage = String.format(
            "Monthly Income: $%s%nMonthly Expenses: $%s%nSavings Goal: %s%nExtra Notes: %s%n%nCreate a personalized financial roadmap for this user.",
            financialData.get("monthlyIncome"),
            financialData.get("monthlyExpenses"),
            financialData.get("savingsGoal"),
            financialData.get("extraNotes")
        );

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "gpt-4o-mini");
        requestBody.put("max_tokens", 500);
        requestBody.put("messages", List.of(
            Map.of("role", "system", "content", systemMessage),
            Map.of("role", "user", "content", userMessage)
        ));

        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        headers.set("Authorization", "Bearer " + openAiApiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> response = restTemplate.exchange(
            "https://api.openai.com/v1/chat/completions",
            HttpMethod.POST,
            entity,
            String.class
        );

        JsonNode root = objectMapper.readTree(response.getBody());
        JsonNode choices = root.path("choices");

        if (choices.isMissingNode() || !choices.isArray() || choices.isEmpty()) {
            return "Kaasu-chan couldn't generate a roadmap right now. Please try again with different inputs.";
        }

        String text = choices.get(0).path("message").path("content").asText("").trim();
        return text.isEmpty()
            ? "Kaasu-chan couldn't generate a roadmap right now. Please try again with different inputs."
            : text;
    }
}
