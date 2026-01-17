package ru.funduruk;

import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import ru.funduruk.manager.SceneManager;

import java.util.Objects;

public class MainApplication extends Application {

    @Override
    public void start(Stage stage) {

        SceneManager.init(stage);

        // Icon
        stage.getIcons().add(
                new Image(
                        Objects.requireNonNull(
                                getClass().getResource("/image/logo/logo.png")
                        ).toExternalForm()
                )
        );

        // Setting Screen
        stage.setTitle("MeetGrid");
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        stage.initStyle(StageStyle.UNDECORATED);

        // login Screen
        SceneManager.setScene(
                "/fxml/LoginView.fxml",
                "/css/style.css"
        );

        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
