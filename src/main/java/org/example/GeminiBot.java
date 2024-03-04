package org.example;

import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import okhttp3.*;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.io.IOException;

public class GeminiBot extends TelegramLongPollingBot {
    private static final String TELEGRAM_BOT_TOKEN = "6823653723:AAH5F1-0cEJ1RQfsapptEU9iA8rCZegoRs4";
    private static final String GEMINI_API_KEY = "AIzaSyBcQeSyC6FsgNZLSlwwinfBkDeHwG_KtOA";

    private static final OkHttpClient client = new OkHttpClient();

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            // Отправляем сообщение пользователя к Gemini
            String geminiResponse = sendToGemini(messageText);

            // Извлекаем текст из JSON ответа
            String geminiText = extractTextFromResponse(geminiResponse);

            // Отправляем ответ от Gemini пользователю
            sendResponseToUser(chatId, geminiText);
        }
    }

    private String sendToGemini(String message) {
        try {
            // Формируем JSON запрос к Gemini AI
            MediaType mediaType = MediaType.parse("application/json");
            String json = "{\"contents\":[{\"parts\":[{\"text\":\"" + message + "\"}]}]}";
            RequestBody body = RequestBody.create(mediaType, json);

            // Отправляем POST запрос к Gemini AI
            Request request = new Request.Builder()
                    .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent?key=" + GEMINI_API_KEY)
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .build();

            Response response = client.newCall(request).execute();
            return response.body().string();
        } catch (IOException e) {
            e.printStackTrace();
            return "Ошибка при отправке запроса к Gemini AI";
        }
    }

    private String extractTextFromResponse(String response) {
        try {
            JSONObject jsonResponse = new JSONObject(response);
            JSONArray candidates = jsonResponse.getJSONArray("candidates");
            if (candidates.length() > 0) {
                JSONObject content = candidates.getJSONObject(0).getJSONObject("content");
                JSONArray parts = content.getJSONArray("parts");
                if (parts.length() > 0) {
                    return parts.getJSONObject(0).getString("text");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "Не удалось извлечь текст из ответа";
    }

    private void sendResponseToUser(long chatId, String response) {
        try {
            // Отправляем ответ пользователю через Telegram API
            execute(SendMessage.builder()
                    .chatId(String.valueOf(chatId))
                    .text(response)
                    .build());
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getBotUsername() {
        return "YourGeminiBot";
    }

    @Override
    public String getBotToken() {
        return TELEGRAM_BOT_TOKEN;
    }

    public static void main(String[] args) throws TelegramApiException {
        // Инициализация Telegram бота
        GeminiBot bot = new GeminiBot();
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        try {
            botsApi.registerBot(bot);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
