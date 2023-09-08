package fr.sncf.d2d.shallenge;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

/** Service de communication avec l'API de challenges. Contient uniquement des méthodes statiques (classe utilitaire). */
public final class ChallengeService {

    // les classes utilitaires ne sont pas faites pour être instanciées, elles ont uniquement des méthodes/propriétés statiques.
    // c'est une bonne pratique d'interdire totalement l'utilisation du constructeur en levant une exception systématique lorsqu'une
    // tentative survient.
    private ChallengeService(){
        throw new UnsupportedOperationException("this is a utility class");
    }

    /** L'origine du serveur distant */
    private static final String ORIGIN = "https://shallenge.onrender.com";

    /** URL de création de challenges */
    private static final URI CREATE_CHALLENGE_URI = URI.create(String.format("%s/challenges", ORIGIN));

    /** Fonction lambda de création d'une URL de réponse à un challenge. */
    private static final Function<Challenge, URI> ANSWER_CHALLENGE_URI = 
        (final Challenge challenge) -> URI.create(String.format(
            "%s/challenges/%s/answer",
            ORIGIN,
            challenge.getId()
        ));

    /** 
     * Générer un nouveau {@link Challenge}. 
     * @return Un {@link Challenge} qui vient d'être généré.
     * */
    public static Challenge generate() throws IOException, InterruptedException {
        final var request = HttpRequest.newBuilder(CREATE_CHALLENGE_URI)
            .POST(BodyPublishers.noBody())
            .build();
        final var response = HttpClient.newHttpClient()
            .send(request, BodyHandlers.ofString(StandardCharsets.UTF_8));
        
        if (response.statusCode() != 201){
            throw new IllegalStateException(String.format(
                "could not generate challenge (status=%d)", 
                response.statusCode()
            ));
        }

        return Challenge.fromJson(response.body());
    }

    /** 
     * Soumettre la réponse à un challenge.
     * @param challenge Le challenge auquel on répond
     * @param answer La mot de passe de réponse
     * @return La réponse de l'API
     * */
    public static String submit(final Challenge challenge, final String answer) throws IOException, InterruptedException {

        final var request = HttpRequest.newBuilder(ANSWER_CHALLENGE_URI.apply(challenge))
            .header("Content-Type", "application/json")
            .POST(BodyPublishers.ofString(String.format("\"%s\"", answer)))
            .build();

        final var response = HttpClient.newHttpClient()
            .send(request, BodyHandlers.ofString());

        if (response.statusCode() != 200){
            throw new IllegalStateException(String.format(
                "could not validate challenge (status=%d)",
                response.statusCode()
            ));
        }

        final var flag = response.body();

        // on retire les guillemets autour de la chaîne JSON de réponse
        return flag.substring(1, flag.length() - 1);
    }
    
}
