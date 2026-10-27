package kaasu_creator.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import kaasu_creator.dao.UserDao;
import kaasu_creator.model.Goal;
import kaasu_creator.service.GoalService;

/**
 * GoalController - handles savings goals and roadmaps.
 *
 * Key concept: Nested objects in the Model
 * We pass both the Goal AND its associated Roadmap entries to the template.
 * The roadmap shows the week-by-week milestones for reaching the goal.
 */
@Controller
public class GoalController {

    private final GoalService goalService;
    private final UserDao userDao;

    public GoalController(GoalService goalService, UserDao userDao) {
        this.goalService = goalService;
        this.userDao = userDao;
    }

    @GetMapping("/goal")
    public String showGoalPage(Authentication authentication, Model model) {
        Long userId = getUserId(authentication);
        model.addAttribute("goals", goalService.getGoalsByUser(userId));
        return "goal";
    }

    @PostMapping("/goal/create")
    public String createGoal(Authentication authentication,
                             @RequestParam String name,
                             @RequestParam BigDecimal targetAmount,
                             @RequestParam String deadline,
                             RedirectAttributes redirectAttributes) {
        Long userId = getUserId(authentication);
        if (name == null || name.isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Goal name is required.");
            return "redirect:/goal";
        }
        if (targetAmount == null || targetAmount.compareTo(BigDecimal.ZERO) <= 0) {
            redirectAttributes.addFlashAttribute("error", "Target amount must be greater than zero.");
            return "redirect:/goal";
        }
        LocalDate deadlineDate;
        try {
            deadlineDate = LocalDate.parse(deadline);
        } catch (DateTimeParseException e) {
            redirectAttributes.addFlashAttribute("error", "Please enter a valid deadline.");
            return "redirect:/goal";
        }
        if (!deadlineDate.isAfter(LocalDate.now())) {
            redirectAttributes.addFlashAttribute("error", "Deadline must be a future date.");
            return "redirect:/goal";
        }
        Goal goal = goalService.createGoal(userId, name.trim(), targetAmount, deadlineDate);
        redirectAttributes.addFlashAttribute("success",
            "Goal created! " + goal.getName() + " - roadmap generated.");
        return "redirect:/goal";
    }

    @PostMapping("/goal/add-savings")
    public String addSavings(Authentication authentication,
                             @RequestParam Long goalId,
                             @RequestParam BigDecimal amount,
                             RedirectAttributes redirectAttributes) {
        Long userId = getUserId(authentication);
        Goal goal = goalService.getGoalById(goalId);
        if (goal == null || !goal.getUserId().equals(userId)) {
            redirectAttributes.addFlashAttribute("error", "Goal not found.");
            return "redirect:/goal";
        }
        if (goal.isCompleted()) {
            redirectAttributes.addFlashAttribute("error", "This goal is already completed.");
            return "redirect:/goal";
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            redirectAttributes.addFlashAttribute("error", "Savings amount must be greater than zero.");
            return "redirect:/goal";
        }
        goalService.addSavings(goalId, amount);
        redirectAttributes.addFlashAttribute("success", "Savings added to goal!");
        return "redirect:/goal";
    }

    @PostMapping("/goal/complete")
    public String completeGoal(Authentication authentication,
                               @RequestParam Long goalId,
                               RedirectAttributes redirectAttributes) {
        Long userId = getUserId(authentication);
        Goal goal = goalService.getGoalById(goalId);
        if (goal == null || !goal.getUserId().equals(userId)) {
            redirectAttributes.addFlashAttribute("error", "Goal not found.");
            return "redirect:/goal";
        }
        goalService.completeGoal(goalId);
        redirectAttributes.addFlashAttribute("success", "🎉 Goal completed! Check it out in your history.");
        return "redirect:/goal";
    }

    @GetMapping("/goal/history")
    public String showGoalHistory(Authentication authentication, Model model) {
        Long userId = getUserId(authentication);
        model.addAttribute("completedGoals", goalService.getCompletedGoalsByUser(userId));
        return "goal-history";
    }

    @PostMapping("/goal/delete")
    public String deleteGoal(Authentication authentication,
                             @RequestParam Long goalId,
                             RedirectAttributes redirectAttributes) {
        Long userId = getUserId(authentication);
        Goal goal = goalService.getGoalById(goalId);
        if (goal == null || !goal.getUserId().equals(userId)) {
            redirectAttributes.addFlashAttribute("error", "Goal not found.");
            return "redirect:/goal";
        }
        goalService.deleteGoal(goalId);
        redirectAttributes.addFlashAttribute("success", "Goal deleted.");
        return "redirect:/goal";
    }

    @GetMapping("/goal/view")
    public String viewGoal(Authentication authentication,
                           @RequestParam Long goalId, Model model) {
        Long userId = getUserId(authentication);
        Goal goal = goalService.getGoalById(goalId);
        if (goal == null || !goal.getUserId().equals(userId)) {
            return "redirect:/goal";
        }
        model.addAttribute("goal", goal);
        model.addAttribute("roadmap", goalService.getRoadmap(goalId));
        model.addAttribute("progress", goalService.getProgress(goalId));
        return "goal";
    }

    private Long getUserId(Authentication authentication) {
        return userDao.findByEmail(authentication.getName())
                      .map(kaasu_creator.model.User::getId)
                      .orElseThrow(() -> new RuntimeException("User not found"));
    }
}