package org.example.model;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class NotificationScheduler {
    private final Connection connection;
    private final ScheduledExecutorService scheduler;
    private final String botToken;
    private final String chatId; // ID чата для уведомлений

    public NotificationScheduler(String botToken, String chatId, Connection connection) {
        this.botToken = botToken;
        this.chatId = chatId;
        this.connection = connection;
        this.scheduler = Executors.newScheduledThreadPool(1);
    }

    public void start() {
        // Проверяем каждую минуту
        scheduler.scheduleAtFixedRate(this::checkAndSendNotifications, 0, 1, TimeUnit.MINUTES);
        System.out.println("✅ Фоновая проверка уведомлений запущена!");
    }

    public void stop() {
        scheduler.shutdown();
        System.out.println("⏹️ Фоновая проверка уведомлений остановлена!");
    }

    private void checkAndSendNotifications() {
        try {
            if (connection == null || connection.isClosed()) {
                return;
            }

            String sql = "SELECT id, name, phone_number, time_to_contact FROM customers " +
                    "WHERE time_to_contact IS NOT NULL " +
                    "AND time_to_contact <= NOW() " +
                    "AND notified = false"; // Добавьте поле notified в таблицу

            try (PreparedStatement ps = connection.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {

                StringBuilder message = new StringBuilder("⏰ КЛИЕНТЫ ДЛЯ СВЯЗИ:\n\n");
                boolean hasNotifications = false;

                while (rs.next()) {
                    hasNotifications = true;
                    int id = rs.getInt("id");
                    String name = rs.getString("name");
                    String phone = rs.getString("phone_number");

                    message.append("Клиент #").append(id).append("\n")
                            .append("Имя: ").append(name).append("\n")
                            .append("Телефон: ").append(phone).append("\n")
                            .append("────────────────\n");

                    // Помечаем как уведомленного
                    markAsNotified(id);
                }

                if (hasNotifications) {
                    sendTelegramMessage(message.toString());
                }
            }

        } catch (SQLException e) {
            System.out.println("Ошибка при проверке уведомлений: " + e.getMessage());
        }
    }

    private void markAsNotified(int customerId) throws SQLException {
        String sql = "UPDATE customers SET notified = true WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            ps.executeUpdate();
        }
    }

    private void sendTelegramMessage(String text) {
        try {
            // Используем HTTP запрос для отправки сообщения
            String url = String.format(
                    "https://api.telegram.org/bot%s/sendMessage?chat_id=%s&text=%s",
                    botToken, chatId, java.net.URLEncoder.encode(text, "UTF-8")
            );

            java.net.URL obj = new java.net.URL(url);
            java.net.HttpURLConnection con = (java.net.HttpURLConnection) obj.openConnection();
            con.setRequestMethod("GET");

            int responseCode = con.getResponseCode();
            if (responseCode != 200) {
                System.out.println("Ошибка отправки уведомления: " + responseCode);
            }

        } catch (Exception e) {
            System.out.println("Ошибка при отправке в Telegram: " + e.getMessage());
        }
    }
}