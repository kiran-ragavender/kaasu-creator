package kaasu_creator.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import kaasu_creator.dao.GoalDao;
import kaasu_creator.dao.RoadmapDao;
import kaasu_creator.model.Goal;
import kaasu_creator.model.Roadmap;

/**
 * GoalService handles savings goals and roadmap generation.
 *
 * Key concept: Roadmap generation
 * A roadmap breaks a big goal into weekly milestones. We calculate how much
 * the user needs to save each week to reach their goal by the deadline.
 */
@Service
public class GoalService {

    private final GoalDao goalDao;
    private final RoadmapDao roadmapDao;

    public GoalService(GoalDao goalDao, RoadmapDao roadmapDao) {
        this.goalDao = goalDao;
        this.roadmapDao = roadmapDao;
    }

    /**
     * Create a new savings goal and generate a roadmap for it.
     */
    public Goal createGoal(Long userId, String name, BigDecimal targetAmount, LocalDate deadline) {
        Goal goal = new Goal(null, userId, name, targetAmount, BigDecimal.ZERO, deadline, null);
        Long id = goalDao.save(goal);
        goal = new Goal(id, userId, name, targetAmount, BigDecimal.ZERO, deadline, null);
        generateRoadmap(goal);
        return goal;
    }

    /**
     * Add savings to a goal (increments current_amount).
     */
    public void addSavings(Long goalId, BigDecimal amount) {
        goalDao.addToCurrentAmount(goalId, amount);
    }

    /**
     * Get all goals for a user.
     */
    public List<Goal> getGoalsByUser(Long userId) {
        return goalDao.findByUserId(userId);
    }

    /**
     * Get a single goal by ID.
     */
    public Goal getGoalById(Long goalId) {
        return goalDao.findById(goalId);
    }

    /**
     * Calculate progress percentage for a goal.
     */
    public BigDecimal getProgress(Long goalId) {
        Goal goal = goalDao.findById(goalId);
        if (goal == null || goal.getTargetAmount().compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return goal.getCurrentAmount()
                   .divide(goal.getTargetAmount(), 4, RoundingMode.HALF_UP)
                   .multiply(new BigDecimal("100"));
    }

    /**
     * Get the roadmap for a specific goal.
     */
    public List<Roadmap> getRoadmap(Long goalId) {
        return roadmapDao.findByGoalId(goalId);
    }

    /**
     * Generate a week-by-week roadmap for a goal.
     *
     * How it works:
     * 1. Calculate total weeks between today and the deadline
     * 2. Divide the target amount by number of weeks to get weekly savings target
     * 3. Create a Roadmap entry for each week
     *
     * Note: In a real app, this would call Gemini API for a smarter plan.
     * For now, we use a simple equal-divison approach.
     */
    private void generateRoadmap(Goal goal) {
        LocalDate today = LocalDate.now();
        long totalWeeks = ChronoUnit.WEEKS.between(today, goal.getDeadline());

        if (totalWeeks < 1) {
            totalWeeks = 1; // minimum 1 week
        }

        BigDecimal weeklyTarget = goal.getTargetAmount()
                                       .divide(new BigDecimal(totalWeeks), 2, RoundingMode.CEILING);

        List<Roadmap> roadmaps = new ArrayList<>();
        for (int week = 1; week <= totalWeeks; week++) {
            Roadmap r = new Roadmap(null, goal.getId(), week, weeklyTarget, "pending");
            roadmaps.add(r);
        }

        roadmapDao.saveAll(roadmaps);
    }

    /**
     * Delete a goal and its roadmap (cascade handled by DB).
     */
    public void deleteGoal(Long goalId) {
        goalDao.deleteById(goalId);
    }

    /**
     * Mark a roadmap week as completed.
     */
    public void completeWeek(Long roadmapId) {
        roadmapDao.updateStatus(roadmapId, "completed");
    }
}