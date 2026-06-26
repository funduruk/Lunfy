package ru.funduruk.dialog;

import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

public class AvatarEditorDialog {

    private static final int CANVAS_SIZE = 360;
    private static final int OUTPUT_SIZE = 256;

    private final Image sourceImage;
    private final Stage stage;
    private File resultFile;

    private double imgScale = 1.0;
    private double imgOffsetX = 0;
    private double imgOffsetY = 0;
    private double dragStartX;
    private double dragStartY;
    private double dragOriginX;
    private double dragOriginY;

    private Canvas canvas;

    public AvatarEditorDialog(File sourceFile) {
        this.sourceImage = new Image(sourceFile.toURI().toString());
        this.stage = new Stage(StageStyle.UTILITY);
    }

    public File showAndWait() {
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Редактирование аватара");
        stage.setResizable(false);

        canvas = new Canvas(CANVAS_SIZE, CANVAS_SIZE);
        StackPane canvasWrap = new StackPane(canvas);
        canvasWrap.setStyle("-fx-background-color: #0d0a1f;");

        Circle mask = new Circle(CANVAS_SIZE / 2.0);
        mask.setFill(Color.TRANSPARENT);
        mask.setStroke(Color.web("#7c5cff"));
        mask.setStrokeWidth(2);

        StackPane previewPane = new StackPane(canvas, mask);
        previewPane.setPrefSize(CANVAS_SIZE, CANVAS_SIZE);
        previewPane.setStyle("-fx-background-color: #0d0a1f; -fx-background-radius: 12;");

        double w = sourceImage.getWidth();
        double h = sourceImage.getHeight();
        imgScale = CANVAS_SIZE / Math.min(w, h);

        redraw();

        canvas.setCursor(Cursor.MOVE);
        canvas.setOnMousePressed(e -> {
            dragStartX = e.getX();
            dragStartY = e.getY();
            dragOriginX = imgOffsetX;
            dragOriginY = imgOffsetY;
        });
        canvas.setOnMouseDragged(e -> {
            imgOffsetX = dragOriginX + (e.getX() - dragStartX);
            imgOffsetY = dragOriginY + (e.getY() - dragStartY);
            redraw();
        });

        canvas.setOnScroll(e -> {
            double delta = e.getDeltaY() > 0 ? 1.1 : 1 / 1.1;
            double newScale = imgScale * delta;
            double minScale = CANVAS_SIZE / Math.min(w, h);
            double maxScale = minScale * 5;
            if (newScale < minScale) newScale = minScale;
            if (newScale > maxScale) newScale = maxScale;
            imgScale = newScale;
            redraw();
        });

        Slider zoomSlider = new Slider(1.0, 5.0, 1.0);
        zoomSlider.setPrefWidth(CANVAS_SIZE);
        double initialScale = imgScale;
        zoomSlider.valueProperty().addListener((obs, old, val) -> {
            imgScale = initialScale * val.doubleValue();
            redraw();
        });
        Label zoomLabel = new Label("Масштаб");
        zoomLabel.setStyle("-fx-text-fill: #9a96c4; -fx-font-size: 11px;");

        VBox zoomBox = new VBox(6, zoomLabel, zoomSlider);
        zoomBox.setAlignment(Pos.CENTER);

        Button cancelBtn = new Button("Отмена");
        cancelBtn.setStyle("-fx-background-color: #1e1b3a; -fx-text-fill: #f0eeff; "
                + "-fx-background-radius: 10; -fx-padding: 10 22; -fx-cursor: hand;");
        cancelBtn.setOnAction(e -> {
            resultFile = null;
            stage.close();
        });

        Button saveBtn = new Button("Сохранить");
        saveBtn.setStyle("-fx-background-color: #7c5cff; -fx-text-fill: white; "
                + "-fx-background-radius: 10; -fx-padding: 10 22; -fx-cursor: hand; "
                + "-fx-font-weight: bold;");
        saveBtn.setOnAction(e -> {
            try {
                resultFile = exportImage();
                stage.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        HBox buttons = new HBox(10, cancelBtn, saveBtn);
        buttons.setAlignment(Pos.CENTER_RIGHT);

        // === Заголовок ===
        Label title = new Label("Подгоните изображение");
        title.setStyle("-fx-text-fill: #f0eeff; -fx-font-size: 18px; -fx-font-weight: bold;");

        Label hint = new Label("Перетаскивайте мышью, колесо для масштабирования");
        hint.setStyle("-fx-text-fill: #9a96c4; -fx-font-size: 12px;");

        VBox root = new VBox(16, title, hint, previewPane, zoomBox, buttons);
        root.setStyle("-fx-background-color: #13102a; -fx-padding: 24; "
                + "-fx-border-color: rgba(124,92,255,0.2); -fx-border-radius: 16; -fx-background-radius: 16;");
        root.setAlignment(Pos.CENTER);

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        stage.setScene(scene);
        stage.showAndWait();

        return resultFile;
    }

    private void redraw() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(Color.web("#0d0a1f"));
        gc.fillRect(0, 0, CANVAS_SIZE, CANVAS_SIZE);

        double w = sourceImage.getWidth() * imgScale;
        double h = sourceImage.getHeight() * imgScale;

        double x = (CANVAS_SIZE - w) / 2 + imgOffsetX;
        double y = (CANVAS_SIZE - h) / 2 + imgOffsetY;

        gc.drawImage(sourceImage, x, y, w, h);

        gc.setFill(Color.color(0.05, 0.04, 0.12, 0.65));
    }

    private File exportImage() throws Exception {
        Canvas exportCanvas = new Canvas(OUTPUT_SIZE, OUTPUT_SIZE);
        GraphicsContext gc = exportCanvas.getGraphicsContext2D();

        gc.setFill(Color.web("#1e1b3a"));
        gc.fillRect(0, 0, OUTPUT_SIZE, OUTPUT_SIZE);

        double k = (double) OUTPUT_SIZE / CANVAS_SIZE;

        double w = sourceImage.getWidth() * imgScale * k;
        double h = sourceImage.getHeight() * imgScale * k;
        double x = (OUTPUT_SIZE - w) / 2 + imgOffsetX * k;
        double y = (OUTPUT_SIZE - h) / 2 + imgOffsetY * k;

        gc.drawImage(sourceImage, x, y, w, h);

        javafx.scene.image.WritableImage snapshot = new javafx.scene.image.WritableImage(OUTPUT_SIZE, OUTPUT_SIZE);
        exportCanvas.snapshot(null, snapshot);

        BufferedImage bImage = SwingFXUtils.fromFXImage(snapshot, null);

        File tempFile = File.createTempFile("lunfy_avatar_", ".png");
        tempFile.deleteOnExit();
        ImageIO.write(bImage, "png", tempFile);

        return tempFile;
    }
}