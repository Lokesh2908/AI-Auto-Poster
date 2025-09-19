package com.aiautoposter.controller;

import com.aiautoposter.entity.LinkedInUser;
import com.aiautoposter.entity.Post;
import com.aiautoposter.entity.User;
import com.aiautoposter.repository.LinkedInUserRepository;
import com.aiautoposter.service.LinkedInService;
import com.aiautoposter.service.PostService;
import com.aiautoposter.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/linkedin")
public class LinkedInController {

    private static final Logger log = LoggerFactory.getLogger(LinkedInController.class);

    @Autowired
    private LinkedInService linkedInService;

    @Autowired
    private UserService userService;

    @Autowired
    private LinkedInUserRepository linkedInUserRepository;

    @Autowired
    private PostService postService;

    // STEP 1: Get Authorization URL (Now requires JWT authentication)
    @GetMapping("/auth-url")
    //@PreAuthorize("hasRole('USER')") // Requires authenticated user
    public ResponseEntity<Map<String, String>> getAuthUrl(
            HttpServletRequest request,
            Authentication authentication) {

        // Get current user from JWT token
        String username = authentication.getName();
        String authUrl = linkedInService.getAuthorizationUrl(username); // Pass username for state

        return ResponseEntity.ok(Map.of(
                "authUrl", authUrl,
                "message", "Visit this URL in browser to authorize LinkedIn access"
        ));
    }

    // STEP 2: Handle OAuth Callback with JWT user linking
    @GetMapping("/callback")
    public ResponseEntity<String> handleCallback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String error,
            @RequestParam(required = false) String error_description,
            @RequestParam(required = false) String state, // Contains user info
            HttpServletRequest request) {

        // Check for LinkedIn OAuth errors
        if (error != null) {
            return ResponseEntity.badRequest().body(buildErrorResponse(error, error_description));
        }

        if (code == null || code.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(buildMissingCodeResponse());
        }

        try {
            // Extract user ID from state parameter (set during auth URL generation)
            Long userId = linkedInService.extractUserIdFromState(state);

            // Exchange code for tokens
            Map<String, Object> tokenResponse = linkedInService.exchangeCodeForToken(code);
            String accessToken = (String) tokenResponse.get("access_token");
            String refreshToken = (String) tokenResponse.get("refresh_token");
            Integer expiresIn = (Integer) tokenResponse.get("expires_in");

            // Get LinkedIn user profile
            Map<String, Object> linkedInProfile = linkedInService.getUserProfile(accessToken);

            // Save or update LinkedIn user data
            LinkedInUser linkedInUser = saveLinkedInUser(userId, linkedInProfile,
                    accessToken, refreshToken, expiresIn);

            return ResponseEntity.ok(String.format("""
                <html>
                <body>
                    <h2>✅ LinkedIn Connected Successfully!</h2>
                    <p>LinkedIn account <strong>%s</strong> has been linked to your profile.</p>
                    <script>
                        setTimeout(() => window.close(), 3000);
                    </script>
                </body>
                </html>
                """, linkedInProfile.get("name")));

        } catch (Exception e) {
            log.error("LinkedIn OAuth callback error: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(buildProcessingErrorResponse(e.getMessage()));
        }
    }

    // Check LinkedIn connection status
    @GetMapping("/status")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> getLinkedInStatus(Authentication authentication) {
        String username = authentication.getName();
        User currentUser = userService.findByUsername(username); // Your user service

        Optional<LinkedInUser> linkedInUser = linkedInUserRepository.findByUser(currentUser);

        if (linkedInUser.isPresent()) {
            LinkedInUser linkedin = linkedInUser.get();
            boolean isExpired = linkedin.getAccessTokenExpiresAt().isBefore(LocalDateTime.now());

            return ResponseEntity.ok(Map.of(
                    "connected", true,
                    "name", linkedin.getName(),
                    "email", linkedin.getEmail(),
                    "expired", isExpired,
                    "connectedAt", linkedin.getCreatedAt()
            ));
        }

        return ResponseEntity.ok(Map.of("connected", false));
    }

    // Disconnect LinkedIn
    @DeleteMapping("/disconnect")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, String>> disconnectLinkedIn(Authentication authentication) {
        String username = authentication.getName();
        User currentUser = userService.findByUsername(username);

        Optional<LinkedInUser> linkedInUser = linkedInUserRepository.findByUser(currentUser);
        if (linkedInUser.isPresent()) {
            linkedInUserRepository.delete(linkedInUser.get());
            return ResponseEntity.ok(Map.of("message", "LinkedIn account disconnected successfully"));
        }

        return ResponseEntity.badRequest().body(Map.of("error", "No LinkedIn account connected"));
    }

    private LinkedInUser saveLinkedInUser(Long userId, Map<String, Object> profile,
                                          String accessToken, String refreshToken, Integer expiresIn) {

        User user = userService.findById(userId).get(); // Your user service

        // Check if LinkedIn user already exists
        Optional<LinkedInUser> existingLinkedIn = linkedInUserRepository.findByUser(user);

        LinkedInUser linkedInUser = existingLinkedIn.orElse(new LinkedInUser());
        linkedInUser.setUser(user);
        linkedInUser.setLinkedinUserId((String) profile.get("id"));
        linkedInUser.setPersonUrn((String) profile.get("sub")); // OpenID Connect subject
        linkedInUser.setName((String) profile.get("name"));
        linkedInUser.setEmail((String) profile.get("email"));
        linkedInUser.setAccessToken(accessToken);
        linkedInUser.setRefreshToken(refreshToken);

        // Calculate expiration times
        LocalDateTime now = LocalDateTime.now();
        linkedInUser.setAccessTokenExpiresAt(now.plusSeconds(expiresIn != null ? expiresIn : 3600));
        linkedInUser.setRefreshTokenExpiresAt(now.plusYears(1)); // LinkedIn refresh tokens expire in 1 year
        linkedInUser.setUpdatedAt(now);

        if (linkedInUser.getCreatedAt() == null) {
            linkedInUser.setCreatedAt(now);
        }

        return linkedInUserRepository.save(linkedInUser);
    }

    private String buildErrorResponse(String error, String description) {
        return String.format("""
            <html>
            <body>
                <h2>❌ LinkedIn Authentication Failed</h2>
                <div style="background: #ffe6e6; padding: 15px; margin: 15px 0; border-radius: 5px;">
                    <p><strong>Error:</strong> %s</p>
                    <p><strong>Description:</strong> %s</p>
                </div>
                <script>setTimeout(() => window.close(), 5000);</script>
            </body>
            </html>
            """, error, description != null ? description : "No description provided");
    }

    private String buildMissingCodeResponse() {
        return """
            <html>
            <body>
                <h2>❌ Authentication Error</h2>
                <p>No authorization code received from LinkedIn.</p>
                <script>setTimeout(() => window.close(), 5000);</script>
            </body>
            </html>
            """;
    }

    private String buildProcessingErrorResponse(String error) {
        return String.format("""
            <html>
            <body>
                <h2>❌ Authentication Processing Error</h2>
                <div style="background: #ffe6e6; padding: 15px; margin: 15px 0; border-radius: 5px;">
                    <p><strong>Error:</strong> %s</p>
                </div>
                <script>setTimeout(() => window.close(), 5000);</script>
            </body>
            </html>
            """, error);
    }


    // STEP 3: Test posting using your existing service
    @GetMapping("/test-post")
    public ResponseEntity<Map<String, Object>> testPost(HttpServletRequest request) {
        // Get stored credentials from session
        String accessToken = (String) request.getSession().getAttribute("linkedin_access_token");
        String personUrn = (String) request.getSession().getAttribute("linkedin_person_urn");
        String userName = (String) request.getSession().getAttribute("linkedin_user_name");

        if (accessToken == null || personUrn == null) {
            return ResponseEntity.status(401).body(Map.of(
                    "error", "Not authenticated with LinkedIn",
                    "message", "Please visit /api/linkedin/auth-url first"
            ));
        }

        try {
            // Use your existing createTestPost method
            String testContent = String.format(
                    "🚀 Testing LinkedIn API Integration!\n\n" +
                            "This post was automatically created using:\n" +
                            "• Spring Boot\n" +
                            "• LinkedIn API v2\n" +
                            "• OAuth 2.0 Authentication\n\n" +
                            "Posted by: %s\n" +
                            "Person URN: %s\n\n" +
                            "#LinkedInAPI #SpringBoot #AutoPosting #TestPost",
                    userName, personUrn
            );

            String postId = linkedInService.createTestPost(accessToken, personUrn, testContent);

            if (postId != null) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "postId", postId,
                        "message", "Test post created successfully!",
                        "author", userName,
                        "personUrn", personUrn
                ));
            } else {
                return ResponseEntity.status(500).body(Map.of(
                        "success", false,
                        "error", "Failed to create post"
                ));
            }

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }


    // STEP 5: Use your existing publishPost method
    @PostMapping("/publish-post/{postId}")
    public ResponseEntity<Map<String, Object>> publishPost(
            @PathVariable Long postId,
            HttpServletRequest request,
            Authentication authentication) {

        String username = authentication.getName();
        User currentUser = userService.findByUsername(username); // Your user service
        Optional<LinkedInUser> linkedInUser = linkedInUserRepository.findByUser(currentUser);

        String accessToken=null;
        String personUrn=null;

        if(linkedInUser.isPresent())
        {
            LinkedInUser linkedin = linkedInUser.get();
            accessToken = linkedin.getAccessToken();
            personUrn = linkedin.getPersonUrn();
        }

        if (accessToken == null || personUrn == null) {
            return ResponseEntity.status(401).body(Map.of(
                    "error", "Not authenticated with LinkedIn"
            ));
        }

        try {
            // Create a mock Post object (replace with your actual Post entity)
            Post post = postService.findById(postId).get();
            // Use your existing publishPost method
            boolean success = linkedInService.publishPost(post, accessToken, personUrn);

            return ResponseEntity.ok(Map.of(
                    "success", success,
                    "message", success ? "Post published successfully!" : "Failed to publish post",
                    "postId", postId
            ));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }
}
