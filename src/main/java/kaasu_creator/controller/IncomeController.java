package kaasu_creator.controller;

import java.math.BigDecimal;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import kaasu_creator.dao.UserDao;
import kaasu_creator.model.Income;
import kaasu_creator.model.Job;
import kaasu_creator.model.TimesheetEntry;
import kaasu_creator.service.IncomeService;
import kaasu_creator.service.JobService;
import kaasu_creator.service.TimesheetService;

@Controller
public class IncomeController {

    private final IncomeService incomeService;
    private final TimesheetService timesheetService;
    private final JobService jobService;
    private final UserDao userDao;

    public IncomeController(IncomeService incomeService, TimesheetService timesheetService,
                            JobService jobService, UserDao userDao) {
        this.incomeService = incomeService;
        this.timesheetService = timesheetService;
        this.jobService = jobService;
        this.userDao = userDao;
    }

    private Long getUserId(Authentication authentication) {
        return userDao.findByEmail(authentication.getName())
                .map(kaasu_creator.model.User::getId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // ── GET /income ────────────────────────────────────────────────────────────
    @GetMapping("/income")
    public String showIncome(Authentication authentication, Model model) {

        var userOptional = userDao.findByEmail(authentication.getName());
        if (userOptional.isEmpty()) {
            model.addAttribute("errorMessage", "User not found.");
            return "income";
        }

        Long userId = userOptional.get().getId();

        // Summary counts — lightweight calls, no heavy join needed here
        List<Job> jobs = jobService.getJobsWithSummary(userId);
        List<TimesheetEntry> allEntries = timesheetService.getEntriesByUser(userId);

        // Timesheet aggregate totals for summary cards
        BigDecimal totalEarned = timesheetService.getTotalEarned(userId);

        // Custom income — full list drives the history table on this page
        List<Income> allCustomIncomes = incomeService.getIncomesByUser(userId);
        BigDecimal totalCustomIncome  = incomeService.getTotalIncome(userId);

        model.addAttribute("jobs", jobs);
        model.addAttribute("allEntries", allEntries);
        model.addAttribute("totalEarned", totalEarned);
        model.addAttribute("allCustomIncomes", allCustomIncomes);
        model.addAttribute("totalCustomIncome", totalCustomIncome);

        return "income";
    }

    // ── POST /income/custom/save ───────────────────────────────────────────────
    @PostMapping("/income/custom/save")
    public String saveCustomIncome(
            @RequestParam String source,
            @RequestParam(required = false) String category,
            @RequestParam BigDecimal amount,
            @RequestParam String incomeDate,
            @RequestParam(required = false) String notes,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            Long userId = getUserId(authentication);

            if (source == null || source.isBlank()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Income source/title is required.");
                return "redirect:/income";
            }
            if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                redirectAttributes.addFlashAttribute("errorMessage", "Amount must be greater than zero.");
                return "redirect:/income";
            }

            Income income = new Income();
            income.setUserId(userId);
            income.setIncomeType("CUSTOM");
            income.setSource(source.trim());
            income.setCategory(category != null && !category.isBlank() ? category.trim() : null);
            income.setAmount(amount);
            income.setIncomeDate(Date.valueOf(incomeDate));
            income.setNotes(notes != null && !notes.isBlank() ? notes.trim() : null);

            incomeService.addIncome(income);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Income '" + source.trim() + "' saved! ($" +
                    amount.setScale(2, java.math.RoundingMode.HALF_UP) + ")");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Error saving income: " + e.getMessage());
        }
        return "redirect:/income";
    }

    // ── POST /income/custom/delete ─────────────────────────────────────────────
    @PostMapping("/income/custom/delete")
    public String deleteCustomIncome(
            @RequestParam Long incomeId,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            Long userId = getUserId(authentication);
            int deleted = incomeService.deleteCustomIncome(incomeId, userId);
            if (deleted > 0) {
                redirectAttributes.addFlashAttribute("successMessage", "Income entry deleted.");
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Entry not found or already deleted.");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Error deleting income: " + e.getMessage());
        }
        return "redirect:/income";
    }

    // ── POST /income/save (legacy bulk extra income — kept for compatibility) ──
    @PostMapping("/income/save")
    public String saveIncomeEntries(
            @RequestParam(value = "sourceName", required = false) List<String> sourceNames,
            @RequestParam(value = "amount",     required = false) List<BigDecimal> amounts,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        if (sourceNames == null || sourceNames.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Please add at least one income row before saving.");
            return "redirect:/income";
        }

        try {
            Long userId = getUserId(authentication);
            List<Income> toSave = new ArrayList<>();

            for (int i = 0; i < sourceNames.size(); i++) {
                String source = sourceNames.get(i);
                BigDecimal amount = (amounts != null && amounts.size() > i) ? amounts.get(i) : null;
                if (source == null || source.isBlank() || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }
                toSave.add(new Income(null, userId, "EXTRA", source.trim(), null, amount,
                        new Date(System.currentTimeMillis()), null));
            }

            if (toSave.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "No valid rows to save.");
                return "redirect:/income";
            }

            toSave.forEach(incomeService::addIncome);
            redirectAttributes.addFlashAttribute("successMessage", "Extra income saved!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Error saving extra income: " + e.getMessage());
        }
        return "redirect:/income";
    }
}
