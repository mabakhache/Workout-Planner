import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;
import java.util.List;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

public class Main {
    // File where data is saved
    private static final String DATA_FILE = "workouts.json";

    // In-memory data
    private java.util.List<Workout> workouts = new ArrayList<>();
    private Profile profile = new Profile();

    // Swing components
    private JFrame frame;
    private DefaultListModel<String> workoutListModel;
    private JList<String> workoutJList;
    private WorkoutTableModel workoutTableModel;
    private JTable exerciseTable;
    private JLabel statusLabel;

    // Gson
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Main app = new Main();
            app.loadData();
            app.createAndShowGUI();
        });
    }

    // GUI
    private void createAndShowGUI() {
        frame = new JFrame("Workout Planner");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(900, 600);
        frame.setLocationRelativeTo(null);

        JPanel main = new JPanel(new BorderLayout(10, 10));
        main.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

        // Left: Workout list
        JPanel leftPanel = new JPanel(new BorderLayout(5,5));
        leftPanel.setPreferredSize(new Dimension(260, 0));
        leftPanel.add(new JLabel("Workout Plans:"), BorderLayout.NORTH);

        workoutListModel = new DefaultListModel<>();
        refreshWorkoutListModel();
        workoutJList = new JList<>(workoutListModel);
        workoutJList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane listScroll = new JScrollPane(workoutJList);
        leftPanel.add(listScroll, BorderLayout.CENTER);

        JPanel leftButtons = new JPanel(new GridLayout(1, 3, 5,5));
        JButton addWorkoutBtn = new JButton("New");
        JButton deleteWorkoutBtn = new JButton("Delete");
        JButton renameWorkoutBtn = new JButton("Rename");
        leftButtons.add(addWorkoutBtn);
        leftButtons.add(renameWorkoutBtn);
        leftButtons.add(deleteWorkoutBtn);
        leftPanel.add(leftButtons, BorderLayout.SOUTH);

        // Right: Workout details
        JPanel rightPanel = new JPanel(new BorderLayout(5,5));
        rightPanel.add(new JLabel("Workout Details:"), BorderLayout.NORTH);
        workoutTableModel = new WorkoutTableModel();
        exerciseTable = new JTable(workoutTableModel);
        exerciseTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane tableScroll = new JScrollPane(exerciseTable);
        rightPanel.add(tableScroll, BorderLayout.CENTER);

        JPanel rightButtonsTop = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addExerciseBtn = new JButton("Add Exercise");
        JButton editExerciseBtn = new JButton("Edit Exercise");
        JButton deleteExerciseBtn = new JButton("Remove Exercise");
        rightButtonsTop.add(addExerciseBtn);
        rightButtonsTop.add(editExerciseBtn);
        rightButtonsTop.add(deleteExerciseBtn);

        rightPanel.add(rightButtonsTop, BorderLayout.SOUTH);

        // Top menu
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        JMenuItem saveItem = new JMenuItem("Save");
        JMenuItem loadItem = new JMenuItem("Load");
        JMenuItem profileItem = new JMenuItem("Profile");
        JMenuItem exitItem = new JMenuItem("Exit");
        fileMenu.add(saveItem);
        fileMenu.add(loadItem);
        fileMenu.addSeparator();
        fileMenu.add(profileItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);
        menuBar.add(fileMenu);
        frame.setJMenuBar(menuBar);

        // Bottom status
        JPanel bottom = new JPanel(new BorderLayout());
        statusLabel = new JLabel("Ready");
        bottom.add(statusLabel, BorderLayout.WEST);

        main.add(leftPanel, BorderLayout.WEST);
        main.add(rightPanel, BorderLayout.CENTER);
        main.add(bottom, BorderLayout.SOUTH);

        frame.getContentPane().add(main);
        frame.setVisible(true);

        // Event handlers
        addWorkoutBtn.addActionListener(e -> createNewWorkout());
        deleteWorkoutBtn.addActionListener(e -> deleteSelectedWorkout());
        renameWorkoutBtn.addActionListener(e -> renameSelectedWorkout());

        workoutJList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1) {
                    int idx = workoutJList.getSelectedIndex();
                    if (idx >= 0) {
                        workoutTableModel.setWorkout(workouts.get(idx));
                        status("Loaded workout: " + workouts.get(idx).name);
                    }
                }
                if (e.getClickCount() == 2) {
                    int idx = workoutJList.getSelectedIndex();
                    if (idx >= 0) {
                        openWorkoutDetailWindow(workouts.get(idx));
                    }
                }
            }
        });

        addExerciseBtn.addActionListener(e -> addExerciseToSelectedWorkout());
        editExerciseBtn.addActionListener(e -> editSelectedExercise());
        deleteExerciseBtn.addActionListener(e -> removeSelectedExercise());

        saveItem.addActionListener(e -> {
            saveData();
            status("Saved data.");
        });
        loadItem.addActionListener(e -> {
            loadData();
            refreshWorkoutListModel();
            workoutTableModel.setWorkout(null);
            status("Loaded data from file.");
        });
        profileItem.addActionListener(e -> openProfileDialog());
        exitItem.addActionListener(e -> {
            saveData();
            System.exit(0);
        });

        // Select first workout if available
        if (!workouts.isEmpty()) {
            workoutJList.setSelectedIndex(0);
            workoutTableModel.setWorkout(workouts.get(0));
        }
    }

    private void status(String msg) {
        statusLabel.setText(msg);
    }


    // Workout operations
    private void createNewWorkout() {
        String name = JOptionPane.showInputDialog(frame, "Enter workout name:", "New Workout", JOptionPane.PLAIN_MESSAGE);
        if (name != null && !name.trim().isEmpty()) {
            Workout w = new Workout(name.trim(), LocalDate.now().toString());
            workouts.add(w);
            refreshWorkoutListModel();
            workoutJList.setSelectedIndex(workouts.size()-1);
            workoutTableModel.setWorkout(w);
            saveData();
            status("Created workout: " + name);
        }
    }

    private void deleteSelectedWorkout() {
        int idx = workoutJList.getSelectedIndex();
        if (idx >= 0) {
            int confirm = JOptionPane.showConfirmDialog(frame, "Delete selected workout?", "Confirm", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                workouts.remove(idx);
                refreshWorkoutListModel();
                workoutTableModel.setWorkout(null);
                saveData();
                status("Deleted workout.");
            }
        } else {
            JOptionPane.showMessageDialog(frame, "No workout selected.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void renameSelectedWorkout() {
        int idx = workoutJList.getSelectedIndex();
        if (idx >= 0) {
            String current = workouts.get(idx).name;
            String name = JOptionPane.showInputDialog(frame, "Rename workout:", current);
            if (name != null && !name.trim().isEmpty()) {
                workouts.get(idx).name = name.trim();
                refreshWorkoutListModel();
                saveData();
                status("Renamed workout.");
            }
        } else {
            JOptionPane.showMessageDialog(frame, "No workout selected.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void refreshWorkoutListModel() {
        if (workoutListModel == null) workoutListModel = new DefaultListModel<>();
        workoutListModel.clear();
        for (Workout w : workouts) {
            workoutListModel.addElement(w.name + " (" + w.date + ")");
        }
        if (workoutJList != null && !workouts.isEmpty()) workoutJList.setSelectedIndex(0);
    }

    private void openWorkoutDetailWindow(Workout workout) {
        JFrame detail = new JFrame("Workout: " + workout.name);
        detail.setSize(600,400);
        detail.setLocationRelativeTo(frame);

        JPanel p = new JPanel(new BorderLayout(5,5));
        WorkoutTableModel model = new WorkoutTableModel();
        model.setWorkout(workout);
        JTable table = new JTable(model);
        JScrollPane sc = new JScrollPane(table);
        p.add(sc, BorderLayout.CENTER);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addBtn = new JButton("Add Exercise");
        JButton editBtn = new JButton("Edit Exercise");
        JButton remBtn = new JButton("Remove Exercise");
        btns.add(addBtn);
        btns.add(editBtn);
        btns.add(remBtn);
        p.add(btns, BorderLayout.SOUTH);

        addBtn.addActionListener(e -> {
            Exercise ex = showExerciseDialog(null);
            if (ex != null) {
                workout.exercises.add(ex);
                model.fireTableDataChanged();
                saveData();
            }
        });
        editBtn.addActionListener(e -> {
            int r = table.getSelectedRow();
            if (r >= 0) {
                Exercise old = workout.exercises.get(r);
                Exercise updated = showExerciseDialog(old);
                if (updated != null) {
                    workout.exercises.set(r, updated);
                    model.fireTableDataChanged();
                    saveData();
                }
            } else JOptionPane.showMessageDialog(detail, "Select an exercise to edit.");
        });
        remBtn.addActionListener(e -> {
            int r = table.getSelectedRow();
            if (r >= 0) {
                workout.exercises.remove(r);
                model.fireTableDataChanged();
                saveData();
            } else JOptionPane.showMessageDialog(detail, "Select an exercise to remove.");
        });

        detail.getContentPane().add(p);
        detail.setVisible(true);
    }

    private void addExerciseToSelectedWorkout() {
        int idx = workoutJList.getSelectedIndex();
        if (idx >= 0) {
            Exercise ex = showExerciseDialog(null);
            if (ex != null) {
                workouts.get(idx).exercises.add(ex);
                workoutTableModel.fireTableDataChanged();
                saveData();
            }
        } else {
            JOptionPane.showMessageDialog(frame, "No workout selected.");
        }
    }

    private void editSelectedExercise() {
        int idx = workoutJList.getSelectedIndex();
        int row = exerciseTable.getSelectedRow();
        if (idx >= 0 && row >= 0) {
            Workout w = workouts.get(idx);
            Exercise old = w.exercises.get(row);
            Exercise updated = showExerciseDialog(old);
            if (updated != null) {
                w.exercises.set(row, updated);
                workoutTableModel.fireTableDataChanged();
                saveData();
            }
        } else {
            JOptionPane.showMessageDialog(frame, "Select a workout and an exercise row to edit.");
        }
    }

    private void removeSelectedExercise() {
        int idx = workoutJList.getSelectedIndex();
        int row = exerciseTable.getSelectedRow();
        if (idx >= 0 && row >= 0) {
            Workout w = workouts.get(idx);
            w.exercises.remove(row);
            workoutTableModel.fireTableDataChanged();
            saveData();
        } else {
            JOptionPane.showMessageDialog(frame, "Select a workout and an exercise row to remove.");
        }
    }
    // Exercise Details
    private Exercise showExerciseDialog(Exercise existing) {
        JTextField nameField = new JTextField();
        JTextField setsField = new JTextField();
        JTextField repsField = new JTextField();
        JTextField weightField = new JTextField();
        if (existing != null) {
            nameField.setText(existing.name);
            setsField.setText(String.valueOf(existing.sets));
            repsField.setText(String.valueOf(existing.reps));
            weightField.setText(existing.weight);
        }

        JPanel panel = new JPanel(new GridLayout(0,1,5,5));
        panel.add(new JLabel("Exercise name:"));
        panel.add(nameField);
        panel.add(new JLabel("Sets (integer):"));
        panel.add(setsField);
        panel.add(new JLabel("Reps (integer):"));
        panel.add(repsField);
        panel.add(new JLabel("Weight (e.g., 60 or bodyweight):"));
        panel.add(weightField);

        int result = JOptionPane.showConfirmDialog(frame, panel,
                existing == null ? "Add Exercise" : "Edit Exercise",
                JOptionPane.OK_CANCEL_OPTION);

        if (result == JOptionPane.OK_OPTION) {
            try {
                String name = nameField.getText().trim();
                int sets = Integer.parseInt(setsField.getText().trim());
                int reps = Integer.parseInt(repsField.getText().trim());
                String weight = weightField.getText().trim();

                if (name.isEmpty()) {
                    JOptionPane.showMessageDialog(frame, "Exercise name cannot be empty.");
                    return null;
                }

                return new Exercise(name, sets, reps, weight);

            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(frame, "Sets and reps must be whole numbers.");
                return null;
            }
        }

        return null;
    }
    // Profile
    private void openProfileDialog() {
        JTextField nameField = new JTextField(profile.name);
        JTextField ageField = new JTextField(String.valueOf(profile.age));
        JTextField heightField = new JTextField(String.valueOf(profile.height));
        JTextField weightField = new JTextField(String.valueOf(profile.weight));
        JTextField rmField = new JTextField(String.valueOf(profile.oneRepMax));

        JPanel p = new JPanel(new GridLayout(0, 1, 5, 5));
        p.add(new JLabel("Name:"));
        p.add(nameField);
        p.add(new JLabel("Age:"));
        p.add(ageField);
        p.add(new JLabel("Height (cm):"));
        p.add(heightField);
        p.add(new JLabel("Weight (kg):"));
        p.add(weightField);
        p.add(new JLabel("1RM Estimate (kg):"));
        p.add(rmField);

        int res = JOptionPane.showConfirmDialog(frame, p, "Profile", JOptionPane.OK_CANCEL_OPTION);
        if (res == JOptionPane.OK_OPTION) {
            try {
                profile.name = nameField.getText().trim();
                profile.age = Integer.parseInt(ageField.getText().trim());
                profile.height = Double.parseDouble(heightField.getText().trim());
                profile.weight = Double.parseDouble(weightField.getText().trim());
                profile.oneRepMax = Double.parseDouble(rmField.getText().trim());

                saveData();
                status("Profile updated.");

                showCalorieRecommendation();

            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(frame, "Invalid number input.");
            }
        }
    }
    private void showCalorieRecommendation() {
        // Mifflin–St Jeor estimate
        double bmr = 10 * profile.weight + 6.25 * profile.height - 5 * profile.age + 5;

        double maintain = bmr * 1.4;  // light activity default
        double gain = maintain * 1.15;
        double lose = maintain * 0.85;

        JOptionPane.showMessageDialog(frame,
                "Caloric Recommendations:\n\n" +
                        "Maintain Weight: " + (int) maintain + " kcal/day\n" +
                        "Gain Muscle: " + (int) gain + " kcal/day\n" +
                        "Lose Weight: " + (int) lose + " kcal/day",
                "Calories", JOptionPane.INFORMATION_MESSAGE);
    }
    // Save & Load
    private void saveData() {
        try (FileWriter writer = new FileWriter(DATA_FILE)) {
            JsonObject root = new JsonObject();

            root.add("profile", gson.toJsonTree(profile));
            root.add("workouts", gson.toJsonTree(workouts));

            gson.toJson(root, writer);

        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame, "Error saving data.");
        }
    }

    private void loadData() {
        if (!Files.exists(Paths.get(DATA_FILE))) return;

        try (FileReader reader = new FileReader(DATA_FILE)) {
            JsonObject root = gson.fromJson(reader, JsonObject.class);

            if (root.has("profile")) {
                profile = gson.fromJson(root.get("profile"), Profile.class);
            }
            if (root.has("workouts")) {
                Type wt = new TypeToken<List<Workout>>() {}.getType();
                workouts = gson.fromJson(root.get("workouts"), wt);
            }

        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame, "Error loading data.");
        }
    }
    // Data Models
    static class Workout {
        String name;
        String date;
        List<Exercise> exercises = new ArrayList<>();

        Workout(String name, String date) {
            this.name = name;
            this.date = date;
        }
    }

    class Exercise {
        String name;
        int sets;
        int reps;
        String weight;

        Exercise(String name, int sets, int reps, String weight) {
            this.name = name;
            this.sets = sets;
            this.reps = reps;
            this.weight = weight;
        }
    }

    static class Profile {
        String name = "";
        int age = 0;
        double height = 0;
        double weight = 0;
        double oneRepMax = 0;
    }

}
