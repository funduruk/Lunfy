package ru.funduruk.manager;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.animation.FadeTransition;
import javafx.util.Duration;

import java.util.Objects;

public class SceneManager {
    private static Stage stage;

    public static void init(Stage primaryStage) {
        stage = primaryStage;
    }

    public static void setScene(String fxml, String css){
        try{
            FXMLLoader loader = new FXMLLoader(
                    SceneManager.class.getResource(fxml)
            );
            Parent root = loader.load();

            Scene scene = new Scene(root);

            if(css != null){
                scene.getStylesheets().add(
                        Objects.requireNonNull(
                                SceneManager.class.getResource(css)
                        ).toExternalForm()
                );
            }

            FadeTransition fade = new FadeTransition(Duration.millis(500), root);
            fade.setFromValue(0);
            fade.setToValue(1);

            stage.setScene(scene);
            fade.play();
        } catch (Exception e){
            e.printStackTrace();
        }
    }
}
