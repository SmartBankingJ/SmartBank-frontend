package ma.bankdirect.api;

import com.google.gson.*;
import java.net.*;
import java.net.http.*;
import java.time.Duration;
import java.util.Map;
import ma.bankdirect.model.Customer;
import ma.bankdirect.model.User;

/** Desktop API client. Never connects directly to SQL and never persists passwords or tokens. */
public final class AuthApi {
    public static final AuthApi INSTANCE=new AuthApi();
    public record Result(String token,User user,Customer customer,boolean mustChangePassword) {}
    public static final class Failure extends RuntimeException {
        private final int status;
        private final String code;
        public Failure(int status,String code,String message){super(message);this.status=status;this.code=code;}
        public int status(){return status;}
        public String code(){return code;}
    }
    private final HttpClient http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(4)).build();
    private final Gson gson=new Gson();
    private final String base;
    private AuthApi() {
        String environment=System.getenv("SMARTBANK_API_URL");
        base=System.getProperty("smartbank.api.url",environment==null?"http://127.0.0.1:8081":environment).replaceAll("/+$","");
        URI uri=URI.create(base);
        boolean local="127.0.0.1".equals(uri.getHost()) || "localhost".equals(uri.getHost()) || "::1".equals(uri.getHost());
        if(!("https".equals(uri.getScheme()) || ("http".equals(uri.getScheme()) && local)) || uri.getUserInfo()!=null)
            throw new IllegalArgumentException("An HTTPS API is required outside localhost");
    }
    public Result login(String username,String password){return result(send("POST","/auth/login",null,Map.of("username",username,"password",password)));}
    public Result changePassword(String token,String current,String replacement){return result(send("POST","/auth/change-password",token,Map.of("currentPassword",current,"newPassword",replacement)));}
    public void forgot(String username){send("POST","/auth/forgot-password",null,Map.of("username",username));}
    public void reset(String code,String replacement){send("POST","/auth/reset-password",null,Map.of("code",code,"newPassword",replacement));}
    public void me(String token){send("GET","/auth/me",token,null);}
    public void logout(String token){if(token!=null)send("POST","/auth/logout",token,null);}

    private Result result(JsonObject response) {
        JsonObject identity=response.getAsJsonObject("identity"), u=identity.getAsJsonObject("user");
        User user=new User();user.setId(u.get("id").getAsLong());user.setUsername(u.get("username").getAsString());
        user.setRole(u.get("role").getAsString());user.setStatus("ACTIVE");
        Customer customer=null;
        if(identity.has("customer") && !identity.get("customer").isJsonNull()) {
            JsonObject c=identity.getAsJsonObject("customer");customer=new Customer();
            customer.setId(c.get("id").getAsLong());customer.setFirstName(c.get("firstName").getAsString());customer.setLastName(c.get("lastName").getAsString());user.setCustomerId(customer.getId());
        }
        return new Result(response.get("token").getAsString(),user,customer,identity.get("mustChangePassword").getAsBoolean());
    }
    private JsonObject send(String method,String path,String token,Object body) {
        HttpRequest.Builder request=HttpRequest.newBuilder(URI.create(base+path)).timeout(Duration.ofSeconds(12)).header("Accept","application/json");
        if(token!=null)request.header("Authorization","Bearer "+token);
        if(method.equals("GET"))request.GET();
        else request.header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(body==null?"{}":gson.toJson(body)));
        try {
            HttpResponse<String> response=http.send(request.build(),HttpResponse.BodyHandlers.ofString(java.nio.charset.StandardCharsets.UTF_8));
            if(response.body().length()>32768)throw new Failure(502,"INVALID_RESPONSE","Réponse du service invalide.");
            JsonObject json=JsonParser.parseString(response.body()).getAsJsonObject();
            if(response.statusCode()!=200)throw new Failure(response.statusCode(),json.has("code")?json.get("code").getAsString():"API_ERROR",
                    json.has("message")?json.get("message").getAsString():"Le service ne peut pas traiter cette demande.");
            return json;
        } catch(java.io.IOException e){throw new Failure(503,"UNAVAILABLE","Impossible de joindre le service de connexion. Vérifiez que le backend est démarré.");}
        catch(InterruptedException e){Thread.currentThread().interrupt();throw new Failure(503,"INTERRUPTED","La demande a été interrompue.");}
        catch(JsonParseException | IllegalStateException e){throw new Failure(502,"INVALID_RESPONSE","Réponse du service invalide.");}
    }
}
