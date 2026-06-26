package ru.funduruk.manager;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Circle;
import ru.funduruk.net.ApiClient;

public class AvatarLoader {


    public static void loadUserAvatar(String username, ImageView target, double size, Runnable onMissing) {
        if (username == null || username.isEmpty()) {
            if (onMissing != null) onMissing.run();
            return;
        }

        String url = ApiClient.HTTP_BASE + "/api/users/" + username
                + "/avatar?t=" + System.currentTimeMillis();

        Image img = new Image(url, size, size, true, true, true);

        img.progressProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.doubleValue() >= 1.0) {
                if (!img.isError()) {
                    target.setImage(img);
                    target.setFitWidth(size);
                    target.setFitHeight(size);
                    target.setClip(new Circle(size / 2, size / 2, size / 2));
                } else {
                    if (onMissing != null) onMissing.run();
                }
            }
        });
    }
}