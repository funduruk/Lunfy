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

import java.util.Objects;

public class MainApplication extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/LoginView.fxml"));
        BorderPane rootPane = loader.load();

        Scene scene = new Scene(rootPane, 900, 600);
        scene.getStylesheets().add(
                Objects.requireNonNull(
                        getClass().getResource("/css/style.css")
                ).toExternalForm()
        );


        Image bg = new Image(
                Objects.requireNonNull(
                        getClass().getResource("/image/background/login_background.jpg")
                ).toExternalForm()
        );

        ImageView bgView = new ImageView(bg);

        bgView.fitWidthProperty().bind(rootPane.widthProperty());
        bgView.fitHeightProperty().bind(rootPane.heightProperty());
        bgView.setPreserveRatio(false);


        rootPane.getChildren().addFirst(bgView);


        Image icon = new Image(Objects.requireNonNull(getClass().getResource("/image/logo/logo.png")).toExternalForm());
        primaryStage.getIcons().add(icon);


        primaryStage.setTitle("MeetGrid Login");
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(600);
        primaryStage.initStyle(StageStyle.UNDECORATED);
        primaryStage.setScene(scene);
        primaryStage.setAlwaysOnTop(true);
        primaryStage.show();
        primaryStage.setAlwaysOnTop(false);
        FadeTransition fade = new FadeTransition(Duration.millis(1500), rootPane);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();
    }

    public static void main(String[] args) {
        launch();
    }
}
