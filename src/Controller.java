import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Store;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Properties;
import java.util.UUID;

public class Controller {
    User user;

    @FXML
    public void initialize() {
        loadCSSFiles();
    }

    @FXML
    private void minimize_stage(MouseEvent e) {
        Main.primaryStage.setIconified(true);
    }

    @FXML
    private void close_app(MouseEvent e) {
        //here you can save user stuff like theme and what not: mallory
        System.exit(0);
    }

    //copy annotation like this if you add a component using scene builder
    //you then need to make sure th fx:id is the same as below for you to use it in the code
    //see how I got the username and password for a better explanation

    @FXML public StackPane masterStack;
    @FXML public AnchorPane parent;
    @FXML public TextField emailField;
    @FXML public PasswordField passField;

    public static String emailAddress;
    public static char[] password;

    @FXML
    ChoiceBox<String> switchCSS;
    ObservableList list = FXCollections.observableArrayList();

    private void loadCSSFiles() {
        list.removeAll();

        File[] files = new File("src\\userStyles").listFiles();
        ArrayList<String> cssFiles = new ArrayList<>();

        for (File f : files) {
            if (f.getName().endsWith(".css")) {
                cssFiles.add(f.getName().replace(".css", ""));
            }
        }

        list.addAll(cssFiles);
        switchCSS.getItems().addAll(list);
        switchCSS.getSelectionModel().select(0);
    }

    private boolean isValidEmail(String email) {
        if (!email.matches("(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|\"" +
                "(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])" +
                "*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:(2(5[0-5]|[0-4][0-9])" +
                "|1[0-9][0-9]|[1-9]?[0-9]))\\.){3}(?:(2(5[0-5]|[0-4][0-9])|1[0-9][0-9]|[1-9]?[0-9])|[a-z0-9-]*[a-z0-9]:(?:[\\x01-" +
                "\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])"))
            return false;


        return (email.endsWith("@gmail.com") ||
                email.endsWith("@yahoo.com") ||
                email.endsWith("@outlook.com")) &&
                email.trim().length() > 0;
    }

    //todo nathan working here, need to get it working then i'll add a getInbox function where you pass in a folder
    //this is what I was talking about that you don't need a database for this program
    public boolean validateCredentials(String user, char[] pass) {
        try {
            Properties props = new Properties();

            props.put("mail.smtp.auth", true);
            props.put("mail.smtp.host", getEmailHost(user));
            props.put("mail.smtp.port", 587);

            Session emailSession = Session.getDefaultInstance(props);
            Store sessionStore = emailSession.getStore("pop3");

            StringBuilder passBuild = new StringBuilder();
            for (char c : pass) passBuild.append(c);

            sessionStore.connect(getEmailHost(user), user, passBuild.toString());

            //folder object and open it, could read from trash and such by changing INBOX
            Folder emailFolder = sessionStore.getFolder("INBOX");
            emailFolder.open(Folder.READ_ONLY); //HOLDS_FOLDERS, HOLDS_MESSAGES, READ_ONLY, WRITE_ONLY

            Message[] messages = emailFolder.getMessages();
            int unreadEmailsNum = messages.length;

            //now you can get a message from messages and call .getSubject, .getFrom, .getContent, etc.

            emailFolder.close(false);
            sessionStore.close();

            //todo try and get inbox of user and if it fails its invalid

            return true;
        }

        catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    @FXML
    private void login(ActionEvent e) {
        emailAddress = emailField.getText();
        password = passField.getText().toCharArray();

//        if (isValidEmail(emailAddress) && validateCredentials(emailAddress, password)) {
            this.user = new User(emailField.getText(), toHexString(getSHA(passField.getText().toCharArray())), switchCSS.getSelectionModel().getSelectedItem());

            try {
                this.user.writeUser();
            }

            catch (IOException ex) {
                ex.printStackTrace();
            }

            loadCompose(e);
//        }

//        else {
//            System.out.println("todo popup informing invalid email");
//        }
    }

    private String getEmailHost(String email) throws Exception {
        if (email.endsWith("gmail.com"))
            return "smtp.gmail.com";
        else if (email.endsWith("yahoo.com"))
            return "smtp.mail.yahoo.com";
        else if (email.endsWith("outlook.com"))
            return "smtp.office365.com";
        else
            throw new Exception("Unsupported email host");
    }

    //Secure Hashing Algorithm 256 bit std encryption
    public byte[] getSHA(char[] input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return md.digest(new String(input).getBytes(StandardCharsets.UTF_8));
        }

        catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }

    //returns hex representation of bit string
    public String toHexString(byte[] hash) {
        BigInteger number = new BigInteger(1, hash);
        StringBuilder hexString = new StringBuilder(number.toString(16));

        while (hexString.length() < 32) {
            hexString.insert(0, '0');
        }

        return hexString.toString();
    }

    //not need this method but here in case we need a UUID generator
    public String generateUUID() {
        try {
            MessageDigest salt =MessageDigest.getInstance("SHA-256");
            salt.update(UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8));
            return UUID.nameUUIDFromBytes(salt.digest()).toString();
        }

        catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    @FXML
    private void loadCompose(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("email.fxml"));
            Scene currentScene = emailField.getScene();
            root.translateXProperty().set(currentScene.getWidth());
            masterStack.getChildren().add(root);

            Timeline tim = new Timeline();
            KeyValue kv = new KeyValue(root.translateXProperty(), 0 , Interpolator.EASE_IN);
            KeyFrame kf = new KeyFrame(Duration.seconds(1), kv);
            tim.getKeyFrames().add(kf);
            tim.setOnFinished(event1 -> masterStack.getChildren().remove(parent));
            tim.play();
        }

        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
