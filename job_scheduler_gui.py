import tkinter as tk
from tkinter import ttk, messagebox

# ======================== TASK CLASS ========================
class Task:
    def __init__(self, id, name, burst_time, arrival_time, priority=10):
        self.id = id
        self.name = name
        self.burst_time = burst_time
        self.remaining_time = burst_time
        self.arrival_time = arrival_time
        self.priority = priority
        self.start_time = -1
        self.completion_time = -1
        self.waiting_time = 0
        self.turnaround_time = 0

# ======================== BASE SCHEDULER CLASS ========================
class Scheduler:
    def __init__(self, tasks):
        self.tasks = [Task(t.id, t.name, t.burst_time, t.arrival_time, t.priority) for t in tasks]

    def run(self):
        raise NotImplementedError("Subclasses must implement the run() method.")

# ======================== FIFO SCHEDULER ========================
def selection_sort_by_arrival(task_list):
    n = len(task_list)
    for i in range(n):
        min_index = i
        for j in range(i + 1, n):
            if task_list[j].arrival_time < task_list[min_index].arrival_time:
                min_index = j
        task_list[i], task_list[min_index] = task_list[min_index], task_list[i]

class FIFOScheduler(Scheduler):
    def run(self):
        selection_sort_by_arrival(self.tasks)
        current_time = 0
        output = ""
        for task in self.tasks:
            if current_time < task.arrival_time:
                current_time = task.arrival_time
            task.start_time = current_time
            output += f"FIFO: Executing {task.name} at time {current_time}\n"
            current_time += task.burst_time
            task.completion_time = current_time
            task.turnaround_time = task.completion_time - task.arrival_time
            task.waiting_time = task.start_time - task.arrival_time
            output += f"FIFO: {task.name} completed at time {current_time}\n"

        output += "\nFIFO: All tasks completed!\n"
        for task in self.tasks:
            output += f"{task.name}: Waiting Time = {task.waiting_time}, Turnaround Time = {task.turnaround_time}\n"
        return output

# ======================== PRIORITY SCHEDULER ========================
class Node:
    def __init__(self, task):
        self.task = task
        self.next = None

class PriorityQueue:
    def __init__(self):
        self.head = None

    def insert_task(self, task):
        new_node = Node(task)
        if self.head is None or task.priority < self.head.task.priority:
            new_node.next = self.head
            self.head = new_node
        else:
            current = self.head
            while current.next and current.next.task.priority <= task.priority:
                current = current.next
            new_node.next = current.next
            current.next = new_node

    def remove_min(self):
        if not self.head:
            return None
        min_task = self.head.task
        self.head = self.head.next
        return min_task

    def is_empty(self):
        return self.head is None

class PriorityScheduler(Scheduler):
    def run(self):
        pq = PriorityQueue()
        for task in self.tasks:
            pq.insert_task(task)

        current_time = 0
        completed_tasks = 0
        total_tasks = len(self.tasks)
        output = ""

        while completed_tasks < total_tasks:
            task = pq.remove_min()
            if task.start_time == -1:
                if current_time < task.arrival_time:
                    current_time = task.arrival_time
                task.start_time = current_time

            output += f"Priority: Executing {task.name} (Priority: {task.priority})\n"
            current_time += task.burst_time
            task.completion_time = current_time
            task.turnaround_time = task.completion_time - task.arrival_time
            task.waiting_time = task.turnaround_time - task.burst_time
            output += f"Priority: {task.name} completed at time {current_time}\n"
            completed_tasks += 1

        output += "\nPriority: All tasks completed!\n"
        for task in self.tasks:
            output += f"{task.name}: Waiting Time = {task.waiting_time}, Turnaround Time = {task.turnaround_time}\n"
        return output

# ======================== ROUND ROBIN WITH CIRCULAR QUEUE ========================
class Array:
    def __init__(self, capacity):
        self.capacity = capacity
        self.array = [None] * capacity
        self.front = -1
        self.rear = -1

    def isFull(self):
        return (self.rear + 1) % self.capacity == self.front

    def isEmpty(self):
        return self.front == -1

    def insert(self, value):
        if self.isFull():
            return
        if self.isEmpty():
            self.front = 0
        self.rear = (self.rear + 1) % self.capacity
        self.array[self.rear] = value

    def remove(self):
        if self.isEmpty():
            return None
        value = self.array[self.front]
        if self.front == self.rear:
            self.front = -1
            self.rear = -1
        else:
            self.front = (self.front + 1) % self.capacity
        return value

class CircularQueue:
    def __init__(self, capacity):
        self.array = Array(capacity)

    def enqueue(self, value):
        self.array.insert(value)

    def dequeue(self):
        return self.array.remove()

    def isEmpty(self):
        return self.array.isEmpty()

class RoundRobinScheduler(Scheduler):
    def __init__(self, tasks, quantum):
        super().__init__(tasks)
        self.quantum = quantum
        self.queue = CircularQueue(100)

    def run(self):
        current_time = 0
        for task in self.tasks:
            self.queue.enqueue(task)

        output = "Round Robin: Starting execution...\n"
        while not self.queue.isEmpty():
            task = self.queue.dequeue()
            if task.start_time == -1:
                if current_time < task.arrival_time:
                    current_time = task.arrival_time
                task.start_time = current_time

            exec_time = min(self.quantum, task.remaining_time)
            output += f"RR: Executing {task.name} for {exec_time} units at time {current_time}\n"
            current_time += exec_time
            task.remaining_time -= exec_time

            if task.remaining_time == 0:
                task.completion_time = current_time
                task.turnaround_time = task.completion_time - task.arrival_time
                task.waiting_time = task.turnaround_time - task.burst_time
                output += f"RR: {task.name} completed at time {current_time}\n"
            else:
                self.queue.enqueue(task)

        output += "\nRound Robin: All tasks completed!\n"
        for task in self.tasks:
            output += f"{task.name}: Waiting Time = {task.waiting_time}, Turnaround Time = {task.turnaround_time}\n"
        return output

# ======================== GUI CODE ========================
class JobSchedulerGUI:
    def __init__(self, root):
        self.root = root
        self.root.title("Job Scheduling Simulator")

        self.tasks = []

        # Entry form
        form_frame = tk.Frame(root)
        form_frame.pack(pady=10)

        tk.Label(form_frame, text="ID").grid(row=0, column=0)
        tk.Label(form_frame, text="Name").grid(row=0, column=1)
        tk.Label(form_frame, text="Burst").grid(row=0, column=2)
        tk.Label(form_frame, text="Arrival").grid(row=0, column=3)
        tk.Label(form_frame, text="Priority").grid(row=0, column=4)

        self.entries = [tk.Entry(form_frame, width=5) for _ in range(5)]
        for i, entry in enumerate(self.entries):
            entry.grid(row=1, column=i)

        tk.Button(form_frame, text="Add Task", command=self.add_task).grid(row=1, column=5)

        # Scheduler Options
        options_frame = tk.Frame(root)
        options_frame.pack()

        self.scheduler_var = tk.StringVar(value="FIFO")
        tk.Label(options_frame, text="Scheduler:").pack(side=tk.LEFT)
        tk.OptionMenu(options_frame, self.scheduler_var, "FIFO", "Priority", "Round Robin").pack(side=tk.LEFT)

        tk.Label(options_frame, text="Quantum:").pack(side=tk.LEFT)
        self.quantum_entry = tk.Entry(options_frame, width=5)
        self.quantum_entry.insert(0, "4")
        self.quantum_entry.pack(side=tk.LEFT)

        tk.Button(root, text="Run Scheduler", command=self.run_scheduler).pack(pady=5)

        # Output area
        self.output_text = tk.Text(root, height=20, width=80)
        self.output_text.pack()

    def add_task(self):
        try:
            id = int(self.entries[0].get())
            name = self.entries[1].get()
            burst = int(self.entries[2].get())
            arrival = int(self.entries[3].get())
            priority = int(self.entries[4].get())
            self.tasks.append(Task(id, name, burst, arrival, priority))
            messagebox.showinfo("Success", f"Task '{name}' added.")
            for entry in self.entries:
                entry.delete(0, tk.END)
        except ValueError:
            messagebox.showerror("Input Error", "Please enter valid task details.")

    def run_scheduler(self):
        if not self.tasks:
            messagebox.showwarning("No Tasks", "Add tasks before running scheduler.")
            return

        algo = self.scheduler_var.get()
        quantum = int(self.quantum_entry.get()) if self.quantum_entry.get().isdigit() else 4

        if algo == "FIFO":
            scheduler = FIFOScheduler(self.tasks)
        elif algo == "Priority":
            scheduler = PriorityScheduler(self.tasks)
        elif algo == "Round Robin":
            scheduler = RoundRobinScheduler(self.tasks, quantum)
        else:
            return

        result = scheduler.run()
        self.output_text.delete(1.0, tk.END)
        self.output_text.insert(tk.END, result)
        self.tasks = []  # Reset for next simulation

# Run GUI
if __name__ == "__main__":
    root = tk.Tk()
    app = JobSchedulerGUI(root)
    root.mainloop()