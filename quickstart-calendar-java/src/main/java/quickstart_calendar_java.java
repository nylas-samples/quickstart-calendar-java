// Import Java Utilities
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import static spark.Spark.*;
import com.nylas.NylasClient;
import com.nylas.models.*;

//Import DotEnv to handle .env files
import com.nylas.models.Calendar;
import io.github.cdimascio.dotenv.Dotenv;

public class quickstart_calendar_java {

    public static void main(String[] args) {
        // Load the .env file
        Dotenv dotenv = Dotenv.load();
        // Connect it to Nylas using the Access Token from the .env file
        NylasClient nylas = new NylasClient.Builder(dotenv.get("NYLAS_API_KEY")).apiUri(dotenv.get("NYLAS_API_URI")).build();

        get("/nylas/auth", (request, response) -> {

            List<String> scope = new ArrayList<>();
            scope.add("https://www.googleapis.com/auth/calendar");

            UrlForAuthenticationConfig config = new UrlForAuthenticationConfig(dotenv.get("NYLAS_CLIENT_ID"),
                    "http://localhost:4567/oauth/exchange",
                    AccessType.ONLINE,
                    AuthProvider.GOOGLE,
                    Prompt.DETECT,
                    scope,
                    true,
                    "sQ6vFQN",
                    "swag@nylas.com");

            String url = nylas.auth().urlForOAuth2(config);
            response.redirect(url);
            return null;
        });

        get("/oauth/exchange", (request, response) -> {
            String code = request.queryParams("code");
            if(code == null) { response.status(401);}
            assert code != null;
            CodeExchangeRequest codeRequest = new CodeExchangeRequest(
                    "http://localhost:4567/oauth/exchange",
                    code,
                    dotenv.get("NYLAS_CLIENT_ID"),
                    null,
                    null);
            try{
                CodeExchangeResponse codeResponse = nylas.auth().exchangeCodeForToken(codeRequest);
                request.session().attribute("grant_id", codeResponse.getGrantId());
                return "%s".formatted(codeResponse.getGrantId());
            }catch(Exception e){
                return  "%s".formatted(e);
            }
        });

        get("/nylas/primary-calendar", (request, response) -> {
            try {
                ListCalendersQueryParams listCalendersQueryParams =
                        new ListCalendersQueryParams.Builder().limit(5).build();
                List<Calendar> calendars =
                        nylas.calendars().list(request.session().attribute("grant_id"),
                                listCalendersQueryParams).getData();
                for (Calendar calendar : calendars) {
                    if (calendar.isPrimary()) {
                        request.session().attribute("primary", calendar.getId());
                    }
                }
                return "%s".formatted(request.session().attribute("primary"));
            } catch (Exception e){
                return "%s".formatted(e);
            }
        });

        get("/nylas/list-events", (request, response) -> {
            try {
                ListEventQueryParams listEventQueryParams =
                        new ListEventQueryParams.
                                Builder(request.session().attribute("primary")).
                                limit(5).build();
                List<Event> events = nylas.events().list(request.session().attribute("grant_id"),
                        listEventQueryParams).getData();
                return "%s".formatted(events);
            }catch (Exception e) {
                return "%s".formatted(e);
            }
        });

        get("/nylas/create-event", (request, response) -> {
            try {
                LocalDate today = LocalDate.now();
                Instant instant = today.atStartOfDay(ZoneId.systemDefault()).toInstant();
                Instant now_plus_5 = instant.plus(5, ChronoUnit.MINUTES);
                long startTime = now_plus_5.getEpochSecond();
                Instant now_plus_10 = now_plus_5.plus(35, ChronoUnit.MINUTES);
                long endTime = now_plus_10.getEpochSecond();
                CreateEventRequest.When.Timespan timespan =
                        new CreateEventRequest.When.Timespan.
                                Builder(Math.toIntExact(startTime),
                                Math.toIntExact(endTime)).build();
                CreateEventRequest createEventRequest = new CreateEventRequest.Builder(timespan)
                        .title("Your event title here")
                        .build();
                CreateEventQueryParams createEventQueryParams =
                        new CreateEventQueryParams.
                                Builder(request.session().attribute("primary")).build();
                Event event = nylas.events().create(request.session().attribute("grant_id"),
                        createEventRequest, createEventQueryParams).getData();
                return "%s".formatted(event);
            }catch (Exception e) {
                return "%s".formatted(e);
            }
        });
    }
}
