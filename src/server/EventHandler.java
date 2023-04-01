package server;

/**
 * L'interface EventHandler contient la méthode "handle" qui sera surchargée selon l'évènement qui sera traité (handled).
 * Ceci assure que l'on puisse appeler la méthode handle sur ses instances sans se soucier de la hiérarchie des classes.
 */
@FunctionalInterface
public interface EventHandler {
    void handle(String cmd, String arg);
}