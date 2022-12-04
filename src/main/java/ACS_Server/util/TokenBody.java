package ACS_Server.util;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

public class TokenBody {

    public UUID getTokenID() {
        return tokenID;
    }

    public void setTokenID(UUID tokenID) {
        this.tokenID = tokenID;
    }

    public String getTokenBody() {
        return tokenBody;
    }

    public void setTokenBody(String tokenBody) {
        this.tokenBody = tokenBody;
    }

    public Date getDateCrea() {
        return dateCrea;
    }

    public void setDateCrea(Date dateCrea) {
        this.dateCrea = dateCrea;
    }

    public Date getDateExpi() {
        return dateExpi;
    }

    public void setDateExpi(Date dateExpi) {
        this.dateExpi = dateExpi;
    }

    protected UUID tokenID;
    protected String tokenBody;
    protected Date dateCrea;
    protected Date dateExpi;

    public TokenBody(Date dateCrea, Date dateExpi) {
        this.dateCrea = dateCrea;
        this.dateExpi = dateExpi;
        tokenID = GenerateUID();
        tokenBody = generateSafeToken();
    }
    public TokenBody()
    {

    }
    private static UUID GenerateUID()
    {
        UUID idOne = UUID.randomUUID();
        return idOne;
    }

    private static String generateSafeToken() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[40];
        random.nextBytes(bytes);
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        return encoder.encodeToString(bytes);
    }
}


