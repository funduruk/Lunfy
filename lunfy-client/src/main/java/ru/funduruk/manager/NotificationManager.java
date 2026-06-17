package ru.funduruk.manager;

import javafx.application.Platform;
import javafx.stage.Stage;

import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

public class NotificationManager {

    private static TrayIcon trayIcon;
    private static Stage primaryStage;

    public static void init(Stage stage) {
        primaryStage = stage;
        if (!SystemTray.isSupported()) {
            System.out.println("SystemTray не поддерживается");
            return;
        }
        try {
            SystemTray tray = SystemTray.getSystemTray();

            Image image;
            try {
                image = ImageIO.read(
                        NotificationManager.class.getResource("/image/icon/lunfy.png"));
            } catch (Exception e) {
                image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            }

            trayIcon = new TrayIcon(image, "Lunfy");
            trayIcon.setImageAutoSize(true);

            trayIcon.addActionListener(e -> Platform.runLater(() -> {
                if (primaryStage != null) {
                    primaryStage.setIconified(false);
                    primaryStage.toFront();
                }
            }));

            tray.add(trayIcon);
        } catch (AWTException e) {
            e.printStackTrace();
        }
    }

    public static void showSystem(String title, String message) {
        if (trayIcon != null) {
            trayIcon.displayMessage(title, message, TrayIcon.MessageType.INFO);
        }
    }

    public static boolean isWindowHidden() {
        if (primaryStage == null) return false;
        return primaryStage.isIconified() || !primaryStage.isFocused();
    }
}