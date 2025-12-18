package org.example.model;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;

public class Bot extends TelegramLongPollingBot {
    private Connection connection;

    private boolean isCreateCustomer = false;
    private boolean isWaitingName = false;
    private boolean isWaitingPhone = false;
    private boolean isWaitingCity = false;
    private boolean isWaitingType = false;

    private String name;
    private Long phoneNumber;
    private String cityForBuyEstate;
    private String typeOfEstate;

    // Кнопка для создания клиента
    InlineKeyboardButton buttonForCreateCustomer = InlineKeyboardButton.builder()
            .text("Добавить нового клиента")
            .callbackData("create_new_client")
            .build();

    // Кнопка для просмотра клиентов, которым написали
    InlineKeyboardButton buttonForQuestionableCustomers = InlineKeyboardButton.builder()
            .text("Клиенты под вопросом")
            .callbackData("questionable_customers")
            .build();

    // Кнопка для просмотра клиентов, работа с которыми закончена
    InlineKeyboardButton buttonForCheckEndedWorkCustomers = InlineKeyboardButton.builder()
            .text("С ними работу завершили")
            .callbackData("ended_work_customers")
            .build();

    InlineKeyboardButton buttonForReturnBack = InlineKeyboardButton.builder()
            .text("Назад")
            .callbackData("back")
            .build();

    //Клавиатура для главного меню
    InlineKeyboardMarkup keyboardForMainMenu = InlineKeyboardMarkup.builder()
            .keyboardRow(List.of(buttonForCreateCustomer))
            .keyboardRow(List.of(buttonForQuestionableCustomers))
            .keyboardRow(List.of(buttonForCheckEndedWorkCustomers))
            .keyboardRow(List.of(buttonForReturnBack))
            .build();

    public void initDBConnection() {
        try {
            String url = "jdbc:mysql://localhost:3306/shoe_store_bot";
            String username = "root";
            String password = "andrEj0077";

            connection = DriverManager.getConnection(url, username, password);
            System.out.println("✅ Соединение с БД установлено!");
        } catch (Exception ex) {
            System.out.println("❌ Ошибка подключения к БД: " + ex.getMessage());
        }
    }

    public void forWorkWithText(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String chatId = update.getMessage().getChatId().toString();
            String textMessage = update.getMessage().getText();

            SendMessage sendMessage = SendMessage.builder()
                    .chatId(chatId)
                    .text("")
                    .build();

            System.out.println("текст: " + textMessage);

            if (textMessage.equals("/start")) {
                isCreateCustomer = false;
                isWaitingName = false;
                isWaitingPhone = false;
                isWaitingCity = false;
                isWaitingType = false;
                sendMessage.setText("Добро пожаловать в телеграм бот для риелторов");
                sendMessage.setReplyMarkup(keyboardForMainMenu);
                try {
                    execute(sendMessage);
                } catch (Exception ex) {
                    System.out.println(ex.getMessage());
                }
                return;
            }

            if (isCreateCustomer && isWaitingName) {
                name = textMessage;
                isWaitingName = false;
                isWaitingPhone = true;
                sendMessage.setText("Введите номер телефона начиная с 8: ");
                try {
                    execute(sendMessage);
                } catch (Exception ex) {
                    System.out.println(ex.getMessage());
                }
            } else if (isCreateCustomer && isWaitingPhone) {
                try {
                    phoneNumber = Long.parseLong(textMessage);
                    isWaitingPhone = false;
                    isWaitingCity = true;
                    sendMessage.setText("Введите город для покупки недвижимости: ");
                    try {
                        execute(sendMessage);
                    } catch (Exception ex) {
                        System.out.println(ex.getMessage());
                    }
                } catch (NumberFormatException e) {
                    sendMessage.setText("Неверный формат номера. Введите номер цифрами:");
                    try {
                        execute(sendMessage);
                    } catch (Exception ex) {
                        System.out.println(ex.getMessage());
                    }
                }
            } else if (isCreateCustomer && isWaitingCity) {
                cityForBuyEstate = textMessage;
                isWaitingCity = false;
                isWaitingType = true;

                InlineKeyboardButton atelier = InlineKeyboardButton.builder()
                        .text("Студия")
                        .callbackData("ATELIER")
                        .build();
                InlineKeyboardButton one_room = InlineKeyboardButton.builder()
                        .text("Однокомнатная")
                        .callbackData("ONE_ROOM_APARTMENT")
                        .build();
                InlineKeyboardButton two_room = InlineKeyboardButton.builder()
                        .text("Двухкомнатная")
                        .callbackData("TWO_ROOM_APARTMENT")
                        .build();
                InlineKeyboardButton three_room = InlineKeyboardButton.builder()
                        .text("Трехкомнатная")
                        .callbackData("THREE_ROOM_APARTMENT")
                        .build();
                InlineKeyboardButton house = InlineKeyboardButton.builder()
                        .text("Дом")
                        .callbackData("HOUSE")
                        .build();
                InlineKeyboardMarkup keyboardForChooseType = InlineKeyboardMarkup.builder()
                        .keyboardRow(List.of(atelier))
                        .keyboardRow(List.of(one_room))
                        .keyboardRow(List.of(two_room))
                        .keyboardRow(List.of(three_room))
                        .keyboardRow(List.of(house))
                        .build();

                sendMessage.setText("Выберите тип недвижимости:");
                sendMessage.setReplyMarkup(keyboardForChooseType);
                try {
                    execute(sendMessage);
                } catch (Exception ex) {
                    System.out.println(ex.getMessage());
                }
            }
        }
    }

    public void forWorkWithButtons(Update update) {
        if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();
            String chatId = update.getCallbackQuery().getMessage().getChatId().toString();
            Integer messageId = update.getCallbackQuery().getMessage().getMessageId();

            System.out.println("current callback: " + callbackData);

            //Для отправки текста
            EditMessageText editMessageText = EditMessageText.builder()
                    .chatId(chatId)
                    .messageId(messageId)
                    .text("")
                    .build();

            if (callbackData.equals(buttonForCreateCustomer.getCallbackData())) {
                isCreateCustomer = true;
                isWaitingName = true;
                editMessageText.setText("Создание клиента: \nВведите имя:");
                try {
                    execute(editMessageText);
                } catch (Exception ex) {
                    System.out.println(ex.getMessage());
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

                    Customers customer = new Customers(name, phoneNumber, cityForBuyEstate, typeOfEstate);
                    System.out.println(customer);



                    editMessageText.setText("Клиент создан!\n" + customer.toString() + "\n\nГлавное меню:");
                    editMessageText.setReplyMarkup(keyboardForMainMenu);
                    try {
                        execute(editMessageText);
                    } catch (Exception ex) {
                        System.out.println(ex.getMessage());
                    }
                }
            } else if (callbackData.equals("back")) {
                resetIs();
                editMessageText.setText("Добро пожаловать в телеграм бот для риелторов");
                editMessageText.setReplyMarkup(keyboardForMainMenu);
                try {
                    execute(editMessageText);
                } catch (Exception ex) {
                    System.out.println(ex.getMessage());
                }
            }
        }
    }

    private void resetIs(){
        isCreateCustomer = false;
        isWaitingName = false;
        isWaitingPhone = false;
        isWaitingCity = false;
        isWaitingType = false;
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
}