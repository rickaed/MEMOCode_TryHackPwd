package fr.sncf.d2d.shallenge;


import java.time.Instant;
import java.util.HexFormat;
import java.util.stream.IntStream;

/** Une application de résolution de challenge */
public final class App {

    /** Notre alphabet à fournir au générateur de mot de passe */
    private static final String[] ALPHABET = 
        // IntStream est un type spécifique de Stream.
        // Les Stream en Java représentent des "flux" de données quelconque.
        // Ils peuvent représenter des flux temporels (par exemple des données qui arrivent au fil du temps), ou des flux spatiaux (par exemple parcourir une liste).
        // Ici, la méthode range nous renvoie un flux d'entiers allant de la valeur du caractère 'a' jusqu'au caractère 'z' inclus.
        IntStream.range('a', 'z' + 1)
            // on peut manipuler des objets Stream via des méthodes de type map/reduce.
            // ici on transforme chaque caractère du flux en String.
            // On fait cela car le PasswordGenerator attend un CharSequence[], et String est un type concret de CharSequence.
            // on obtient ainsi un type Stream<String>
            .mapToObj(c -> new String(new char[]{ (char)c }))
            // on convertir le Stream<String> vers un type String[]
            // le flux est ainsi consommé.
            .toArray(String[]::new);

    /** La taille du mot de passe à trouver */
    private static final int PASSWORD_LENGTH = 6;

    public static void main(final String[] args) throws Exception {

        System.out.println("generating new challenge...");
 
        // on génère le challenge
        final var challenge = ChallengeService.generate();

        System.out.println(String.format("got new challenge with id %s", challenge.getId()));
        System.out.println(String.format("hash: %s (%d bytes)", HexFormat.of().formatHex(challenge.getHash()), challenge.getHash().length));
        System.out.println(String.format("salt: %s (%d bytes)", HexFormat.of().formatHex(challenge.getSalt()), challenge.getSalt().length));

        // on créé un générateur de mots de passe
        final var generator = new PasswordGenerator(PASSWORD_LENGTH, ALPHABET);

        System.out.println(String.format("got %d passwords to test", generator.getTotal()));

        // temps de départ
        final var start = Instant.now();
        // nombre d'essais
        var tries = 0L;

        System.out.println("starting to crack...");

        // on boucle sur chaque mot de passe à tester.
        // vu que `PasswordGenerator` implémente l'interface spéciale `Iterable`, on peut utiliser une syntaxe `for` dessus.
        // `password` contiendra chaque mot de passe renvoyé par la méthode next() de l'objet `Iterator` renvoyé par la méthode `iterator()`
        for (final var password: generator){

            // on met à jour le nombre d'essais
            tries++;

            // afficher des statistiques tous les 10 millions d'essais.
            if (tries % 10_000_000 == 0){
                System.out.println(String.format(
                    "last=%s tried=%d remains=%d elapsed=%d",
                    // le dernier mot de passe testé
                    password,
                    // le nombre total de mots de passe testés
                    tries,
                    // le nombre de mot de passe restants à tester
                    generator.getTotal() - tries,
                    // le nombre de secondes écoulées
                    Instant.now().getEpochSecond() - start.getEpochSecond()
                ));
            }

            // si le mot de passe n'est pas le bon, on passe au tour de boucle suivant.
            if (!challenge.matches(password)){
                continue;
            }

            System.out.println(String.format("valid password found: %s", password));

            // le mot de passe a été trouvé, on soumet la réponse.
            final var flag = ChallengeService.submit(challenge, password);

            System.out.println(flag);
            // on termine le programme avec un code de succès (0).
            System.exit(0);
        }

        // tous les mots de passe ont été testés et l'attaque n'a pas marché.
        // ca voudrait dire que notre programme ne fonctionne pas...
        System.err.println("woops, attack didnt work. time to debug !");
        // un programme qui se termine de lui-même (qui n'a pas été terminé par le système) renvoie généralement un code de status.
        // un code différent de zéro signifie que l'exécution ne s'est pas déroulé convenablement.
        // ce système de code de status est utilisé notamment lorsqu'un programme lance un autre programme et souhaite savoir si tout s'est bien déroulé.
        System.exit(1);
    }
}