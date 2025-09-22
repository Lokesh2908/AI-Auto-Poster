# LinkedIn OAuth Token Information

## Important: LinkedIn Refresh Token Behavior

### Current LinkedIn OAuth 2.0 Behavior
LinkedIn's OAuth 2.0 implementation **does not provide refresh tokens** in the standard authorization flow. This is by design and is normal behavior.

### What LinkedIn Provides
- **Access Token**: Valid for 60 days
- **Token Type**: "Bearer"
- **Expires In**: 5184000 seconds (60 days)
- **Refresh Token**: ❌ **NOT PROVIDED**

### Why No Refresh Token?
LinkedIn requires users to re-authenticate every 60 days for security reasons. This is different from other OAuth providers like Google or Microsoft.

### How Our System Handles This

1. **Token Storage**: We store the access token with its expiration date
2. **Refresh Token Field**: Will be NULL in the database (this is expected)
3. **Token Validation**: We check if the access token is expired before making API calls
4. **Re-authentication**: When the token expires, users must reconnect their LinkedIn account

### Token Lifecycle

```
User Connects LinkedIn → Access Token (60 days) → Token Expires → User Must Reconnect
```

### Implementation Details

#### Database Fields
- `access_token`: The 60-day token from LinkedIn
- `refresh_token`: Will be NULL (LinkedIn doesn't provide this)
- `access_token_expires_at`: 60 days from token creation
- `refresh_token_expires_at`: Set to 1 year (placeholder, not used)

#### Utility Methods Added
- `isAccessTokenExpired()`: Check if access token is expired
- `hasRefreshToken()`: Will always return false for LinkedIn
- `needsReauthorization()`: Returns true when access token is expired

### Handling Expired Tokens

When a LinkedIn token expires:

1. **Detection**: API calls will return 401 Unauthorized
2. **User Notification**: Show message that LinkedIn connection expired
3. **Re-connection**: User clicks "Connect LinkedIn" again
4. **New Token**: Fresh 60-day access token is obtained

### Best Practices

1. **Check Token Before API Calls**: Always verify token validity
2. **Graceful Degradation**: Handle expired tokens gracefully
3. **User Communication**: Clearly explain re-connection requirement
4. **Monitoring**: Log token expiration events for analytics

### Code Example

```java
// Check if LinkedIn connection is valid
LinkedInUser linkedInUser = linkedInUserRepository.findByUser(user);
if (linkedInUser == null || linkedInUser.needsReauthorization()) {
    // Redirect to LinkedIn connection flow
    return "redirect:/linkedin/connect";
}

// Token is valid, proceed with API call
linkedInService.publishPost(post, linkedInUser.getAccessToken(), linkedInUser.getPersonUrn());
```

### Troubleshooting

**Q: Why is refresh_token NULL in the database?**
A: This is expected. LinkedIn doesn't provide refresh tokens.

**Q: How often do users need to reconnect?**
A: Every 60 days when the access token expires.

**Q: Can we get longer-lived tokens?**
A: No, 60 days is LinkedIn's maximum for security reasons.

**Q: What happens if we try to use an expired token?**
A: LinkedIn API returns 401 Unauthorized error.

### Monitoring Token Health

Add these logs to monitor token status:
- Token creation/update events
- Token expiration warnings (e.g., 7 days before expiry)
- Failed API calls due to expired tokens
- Re-authentication events
