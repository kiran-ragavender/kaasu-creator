package kaasu_creator.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kaasu_creator.model.Job;
import kaasu_creator.repository.JobRepository;

@Service
public class JobService {

    private final JobRepository repository;

    public JobService(JobRepository repository) {
        this.repository = repository;
    }

    public void addJob(Job job) {
        repository.save(job);
    }

    public void updateJob(Job job) {
        repository.update(job);
    }

    @Transactional
    public void deleteJob(Long id, Long userId) {
        repository.deleteById(id, userId);
    }

    public List<Job> getJobsWithSummary(Long userId) {
        return repository.findWithSummaryByUserId(userId);
    }

    public Optional<Job> getJobByIdAndUser(Long id, Long userId) {
        return repository.findByIdAndUserId(id, userId);
    }
}
