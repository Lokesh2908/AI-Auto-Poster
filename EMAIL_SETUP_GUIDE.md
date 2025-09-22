# Email Configuration Guide

## Gmail Setup for Email Notifications

To enable email notifications, you need to set up Gmail App Passwords:

### Step 1: Enable 2-Factor Authentication
1. Go to your Google Account settings
2. Navigate to Security
3. Enable 2-Step Verification if not already enabled

### Step 2: Generate App Password
1. Go to Google Account > Security > 2-Step Verification
2. Scroll down to "App passwords"
3. Select "Mail" and your device
4. Copy the generated 16-character password

### Step 3: Set Environment Variables
Create a `.env` file or set environment variables:

```bash
# Windows (Command Prompt)
set MAIL_USERNAME=your-email@gmail.com
set MAIL_PASSWORD=your-16-character-app-password
set MAIL_ENABLED=true

# Windows (PowerShell)
$env:MAIL_USERNAME="your-email@gmail.com"
$env:MAIL_PASSWORD="your-16-character-app-password"
$env:MAIL_ENABLED="true"

# Linux/Mac
export MAIL_USERNAME=your-email@gmail.com
export MAIL_PASSWORD=your-16-character-app-password
export MAIL_ENABLED=true
```

### Step 4: Alternative - Update application.yml
You can also directly update the application.yml file:

```yaml
spring:
  mail:
    username: your-email@gmail.com
    password: your-16-character-app-password
    enabled: true
```

### Step 5: Restart Application
Restart your Spring Boot application to apply the changes.

## Disable Email Notifications
If you don't want email notifications, set:
```bash
MAIL_ENABLED=false
```

Or in application.yml:
```yaml
spring:
  mail:
    enabled: false
```

## Troubleshooting
- Make sure 2FA is enabled on your Google account
- Use the 16-character App Password, not your regular Gmail password
- Check that the email address is correct
- Verify firewall settings allow SMTP connections on port 587
