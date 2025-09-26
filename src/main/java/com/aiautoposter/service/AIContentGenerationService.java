package com.aiautoposter.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AIContentGenerationService {
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;

    @Value("${ai.openai.apiKey}")
    private String openaiApiKey;

    @Value("${ai.openai.url}")
    private String openaiApiUrl;

    @Value("${ai.azure.apiKey}")
    private String azureApiKey;

    @Value("${ai.azure.url}")
    private String azureApiUrl;

    @Value("${ai.azure.dalleDeploymentName:dall-e-3}")
    private String dalleDeploymentName;

    @Value("${ai.azure.endpoint}")
    private String azureEndpoint;

    @Value("${ai.azure.apiVersion:2024-04-01-preview}")
    private String apiVersion;

    public String generateLinkedInContent(String sourceDiscussion, String title) {
        // Try OpenAI first
        String content = tryOpenAI(sourceDiscussion, title);
        if (content != null && !content.isEmpty()) {
            return content;
        }
        // Try Azure OpenAI if OpenAI fails
        content = tryAzureOpenAI(sourceDiscussion, title);
        if (content != null && !content.isEmpty()) {
            return content;
        }
        
        // Fallback content if both APIs fail
        return generateFallbackContent(title, sourceDiscussion);
    }
    
    private String tryOpenAI(String sourceDiscussion, String title) {
        try {
            String prompt = String.format(
                "Generate a professional LinkedIn post based on the following discussion and title:\n\n" +
                "Title: %s\n\n" +
                "Discussion: %s\n\n" +
                "Please create engaging content that:\n" +
                "1. Is professional and appropriate for LinkedIn\n" +
                "2. Includes relevant hashtags\n" +
                "3. Encourages engagement\n" +
                "4. Is between 100-300 words\n" +
                "5. Has a clear call-to-action",
                title, sourceDiscussion
            );
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "text-davinci-003");
            requestBody.put("prompt", prompt);
            requestBody.put("max_tokens", 500);
            requestBody.put("temperature", 0.7);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + openaiApiKey);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<Map> response = restTemplate.postForEntity(openaiApiUrl, request, Map.class);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                if (responseBody.containsKey("choices")) {
                    Object[] choices = (Object[]) responseBody.get("choices");
                    if (choices.length > 0) {
                        Map<String, Object> choice = (Map<String, Object>) choices[0];
                        return (String) choice.get("text");
                    }
                }
            }
            return null;
        } catch (Exception e) {
            System.err.println("Error calling OpenAI API: " + e.getMessage());
            return null;
        }
    }

    private String tryAzureOpenAI(String sourceDiscussion, String title) {
        try {
            String systemMessage = "Generate a professional LinkedIn post based on the following discussion and title. " +
                    "Please create engaging content that: " +
                    "1. Is professional and appropriate for LinkedIn " +
                    "2. Includes relevant hashtags " +
                    "3. Encourages engagement " +
                    "4. Is between 100-300 words " +
                    "5. Has a clear call-to-action";

            String userMessage = String.format("Title: %s\n\nDiscussion: %s", title, sourceDiscussion);

            // Create messages array for chat completions
            List<Map<String, String>> messages = new ArrayList<>();

            Map<String, String> systemMsg = new HashMap<>();
            systemMsg.put("role", "system");
            systemMsg.put("content", systemMessage);
            messages.add(systemMsg);

            Map<String, String> userMsg = new HashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", userMessage);
            messages.add(userMsg);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("messages", messages);  // Use messages instead of prompt
            requestBody.put("max_tokens", 500);
            requestBody.put("temperature", 0.7);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("api-key", azureApiKey);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(azureApiUrl, request, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                if (responseBody.containsKey("choices")) {
                    List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
                    if (!choices.isEmpty()) {
                        Map<String, Object> choice = choices.get(0);
                        Map<String, Object> message = (Map<String, Object>) choice.get("message");
                        return (String) message.get("content");
                    }
                }
            }



            return null;

        } catch (Exception e) {
            System.err.println("Error calling Azure OpenAI API: " + e.getMessage());
            return null;
        }
    }


    public String generateImagePrompt(String content) {
        // Try OpenAI first
        String prompt = tryOpenAIImagePrompt(content);
        if (prompt != null && !prompt.isEmpty()) {
            return prompt;
        }
        // Try Azure OpenAI if OpenAI fails
        prompt = tryAzureImagePrompt(content);
        if (prompt != null && !prompt.isEmpty()) {
            return prompt;
        }

        // Fallback prompt if both APIs fail
        return "Professional business image related to: " + content.substring(0, Math.min(100, content.length()));
    }

    private String tryOpenAIImagePrompt(String content) {
        try {
            String prompt = String.format(
                    "Generate a detailed image prompt for a LinkedIn post with the following content:\n\n%s\n\n" +
                            "The image should be professional, engaging, and relevant to the content. " +
                            "Focus on visual elements that would make the post more appealing on LinkedIn.",
                    content
            );

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "text-davinci-003");
            requestBody.put("prompt", prompt);
            requestBody.put("max_tokens", 200);
            requestBody.put("temperature", 0.8);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + openaiApiKey);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<Map> response = restTemplate.postForEntity(openaiApiUrl, request, Map.class);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                if (responseBody.containsKey("choices")) {
                    Object[] choices = (Object[]) responseBody.get("choices");
                    if (choices.length > 0) {
                        Map<String, Object> choice = (Map<String, Object>) choices[0];
                        return (String) choice.get("text");
                    }
                }
            }
            
            return null;
        } catch (Exception e) {
            System.err.println("Error calling OpenAI for image prompt: " + e.getMessage());
            return null;
        }
    }

    private String tryAzureImagePrompt(String content) {
        try {
            String prompt = String.format(
                    "Generate a detailed image prompt for a LinkedIn post with the following content:\n\n%s\n\n" +
                            "The image should be professional, engaging, and relevant to the content. " +
                            "Focus on visual elements that would make the post more appealing on LinkedIn.",
                    content
            );
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("prompt", prompt);
            requestBody.put("max_tokens", 200);
            requestBody.put("temperature", 0.8);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("api-key", azureApiKey);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(azureApiUrl + "/openai/deployments/gpt-35-turbo/completions?api-version=2023-05-15", request, Map.class);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                if (responseBody.containsKey("choices")) {
                    Object[] choices = (Object[]) responseBody.get("choices");
                    if (choices.length > 0) {
                        Map<String, Object> choice = (Map<String, Object>) choices[0];
                        return (String) choice.get("text");
                    }
                }
            }
            return null;
        } catch (Exception e) {
            System.err.println("Error calling Azure OpenAI for image prompt: " + e.getMessage());
            return null;
        }
    }

    private String generateFallbackContent(String title, String sourceDiscussion) {
        return String.format(
                "🚀 %s\n\n" +
                        "Based on our recent discussion: %s\n\n" +
                        "Key insights:\n" +
                        "• Important point 1\n" +
                        "• Important point 2\n" +
                        "• Important point 3\n\n" +
                        "What are your thoughts on this topic? Share your experience in the comments below!\n\n" +
                        "#Business #Innovation #Professional #Networking",
                title,
                sourceDiscussion.length() > 100 ? sourceDiscussion.substring(0, 100) + "..." : sourceDiscussion
        );
    }

    public String tryAzureOpenAIImageGeneration(String prompt, String title) {
        try {
            System.out.println("AIContentGenerationService - Starting Azure OpenAI image generation");
            System.out.println("Azure Endpoint: " + azureEndpoint);
            System.out.println("DALL-E Deployment: " + dalleDeploymentName);
            System.out.println("API Version: " + apiVersion);
            
            // Validate configuration
            if (azureEndpoint == null || azureEndpoint.trim().isEmpty()) {
                throw new RuntimeException("Azure endpoint not configured");
            }
            if (azureApiKey == null || azureApiKey.trim().isEmpty()) {
                throw new RuntimeException("Azure API key not configured");
            }
            if (dalleDeploymentName == null || dalleDeploymentName.trim().isEmpty()) {
                throw new RuntimeException("DALL-E deployment name not configured");
            }
            
            // Create the image generation URL
            String imageApiUrl = String.format("%s/openai/deployments/%s/images/generations?api-version=%s",
                    azureEndpoint, dalleDeploymentName, apiVersion);
            
            System.out.println("Image API URL: " + imageApiUrl);

            // Enhanced prompt for professional content
            String enhancedPrompt = String.format(
                    "Create a professional, high-quality image for: %s. Description: %s. " +
                            "Style: clean, modern, business-appropriate, visually engaging",
                    title, prompt);
            
            System.out.println("Enhanced prompt: " + enhancedPrompt);

            // Create request body for DALL-E API
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("prompt", enhancedPrompt);
            requestBody.put("n", 1);                              // Number of images to generate
            requestBody.put("size", "1024x1024");                // Image size
            requestBody.put("quality", "standard");               // Quality: standard or hd
            requestBody.put("style", "vivid");                    // Style: vivid or natural

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("api-key", azureApiKey);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            System.out.println("Sending request to Azure OpenAI...");
            ResponseEntity<Map> response = restTemplate.postForEntity(imageApiUrl, request, Map.class);

            System.out.println("Response status: " + response.getStatusCode());
            System.out.println("Response body: " + response.getBody());

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                if (responseBody.containsKey("data")) {
                    List<Map<String, Object>> data = (List<Map<String, Object>>) responseBody.get("data");
                    if (!data.isEmpty()) {
                        Map<String, Object> imageData = data.get(0);
                        String imageUrl = (String) imageData.get("url");
                        System.out.println("Image URL received: " + imageUrl);
                        return imageUrl;
                    } else {
                        System.err.println("No image data in response");
                    }
                } else {
                    System.err.println("No 'data' field in response");
                }
            } else {
                System.err.println("Non-OK response or null body");
            }

            return null;

        } catch (Exception e) {
            System.err.println("Error calling Azure OpenAI Image Generation API: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}
