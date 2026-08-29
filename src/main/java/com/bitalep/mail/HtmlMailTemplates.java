package com.bitalep.mail;

import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class HtmlMailTemplates {

    private static final String NAVY = "#1e3a5f";
    private static final String TEAL = "#0d9488";
    private static final String MUTED = "#64748b";
    private static final String CARD_BG = "#f0fdfa";
    private static final String BORDER = "#ccfbf1";

    public String invite(String fullName, String password, String loginUrl) {
        return shell(
                "BiTalep hesabınız hazır",
                "Geçici şifrenizle panele giriş yapabilirsiniz.",
                greeting(fullName),
                "Yöneticiniz sizi BiTalep paneline ekledi. Aşağıdaki geçici şifre ile giriş yapın ve ilk fırsatta şifrenizi değiştirin.",
                "GEÇİCİ ŞİFRE",
                password,
                "Bu kutudaki değeri kopyalayıp giriş ekranına yapıştırabilirsiniz.",
                loginUrl,
                "Panele gir",
                "Güvenlik:",
                "Bu şifreyi kimseyle paylaşmayın. İlk girişten sonra profilinizden yeni bir şifre belirleyin.",
                "Bu ileti BiTalep tarafından gönderildi."
        );
    }

    public String forgotPassword(String fullName, String resetUrl) {
        return shell(
                "Şifre sıfırlama",
                "Şifrenizi yenilemek için bağlantıya tıklayın.",
                greeting(fullName),
                "Hesabınız için bir şifre sıfırlama isteği aldık. Bağlantı 30 dakika geçerlidir. Siz istemediyseniz bu iletiyi yok sayın.",
                "SIFIRLAMA",
                "Bağlantı 30 dakika içinde geçerliliğini yitirir.",
                "",
                resetUrl,
                "Şifremi yenile",
                "Güvenlik:",
                "Bağlantıyı yalnızca siz kullanmalısınız. Şifrenizi BiTalep çalışanları asla sormaz.",
                "Bu ileti BiTalep tarafından gönderildi."
        );
    }

    public String passwordResetConfirm(String fullName, String loginUrl) {
        return shell(
                "Şifreniz güncellendi",
                "Yeni şifrenizle giriş yapabilirsiniz.",
                greeting(fullName),
                "BiTalep şifreniz başarıyla değiştirildi. Panele yeni şifrenizle giriş yapın.",
                "GİRİŞ",
                "Hesabınız güvende.",
                "",
                loginUrl,
                "Giriş yap",
                "",
                "",
                "Bu ileti BiTalep tarafından gönderildi."
        );
    }

    public String welcomeRegister(String fullName, String loginUrl) {
        return shell(
                "BiTalep’e hoş geldiniz",
                "Firmanızın paneli hazır.",
                greeting(fullName),
                "Kayıt işleminiz tamamlandı. Yönetici hesabınızla talepleri yönetebilir, personel ekleyebilirsiniz.",
                "PANEL",
                "https://panel.bitalep.com.tr",
                "",
                loginUrl,
                "Panele git",
                "",
                "",
                "Bu ileti BiTalep tarafından gönderildi."
        );
    }

    private static String greeting(String fullName) {
        return "Merhaba " + escape(fullName) + ",";
    }

    private String shell(
            String subject,
            String preheader,
            String greeting,
            String intro,
            String label,
            String highlight,
            String hint,
            String ctaUrl,
            String cta,
            String warningTitle,
            String warning,
            String footer
    ) {
        String highlightBlock = highlight == null || highlight.isBlank() ? "" : """
              <p style="margin:0 0 8px;font-size:12px;font-weight:700;letter-spacing:0.08em;text-transform:uppercase;color:%s;">%s</p>
              <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background:%s;border:1px solid %s;border-radius:12px;">
                <tr>
                  <td style="padding:20px 24px;text-align:center;">
                    <p style="margin:0;font-size:22px;line-height:1.4;font-weight:700;letter-spacing:0.04em;color:%s;font-family:ui-monospace,SFMono-Regular,Menlo,Consolas,monospace;">%s</p>
                    %s
                  </td>
                </tr>
              </table>
            """.formatted(TEAL, escape(label), CARD_BG, BORDER, NAVY, escape(highlight),
                hint.isBlank() ? "" : "<p style=\"margin:10px 0 0;font-size:12px;color:%s;\">%s</p>".formatted(MUTED, escape(hint)));

        String warn = warningTitle.isBlank() ? "" : """
              <p style="margin:28px 0 0;font-size:14px;line-height:1.6;"><strong style="color:%s;">%s</strong> %s</p>
            """.formatted(NAVY, escape(warningTitle), escape(warning));

        return """
            <!DOCTYPE html>
            <html lang="tr">
            <head>
              <meta charset="UTF-8" />
              <meta name="viewport" content="width=device-width, initial-scale=1.0" />
              <title>%s</title>
            </head>
            <body style="margin:0;padding:0;background:#f1f5f9;font-family:Inter,Segoe UI,Helvetica,Arial,sans-serif;color:#0f172a;">
              <div style="display:none;max-height:0;overflow:hidden;">%s</div>
              <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background:#f1f5f9;padding:32px 16px;">
                <tr>
                  <td align="center">
                    <table role="presentation" width="560" cellpadding="0" cellspacing="0" style="max-width:560px;width:100%%;background:#ffffff;border-radius:16px;overflow:hidden;box-shadow:0 8px 24px rgba(15,23,42,0.08);">
                      <tr>
                        <td style="background:%s;padding:28px 32px;text-align:center;">
                          <p style="margin:0;font-size:22px;font-weight:800;letter-spacing:0.04em;color:#ffffff;">BiTalep</p>
                        </td>
                      </tr>
                      <tr>
                        <td style="padding:36px 32px 8px;">
                          <p style="margin:0 0 12px;font-size:20px;line-height:1.4;font-weight:700;color:%s;">%s</p>
                          <p style="margin:0 0 24px;font-size:15px;line-height:1.6;color:#334155;">%s</p>
                          %s
                          <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="margin-top:24px;">
                            <tr>
                              <td align="center">
                                <a href="%s" style="display:inline-block;background:%s;color:#ffffff;text-decoration:none;font-weight:700;font-size:14px;padding:12px 28px;border-radius:8px;">%s</a>
                              </td>
                            </tr>
                          </table>
                          %s
                        </td>
                      </tr>
                      <tr>
                        <td style="padding:24px 32px 32px;font-size:12px;line-height:1.5;color:%s;border-top:1px solid #e2e8f0;">
                          %s
                        </td>
                      </tr>
                    </table>
                  </td>
                </tr>
              </table>
            </body>
            </html>
            """.formatted(
                escape(subject),
                escape(preheader),
                NAVY,
                NAVY,
                greeting,
                escape(intro),
                highlightBlock,
                escape(ctaUrl),
                TEAL,
                escape(cta),
                warn,
                MUTED,
                escape(footer)
        );
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    public static String fullName(String name, String surname) {
        return (name + " " + surname).trim();
    }

    public static Locale tr() {
        return Locale.forLanguageTag("tr-TR");
    }
}
