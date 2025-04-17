package guiapp;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.event.*;
import java.util.*;

public class JobSchedulerGUI extends JFrame {
    private JTable table;
    private DefaultTableModel tableModel;
    private JTextField nameField, burstField, arrivalField, priorityField;
    private List<Task> userTasks = new ArrayList<>();

    public JobSchedulerGUI() {
        setTitle("Job Scheduling Simulator");
        setSize(750, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(null);

        // --- Title Label ---
        JLabel titleLabel = new JLabel("Job Scheduling Simulator", SwingConstants.CENTER);
        titleLabel.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 24));
        titleLabel.setBounds(10, 0, 710, 40);
        add(titleLabel);

        // --- Input Fields ---
        JLabel nameLabel = new JLabel("Task Name:");
        nameLabel.setBounds(10, 50, 80, 25);
        add(nameLabel);

        nameField = new JTextField();
        nameField.setBounds(90, 50, 80, 25);
        add(nameField);

        JLabel burstLabel = new JLabel("Burst:");
        burstLabel.setBounds(180, 50, 50, 25);
        add(burstLabel);

        burstField = new JTextField();
        burstField.setBounds(220, 50, 50, 25);
        add(burstField);

        JLabel arrivalLabel = new JLabel("Arrival:");
        arrivalLabel.setBounds(280, 50, 60, 25);
        add(arrivalLabel);

        arrivalField = new JTextField();
        arrivalField.setBounds(340, 50, 50, 25);
        add(arrivalField);

        JLabel priorityLabel = new JLabel("Priority:");
        priorityLabel.setBounds(400, 50, 60, 25);
        add(priorityLabel);

        priorityField = new JTextField();
        priorityField.setBounds(460, 50, 50, 25);
        add(priorityField);

        JButton addButton = new JButton("Add Task");
        addButton.setBounds(530, 50, 100, 25);
        add(addButton);

        // --- Buttons ---
        JButton fifoButton = new JButton("Run FIFO");
        fifoButton.setBounds(10, 420, 150, 30);
        add(fifoButton);

        JButton priorityButton = new JButton("Run Priority Queue");
        priorityButton.setBounds(180, 420, 150, 30);
        add(priorityButton);

        JButton rrButton = new JButton("Run Round Robin");
        rrButton.setBounds(350, 420, 170, 30);
        add(rrButton);

        // --- Table ---
        tableModel = new DefaultTableModel(
            new String[]{"Task", "Start", "Completion", "Turnaround", "Waiting"}, 0
        );
        table = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBounds(10, 90, 710, 310);
        add(scrollPane);

        // --- Event Handlers ---
        addButton.addActionListener(e -> {
            try {
                String name = nameField.getText().trim();
                int burst = Integer.parseInt(burstField.getText().trim());
                int arrival = Integer.parseInt(arrivalField.getText().trim());
                int priority = Integer.parseInt(priorityField.getText().trim());
                userTasks.add(new Task(name, burst, arrival, priority));
                JOptionPane.showMessageDialog(this, "Task added!");
                nameField.setText("");
                burstField.setText("");
                arrivalField.setText("");
                priorityField.setText("");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Invalid input!");
            }
        });

        fifoButton.addActionListener(e -> runScheduler("FIFO"));
        priorityButton.addActionListener(e -> runScheduler("PRIORITY"));
        rrButton.addActionListener(e -> runScheduler("RR"));

        setLocationRelativeTo(null);
    }

    private void runScheduler(String type) {
        List<Task> tasks = new ArrayList<>();
        for (Task t : userTasks) tasks.add(t.copy());

        Scheduler scheduler = switch (type) {
            case "FIFO" -> new FIFOScheduler(tasks, tableModel);
            case "PRIORITY" -> new PriorityScheduler(tasks, tableModel);
            case "RR" -> new RoundRobinScheduler(tasks, tableModel, 4);
            default -> null;
        };

        if (scheduler != null) scheduler.run();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new JobSchedulerGUI().setVisible(true));
    }
}

// ------------------------- Task Class -------------------------

class Task {
    String name;
    int burstTime, arrivalTime, priority;
    int startTime = -1, completionTime = -1;
    int remainingTime, turnaroundTime, waitingTime;

    public Task(String name, int burstTime, int arrivalTime, int priority) {
        this.name = name;
        this.burstTime = burstTime;
        this.arrivalTime = arrivalTime;
        this.priority = priority;
        this.remainingTime = burstTime;
    }

    public Task copy() {
        return new Task(name, burstTime, arrivalTime, priority);
    }
}

// ------------------------- Scheduler Base Class -------------------------

abstract class Scheduler {
    protected List<Task> tasks;
    protected DefaultTableModel tableModel;

    public Scheduler(List<Task> tasks, DefaultTableModel tableModel) {
        this.tasks = new ArrayList<>();
        for (Task t : tasks) this.tasks.add(t.copy());
        this.tasks.sort(Comparator.comparingInt(t -> t.arrivalTime));
        this.tableModel = tableModel;
    }

    public abstract void run();

    protected void updateTable() {
        SwingUtilities.invokeLater(() -> {
            tableModel.setRowCount(0);
            for (Task task : tasks) {
                tableModel.addRow(new Object[]{
                    task.name,
                    task.startTime,
                    task.completionTime,
                    task.turnaroundTime,
                    task.waitingTime
                });
            }
        });
    }
}

// ------------------------- FIFO Scheduler -------------------------

class FIFOScheduler extends Scheduler {
    public FIFOScheduler(List<Task> tasks, DefaultTableModel tableModel) {
        super(tasks, tableModel);
    }

    @Override
    public void run() {
        int currentTime = 0;
        for (Task task : tasks) {
            if (currentTime < task.arrivalTime) currentTime = task.arrivalTime;
            task.startTime = currentTime;
            currentTime += task.burstTime;
            task.completionTime = currentTime;
            task.turnaroundTime = task.completionTime - task.arrivalTime;
            task.waitingTime = task.startTime - task.arrivalTime;
        }
        updateTable();
    }
}

// ------------------------- Priority Scheduler -------------------------

class PriorityScheduler extends Scheduler {
    public PriorityScheduler(List<Task> tasks, DefaultTableModel tableModel) {
        super(tasks, tableModel);
    }

    @Override
    public void run() {
        int currentTime = 0, index = 0;
        PriorityQueue<Task> pq = new PriorityQueue<>(Comparator.comparingInt(t -> t.priority));

        while (index < tasks.size() || !pq.isEmpty()) {
            while (index < tasks.size() && tasks.get(index).arrivalTime <= currentTime) {
                pq.add(tasks.get(index++));
            }

            if (pq.isEmpty()) {
                currentTime = tasks.get(index).arrivalTime;
                continue;
            }

            Task task = pq.poll();
            task.startTime = currentTime;
            currentTime += task.burstTime;
            task.completionTime = currentTime;
            task.turnaroundTime = task.completionTime - task.arrivalTime;
            task.waitingTime = task.turnaroundTime - task.burstTime;
        }
        updateTable();
    }
}

// ------------------------- Round Robin Scheduler -------------------------

class RoundRobinScheduler extends Scheduler {
    private final int quantum;

    public RoundRobinScheduler(List<Task> tasks, DefaultTableModel tableModel, int quantum) {
        super(tasks, tableModel);
        this.quantum = quantum;
    }

    @Override
    public void run() {
        int currentTime = 0, index = 0;
        CircularQueue queue = new CircularQueue(tasks.size() * 2);

        while (index < tasks.size() || !queue.isEmpty()) {
            while (index < tasks.size() && tasks.get(index).arrivalTime <= currentTime) {
                queue.enqueue(tasks.get(index++));
            }

            if (queue.isEmpty()) {
                currentTime = tasks.get(index).arrivalTime;
                continue;
            }

            Task task = queue.dequeue();

            if (task.startTime == -1) task.startTime = currentTime;

            int timeSlice = Math.min(task.remainingTime, quantum);
            task.remainingTime -= timeSlice;
            currentTime += timeSlice;

            while (index < tasks.size() && tasks.get(index).arrivalTime <= currentTime) {
                queue.enqueue(tasks.get(index++));
            }

            if (task.remainingTime > 0) {
                queue.enqueue(task);
            } else {
                task.completionTime = currentTime;
                task.turnaroundTime = task.completionTime - task.arrivalTime;
                task.waitingTime = task.turnaroundTime - task.burstTime;
            }
        }

        updateTable();
    }
}

// ------------------------- Circular Queue -------------------------

class CircularQueue {
    private final Task[] array;
    private int front, rear, size;

    public CircularQueue(int capacity) {
        array = new Task[capacity];
        front = 0;
        rear = -1;
        size = 0;
    }

    public void enqueue(Task task) {
        if (size == array.length) return;
        rear = (rear + 1) % array.length;
        array[rear] = task;
        size++;
    }

    public Task dequeue() {
        if (size == 0) return null;
        Task task = array[front];
        front = (front + 1) % array.length;
        size--;
        return task;
    }

    public boolean isEmpty() {
        return size == 0;
    }
}
