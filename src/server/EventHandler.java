package server;

import java.io.IOException;

/**
 * L'interface fonctionnelle EventHandler contient la méthode "handle" qui sera redéfinie selon l'évènement qui sera traité.
 * Ceci assure que l'on puisse appeler la méthode handle sur ses instances sans se soucier de la hiérarchie des classes.
 */
@FunctionalInterface
public interface EventHandler {
    void handle(String cmd, String arg) throws IOException;
}