package kaasu_creator.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;

import kaasu_creator.model.TimesheetEntry;
import kaasu_creator.repository.TimesheetRepository;

@Service
public class TimesheetService {

    private final TimesheetRepository repository;

    public TimesheetService(TimesheetRepository repository) {
        this.repository = repository;
    }

    public void save(TimesheetEntry entry) {
        repository.save(entry);
    }

    public List<TimesheetEntry> getEntriesByUser(Long userId) {
        return repository.findByUserId(userId);
    }

    public int deleteEntry(Long id, Long userId) {
        return repository.deleteByIdAndUserId(id, userId);
    }

    public BigDecimal getTotalEarned(Long userId) {
        return repository.sumEarnedAmountByUserId(userId);
    }

    public BigDecimal getTotalHours(Long userId) {
        return repository.sumHoursWorkedByUserId(userId);
    }
}
