package main;

import javafx.util.Pair;
import main.models.Course;
import main.models.RegistrationForm;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;


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
     *
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
                if(client.getInputStream() == null){
                    throw new IOException();
                }
                objectOutputStream = new ObjectOutputStream(client.getOutputStream());
                objectInputStream = new ObjectInputStream(client.getInputStream());
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
     *
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public void listen() throws IOException, ClassNotFoundException {
        String line;
        System.out.println("Listening..."); //!!//

//        try {
//            Object obj = objectInputStream.readObject();
//            //line = (String) obj.toString();
//        } catch (IOException e) {
//            e.printStackTrace();
//            // Handle exception
//        }
        if ((line = this.objectInputStream.readObject().toString()) != null) {
            Pair<String, String> parts = processCommandLine(line);
            System.out.println(parts); //!!//
            String cmd = parts.getKey();
            String arg = parts.getValue();
            this.alertHandlers(cmd, arg);
        }
    }

    /**
     * Cette méthode lit la ligne de commande (line) et la découpe en variables utiles pour le traitement
     * des requêtes par le client.
     *
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
     *
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
     *
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
     * Lire un fichier texte contenant des informations sur les cours et les transformer en liste d'objets 'Course'.
     * La méthode filtre les cours par la session spécifiée en argument.
     * Ensuite, elle renvoie la liste des cours pour une session au client en utilisant l'objet 'objectOutputStream'.
     * La méthode gère les exceptions si une erreur se produit lors de la lecture du fichier ou de l'écriture de l'objet dans le flux.
     *
     * @param arg la session pour laquelle on veut récupérer la liste des cours
     */
    // EXCEPTIONS SEPAREES POUR LECTURE DU FICHIER ET ECRITURE DANS LE FULUX
    public void handleLoadCourses(String arg) throws IOException {
        try{
            FileReader fileReader = new FileReader("src/main/resources/cours.txt");
            BufferedReader br = new BufferedReader(fileReader);
            ArrayList<String> coursSessionChoisie = new ArrayList<>();
            String semester = null; String line;
                while ((line = br.readLine()) != null) {
                    String[] infos = line.split("\t");
                    String codeCours = "";
                    String nomCours = "";
                    String sessionCours = "";

                    codeCours = infos[0]; // premier element sur la ligne
                    nomCours = infos[1];
                    sessionCours = infos[2];

                    switch (arg) {
                        case "1":
                            semester = "automne";
                            break;
                        case "2":
                            semester = "hiver";
                            break;
                        case "3":
                            semester = "ete";
                            break;
                        default:
                            throw new IllegalArgumentException("Session invalide: " + semester);
                    }

                    if (sessionCours.equalsIgnoreCase(semester)) {
                        Course course = new Course(nomCours, codeCours, sessionCours);
                        coursSessionChoisie.add(course.toString()); // add to the arraylist of objects
                    }
                }
                System.out.println(coursSessionChoisie);
            objectOutputStream.writeObject(coursSessionChoisie);
            objectOutputStream.flush();

            } catch (IOException e) {
                System.err.println("Error reading file: " + e.getMessage());
                System.out.println("Absolute path:" + new File("cours.txt").getAbsolutePath());
            }
    }

    /**
     * Récupérer l'objet 'RegistrationForm' envoyé par le client en utilisant 'objectInputStream', l'enregistrer dans un fichier texte
     * et renvoyer un message de confirmation au client.
     * La méthode gère les exceptions si une erreur se produit lors de la lecture de l'objet, l'écriture dans un fichier ou dans le flux de sortie.
     */
    public void handleRegistration() {
        // le client va fournir plusieurs informations et on doit lire chaque ligne et l'écrire dans inscription.txt comme une longue ligne
        System.out.println("handleReg");
        String inscription = null;

        try {

            try {
                RegistrationForm registrationForm = (RegistrationForm) objectInputStream.readObject();
                System.out.println(registrationForm);
                inscription = (registrationForm.getCourse().getSession() + "\t" + registrationForm.getCourse().getCode() + "\t" + registrationForm.getMatricule()
                        + "\t" + registrationForm.getPrenom() + "\t" + registrationForm.getNom() + "\t" + registrationForm.getEmail());


            } catch (ClassNotFoundException e ) {
                e.printStackTrace();
            } catch (IOException e ) {
                e.printStackTrace();
            } catch (ClassCastException e ) {
                e.printStackTrace();
            } finally {
                if (objectInputStream != null) {
                    System.out.println("received form from client");
                    try {
                        objectInputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            FileOutputStream fileOs = new FileOutputStream("src/main/resources/inscription.txt", true);
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(fileOs));

            writer.newLine();
            writer.write(inscription);
            writer.close();

        } catch (IOException e) {
            throw new RuntimeException(e);


        }
    }
}
