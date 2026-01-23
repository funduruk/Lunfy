package ru.funduruk.manager;

import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class TitleBarManager {
    private static double xOffset = 0;
    private static double yOffset = 0;

    public static void enableWindowDragging(HBox titleBar) {
        titleBar.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });

        titleBar.setOnMouseDragged(event -> {
            Stage stage = (Stage) titleBar.getScene().getWindow();
            stage.setX(event.getScreenX() - xOffset);
            stage.setY(event.getScreenY() - yOffset);
        });
    }

    private static boolean isInResizeZone(MouseEvent event, BorderPane rootPane) {
        double mouseX = event.getX();
        double mouseY = event.getY();
        double width = rootPane.getWidth();
        double height = rootPane.getHeight();
        final int RESIZE_MARGIN = 10;

        return mouseX > width - RESIZE_MARGIN || mouseY > height - RESIZE_MARGIN;
    }

    public static void enableWindowResize(BorderPane rootPane) {
        rootPane.setOnMouseMoved(event -> {
            if (isInResizeZone(event, rootPane)) {
                rootPane.setCursor(Cursor.SE_RESIZE);
            } else {
                rootPane.setCursor(Cursor.DEFAULT);
            }
        });

        rootPane.setOnMouseDragged(event -> {
            Stage stage = (Stage) rootPane.getScene().getWindow();

            if (rootPane.getCursor() == Cursor.SE_RESIZE) {
                double newWidth = event.getX();
                double newHeight = event.getY();


                if (newWidth < stage.getMinWidth()) newWidth = stage.getMinWidth();
                if (newHeight < stage.getMinHeight()) newHeight = stage.getMinHeight();

                stage.setWidth(newWidth);
                stage.setHeight(newHeight);
            }
        });
    }

    public static void maximizeWithoutTaskbar(Stage stage) {
        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();

        stage.setX(bounds.getMinX());
        stage.setY(bounds.getMinY());
        stage.setWidth(bounds.getWidth());
        stage.setHeight(bounds.getHeight());
        stage.setMaximized(true);
    }
}
