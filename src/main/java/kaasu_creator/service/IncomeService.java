package kaasu_creator.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;

import kaasu_creator.dao.IncomeDao;
import kaasu_creator.model.Income;

@Service
public class IncomeService {

    private final IncomeDao incomeDao;

    public IncomeService(IncomeDao incomeDao) {
        this.incomeDao = incomeDao;
    }

    public void addIncome(Income income) {
        incomeDao.save(income);
    }

    public int deleteCustomIncome(Long id, Long userId) {
        return incomeDao.deleteById(id, userId);
    }

    public List<Income> getIncomesByUser(Long userId) {
        return incomeDao.findByUserId(userId);
    }

    public List<Income> getExtraIncomesByUser(Long userId) {
        return incomeDao.findExtraByUserId(userId);
    }

    public BigDecimal getTotalIncome(Long userId) {
        return incomeDao.sumByUserId(userId);
    }

    public BigDecimal getTotalExtraIncome(Long userId) {
        return incomeDao.sumExtraByUserId(userId);
    }
}
