import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class TM {
    private static final String LOG_FILE = "task_log.txt";
    private static final Map<String, Task> taskMap = new HashMap<>();
    private static final SimpleDateFormat dateFormat = 
        new SimpleDateFormat("dd/MM/yyyy HH:mm");

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Insufficient arguments.");
            return;
        }

        TaskManager taskManager = new TaskManager(taskMap);

        loadTasksFromLogFile();
       
        String command = args[0];
        String taskName = args.length > 1 ? args[1] : null;

        switch (command) {
            case "start":
                taskManager.startTask(taskName);
                break;
            case "stop":
                taskManager.stopTask(taskName);
                break;
            case "describe":
                if (args.length < 3) {
                    System.out.println("Insufficient arguments");
                    return;
                }
                String size = null;
                String description = args[2];
                if(args.length > 2){
                    size = args[3];
                }
                taskManager.describeTask(taskName, description, size);
                updateLogFile(command, taskName, description, size);
                break;
            case "summary":
                if (args.length > 1) {
                    taskManager.summaryTask(taskName);
                } else {
                    taskManager.summaryAllTasks();
                }
                break;
            case "size":
                if (args.length < 2) {
                    System.out.println("Insufficient arguments");
                    return;
                }
                taskManager.sizeTask(taskName, args[2]);
                break;
            case "rename":
                if (args.length < 2) {
                    System.out.println("Insufficient arguments ");
                    return;
                }
                String newTaskName = args[2];
                taskManager.renameTask(taskName, newTaskName);
                break;
            case "delete":
                taskManager.deleteTask(taskName);
                break;
            default:
                System.out.println("Unknown command: " + command);
        }

        // Pass the command directly to updateLogFile
        if (!command.equals("summary")&& !command.equals("describe")) {
            updateLogFile(command, taskName, null, null);
        }
    }
    private static void loadTasksFromLogFile() {
        try {
            List<String> lines = Files.readAllLines(Paths.get(LOG_FILE));
            for (String line : lines) {
                parseLogEntry(line);
            }
        } catch (IOException e) {
            System.out.println("Error loading log file: " + e.getMessage());
        }
    }
    private static void updateLogFile(String command, String taskName, 
        String description, String size) {
            try (PrintWriter writer = new PrintWriter(
                    new FileWriter(LOG_FILE, true))) {
                if (description == null) {
                    writer.println(String.format("%s,%s,%s", 
                        dateFormat.format(new Date()), command, taskName));
                } else {
                    writer.println(String.format("%s,%s,%s,%s,%s", 
                        dateFormat.format(new Date()), command, 
                            taskName, description, size));
                }
            } catch (IOException e) {
                System.out.println("Error updating log file: " + e.getMessage());
            }
    }

    static void parseLogEntry(String logEntry) {
    String[] parts = logEntry.split(",");
    String command = parts[1];
    String taskName = parts[2];

    try {
        String myDate = parts[0];
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        Date date = sdf.parse(myDate);
        long millis = date.getTime();
        if (taskMap.containsKey(taskName)) {
            if (command.equals("rename")) {
                taskMap.get(taskName).setName(parts[3]);
            } else if (command.equals("describe")) {
                taskMap.get(taskName).setDescription(taskMap.get(taskName)
                    .getDescription()+ " " + parts[3]);
                if (parts.length > 3) {
                    taskMap.get(taskName).setSize(parts[4]);
                }
            } else if (command.equals("stop")) {
                taskMap.get(taskName).stop(millis);
            }
            else if (command.equals("start")){
                if(taskMap.get(taskName).isInUse()){
                    throw new IllegalArgumentException("Invalid log file entry: "
                         + logEntry);
                }
                else{
                    taskMap.get(taskName).start(millis);
                }
            }
        } else {
            // New task handling
            if (command.equals("start")) {
                Task newTask = new Task(taskName);
                newTask.start(millis);
                taskMap.put(taskName, newTask);
            } else if (command.equals("stop")) {
                throw new IllegalArgumentException("Invalid log file entry: " 
                    + logEntry);
            } else if (command.equals("describe")) {
                Task newTask = new Task(taskName);
                newTask.setDescription(parts[3]);
                if (parts.length > 3) {
                    newTask.setSize(parts[4]);
                }
                taskMap.put(taskName, newTask);
            } else {
                throw new IllegalArgumentException("Invalid log file entry: "
                     + logEntry);
            }
        }

    } catch (ParseException e) {
        e.printStackTrace();
        System.out.println("Error parsing date in log entry: " 
            + logEntry);
    }
}

    
    public static String taskToLogEntry(Task task, String operation) {
        return String.format("%s,%s,%s,%s",
                dateFormat.format(new Date()),
                task.getName(),
                operation,
                task.getSize());
    }
}

class TaskManager {
    private final Map<String, Task> taskMap;

    public TaskManager(Map<String, Task> taskMap) {
        this.taskMap = taskMap;
    }

    public void startTask(String taskName) {
        // Check if the task is currently being worked on
        if(taskMap.containsKey(taskName)){
            if(taskMap.get(taskName).isInUse()){
                throw new IllegalStateException("Task '" + taskName 
                    + "' is already started and not stopped.");
            }else {
                taskMap.get(taskName).start();
                System.out.println("Task '" + taskName + "' started.");  
                return; 
            }
        } 
        Task newTask = new Task(taskName);
        taskMap.put(taskName, newTask);
    }

    public void stopTask(String taskName) {
        if(taskMap.containsKey(taskName)){
            if (taskMap.get(taskName).isInUse()) {
                taskMap.get(taskName).stop();
                System.out.println("Task '" + taskName + "' stopped.");
            } else {
                throw new IllegalStateException("Task '" + taskName 
                    + "' is stopped.");
            }
        }
    }

    public void describeTask(String taskName, String description,
         String size) {
        Task task = taskMap.computeIfAbsent(taskName, Task::new);

        if (task.getDescription() != null) {
            task.setDescription(task.getDescription() + " " + description);
        } else {
            task.setDescription(description);
        }

        if (size != null) {
            task.setSize(size);
        }

        System.out.println("Task '" + taskName + "' described. Description: " 
            + description + ", Size: " + size);
    }

    public void summaryTask (String taskName) {
        if(taskName.equals("S") || 
            taskName.equals("M") || 
            taskName.equals("L") || 
            taskName.equals("XL")){
                summarySize(taskName);
        }
        Task task = taskMap.get(taskName);
        if (task != null) {
            System.out.println("Summary for task '" + taskName 
                + "': " + task.getSummary());
        } else {
            System.out.println("Task '" + taskName + "' not found.");
        }
    }

    public void summarySize(String size) {
        List<Task> tasksOfSize = new ArrayList<>();
        for (Task task : taskMap.values()) {
            if (size == task.getSize()) {
                tasksOfSize.add(task);
            }
        }

        if (tasksOfSize.size() < 2) {
            System.out.println("Not enough taskMap of size " 
                + size + " for summary statistics.");
            return;
        }

        // Calculate min, max, and average time spent
        long minTime = Long.MAX_VALUE;
        long maxTime = Long.MIN_VALUE;
        long total = 0;

        for (Task task : tasksOfSize) {
            long timeSpent = task.getTimeSpent();
            
            if (timeSpent < minTime) {
                minTime = timeSpent;
            }

            if (timeSpent > maxTime) {
                maxTime = timeSpent;
            }

            total += timeSpent;
        }

        double avgTime = (double) total / tasksOfSize.size();

        // Print summary statistics
        System.out.println("Summary statistics for taskMap of size " 
            + size + ":");
        System.out.println("Min time spent: " + minTime 
            + " minutes");
        System.out.println("Max time spent: " + maxTime 
            + " minutes");
        System.out.println("Average time spent: " 
            + avgTime + " minutes");
    }

    public void summaryAllTasks() {
        System.out.println("Summary for all taskMap: ");
        for (Task task : taskMap.values()) {
            System.out.println(task.getSummary());
        }
    }

    public void sizeTask(String taskName, String size) {
        Task task = taskMap.computeIfAbsent(taskName, Task::new);
        task.setSize(size);
        System.out.println("Task '" + taskName + 
            "' sized. Size: " + size);
    }

    public void renameTask(String oldTaskName, String newTaskName) {
        Task task = taskMap.remove(oldTaskName);
        if (task != null) {
            task.setName(newTaskName);
            taskMap.put(newTaskName, task);
            System.out.println("Task '" + oldTaskName 
                + "' renamed to '" + newTaskName + "'.");
        } else {
            System.out.println("Task '" + oldTaskName + "' not found.");
        }
    }

    public void deleteTask(String taskName) {
        Task task = taskMap.remove(taskName);
        if (task != null) {
            System.out.println("Task '" + taskName + "' deleted.");
        } else {
            System.out.println("Task '" + taskName + "' not found.");
        }
    }

    public static String formatTime(long minutes) {
        long hours = minutes / 60;
        long remainingMinutes = minutes % 60;
        return String.format("%d hours %d minutes",
             hours, remainingMinutes);
    }
}

class Task {
    private String name;
    private String description;
    private String size;
    private long startTime;
    private long endTime;
    private long timeWorked;
    private boolean inUse;

    public Task(String name) {
        this.name = name;
        this.startTime = System.currentTimeMillis();
        this.inUse = true;
    }
     public void start() {
        this.startTime = System.currentTimeMillis();
        this.inUse = true;
    }
    public void start(long time) {
        this.startTime = time;
        this.inUse = true;
    }
    public void stop() {
        this.endTime = System.currentTimeMillis();
        this.inUse = false;
        this.timeWorked += (endTime - startTime);
    }
      public void stop(long time) {
        this.endTime = time;
        this.inUse = true;
        this.timeWorked += (endTime - startTime);
    }
    public void setInUse(boolean b){
        this.inUse = b;
    }
    public boolean isInUse() {
        return inUse;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getTimeSpent(){
        return timeWorked;
    }
    public String getSummary() {
        return "Name: " + name + ", Description: " + description
                 + ", Size: " + size +", Time logged: " 
                 + (timeWorked /(1000*60)) + " minutes.";
    }

    public String getName() {
        return name;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public String getDescription() {
        return description;
    }

    public String getSize() {
        return size;
    }

    public long getStartTime() {
        return startTime;
    }
}