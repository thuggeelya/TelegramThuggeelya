package ru.thuggeelya.controller;

import lombok.extern.log4j.Log4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.thuggeelya.service.UpdateProducer;
import ru.thuggeelya.utils.MessageUtils;

import static ru.thuggeelya.model.RabbitQueue.*;

@Component
@Log4j
public class UpdateController {

    private TelegramBot telegramBot;
    private final MessageUtils messageUtils;
    private UpdateProducer updateProducer;

    public UpdateController(MessageUtils messageUtils, UpdateProducer updateProducer) {
        this.messageUtils = messageUtils;
        this.updateProducer = updateProducer;
    }

    public void registerBot(TelegramBot telegramBot) {
        this.telegramBot = telegramBot;
    }

    public void processUpdate(Update update) {
        if (update == null) {
            log.error("Received update is null");
            return;
        }

        if (update.getMessage() != null) {
            distributeMessagesByType(update);
        } else {
            log.error("Unsupported message type received: " + update);
        }
    }

    private void distributeMessagesByType(Update update) {
        var message = update.getMessage();

        if (message.getText() != null) {
            processTextMessage(update);
        } else if (message.getDocument() != null) {
            processDocMessage(update);
        } else if (message.getPhoto() != null) {
            processPhotoMessage(update);
        } else {
            setUnsupportedMessageTypeView(update);
        }
    }

    private void setUnsupportedMessageTypeView(Update update) {
        var sendMessage = messageUtils.generateSendMessage(update,
                "Unsupported message type");
        setView(sendMessage);
    }

    private void setFileIsReceivedView(Update update) {
        var sendMessage = messageUtils.generateSendMessage(update,
                "File is received. Processing ...");
        setView(sendMessage);
    }

    private void setView(SendMessage sendMessage) {
        telegramBot.sendAnswerMessage(sendMessage);
    }

    private void processPhotoMessage(Update update) {
        updateProducer.produce(PHOTO_MESSAGE_UPDATE, update);
        setFileIsReceivedView(update);
    }

    private void processDocMessage(Update update) {
        updateProducer.produce(DOC_MESSAGE_UPDATE, update);
        setFileIsReceivedView(update);
    }

    private void processTextMessage(Update update) {
        updateProducer.produce(TEXT_MESSAGE_UPDATE, update);
    }

}
