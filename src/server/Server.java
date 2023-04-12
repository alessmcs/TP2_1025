package server;

import javafx.util.Pair;
import server.models.RegistrationForm;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;
import server.models.*;

public class Server {

    public final static String REGISTER_COMMAND = "INSCRIRE";
    public final static String LOAD_COMMAND = "CHARGER";
    private final ServerSocket server;
    private Socket client;
    private ObjectInputStream objectInputStream;
    private ObjectOutputStream objectOutputStream;
    private final ArrayList<EventHandler> handlers;

    /**
     * Cette méthode sert de construction pour cette classe. Elle associe notamment un port au serveur sur lequel on
     * va travailler, puis une liste de méthodes de traitements d'évènements pouvant provenir du serveur.
     * @param port
     * @throws IOException
     */
    public Server(int port) throws IOException {
        this.server = new ServerSocket(port, 1); // the constructor starts the connection to the server
        this.handlers = new ArrayList<EventHandler>();
        this.addEventHandler(this::handleEvents);
    }

    /**
     * Cette méthode ajoute un handler à une liste selon la requête du client.
     * @param h
     */
    public void addEventHandler(EventHandler h) {
        this.handlers.add(h);
    }

    /**
     * Cette méthode prend comme arguments cmd et arg, qui sont definis en traitant chaque ligne dans la ligne de commande
     * objectInputStream (du client). Elle itère à travers la liste de handlers et l'associe à la méthode handle de
     * l'interface EventHandler, ce qui rend cette méthode unique aux objets de ce type.
     *
     * @param cmd
     * @param arg
     */
    private void alertHandlers(String cmd, String arg) throws IOException {
        for (EventHandler h : this.handlers) {
            h.handle(cmd, arg);
        }
    }

    /**
     *
     */
    public void run() {
        while (true) {
            try {
                client = server.accept();
                System.out.println("Connecté au client: " + client);
                objectInputStream = new ObjectInputStream(client.getInputStream());
                objectOutputStream = new ObjectOutputStream(client.getOutputStream());
                listen();
                disconnect();
                System.out.println("Client déconnecté!");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Cette méthode permet d'associer certains éléments d'une ligne du input du client.
     * Elle utilise la classe Pair pour associer chaque élément de la ligne à une valeur et une clé propre.
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public void listen() throws IOException, ClassNotFoundException {
        String line;
        if ((line = this.objectInputStream.readObject().toString()) != null) {
            Pair<String, String> parts = processCommandLine(line);
            String cmd = parts.getKey();
            String arg = parts.getValue();
            this.alertHandlers(cmd, arg);
        }
    }

    /**
     * Cette méthode lit la ligne de commande (line) et la découpe en variables utiles pour le traitement
     * des requêtes par le client.
     * @param line
     * @return
     */
    public Pair<String, String> processCommandLine(String line) {
        String[] parts = line.split(" ");
        String cmd = parts[0];
        String args = String.join(" ", Arrays.asList(parts).subList(1, parts.length));
        return new Pair<>(cmd, args);
    }

    /**
     * Cette méthode referme simplement tous les flux de communication entre le client et le serveur. Elle
     * est appelée à la fin de run, suite à l'interaction avec le client.
     * @throws IOException
     */
    public void disconnect() throws IOException {
        objectOutputStream.close();
        objectInputStream.close();
        client.close();
    }

    /**
     * Le client peut envoyer deux types de requêtes au serveur : charger (load) ou inscrire (register). Cette
     * méthode exécute différents traitements des données fournies du côté client selon la requête indiquée.
     * @param cmd
     * @param arg
     */
    public void handleEvents(String cmd, String arg) throws IOException {
        if (cmd.equals(REGISTER_COMMAND)) {
            handleRegistration();
        } else if (cmd.equals(LOAD_COMMAND)) {
            handleLoadCourses(arg);
        }
    }

    /**
     Lire un fichier texte contenant des informations sur les cours et les transformer en liste d'objets 'Course'.
     La méthode filtre les cours par la session spécifiée en argument.
     Ensuite, elle renvoie la liste des cours pour une session au client en utilisant l'objet 'objectOutputStream'.
     La méthode gère les exceptions si une erreur se produit lors de la lecture du fichier ou de l'écriture de l'objet dans le flux.
     @param arg la session pour laquelle on veut récupérer la liste des cours
     */
    public void handleLoadCourses(String arg) throws IOException {
        try {
            FileReader fr = new FileReader("src/server/data/cours.txt");
            BufferedReader reader = new BufferedReader(fr);

            ArrayList<Course> coursSessionChoisie = new ArrayList<>();

            Scanner scan = new Scanner(new File("src/server/data/cours.txt"));

            // filtrer les cours selon la session dans l'argument & les mettre dans la liste
            // once filtered, parse through the file
            while (scan.hasNext()) {
                String semester = arg;
                    String line = scan.nextLine();

                    String[] infos = line.split(" ");
                    String codeCours = ""; String nomCours = ""; String sessionCours = "";

                    codeCours = infos[0]; // premier element sur la ligne
                    nomCours = infos[1];
                    sessionCours = infos[2];
                    if (sessionCours.equals(arg)) {
                        Course course = new Course(nomCours, codeCours, sessionCours);
                        coursSessionChoisie.add(course); // add to the arraylist of objects
                    }
                }

        // output to the user
            // you have to create a writer object, look aux notes de cours fichiers & serveur
            objectOutputStream.writeObject(coursSessionChoisie);
            objectOutputStream.close();
            client.close();
            server.close();
        } catch (FileNotFoundException ex) {
            throw new RuntimeException(ex);
        }   catch (IOException e) {
            e.printStackTrace();
        }
        }       // ON A BESOIN DE CODE COTE CLIENT FOR THIS TO WORK

    /**
     Récupérer l'objet 'RegistrationForm' envoyé par le client en utilisant 'objectInputStream', l'enregistrer dans un fichier texte
     et renvoyer un message de confirmation au client.
     La méthode gère les exceptions si une erreur se produit lors de la lecture de l'objet, l'écriture dans un fichier ou dans le flux de sortie.
     */
    public void handleRegistration() {
        // le client va fournir plusieurs informations et on doit lire chaque ligne et l'écrire dans inscription.txt comme une longue ligne
        try{
            // objectInputStream is already made in run()
            // créer l'instance de registrationForm dans ce fichier en utilisant objectInputStream
            RegistrationForm registrationForm = (RegistrationForm) objectInputStream.readObject();

            String inscription = ( registrationForm.getCourse().getSession()  + "\t" + registrationForm.getCourse().getCode()  + "\t" + registrationForm.getMatricule()
                    + "\t" + registrationForm.getPrenom() + "\t" + registrationForm.getNom() + "\t" + registrationForm.getEmail() );

            FileOutputStream fileOs = new FileOutputStream("inscription.txt");

            ObjectOutputStream os = new ObjectOutputStream(fileOs);
            // écrire le string dans le fichier
            os.writeObject(inscription);

            // fermeture des flux
            objectInputStream.close();
            os.close();
            server.close();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

    }
}
