package ru.funduruk;

import javafx.application.Application;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
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
        stage.setTitle("lunfy");
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
