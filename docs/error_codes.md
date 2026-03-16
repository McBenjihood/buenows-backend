# Internal Error Codes List

This document lists the internal error codes used in the `buenws-backend` project, along with their descriptions and where they are used.

| Error Code             | Description | Used In |
|:-----------------------| :--- |:---|
| `DUPLICATE_USER`       | Triggered when attempting to register a user with an email that is already in use. | UserService |
| `INVALID_USER`         | Used when a user cannot be created or when login credentials (email/password) are incorrect. | UserService |
| `GENERATE_TOKEN_ERROR` | Indicates a failure during the JWT token generation process during login. | UserService |
| `INVALID_TOKEN`        | General error for invalid, malformed, or unparseable JWT or refresh tokens. | UserService, TokenService |
| `EXPIRED_TOKEN`        | Specifically indicates that a JWT or refresh token has expired. | TokenService |
| `INVALID_INQUIRY`      | Triggered when an inquiry submission to the database fails. | InquiryService |
| `INVALID_FILE_UPLOAD`  |  Used for general I/O failures during file uploads. | FileService |