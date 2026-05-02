package kaasu_creator.controller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import kaasu_creator.dao.UserDao;
import kaasu_creator.model.Job;
import kaasu_creator.model.TimesheetEntry;
import kaasu_creator.service.JobService;
import kaasu_creator.service.TimesheetService;

@Controller
public class TimesheetController {

    private final TimesheetService timesheetService;
    private final JobService jobService;
    private final UserDao userDao;

    public TimesheetController(TimesheetService timesheetService, JobService jobService, UserDao userDao) {
        this.timesheetService = timesheetService;
        this.jobService = jobService;
        this.userDao = userDao;
    }

    private Long getUserId(Authentication authentication) {
        return userDao.findByEmail(authentication.getName())
                .map(kaasu_creator.model.User::getId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // ── GET /timesheet ─────────────────────────────────────────────────────────
    @GetMapping("/timesheet")
    public String showTimesheet(
            Authentication authentication,
            Model model,
            @RequestParam(required = false) String tab,
            @RequestParam(required = false) Long selectedJobId) {

        Long userId = getUserId(authentication);

        // Jobs with aggregated totals needed for My Jobs cards
        List<Job> jobs = jobService.getJobsWithSummary(userId);

        // Enriched entries (include jobName, wage, earnedAmount) for History tab
        List<TimesheetEntry> allEntries = timesheetService.getEntriesByUser(userId);

        BigDecimal totalEarned = timesheetService.getTotalEarned(userId);
        BigDecimal totalHours  = timesheetService.getTotalHours(userId);

        model.addAttribute("jobs", jobs);
        model.addAttribute("allEntries", allEntries);
        model.addAttribute("totalEarned", totalEarned);
        model.addAttribute("totalHours", totalHours);
        model.addAttribute("selectedJobId", selectedJobId);
        model.addAttribute("today", java.time.LocalDate.now().toString());

        // activeTab driven by flash redirect or URL param
        if (!model.containsAttribute("activeTab")) {
            model.addAttribute("activeTab", tab != null ? tab : "jobs");
        }

        return "timesheet";
    }

    // ── POST /timesheet/job/save (add or update job) ───────────────────────────
    @PostMapping("/timesheet/job/save")
    public String saveJob(
            @RequestParam(value = "jobId", required = false) Long jobId,
            @RequestParam("jobName") String jobName,
            @RequestParam("hourlyWage") BigDecimal hourlyWage,
            @RequestParam(value = "notes", required = false) String notes,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        if (jobName == null || jobName.isBlank() || hourlyWage == null || hourlyWage.compareTo(BigDecimal.ZERO) <= 0) {
            redirectAttributes.addFlashAttribute("errorMessage", "Job name and a positive hourly wage are required.");
            redirectAttributes.addFlashAttribute("activeTab", "jobs");
            return "redirect:/timesheet";
        }

        try {
            Long userId = getUserId(authentication);
            Job job = new Job(jobId, userId, jobName.trim(), hourlyWage,
                    notes != null ? notes.trim() : "", null);

            if (jobId != null) {
                jobService.updateJob(job);
                redirectAttributes.addFlashAttribute("successMessage", "Job updated successfully.");
            } else {
                jobService.addJob(job);
                redirectAttributes.addFlashAttribute("successMessage",
                        "Job '" + jobName.trim() + "' added successfully!");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error saving job: " + e.getMessage());
        }

        redirectAttributes.addFlashAttribute("activeTab", "jobs");
        return "redirect:/timesheet";
    }

    // ── POST /timesheet/job/delete ─────────────────────────────────────────────
    @PostMapping("/timesheet/job/delete")
    public String deleteJob(
            @RequestParam Long jobId,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            Long userId = getUserId(authentication);
            var jobOpt = jobService.getJobByIdAndUser(jobId, userId);
            if (jobOpt.isPresent()) {
                jobService.deleteJob(jobId);
                redirectAttributes.addFlashAttribute("successMessage", "Job deleted.");
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Job not found.");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting job: " + e.getMessage());
        }
        redirectAttributes.addFlashAttribute("activeTab", "jobs");
        return "redirect:/timesheet";
    }

    // ── POST /timesheet/work/save (single-entry form) ──────────────────────────
    @PostMapping("/timesheet/work/save")
    public String saveWorkEntry(
            @RequestParam Long jobId,
            @RequestParam String workDate,
            @RequestParam BigDecimal hoursWorked,
            @RequestParam(required = false) String notes,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            Long userId = getUserId(authentication);

            var jobOpt = jobService.getJobByIdAndUser(jobId, userId);
            if (jobOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Job not found.");
                redirectAttributes.addFlashAttribute("activeTab", "log");
                return "redirect:/timesheet";
            }

            TimesheetEntry entry = new TimesheetEntry();
            entry.setUserId(userId);
            entry.setJobId(jobId);
            entry.setWorkDate(Date.valueOf(workDate));
            entry.setHoursWorked(hoursWorked);
            entry.setNotes(notes != null ? notes.trim() : null);

            timesheetService.save(entry);

            BigDecimal wage     = jobOpt.get().getHourlyWage();
            BigDecimal earnings = hoursWorked.multiply(wage);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Work entry saved! Earned: $" + earnings.setScale(2, RoundingMode.HALF_UP));
            redirectAttributes.addFlashAttribute("activeTab", "history");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Error saving entry: " + e.getMessage());
            redirectAttributes.addFlashAttribute("activeTab", "log");
        }
        return "redirect:/timesheet";
    }

    // ── POST /timesheet/work/delete (single entry) ─────────────────────────────
    @PostMapping("/timesheet/work/delete")
    public String deleteWorkEntry(
            @RequestParam Long entryId,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            Long userId = getUserId(authentication);
            int deleted = timesheetService.deleteEntry(entryId, userId);
            if (deleted > 0) {
                redirectAttributes.addFlashAttribute("successMessage", "Entry deleted.");
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Entry not found.");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting entry: " + e.getMessage());
        }
        redirectAttributes.addFlashAttribute("activeTab", "history");
        return "redirect:/timesheet";
    }

    // ── POST /timesheet/entries (bulk save — preserved) ────────────────────────
    @PostMapping("/timesheet/entries")
    public String saveEntries(
            @RequestParam(value = "jobId",        required = false) Long[]   jobIds,
            @RequestParam(value = "workDate",     required = false) String[] workDates,
            @RequestParam(value = "hoursWorked",  required = false) String[] hoursWorkedStrings,
            @RequestParam(value = "notes",        required = false) String[] notes,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        if (jobIds == null || workDates == null || hoursWorkedStrings == null) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Please add at least one valid row before saving.");
            redirectAttributes.addFlashAttribute("activeTab", "log");
            return "redirect:/timesheet";
        }

        Long userId = getUserId(authentication);
        int savedCount = 0;
        int rows = Math.min(Math.min(jobIds.length, workDates.length), hoursWorkedStrings.length);

        try {
            for (int i = 0; i < rows; i++) {
                Long jobId     = jobIds[i];
                String dateVal = workDates[i];
                String hrsVal  = hoursWorkedStrings[i];
                String noteVal = (notes != null && notes.length > i) ? notes[i] : "";

                if (jobId == null || dateVal == null || dateVal.isBlank()
                        || hrsVal == null || hrsVal.isBlank()) continue;

                var optJob = jobService.getJobByIdAndUser(jobId, userId);
                if (optJob.isEmpty()) continue;

                BigDecimal hrs = new BigDecimal(hrsVal).setScale(2, RoundingMode.HALF_UP);
                if (hrs.compareTo(BigDecimal.ZERO) <= 0) continue;

                timesheetService.save(new TimesheetEntry(
                        null, userId, jobId,
                        Date.valueOf(dateVal), hrs,
                        noteVal != null ? noteVal.trim() : "", null));
                savedCount++;
            }

            if (savedCount == 0) {
                redirectAttributes.addFlashAttribute("errorMessage",
                        "No valid rows to save — check that each row has a job, date, and positive hours.");
            } else {
                redirectAttributes.addFlashAttribute("successMessage",
                        savedCount + " timesheet row(s) saved successfully.");
                redirectAttributes.addFlashAttribute("activeTab", "history");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Error saving entries: " + e.getMessage());
        }

        return "redirect:/timesheet";
    }

    // ── POST /timesheet/entry/delete (legacy route — kept for safety) ──────────
    @PostMapping("/timesheet/entry/delete")
    public String deleteEntry(
            @RequestParam("entryId") Long entryId,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            Long userId = getUserId(authentication);
            int deleted = timesheetService.deleteEntry(entryId, userId);
            if (deleted == 0) {
                redirectAttributes.addFlashAttribute("errorMessage", "Entry not found.");
            } else {
                redirectAttributes.addFlashAttribute("successMessage", "Entry deleted.");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting entry: " + e.getMessage());
        }
        redirectAttributes.addFlashAttribute("activeTab", "history");
        return "redirect:/timesheet";
    }
}
