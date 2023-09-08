package fr.sncf.d2d.shallenge;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.UUID;
import java.util.function.Function;

/** Un challenge à résoudre */
public final class Challenge {
    
    private final byte[] hash;

    private final byte[] salt;

    private final UUID id;

    /**
     * On rend le constructeur privé. la seule façon d'instancier un {@link Challenge} est de passer par la méthode statique {@link #fromJson(String)}
     */
    private Challenge(final UUID id, final byte[] hash, final byte[] salt){
        this.hash = hash;
        this.salt = salt;
        this.id = id;
    }

    /**
     * @return Le hash à cracker pour ce challenge
     */
    public byte[] getHash() {
        return this.hash;
    }

    /** @return Le sel qui a été utilisé lors de la génération du hash */
    public byte[] getSalt() {
        return this.salt;
    }

    /** @return l'identifiant unique du challenge  */
    public UUID getId() {
        return this.id;
    }

    /** 
     * Créer un {@link Challenge} à partir d'une chaîne JSON.
     * Méthode manuelle, (évidemment non stable).
     * @return Un {@link Challenge}. 
     */
    public static Challenge fromJson(final String json){

        // on enlève les accolades {}
        final var withoutBrackets = json.substring(1, json.length() - 1);
        // on récupère chaque clé/valeur en séparant au niveau des virgules.  
        final var members = withoutBrackets.split(",");

        // fonction lambda servant à récupérer une valeur de type chaîne de caractère en se basant sur le nom de la clé.
        final Function<String, String> getFieldAsString = (id) ->
            // on créé un flux qui se base sur toutes les clé/valeur du JSON 
            Arrays.stream(members)
                // on filtre les éléments du flux pour trouver celui qui a pour clé celle passée en paramètre
                .filter(member -> member.startsWith(String.format("\"%s\"", id)))
                // on récupère un Optional<String> qui symbolise le premier élément trouvé dans le flux
                // un Optional représente une valeur présente ou absente.
                .findFirst()
                // la méthode map() sur les Optional permet d'appliquer une transformation sur la valeur contenue.
                // elle renvoie un nouvel Optional avec un nouveau type générique qui dépend de ce qu'on retourne dans la fonction de transformation.
                // si l'Optional était vide, alors le nouvel Optional restera vide.
                .map(member -> {
                    // on sépare la clé de la valeur au niveau du symbole ":"
                    final var keyAndValue = member.split(":");
                    // on enlève les guillemets autour de la chaîne et on retourne la valeur.
                    return keyAndValue[1].substring(1, keyAndValue[1].length() - 1);
                })
                // si le champ n'a pas été trouvé (Optional vide), on lève une exception, sinon la valeur finale du champ est retournée.
                .orElseThrow(() -> new NoSuchFieldError(id));

        // on récupére le champ "id" et on le convertit vers un type UUID
        final var id = UUID.fromString(getFieldAsString.apply("id"));
        // on récupère le champ "hash" et on le convertit vers un type byte[]
        final var hash = HexFormat.of().parseHex(getFieldAsString.apply("hash"));
        // on récupère le champ "salt" et on le convertit vers un type byte[]
        final var salt = HexFormat.of().parseHex(getFieldAsString.apply("salt"));

        return new Challenge(id, hash, salt);
    }

    /** 
     * Vérifier si le challenge a pour réponse le mot de passe fourni en paramètre. 
     * @return {@code true} si le mot de passe correspond, sinon {@code false}.
     */
    public boolean matches(final String password) throws NoSuchAlgorithmException {
        // on récupère une instance de MessageDigest. en fonction du système, l'implémentation concrète sera différente.
        final var md = MessageDigest.getInstance("SHA-256");
        // on passe le sel en premier. la méthode update() permet de fournir les données "bout par bout" à la fonction de hachage. 
        md.update(this.salt);
        // la méthode digest() permet de finaliser la création du hash en passant optionnellement des données finales.
        // on en profite pour passer le mot de passe à tester.
        // on aurait pu à la place faire un autre appel à update() et ne rien passer à digest().
        // dans les deux cas, on a bien SHA256(salt + password).
        final var computed = md.digest(password.getBytes(StandardCharsets.UTF_8));
        // on renvoie true si le contenu du hash présent dans le challenge est le même que celui qu'on vient de calculer.
        return Arrays.equals(computed, this.hash);
    }
}
