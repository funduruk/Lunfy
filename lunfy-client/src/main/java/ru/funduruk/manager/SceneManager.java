package ru.funduruk.manager;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.animation.FadeTransition;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.io.IOException;
import java.util.Objects;

public class SceneManager {
    private static Stage stage;

    public static void init(Stage primaryStage) {
        stage = primaryStage;
    }

    public static void setScene(String fxml, String css) {


        Scene scene;
        Parent root;
        try {
            double w = stage.getWidth();
            double h = stage.getHeight();

            FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource(fxml));
            root = loader.load();

            scene = new Scene(root, w, h);
            scene.getStylesheets().add(
                    Objects.requireNonNull(
                            SceneManager.class.getResource(css)
                    ).toExternalForm()
            );
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

        FadeTransition fade = new FadeTransition(Duration.millis(500), root);
        fade.setFromValue(0);
        fade.setToValue(1);

        stage.setScene(scene);
        fade.play();
    }
    }
