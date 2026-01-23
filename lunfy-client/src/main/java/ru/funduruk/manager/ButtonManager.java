package ru.funduruk.manager;

import javafx.scene.control.Button;
import javafx.scene.effect.Glow;

public class ButtonManager {
    public static void setGlowButton(Button button, double size, double glowValue) {
        Glow glow = new Glow(0);

        button.setOnMouseEntered(e -> glow.setLevel(glowValue));
        button.setOnMouseExited(e -> glow.setLevel(0));

        button.setOnMousePressed(e -> {
            button.setScaleX(size);
            button.setScaleY(size);
        });

        button.setOnMouseReleased(e -> {
            button.setScaleX(1);
            button.setScaleY(1);
        });


        button.setEffect(glow);
    }
}
