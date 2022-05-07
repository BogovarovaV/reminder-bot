package pro.sky.telegrambot.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import pro.sky.telegrambot.model.NotificationTask;
import pro.sky.telegrambot.repository.NotificationTaskRepository;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TelegramBotUpdatesListener implements UpdatesListener {

    private final Logger logger = LoggerFactory.getLogger(TelegramBotUpdatesListener.class);
    private final NotificationTaskRepository repository;

    @Autowired
    private TelegramBot telegramBot;

    public TelegramBotUpdatesListener(NotificationTaskRepository repository) {
        this.repository = repository;
    }

    @PostConstruct
    public void init() {
        telegramBot.setUpdatesListener(this);
    }

    @Override
    public int process(List<Update> updates) {
        updates.forEach(update -> {
            logger.info("Processing update: {}", update);
            Message message = update.message();
            if (update.message().text().equals("/start")) {
                logger.info("User sent /start");
                telegramBot.execute(new SendMessage(getChatId(message),
                        "Привет! Этот бот создан для того, чтобы ты не забыл(а) о своих важных делах ;) " +
                                "\nДля создания напоминания, отправь сообщение вида \"01.01.2022 20:00 Сделать домашнюю работу\", " +
                                "указав дату и время, в которые ты хочешь получить напоминание, и его текст."));
            } else {
                try {
                    parseMessage(message);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        return UpdatesListener.CONFIRMED_UPDATES_ALL;
    }

    public void parseMessage(Message message) throws Exception {
        Pattern pattern = Pattern.compile("([0-9\\.\\:\\s]{16})(\\s)([\\W+]+)");
        Matcher matcher = pattern.matcher(message.text());
        if (matcher.matches()) {
            String dateTime = matcher.group(1);
            String text = matcher.group(3);
            LocalDateTime reminderDateTime = LocalDateTime.parse(dateTime,
                    DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
            repository.save(new NotificationTask(getChatId(message), text, reminderDateTime));
            telegramBot.execute(new SendMessage(getChatId(message),
                    "Я напомню тебе о задаче \"" + text + "\" " + dateTime));
        } else {
            logger.error("Can not parse reminder message: " + message);
            telegramBot.execute(new SendMessage(getChatId(message),
                    "Упс, не могу понять, что ты написал(а) :) " +
                            "Пожалуйста, отправь сообщение вида " +
                            "\"01.01.2022 20:00 Сделать домашнюю работу\"."));
        }
    }

    @Scheduled(cron = "0 0/1 * * * *")
    public void getScheduledMessage() {
        try {
            LocalDateTime currentTime = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
            List<NotificationTask> notifications = repository.findByDateAndTimeEquals(currentTime);
            logger.info("Finding...");
            for (NotificationTask task : notifications) {
                if (task != null) {
                    telegramBot.execute(new SendMessage(task.getChatId(),
                            "Ты просил(а) напомнить о задаче: " + task));
                    logger.info("Reminders are sent");
                }
            }
        } catch (RuntimeException e) {
            logger.info("No reminders for this moment");
            throw new RuntimeException();
        }
    }

    private Long getChatId(Message message) {
        return message.chat().id();
    }

}
