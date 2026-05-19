package com.transfer.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /** Clave secreta HS256 (mínimo 256 bits = 32 chars). Sobreescribir en producción con env var. */
    private String secret = "transfer-service-super-secret-key-local-dev-only-32chars!!";

    /** Expiración del access token en milisegundos (default: 1 hora) */
    private long expirationMs = 3_600_000L;

    /** Expiración del refresh token en milisegundos (default: 7 días) */
    private long refreshExpirationMs = 604_800_000L;

    public String getSecret()                     { return secret; }
    public void setSecret(String secret)          { this.secret = secret; }
    public long getExpirationMs()                 { return expirationMs; }
    public void setExpirationMs(long ms)          { this.expirationMs = ms; }
    public long getRefreshExpirationMs()          { return refreshExpirationMs; }
    public void setRefreshExpirationMs(long ms)   { this.refreshExpirationMs = ms; }
}
