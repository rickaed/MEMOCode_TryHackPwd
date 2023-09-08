package fr.sncf.d2d.shallenge;

import java.util.Arrays;
import java.util.Iterator;

/** Générateur de mots de passe en mode <i>bruteforce</i> */
public final class PasswordGenerator implements Iterable<String> {

    /** Les combinaisons de chaînes de caractères qui seront utilisables lors de la génération. */
    private final CharSequence[] alphabet;

    /** La taille des mots de passe à générer */
    private final int length;

    /** Le nombre total de mots de passe à générer */
    private final long total;

    /**
     * Créer un nouveau génerateur de mot de passe.
     * @param length La taille des mots de passe à générer.
     * @param alphabet Les séquences de caractères à utiliser lors de la génération. Peuvent être des séquences de caractères uniques ou multiples.
     */
    public PasswordGenerator(final int length, final CharSequence ...alphabet){

        assert length > 0;
        assert alphabet.length > 0;
        assert Arrays.stream(alphabet).noneMatch(CharSequence::isEmpty);

        this.length = length;
        this.alphabet = alphabet;
        // Le nombre total de mots de passe à générer est la taille de l'alphabet élevée à la puissance N, où N est la taille des mots de passe à générer.
        this.total = (long)Math.pow((double)this.alphabet.length, (double)this.length);
    }

    /** @return Le nombre total de mots de passe à générer.  */
    public long getTotal(){
        return this.total;
    }

    @Override
    public Iterator<String> iterator() {
        return new PasswordGeneratorIterator();
    }

    private class PasswordGeneratorIterator implements Iterator<String> {

        /** Le nombre de mots de passe déjà générés */
        private long state = 0L;

        /**
         * @return combien de mots de passe sont encore à générer avant que {@link #next()} ne renvoie {@link null}.
        */
        public long getRemaining(){
            return PasswordGenerator.this.total - this.state;
        }

        @Override
        public boolean hasNext() {
            return this.getRemaining() > 0;
        }

        @Override
        public String next() {

            final var alphabet = PasswordGenerator.this.alphabet;
            final var length = PasswordGenerator.this.length;
            
            // on copie le compteur dans une variable mutable.
            var state = this.state;

            // les objets StringBuilder permettent de construire des String via des concaténations successives.
            final var password = new StringBuilder();
            
            // la génération doit être vue comme un système de numération.
            // les éléments de l'alphabet représentent ceux de la base B à utiliser.
            // décider quel caractères constituent chaque mot de passe à générer revient à convertir un compteur  
            // vers sa représentation en base B.
            for (var i = 0; i < length; i++){
                password.append(alphabet[(int)(state % alphabet.length)]);
                state /= alphabet.length;
            }

            // après chaque génération, on incrémente le compteur. 
            this.state++;

            return password.toString();
        }
    }

}
