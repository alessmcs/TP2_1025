package server;

import javafx.util.Pair;
import server.models.RegistrationForm;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

/***
 * Représenter un serveur sur un port donné
 */
public class Server {
    /***
     * Désigne un bouton évènement pour l'inscription aux cours
     */
    public final static String REGISTER_COMMAND = "INSCRIRE";
    /***
     * Désigne un bouton évènement pour le chargement de la session en question
     */
    public final static String LOAD_COMMAND = "CHARGER";
    private final ServerSocket server;
    private Socket client;
    private ObjectInputStream objectInputStream;
    private ObjectOutputStream objectOutputStream;
    private final ArrayList<EventHandler> handlers;

    /**
     * Créer le constructeur de l'objet serveur qui est une instance de 'ServerSocket' et traite au maximum 1 client.
     * L'objet contient une liste de gestionnaire d'événements.
     * @param port la valeur par laquelle on définit le port dans la création du serveur
     * @throws IOException
     */
    public Server(int port) throws IOException {
        this.server = new ServerSocket(port, 1); // the constructor starts the connection to the server
        this.handlers = new ArrayList<EventHandler>();
        this.addEventHandler(this::handleEvents); // handleEvents returns
    }

    /**
     * Ajouter la séquence d'élément dans la liste d'éléments associés à cet objet
     * @param h séquence d'évènement de type 'EventHandler'
     */
    public void addEventHandler(EventHandler h) {
        this.handlers.add(h);
    }

    /**
     * Associer une alerte à tous les éléments de la liste en appelant la commande de l'argument donné
     * @param cmd la commande à transmettre à chaque gestionnaire d'événements enregistré.
     * @param arg l'argument à transmettre à chaque gestionnaire d'événements enregistré.
     */
    private void alertHandlers(String cmd, String arg) {
        for (EventHandler h : this.handlers) {
            h.handle(cmd, arg);
        }
    }

    /**
     * Répond de manière continue aux demandes de connection du côté client.
     * Gère la connection client-serveur en traitant les données envoyées par les clients.
     * @throws Exception pour attraper une erreur produite lors de la lecture ou l'écriture de l'objet.
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

    /***
     * Cette méthode traite les types d'exceptions suivantes :
     * @throws IOException lorsqu'une erreur survient lors de la lecture du flux d'entrée
     * @throws ClassNotFoundException pour une erreur qui se produit sur une classe inexistante
     * @throws NullPointerException pour une ligne de commande vide
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

    /***
     * Cette méthode prend une ligne de commande en entrée et sépare la commande de ses arguments en deux.
     * @param line entrée à analyser
     * @return Pair un objet qui contient la commande et ses arguments
     */

    public Pair<String, String> processCommandLine(String line) {
        String[] parts = line.split(" ");
        String cmd = parts[0];
        String args = String.join(" ", Arrays.asList(parts).subList(1, parts.length));
        return new Pair<>(cmd, args);
    }

    /***
     * Cette méthode déconnecte le client du serveur en fermant les flux de sortie et d'entrée, ainsi que la connexion.
     * @throws IOException
     */

    public void disconnect() throws IOException {
        objectOutputStream.close();
        objectInputStream.close();
        client.close();
    }

    /***
     * Cette méthode traite l'inscription ou le chargement des cours selon la commande passée en argument
     * @param cmd la commande à envoyer aux gestionnaires d'événement
     * @param arg l'argument associé à la commande
     */


    public void handleEvents(String cmd, String arg) {
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
     La méthode gère les exceptions si une erreur se produit lors de la lecture du fichier ou de l'écriture de l'objet
     dans le flux.
     @param arg la session pour laquelle on veut récupérer la liste des cours
     */
    public void handleLoadCourses(String arg) {
        try {
            FileReader fr = new FileReader("src/src/server/data/cours.txt");
            BufferedReader reader = new BufferedReader(fr);

            ArrayList<String> cours = new ArrayList<>();
            // read the file line by line
            Scanner scan = new Scanner(new File("src/src/server/data/cours.txt"));

            // filtrer les cours selon la session dans l'argument & les mettre dans la liste
            while (scan.hasNext()) {
                String line = reader.readLine();
                String semester = scan.next(); semester = scan.next(); semester = scan.next();
                if(semester.equalsIgnoreCase(arg)){
                    cours.add(line); // create the list
                }
            }

            // output to the user
            // you have to create a writer object, look aux notes de cours fichiers & serveur
            objectOutputStream.writeObject(cours);
            objectOutputStream.close();
            client.close();
            server.close();

        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }       // ON A BESOIN DE CODE COTE CLIENT FOR THIS TO WORK

    /***
     * Traite une demande d'inscription en enregistrant les informations du client via un objet RegistrationForm dans le
     * fichier d'inscription
     * @throws IOException pour une erreur qui se produit lors de la lecture ou l'écriture du formulaire d'inscription
     * @throws ClassNotFoundException si la classe RegistrationForm n'est pas trouvée lors de la sérialisation
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
            }
        }
    }
}


