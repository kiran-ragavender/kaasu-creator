package kaasu_creator.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kaasu_creator.dao.UserDao;
import kaasu_creator.service.BudgetService;
import kaasu_creator.service.IncomeService;
import kaasu_creator.service.TimesheetService;

/**
 * DashboardController - the main hub of the app.
 */
@Controller
public class DashboardController {

    private final UserDao userDao;
    private final BudgetService budgetService;
    private final IncomeService incomeService;
    private final TimesheetService timesheetService;

    public DashboardController(UserDao userDao, BudgetService budgetService,
                               IncomeService incomeService, TimesheetService timesheetService) {
        this.userDao = userDao;
        this.budgetService = budgetService;
        this.incomeService = incomeService;
        this.timesheetService = timesheetService;
    }

    @GetMapping("/dashboard")
    public String showDashboard(Authentication authentication, Model model) {
        String email = authentication.getName();
        String displayName = email;

        var userOptional = userDao.findByEmail(email);
        if (userOptional.isPresent()) {
            var user = userOptional.get();
            String fullName = user.getFullName();
            if (fullName != null && !fullName.trim().isEmpty()) {
                displayName = fullName;
            }

            Long userId = user.getId();
            // Custom income + timesheet earnings, matching the Income page
            var totalIncome = incomeService.getTotalIncome(userId)
                                           .add(timesheetService.getTotalEarned(userId));
            var totalExpenses = budgetService.getTotalExpenses(userId);
            var currentBalance = totalIncome.subtract(totalExpenses);

            model.addAttribute("totalIncome", totalIncome);
            model.addAttribute("totalExpenses", totalExpenses);
            model.addAttribute("currentBalance", currentBalance);
            model.addAttribute("userId", userId);
        }

        model.addAttribute("userName", displayName);
        return "dashboard";
    }

    @GetMapping("/")
    public String showHome() {
        return "redirect:/dashboard";
    }

    @PostMapping("/dashboard/delete-account")
    public String deleteAccount(Authentication authentication,
                                HttpServletRequest request,
                                HttpServletResponse response) {
        userDao.deleteByEmail(authentication.getName());
        // End the session so the deleted user isn't left logged in
        new SecurityContextLogoutHandler().logout(request, response, authentication);
        return "redirect:/login?deleted";
    }
}