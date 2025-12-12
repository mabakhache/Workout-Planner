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
}

class Workout {}
class Exercise {}
class Profile {}
class WorkoutTableModel extends AbstractTableModel {
    // write in later
}
