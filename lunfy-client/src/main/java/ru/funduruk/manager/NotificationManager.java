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
                        NotificationManager.class.getResource("/image/logo/logo.png"));
            } catch (Exception e) {
                image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            }

            PopupMenu popup = new PopupMenu();

            MenuItem openItem = new MenuItem("Открыть Lunfy");
            openItem.addActionListener(e -> restoreWindow());

            MenuItem exitItem = new MenuItem("Выход");
            exitItem.addActionListener(e -> {
                if (trayIcon != null) {
                    SystemTray.getSystemTray().remove(trayIcon);
                }
                Platform.exit();
                System.exit(0);
            });

            popup.add(openItem);
            popup.addSeparator();
            popup.add(exitItem);

            trayIcon = new TrayIcon(image, "Lunfy", popup);
            trayIcon.setImageAutoSize(true);
            trayIcon.addActionListener(e -> restoreWindow());

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

    private static void restoreWindow() {
        System.out.println(">>> restoreWindow вызван, primaryStage=" + primaryStage);
        Platform.runLater(() -> {
            if (primaryStage == null) {
                System.out.println(">>> primaryStage == null!");
                return;
            }
            System.out.println(">>> showing=" + primaryStage.isShowing()
                    + " iconified=" + primaryStage.isIconified());
            primaryStage.show();
            primaryStage.setIconified(false);
            primaryStage.toFront();
            primaryStage.requestFocus();
            System.out.println(">>> после show: showing=" + primaryStage.isShowing());
        });
    }

}