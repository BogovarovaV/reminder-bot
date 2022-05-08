package pro.sky.telegrambot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pro.sky.telegrambot.model.Reminders;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationTaskRepository extends JpaRepository<Reminders, Long> {

    List<Reminders> findByDateTimeEquals(LocalDateTime currentTime);

}
