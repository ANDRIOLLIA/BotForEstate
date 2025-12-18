package org.example.model;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class Bot extends TelegramLongPollingBot {
    private Connection connection;

    private boolean isCreateCustomer = false;
    private boolean isWaitingName = false;
    private boolean isWaitingPhone = false;
    private boolean isWaitingCity = false;
    private boolean isWaitingType = false;
    private boolean isWaitingId = false;

    private String name;
    private Long phoneNumber;
    private String cityForBuyEstate;
    private String typeOfEstate;
    private int customerId;
    private String currentUserId; // ID текущего пользователя (риелтора)

    Map<Integer, Customers> mapCustomer = new HashMap<>();

    // Форматтер для красивого вывода даты
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    // Кнопка для создания клиента
    InlineKeyboardButton buttonForCreateCustomer = InlineKeyboardButton.builder()
            .text("✨ Добавить нового клиента")
            .callbackData("create_new_client")
            .build();

    // Кнопка для просмотра клиентов, которым написали
    InlineKeyboardButton buttonForQuestionableCustomers = InlineKeyboardButton.builder()
            .text("📋 Клиенты под вопросом")
            .callbackData("questionable_customers")
            .build();

    // Кнопка для просмотра клиентов, работа с которыми закончена
    InlineKeyboardButton buttonForCheckEndedWorkCustomers = InlineKeyboardButton.builder()
            .text("📁 Архив клиентов")
            .callbackData("ended_work_customers")
            .build();

    InlineKeyboardButton buttonForReturnBack = InlineKeyboardButton.builder()
            .text("🔙 Назад")
            .callbackData("back")
            .build();

    InlineKeyboardButton buttonForInstruction = InlineKeyboardButton.builder()
            .text("\uD83D\uDCD6Инструкция по работе с ботом")
            .callbackData("instruction")
            .build();

    //Клавиатура для главного меню
    InlineKeyboardMarkup keyboardForMainMenu = InlineKeyboardMarkup.builder()
            .keyboardRow(List.of(buttonForCreateCustomer))
            .keyboardRow(List.of(buttonForQuestionableCustomers))
            .keyboardRow(List.of(buttonForCheckEndedWorkCustomers))
            .keyboardRow(List.of(buttonForInstruction))
            .build();

    InlineKeyboardMarkup backboard = InlineKeyboardMarkup.builder()
            .keyboardRow(List.of(buttonForReturnBack))
            .build();

    InlineKeyboardButton buttonForSetTime = InlineKeyboardButton.builder()
            .text("⏰ Выбрать время")
            .callbackData("choice_time")
            .build();

    InlineKeyboardMarkup keyboardForTime = InlineKeyboardMarkup.builder()
            .keyboardRow(List.of(buttonForSetTime))
            .keyboardRow(List.of(buttonForReturnBack))
            .build();

    InlineKeyboardButton buttonFor1h = InlineKeyboardButton.builder()
            .text("⏰ 1 час")
            .callbackData("1_hour")
            .build();

    InlineKeyboardButton buttonFor2h = InlineKeyboardButton.builder()
            .text("⏰ 2 часа")
            .callbackData("2_hours")
            .build();

    InlineKeyboardButton buttonFor3h = InlineKeyboardButton.builder()
            .text("⏰ 3 часа")
            .callbackData("3_hours")
            .build();

    InlineKeyboardButton buttonFor24h = InlineKeyboardButton.builder()
            .text("🌙 24 часа")
            .callbackData("24_hours")
            .build();

    InlineKeyboardButton buttonFor2Days = InlineKeyboardButton.builder()
            .text("📅 2 дня")
            .callbackData("2_days")
            .build();

    InlineKeyboardMarkup keyboardForChooseTime = InlineKeyboardMarkup.builder()
            .keyboardRow(List.of(buttonFor1h, buttonFor2h))
            .keyboardRow(List.of(buttonFor3h, buttonFor24h))
            .keyboardRow(List.of(buttonFor2Days))
            .keyboardRow(List.of(buttonForReturnBack))
            .build();

    // Фоновый поток для проверки уведомлений
    private Thread notificationThread;
    private volatile boolean notificationThreadRunning = false;

    public void initDBConnection() {
        try {
            String url = "jdbc:mysql://localhost:3306/estate_bot";
            String username = "root";
            String password = "andrEj0077";

            connection = DriverManager.getConnection(url, username, password);
            System.out.println("✅ Соединение с БД установлено!");

            // Проверяем и добавляем поля если их нет
            checkAndAddDatabaseFields();

            // Проверяем существование таблицы end_customers
            checkEndCustomersTable();

            // Запускаем фоновую проверку уведомлений
            startBackgroundNotificationChecker();

        } catch (Exception ex) {
            System.out.println("❌ Ошибка подключения к БД: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void checkAndAddDatabaseFields() {
        try {
            DatabaseMetaData metaData = connection.getMetaData();

            // Проверяем поле notified
            ResultSet columns = metaData.getColumns(null, null, "customers", "notified");
            if (!columns.next()) {
                String sql = "ALTER TABLE customers ADD COLUMN notified BOOLEAN DEFAULT FALSE";
                try (Statement stmt = connection.createStatement()) {
                    stmt.executeUpdate(sql);
                    System.out.println("✅ Добавлено поле 'notified' в таблицу customers");
                }
            }
            columns.close();

            // Проверяем поле id_realtor
            columns = metaData.getColumns(null, null, "customers", "id_realtor");
            if (!columns.next()) {
                String sql = "ALTER TABLE customers ADD COLUMN id_realtor VARCHAR(50) DEFAULT NULL";
                try (Statement stmt = connection.createStatement()) {
                    stmt.executeUpdate(sql);
                    System.out.println("✅ Добавлено поле 'id_realtor' в таблицу customers");
                }
            }
            columns.close();

            // Проверяем поле id_realtor в end_customers
            columns = metaData.getColumns(null, null, "end_customers", "id_realtor");
            if (!columns.next()) {
                String sql = "ALTER TABLE end_customers ADD COLUMN id_realtor VARCHAR(50) DEFAULT NULL";
                try (Statement stmt = connection.createStatement()) {
                    stmt.executeUpdate(sql);
                    System.out.println("✅ Добавлено поле 'id_realtor' в таблицу end_customers");
                }
            }
            columns.close();

        } catch (SQLException e) {
            System.out.println("❌ Ошибка при проверке полей БД: " + e.getMessage());
        }
    }

    private void checkEndCustomersTable() {
        try {
            DatabaseMetaData metaData = connection.getMetaData();
            ResultSet tables = metaData.getTables(null, null, "end_customers", null);
            if (!tables.next()) {
                // Таблица не существует, создаем ее
                String sql = "CREATE TABLE IF NOT EXISTS end_customers (" +
                        "id INT AUTO_INCREMENT PRIMARY KEY, " +
                        "name TEXT, " +
                        "phone_number VARCHAR(15), " +
                        "city_for_buy_estate VARCHAR(30), " +
                        "type_of_estate VARCHAR(50), " +
                        "id_realtor VARCHAR(50)" +
                        ")";
                try (Statement stmt = connection.createStatement()) {
                    stmt.executeUpdate(sql);
                    System.out.println("✅ Таблица 'end_customers' создана");
                }
            }
            tables.close();
        } catch (SQLException e) {
            System.out.println("❌ Ошибка при проверке таблицы end_customers: " + e.getMessage());
        }
    }

    private void startBackgroundNotificationChecker() {
        if (notificationThreadRunning) {
            return;
        }

        notificationThreadRunning = true;
        notificationThread = new Thread(() -> {
            System.out.println("🚀 Фоновая проверка уведомлений запущена!");

            while (notificationThreadRunning) {
                try {
                    // Проверяем каждые 30 секунд
                    Thread.sleep(30000);

                    // Проверяем уведомления
                    checkAndSendAutomaticNotifications();

                } catch (InterruptedException e) {
                    System.out.println("⏹️ Поток уведомлений прерван");
                    break;
                } catch (Exception e) {
                    System.out.println("⚠️ Ошибка в фоновой проверке: " + e.getMessage());
                }
            }

            System.out.println("⏹️ Фоновая проверка уведомлений остановлена");
        });

        notificationThread.setDaemon(true);
        notificationThread.start();
    }

    private void checkAndSendAutomaticNotifications() {
        if (connection == null) {
            return;
        }

        try {
            // Получаем всех клиентов, у которых время наступило и не отправлено уведомление
            String sql = "SELECT id, name, phone_number, city_for_buy_estate, type_of_estate, time_to_contact, id_realtor " +
                    "FROM customers " +
                    "WHERE time_to_contact IS NOT NULL " +
                    "AND time_to_contact <= NOW() " +
                    "AND notified = FALSE";

            try (PreparedStatement ps = connection.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {

                // Группируем клиентов по риелторам
                Map<String, List<CustomerData>> realtorsCustomers = new HashMap<>();

                while (rs.next()) {
                    String realtorId = rs.getString("id_realtor");
                    if (realtorId == null) continue;

                    int id = rs.getInt("id");
                    String name = rs.getString("name");
                    String phone = rs.getString("phone_number");
                    String city = rs.getString("city_for_buy_estate");
                    String type = rs.getString("type_of_estate");

                    // Преобразуем тип недвижимости
                    type = getEstateTypeInRussian(type);

                    CustomerData customerData = new CustomerData(id, name, phone, city, type, realtorId);

                    // Добавляем клиента в список соответствующего риелтора
                    realtorsCustomers.computeIfAbsent(realtorId, k -> new ArrayList<>()).add(customerData);
                }

                // Отправляем уведомления каждому риелтору
                for (Map.Entry<String, List<CustomerData>> entry : realtorsCustomers.entrySet()) {
                    String realtorId = entry.getKey();
                    List<CustomerData> customers = entry.getValue();

                    sendNotificationToRealtor(realtorId, customers);
                }

            }

        } catch (SQLException e) {
            System.out.println("❌ Ошибка при автоматической проверке уведомлений: " + e.getMessage());
        }
    }

    private void sendNotificationToRealtor(String realtorId, List<CustomerData> customers) {
        if (customers.isEmpty()) return;

        StringBuilder message = new StringBuilder();
        message.append("🔔 *ВНИМАНИЕ! ПОРА СВЯЗАТЬСЯ С КЛИЕНТАМИ*\n\n");
        message.append("⏰ Время для связи наступило у следующих клиентов:\n\n");
        message.append("══════════════════════════════\n");

        List<Integer> customerIds = new ArrayList<>();

        for (CustomerData customer : customers) {
            message.append("👤 *Клиент #").append(customer.id).append("*\n");
            message.append("   📝 Имя: ").append(customer.name).append("\n");
            message.append("   📱 Телефон: ").append(customer.phone).append("\n");
            message.append("   🏙️ Город: ").append(customer.city).append("\n");
            message.append("   🏠 Тип: ").append(customer.type).append("\n");
            message.append("   ──────────────────\n");

            customerIds.add(customer.id);
        }

        message.append("\n✅ Клиенты будут автоматически перемещены в архив\n");
        message.append("💡 Свяжитесь с ними как можно скорее!");

        try {
            // Отправляем уведомление риелтору
            SendMessage notification = SendMessage.builder()
                    .chatId(realtorId)
                    .text(message.toString())
                    .parseMode("Markdown")
                    .build();

            execute(notification);
            System.out.println("✅ Уведомление отправлено риелтору: " + realtorId + " (" + customers.size() + " клиентов)");

            // Перемещаем клиентов в архив
            moveCustomersToEndTable(customers);

        } catch (Exception e) {
            System.out.println("❌ Ошибка отправки уведомления риелтору " + realtorId + ": " + e.getMessage());
        }
    }

    // Вспомогательный класс для хранения данных клиента
    private static class CustomerData {
        int id;
        String name;
        String phone;
        String city;
        String type;
        String realtorId;

        CustomerData(int id, String name, String phone, String city, String type, String realtorId) {
            this.id = id;
            this.name = name;
            this.phone = phone;
            this.city = city;
            this.type = type;
            this.realtorId = realtorId;
        }
    }

    private void moveCustomersToEndTable(List<CustomerData> customersToMove) {
        if (customersToMove.isEmpty()) {
            return;
        }

        Connection conn = null;
        try {
            conn = connection;
            conn.setAutoCommit(false);

            // 1. Копируем клиентов в таблицу end_customers
            String insertSql = "INSERT INTO end_customers (name, phone_number, city_for_buy_estate, type_of_estate, id_realtor) " +
                    "VALUES (?, ?, ?, ?, ?)";

            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                for (CustomerData customer : customersToMove) {
                    insertStmt.setString(1, customer.name);
                    insertStmt.setString(2, customer.phone);
                    insertStmt.setString(3, customer.city);
                    insertStmt.setString(4, customer.type);
                    insertStmt.setString(5, customer.realtorId);
                    insertStmt.addBatch();
                }
                insertStmt.executeBatch();
            }

            // 2. Удаляем клиентов из основной таблицы
            String deleteSql = "DELETE FROM customers WHERE id = ?";
            try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {
                for (CustomerData customer : customersToMove) {
                    deleteStmt.setInt(1, customer.id);
                    deleteStmt.addBatch();
                }
                deleteStmt.executeBatch();
            }

            // 3. Фиксируем транзакцию
            conn.commit();
            System.out.println("✅ Перемещено клиентов в архив: " + customersToMove.size());

        } catch (SQLException e) {
            System.out.println("❌ Ошибка при перемещении клиентов: " + e.getMessage());
            try {
                if (conn != null) {
                    conn.rollback();
                }
            } catch (SQLException ex) {
                System.out.println("❌ Ошибка при откате транзакции: " + ex.getMessage());
            }
        } finally {
            try {
                if (conn != null) {
                    conn.setAutoCommit(true);
                }
            } catch (SQLException e) {
                System.out.println("⚠️ Ошибка при восстановлении autocommit: " + e.getMessage());
            }
        }
    }

    private void mainMenu(SendMessage sendMessage) {
        sendMessage.setText("🏠 *Добро пожаловать в Estate Manager Bot!*\n\n" +
                "Я ваш личный помощник в работе с клиентами по недвижимости. \n" +
                "С моей помощью вы сможете:\n\n" +
                "✨ Создавать карточки клиентов\n" +
                "⏰ Настраивать напоминания о связи\n" +
                "📋 Управлять списком клиентов\n" +
                "🔔 Получать автоматические уведомления\n\n" +
                "👇 *Выберите действие в меню ниже:*");
        sendMessage.setParseMode("Markdown");
        sendMessage.setReplyMarkup(keyboardForMainMenu);
        try {
            execute(sendMessage);
        } catch (Exception ex) {
            System.out.println("❌ Ошибка при отправке главного меню: " + ex.getMessage());
        }
    }

    public void forWorkWithText(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String chatId = update.getMessage().getChatId().toString();
            String textMessage = update.getMessage().getText();

            // Сохраняем ID пользователя
            if (currentUserId == null) {
                currentUserId = chatId;
            }

            SendMessage sendMessage = SendMessage.builder()
                    .chatId(chatId)
                    .text("")
                    .parseMode("Markdown")
                    .build();

            System.out.println("📝 Текст от пользователя " + chatId + ": " + textMessage);

            if (textMessage.equals("/start")) {
                isCreateCustomer = false;
                isWaitingName = false;
                isWaitingPhone = false;
                isWaitingCity = false;
                isWaitingType = false;
                isWaitingId = false;
                mainMenu(sendMessage);
                return;
            }

            if (isCreateCustomer && isWaitingName) {
                name = textMessage;
                isWaitingName = false;
                isWaitingPhone = true;
                sendMessage.setText("📱 *Отлично! Теперь введите номер телефона клиента:*\n\n" +
                        "_Формат: начинайте с 8 (например: 89991234567)_");
                try {
                    execute(sendMessage);
                } catch (Exception ex) {
                    System.out.println("❌ Ошибка: " + ex.getMessage());
                }
            } else if (isCreateCustomer && isWaitingPhone) {
                try {
                    phoneNumber = Long.parseLong(textMessage);
                    isWaitingPhone = false;
                    isWaitingCity = true;
                    sendMessage.setText("🏙️ *Введите город, в котором клиент хочет купить недвижимость:*\n\n" +
                            "_Например: Москва, Санкт-Петербург, Сочи_");
                    try {
                        execute(sendMessage);
                    } catch (Exception ex) {
                        System.out.println("❌ Ошибка: " + ex.getMessage());
                    }
                } catch (NumberFormatException e) {
                    sendMessage.setText("❌ *Неверный формат номера!*\n\n" +
                            "Пожалуйста, введите номер телефона в правильном формате:\n" +
                            "• Только цифры\n" +
                            "• Начинайте с 8\n" +
                            "• Без пробелов и дефисов\n\n" +
                            "_Пример: 89991234567_");
                    try {
                        execute(sendMessage);
                    } catch (Exception ex) {
                        System.out.println("❌ Ошибка: " + ex.getMessage());
                    }
                }
            } else if (isCreateCustomer && isWaitingCity) {
                cityForBuyEstate = textMessage;
                isWaitingCity = false;
                isWaitingType = true;

                InlineKeyboardButton atelier = InlineKeyboardButton.builder()
                        .text("🏢 Студия")
                        .callbackData("ATELIER")
                        .build();
                InlineKeyboardButton one_room = InlineKeyboardButton.builder()
                        .text("🏠 1-комнатная")
                        .callbackData("ONE_ROOM_APARTMENT")
                        .build();
                InlineKeyboardButton two_room = InlineKeyboardButton.builder()
                        .text("🏠 2-комнатная")
                        .callbackData("TWO_ROOM_APARTMENT")
                        .build();
                InlineKeyboardButton three_room = InlineKeyboardButton.builder()
                        .text("🏠 3-комнатная")
                        .callbackData("THREE_ROOM_APARTMENT")
                        .build();
                InlineKeyboardButton house = InlineKeyboardButton.builder()
                        .text("🏡 Дом")
                        .callbackData("HOUSE")
                        .build();
                InlineKeyboardMarkup keyboardForChooseType = InlineKeyboardMarkup.builder()
                        .keyboardRow(List.of(atelier, one_room))
                        .keyboardRow(List.of(two_room, three_room))
                        .keyboardRow(List.of(house))
                        .build();

                sendMessage.setText("🏘️ *Выберите тип недвижимости, которую ищет клиент:*");
                sendMessage.setReplyMarkup(keyboardForChooseType);
                try {
                    execute(sendMessage);
                } catch (Exception ex) {
                    System.out.println("❌ Ошибка: " + ex.getMessage());
                }
            } else if (isWaitingId) {
                try {
                    customerId = Integer.parseInt(textMessage);
                    Customers selectedCustomer = mapCustomer.get(customerId);

                    if (selectedCustomer != null) {
                        sendMessage.setText("👤 *Выбран клиент:*\n\n" +
                                formatCustomerForDisplay(selectedCustomer, customerId) +
                                "\n\n⏰ *Выберите действие:*");
                        sendMessage.setReplyMarkup(keyboardForTime);
                    } else {
                        sendMessage.setText("❌ *Клиент не найден!*\n\n" +
                                "Пожалуйста, введите правильный ID клиента из списка.");
                    }

                    try {
                        execute(sendMessage);
                    } catch (Exception ex) {
                        System.out.println("❌ Ошибка: " + ex.getMessage());
                    }

                } catch (NumberFormatException e) {
                    sendMessage.setText("❌ *Неверный формат ID!*\n\n" +
                            "Пожалуйста, введите числовой ID клиента.");
                    try {
                        execute(sendMessage);
                    } catch (Exception ex) {
                        System.out.println("❌ Ошибка: " + ex.getMessage());
                    }
                }
            }
        }
    }

    public void forWorkWithButtons(Update update) {
        if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();
            String chatId = update.getCallbackQuery().getMessage().getChatId().toString();
            Integer messageId = update.getCallbackQuery().getMessage().getMessageId();

            // Сохраняем ID пользователя
            currentUserId = chatId;

            System.out.println("🔄 Callback от пользователя " + chatId + ": " + callbackData);

            SendMessage sendMessage = SendMessage.builder()
                    .chatId(chatId)
                    .text("")
                    .parseMode("Markdown")
                    .build();

            if (callbackData.equals(buttonForCreateCustomer.getCallbackData())) {
                isCreateCustomer = true;
                isWaitingName = true;

                sendMessage.setText("✨ *Создание нового клиента*\n\n" +
                        "Отлично! Давайте создадим карточку клиента.\n" +
                        "Я задам несколько вопросов, чтобы собрать всю необходимую информацию.\n\n" +
                        "📝 *Введите имя клиента:*");
                try {
                    execute(sendMessage);
                } catch (Exception ex) {
                    System.out.println("❌ Ошибка: " + ex.getMessage());
                }

            } else if (callbackData.equals(buttonForQuestionableCustomers.getCallbackData())) {
                String finalMessage = getMyCustomersFromDB(chatId);
                sendMessage.setText(finalMessage + "\n\n👇 *Введите ID клиента для работы с ним:*");
                isWaitingId = true;

                try {
                    execute(sendMessage);
                } catch (Exception ex) {
                    System.out.println("❌ Ошибка: " + ex.getMessage());
                }

            } else if (callbackData.equals("ATELIER") ||
                    callbackData.equals("ONE_ROOM_APARTMENT") ||
                    callbackData.equals("TWO_ROOM_APARTMENT") ||
                    callbackData.equals("THREE_ROOM_APARTMENT") ||
                    callbackData.equals("HOUSE")) {

                if (isCreateCustomer && isWaitingType) {
                    typeOfEstate = callbackData;
                    isWaitingType = false;
                    isCreateCustomer = false;

                    String sql = "INSERT INTO customers (name, phone_number, city_for_buy_estate, type_of_estate, id_realtor) " +
                            "VALUES (?, ?, ?, ?, ?)";

                    String resultMessage = "";

                    try {
                        if (connection != null && !connection.isClosed()) {
                            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                                ps.setString(1, name);
                                ps.setString(2, phoneNumber != null ? phoneNumber.toString() : null);
                                ps.setString(3, cityForBuyEstate);
                                ps.setString(4, typeOfEstate);
                                ps.setString(5, chatId); // Сохраняем ID риелтора

                                int rowsAffected = ps.executeUpdate();
                                if (rowsAffected > 0) {
                                    resultMessage = "✅ *Клиент успешно создан!*\n\n";
                                } else {
                                    resultMessage = "❌ *Ошибка при добавлении клиента!*\n\n";
                                }
                            }
                        } else {
                            resultMessage = "❌ *Нет соединения с базой данных!*\n\n";
                        }
                    } catch (SQLException ex) {
                        System.out.println("❌ Ошибка при добавлении клиента: " + ex.getMessage());
                        if (ex.getMessage().contains("Duplicate entry")) {
                            resultMessage = "❌ *Клиент с таким номером телефона уже существует!*\n\n";
                        } else {
                            resultMessage = "❌ *Ошибка базы данных!*\n\n";
                        }
                    } catch (Exception ex) {
                        System.out.println("❌ Ошибка: " + ex.getMessage());
                        resultMessage = "❌ *Ошибка!*\n\n";
                    }

                    // Получаем тип недвижимости на русском
                    String typeInRussian = getEstateTypeInRussian(typeOfEstate);

                    EditMessageText finalMessage = EditMessageText.builder()
                            .chatId(chatId)
                            .messageId(messageId)
                            .parseMode("Markdown")
                            .text(resultMessage +
                                    "✓ Данные сохранены в базе\n" +
                                    "✓ Клиент добавлен в ваш список\n" +
                                    "✓ Готов к работе!\n\n" +
                                    "📊 *Карточка клиента:*\n" +
                                    "────────────────\n" +
                                    "👤 Имя: " + name + "\n" +
                                    "📱 Телефон: " + phoneNumber + "\n" +
                                    "🏙️ Город: " + cityForBuyEstate + "\n" +
                                    "🏠 Тип: " + typeInRussian + "\n" +
                                    "────────────────\n\n" +
                                    "👇 *Выберите дальнейшее действие:*")
                            .replyMarkup(keyboardForMainMenu)
                            .build();

                    try {
                        execute(finalMessage);
                    } catch (Exception ex) {
                        System.out.println("❌ Ошибка: " + ex.getMessage());
                    }
                }
            } else if (callbackData.equals(buttonForReturnBack.getCallbackData())) {
                mainMenu(sendMessage);
            } else if (callbackData.equals(buttonForSetTime.getCallbackData())) {
                sendMessage.setText("⏳ *Настройка напоминания*\n\n" +
                        "Выберите, через сколько времени нужно связаться с клиентом повторно:\n\n" +
                        "⏰ 1 час - Для срочных клиентов\n" +
                        "⏰ 2 часа - Для важных вопросов\n" +
                        "⏰ 3 часа - Стандартное время\n" +
                        "🌙 24 часа - На следующий день\n" +
                        "📅 2 дня - Через пару дней\n\n" +
                        "👇 *Выберите вариант:*");
                sendMessage.setReplyMarkup(keyboardForChooseTime);
                try {
                    execute(sendMessage);
                } catch (Exception ex) {
                    System.out.println("❌ Ошибка: " + ex.getMessage());
                }
            } else if (callbackData.equals(buttonFor1h.getCallbackData())) {
                handleTimeSelection(1, "1 час", chatId, sendMessage);
            } else if (callbackData.equals(buttonFor2h.getCallbackData())) {
                handleTimeSelection(2, "2 часа", chatId, sendMessage);
            } else if (callbackData.equals(buttonFor3h.getCallbackData())) {
                handleTimeSelection(3, "3 часа", chatId, sendMessage);
            } else if (callbackData.equals(buttonFor24h.getCallbackData())) {
                handleTimeSelection(24, "24 часа", chatId, sendMessage);
            } else if (callbackData.equals(buttonFor2Days.getCallbackData())) {
                handleTimeSelection(48, "2 дня", chatId, sendMessage);
            } else if (callbackData.equals(buttonForCheckEndedWorkCustomers.getCallbackData())) {
                String endedCustomers = getMyEndedCustomersFromDB(chatId);
                sendMessage.setText(endedCustomers);
                sendMessage.setReplyMarkup(keyboardForMainMenu);
                try {
                    execute(sendMessage);
                } catch (Exception ex) {
                    System.out.println("❌ Ошибка: " + ex.getMessage());
                }
            } else if(callbackData.equals(buttonForInstruction.getCallbackData())){
                String instruction = "\uD83D\uDCCB ИНСТРУКЦИЯ\n" +
                        "\n" +
                        "**\uD83D\uDE80 Старт:** /start → Главное меню\n" +
                        "\n" +
                        "**\uD83D\uDCDD Создать клиента:**\n" +
                        "1. Выберите \"Добавить клиента\"\n" +
                        "2. Введите: имя, телефон, город\n" +
                        "3. Выберите тип недвижимости\n" +
                        "4. Клиент сохранен ✅\n" +
                        "\n" +
                        "**⏰ Напоминания:**\n" +
                        "1. Выберите клиента из списка\n" +
                        "2. Нажмите \"Установить время\"\n" +
                        "3. Выберите интервал (1ч-2дня)\n" +
                        "4. Бот напомнит автоматически \uD83D\uDD14\n" +
                        "\n" +
                        "**\uD83D\uDCC1 Архив:** Клиенты перемещаются автоматически после уведомлений\n" +
                        "\n" +
                        "**\uD83D\uDD10 Важно:** Каждый риелтор видит только своих клиентов\n";
                sendMessage.setText(instruction);
                sendMessage.setReplyMarkup(backboard);
                try {
                    execute(sendMessage);
                }catch (Exception ex){
                    System.out.println(ex.getMessage());
                }
            }
        }
    }

    private void handleTimeSelection(int hours, String timeText, String chatId, SendMessage sendMessage) {
        if (updateCustomerTimes(customerId, hours, chatId)) {
            sendMessage.setText("✅ *Напоминание установлено!*\n\n" +
                    "🔔 Я напомню вам через " + timeText + "\n" +
                    "💡 Уведомление придет автоматически\n\n" +
                    "👇 *Возвращаемся в главное меню:*");
        } else {
            sendMessage.setText("❌ *Ошибка при установке времени!*\n\n" +
                    "Пожалуйста, попробуйте еще раз.");
        }
        sendMessage.setReplyMarkup(keyboardForMainMenu);
        try {
            execute(sendMessage);
        } catch (Exception ex) {
            System.out.println("❌ Ошибка: " + ex.getMessage());
        }
    }

    private boolean updateCustomerTimes(int customerId, int hoursToAdd, String realtorId) {
        if (connection == null || customerId <= 0) {
            return false;
        }

        String sql = "UPDATE customers SET " +
                "time_of_create_query = IFNULL(time_of_create_query, NOW()), " +
                "time_to_contact = DATE_ADD(NOW(), INTERVAL ? SECOND), " +
                "notified = FALSE " +
                "WHERE id = ? AND id_realtor = ?"; // Обновляем только клиентов текущего риелтора

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, hoursToAdd);
            ps.setInt(2, customerId);
            ps.setString(3, realtorId);
            int rowsUpdated = ps.executeUpdate();

            return rowsUpdated > 0;

        } catch (SQLException e) {
            System.out.println("❌ Ошибка SQL при обновлении времени: " + e.getMessage());
            return false;
        }
    }

    private String getMyCustomersFromDB(String realtorId) {
        StringBuilder result = new StringBuilder();
        String sql = "SELECT id, name, phone_number, city_for_buy_estate, type_of_estate, " +
                "time_of_create_query, time_to_contact " +
                "FROM customers " +
                "WHERE id_realtor = ? " +
                "ORDER BY id ASC";

        try {
            if (connection != null && !connection.isClosed()) {
                try (PreparedStatement ps = connection.prepareStatement(sql)) {
                    ps.setString(1, realtorId);

                    try (ResultSet rs = ps.executeQuery()) {
                        result.append("📋 *Клиенты под вопросом*\n\n");
                        result.append("Здесь вы видите всех ваших клиентов.\n");
                        result.append("Для каждого клиента указано время следующей связи.\n\n");
                        result.append("══════════════════════════════\n\n");

                        int count = 0;
                        mapCustomer.clear(); // Очищаем map перед заполнением

                        while (rs.next()) {
                            count++;
                            int id = rs.getInt("id");
                            String name = rs.getString("name");
                            String phone = rs.getString("phone_number");
                            String city = rs.getString("city_for_buy_estate");
                            String type = rs.getString("type_of_estate");
                            type = getEstateTypeInRussian(type);

                            LocalDateTime current = null;
                            LocalDateTime toContact = null;

                            Timestamp currentTs = rs.getTimestamp("time_of_create_query");
                            if (currentTs != null) {
                                current = currentTs.toLocalDateTime();
                            }

                            Timestamp toContactTs = rs.getTimestamp("time_to_contact");
                            if (toContactTs != null) {
                                toContact = toContactTs.toLocalDateTime();
                            }

                            Customers customer = new Customers(name,
                                    phone != null ? Long.parseLong(phone) : 0,
                                    city, type, current, toContact);

                            mapCustomer.put(id, customer);

                            result.append("👤 *Клиент #").append(id).append("*\n");
                            result.append("   📝 Имя: ").append(name).append("\n");
                            result.append("   📱 Телефон: ").append(phone).append("\n");
                            result.append("   🏙️ Город: ").append(city).append("\n");
                            result.append("   🏠 Тип: ").append(type).append("\n");

                            if (current != null) {
                                result.append("   📅 Создан: ").append(current.format(DATE_FORMATTER)).append("\n");
                            }
                            if (toContact != null) {
                                result.append("   ⏰ Связь: ").append(toContact.format(DATE_FORMATTER)).append("\n");
                            }
                            result.append("\n   ──────────────────\n\n");
                        }

                        if (count == 0) {
                            result.append("📭 *У вас пока нет активных клиентов.*\n\n");
                            result.append("✨ Начните с создания первого клиента!");
                        } else {
                            result.append("\n📊 *Всего активных клиентов: ").append(count).append("*");
                        }
                    }
                }
            } else {
                result.append("❌ *Нет соединения с базой данных!*");
            }
        } catch (SQLException ex) {
            System.out.println("❌ Ошибка при получении клиентов: " + ex.getMessage());
            result.append("❌ *Ошибка при получении данных из базы!*");
        } catch (NumberFormatException e) {
            System.out.println("❌ Ошибка преобразования номера телефона: " + e.getMessage());
            result.append("❌ *Ошибка в данных клиента!*");
        }

        return result.toString();
    }

    private String getMyEndedCustomersFromDB(String realtorId) {
        StringBuilder result = new StringBuilder();
        String sql = "SELECT id, name, phone_number, city_for_buy_estate, type_of_estate " +
                "FROM end_customers " +
                "WHERE id_realtor = ? " +
                "ORDER BY id ASC";

        try {
            if (connection != null && !connection.isClosed()) {
                try (PreparedStatement ps = connection.prepareStatement(sql)) {
                    ps.setString(1, realtorId);

                    try (ResultSet rs = ps.executeQuery()) {
                        result.append("📁 *Мой архив клиентов*\n\n");
                        result.append("Здесь хранятся клиенты, с которыми работа была завершена:\n\n");
                        result.append("✓ Уведомления отправлены\n");
                        result.append("✓ Все задачи выполнены\n");
                        result.append("✓ Клиенты перемещены в архив\n\n");
                        result.append("══════════════════════════════\n\n");

                        int count = 0;
                        while (rs.next()) {
                            count++;
                            String name = rs.getString("name");
                            String phone = rs.getString("phone_number");
                            String city = rs.getString("city_for_buy_estate");
                            String type = rs.getString("type_of_estate");
                            type = getEstateTypeInRussian(type);

                            result.append("👤 *Клиент #").append(count).append("*\n");
                            result.append("   📝 Имя: ").append(name).append("\n");
                            result.append("   📱 Телефон: ").append(phone).append("\n");
                            result.append("   🏙️ Город: ").append(city).append("\n");
                            result.append("   🏠 Тип: ").append(type).append("\n");
                            result.append("\n   ──────────────────\n\n");
                        }

                        if (count == 0) {
                            result.append("📭 *Архив пуст.*\n\n");
                            result.append("У вас пока нет завершенных клиентов.");
                        } else {
                            result.append("\n📊 *Всего в архиве: ").append(count).append("*");
                        }
                    }
                }
            } else {
                result.append("❌ *Нет соединения с базой данных!*");
            }
        } catch (SQLException ex) {
            System.out.println("❌ Ошибка при получении завершенных клиентов: " + ex.getMessage());
            result.append("❌ *Ошибка при получении данных из базы!*");
        }

        return result.toString();
    }

    private String formatCustomerForDisplay(Customers customer, int customerId) {
        StringBuilder sb = new StringBuilder();
        sb.append("👤 *Клиент #").append(customerId).append("*\n");
        sb.append("══════════════════════════════\n");
        sb.append("📝 *Имя:* ").append(customer.getName()).append("\n");
        sb.append("📱 *Телефон:* ").append(customer.getPhoneNumber()).append("\n");
        sb.append("🏙️ *Город:* ").append(customer.getCityForBuyEstate()).append("\n");
        sb.append("🏠 *Тип:* ").append(customer.getTypeOfEstate()).append("\n");

        if (customer.getTimeOfCreateQuery() != null) {
            sb.append("📅 *Создан:* ").append(customer.getTimeOfCreateQuery().format(DATE_FORMATTER)).append("\n");
        }

        if (customer.getTimeToContact() != null) {
            sb.append("⏰ *Связь:* ").append(customer.getTimeToContact().format(DATE_FORMATTER)).append("\n");
        }

        sb.append("══════════════════════════════");
        return sb.toString();
    }

    private String getEstateTypeInRussian(String type) {
        switch (type) {
            case "ATELIER":
                return "Студия";
            case "ONE_ROOM_APARTMENT":
                return "Однокомнатная квартира";
            case "TWO_ROOM_APARTMENT":
                return "Двухкомнатная квартира";
            case "THREE_ROOM_APARTMENT":
                return "Трехкомнатная квартира";
            case "HOUSE":
                return "Дом";
            default:
                return type;
        }
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (connection == null) {
            initDBConnection();
        }

        if (update.hasCallbackQuery()) {
            forWorkWithButtons(update);
        } else if (update.hasMessage()) {
            forWorkWithText(update);
        }
    }

    @Override
    public String getBotUsername() {
        return "@MatosyanTGBot";
    }

    @Override
    public String getBotToken() {
        return "8004012680:AAEfvyYY8R44wFfIGunrWkTFaowWxH5-zbE";
    }

    @Override
    public void onClosing() {
        notificationThreadRunning = false;
        if (notificationThread != null) {
            notificationThread.interrupt();
        }
        super.onClosing();
    }
}