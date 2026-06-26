package ru.funduruk.lunfyServer.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;

@Service
public class MailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromAddress;

    @Value("${app.mail.from-name}")
    private String fromName;

    public MailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendVerificationCode(String toEmail, String code) {
        System.out.println(">>> Отправка кода " + code + " на " + toEmail);
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");

            helper.setFrom(fromAddress, fromName);
            helper.setTo(toEmail);
            helper.setSubject("Код подтверждения Lunfy");
            helper.setText(buildHtml(code), true);

            mailSender.send(message);
            System.out.println(">>> Письмо отправлено успешно");
        } catch (MessagingException | UnsupportedEncodingException e) {
            System.err.println(">>> Ошибка отправки: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Не удалось отправить письмо", e);
        }
    }

    private String buildHtml(String code) {
        return """
            <!DOCTYPE html>
            <html><body style="margin:0;padding:40px;background:#0d0a1f;font-family:Arial,sans-serif;">
              <div style="max-width:480px;margin:0 auto;background:#1e1b3a;border-radius:16px;padding:40px;border:1px solid rgba(124,92,255,0.2);">
                <div style="font-size:14px;color:#a78bff;letter-spacing:0.15em;font-weight:600;margin-bottom:16px;">LUNFY</div>
                <h1 style="color:#f0eeff;font-size:24px;margin:0 0 12px 0;">Подтвердите почту</h1>
                <p style="color:#9a96c4;font-size:14px;line-height:1.5;margin:0 0 32px 0;">
                  Введите этот код в приложении, чтобы завершить регистрацию.
                </p>
                <div style="background:#0d0a1f;border:1px solid rgba(124,92,255,0.3);border-radius:12px;padding:24px;text-align:center;">
                  <div style="font-size:36px;font-weight:900;color:#a78bff;letter-spacing:0.3em;font-family:monospace;">%s</div>
                </div>
                <p style="color:#5a5680;font-size:12px;margin:24px 0 0 0;">
                  Код действителен 10 минут. Если вы не регистрировались в Lunfy, проигнорируйте это письмо.
                </p>
              </div>
            </body></html>
            """.formatted(code);
    }

    public void sendPasswordResetCode(String toEmail, String code) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");

            helper.setFrom(fromAddress, fromName);
            helper.setTo(toEmail);
            helper.setSubject("Восстановление пароля Lunfy");
            helper.setText(buildResetHtml(code), true);

            mailSender.send(message);
        } catch (MessagingException | UnsupportedEncodingException e) {
            throw new RuntimeException("Не удалось отправить письмо", e);
        }
    }

    private String buildResetHtml(String code) {
        return """
        <!DOCTYPE html>
        <html><body style="margin:0;padding:40px;background:#0d0a1f;font-family:Arial,sans-serif;">
          <div style="max-width:480px;margin:0 auto;background:#1e1b3a;border-radius:16px;padding:40px;border:1px solid rgba(124,92,255,0.2);">
            <div style="font-size:14px;color:#a78bff;letter-spacing:0.15em;font-weight:600;margin-bottom:16px;">LUNFY</div>
            <h1 style="color:#f0eeff;font-size:24px;margin:0 0 12px 0;">Восстановление пароля</h1>
            <p style="color:#9a96c4;font-size:14px;line-height:1.5;margin:0 0 32px 0;">
              Введите этот код в приложении, чтобы установить новый пароль.
            </p>
            <div style="background:#0d0a1f;border:1px solid rgba(124,92,255,0.3);border-radius:12px;padding:24px;text-align:center;">
              <div style="font-size:36px;font-weight:900;color:#a78bff;letter-spacing:0.3em;font-family:monospace;">%s</div>
            </div>
            <p style="color:#5a5680;font-size:12px;margin:24px 0 0 0;">
              Код действителен 10 минут. Если вы не запрашивали восстановление пароля, проигнорируйте это письмо.
            </p>
          </div>
        </body></html>
        """.formatted(code);
    }
}