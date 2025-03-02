import java.util.*;

// ---------------------------------
// Task Class Definition
// ---------------------------------
class Task {
    int id, burstTime, arrivalTime, priority;
    int remainingTime, startTime = -1, completionTime = -1;
    int waitingTime = 0, turnaroundTime = 0;
    String name;

    public Task(int id, String name, int burstTime, int arrivalTime, int priority) {
        this.id = id;
        this.name = name;
        this.burstTime = burstTime;
        this.arrivalTime = arrivalTime;
        this.priority = priority;
        this.remainingTime = burstTime;
    }
}

// ---------------------------------
// Base Scheduler Class
// ---------------------------------
abstract class Scheduler {
    List<Task> tasks;

    public Scheduler(List<Task> tasks) {
        this.tasks = new ArrayList<>(tasks);
        this.tasks.sort(Comparator.comparingInt(t -> t.arrivalTime));
    }

    public abstract void run();

    protected void printStats() {
        System.out.println("\nAll tasks completed!");
        for (Task task : tasks) {
            System.out.println(task.name + " | Waiting Time = " + task.waitingTime + ", Turnaround Time = " + task.turnaroundTime);
        }
    }
}

// ---------------------------------
// FIFO Scheduler (First-In, First-Out)
// ---------------------------------
class FIFOScheduler extends Scheduler {
    public FIFOScheduler(List<Task> tasks) {
        super(tasks);
    }

    public void run() {
        int currentTime = 0;
        System.out.println("\n=== FIFO Scheduler ===");
        for (Task task : tasks) {
            if (currentTime < task.arrivalTime) {
                currentTime = task.arrivalTime;
            }
            task.startTime = currentTime;
            System.out.println("FIFO: Executing " + task.name + " at time " + currentTime);
            try { Thread.sleep(300); } catch (InterruptedException e) { }
            currentTime += task.burstTime;
            task.completionTime = currentTime;
            task.turnaroundTime = task.completionTime - task.arrivalTime;
            task.waitingTime = task.startTime - task.arrivalTime;
            System.out.println("FIFO: " + task.name + " completed at time " + currentTime);
        }
        printStats();
    }
}

// ---------------------------------
// Round Robin Scheduler
// ---------------------------------
class RoundRobinScheduler extends Scheduler {
    int timeQuantum;

    public RoundRobinScheduler(List<Task> tasks, int timeQuantum) {
        super(tasks);
        this.timeQuantum = timeQuantum;
    }

    public void run() {
        int currentTime = 0;
        Queue<Task> queue = new LinkedList<>();
        int index = 0;
        System.out.println("\n=== Round Robin Scheduler ===");

        while (!queue.isEmpty() || index < tasks.size()) {
            while (index < tasks.size() && tasks.get(index).arrivalTime <= currentTime) {
                queue.add(tasks.get(index++));
            }
            if (queue.isEmpty()) {
                if (index < tasks.size()) currentTime = tasks.get(index).arrivalTime;
                continue;
            }
            Task task = queue.poll();
            if (task.startTime == -1) task.startTime = currentTime;
            int executeTime = Math.min(task.remainingTime, timeQuantum);
            System.out.println("RR: Executing " + task.name + " for " + executeTime + " time units.");
            try { Thread.sleep(300); } catch (InterruptedException e) { }
            currentTime += executeTime;
            task.remainingTime -= executeTime;
            while (index < tasks.size() && tasks.get(index).arrivalTime <= currentTime) {
                queue.add(tasks.get(index++));
            }
            if (task.remainingTime > 0) {
                queue.add(task);
            } else {
                task.completionTime = currentTime;
                task.turnaroundTime = task.completionTime - task.arrivalTime;
                task.waitingTime = task.turnaroundTime - task.burstTime;
                System.out.println("RR: " + task.name + " completed at time " + currentTime);
            }
        }
        printStats();
    }
}

// ---------------------------------
// Priority Scheduler (Non-Preemptive)
// ---------------------------------
class PriorityScheduler extends Scheduler {
    public PriorityScheduler(List<Task> tasks) {
        super(tasks);
    }

    public void run() {
        int currentTime = 0;
        PriorityQueue<Task> pq = new PriorityQueue<>(Comparator.comparingInt(t -> t.priority));
        int index = 0;
        System.out.println("\n=== Priority Scheduler ===");

        while (!pq.isEmpty() || index < tasks.size()) {
            while (index < tasks.size() && tasks.get(index).arrivalTime <= currentTime) {
                pq.add(tasks.get(index++));
            }
            if (pq.isEmpty()) {
                if (index < tasks.size()) currentTime = tasks.get(index).arrivalTime;
                continue;
            }
            Task task = pq.poll();
            if (task.startTime == -1) task.startTime = currentTime;
            System.out.println("Priority: Executing " + task.name + " (Priority: " + task.priority + ")");
            try { Thread.sleep(300); } catch (InterruptedException e) { }
            currentTime += task.burstTime;
            task.completionTime = currentTime;
            task.turnaroundTime = task.completionTime - task.arrivalTime;
            task.waitingTime = task.turnaroundTime - task.burstTime;
            System.out.println("Priority: " + task.name + " completed at time " + currentTime);
        }
        printStats();
    }
}

// ---------------------------------
// Main Execution
// ---------------------------------
public class SchedulingSimulator {
    public static void main(String[] args) {
        List<Task> tasks = Arrays.asList(
            new Task(1, "Task A", 10, 0, 2),
            new Task(2, "Task B", 5, 2, 1),
            new Task(3, "Task C", 8, 4, 3)
        );

        new FIFOScheduler(tasks).run();
        new RoundRobinScheduler(tasks, 3).run();
        new PriorityScheduler(tasks).run();
    }
}
