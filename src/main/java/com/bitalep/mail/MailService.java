package com.bitalep.mail;

import com.bitalep.config.AppProperties;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final JavaMailSender mailSender;
    private final AppProperties props;
    private final HtmlMailTemplates templates;

    public MailService(JavaMailSender mailSender, AppProperties props, HtmlMailTemplates templates) {
        this.mailSender = mailSender;
        this.props = props;
        this.templates = templates;
    }

    @Async
    public void sendInvite(String to, String name, String surname, String password) {
        String html = templates.invite(HtmlMailTemplates.fullName(name, surname), password, loginUrl());
        send(to, "BiTalep hesabınız hazır", html);
    }

    @Async
    public void sendForgotPassword(String to, String name, String surname, String resetUrl) {
        String html = templates.forgotPassword(HtmlMailTemplates.fullName(name, surname), resetUrl);
        send(to, "BiTalep şifre sıfırlama", html);
    }

    @Async
    public void sendPasswordChanged(String to, String name, String surname) {
        String html = templates.passwordResetConfirm(HtmlMailTemplates.fullName(name, surname), loginUrl());
        send(to, "BiTalep şifreniz güncellendi", html);
    }

    @Async
    public void sendWelcome(String to, String name, String surname) {
        String html = templates.welcomeRegister(HtmlMailTemplates.fullName(name, surname), loginUrl());
        send(to, "BiTalep’e hoş geldiniz", html);
    }

    private String loginUrl() {
        String base = props.panelBaseUrl();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/login";
    }

    public String resetUrl(String token) {
        String base = props.panelBaseUrl();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/reset-password?token=" + token;
    }

    private void send(String to, String subject, String html) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(props.mail().from());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
            log.info("mail_sent to={} subject={}", redact(to), subject);
        } catch (Exception ex) {
            log.error("mail_failed to={} subject={}", redact(to), subject, ex);
        }
    }

    private static String redact(String email) {
        int at = email.indexOf('@');
        if (at <= 1) {
            return "***";
        }
        return email.charAt(0) + "***" + email.substring(at);
    }
}
