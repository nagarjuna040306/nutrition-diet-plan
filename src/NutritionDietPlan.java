import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.*;

public class NutritionDietPlan {

    private JTextField heightField, weightField, ageField;
    private Connection connection;
    private boolean hasDiabetes; // Stores the user's diabetes status

    public NutritionDietPlan() {
        // Set up database connection
        connectToDatabase();

        // Set up the main input frame
        JFrame inputFrame = new JFrame("Enter Your Details");
        inputFrame.setSize(700, 700);
        inputFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        inputFrame.setLocationRelativeTo(null);
        inputFrame.setLayout(new GridLayout(5, 2, 5, 5));

        // Create labels and text fields for input
        JLabel heightLabel = new JLabel("Enter Height (cm): ");
        heightField = new JTextField();

        JLabel weightLabel = new JLabel("Enter Weight (kg): ");
        weightField = new JTextField();

        JLabel ageLabel = new JLabel("Enter Age: ");
        ageField = new JTextField();

        JButton submitButton = new JButton("Next");

        // Add action listener for the button
        submitButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    double height = Double.parseDouble(heightField.getText()) / 100;
                    double weight = Double.parseDouble(weightField.getText());
                    int age = Integer.parseInt(ageField.getText());
                    double bmi = weight / (height * height);

                    // Store user information in the database
                    storeUserData(height, weight, age, bmi);

                    // Open the diabetes input GUI
                    openDiabetesInputGUI(bmi);
                    inputFrame.dispose();

                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(null, "Please enter valid numerical values for height, weight, and age.", "Input Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        // Add components to the input frame
        inputFrame.add(heightLabel);
        inputFrame.add(heightField);
        inputFrame.add(weightLabel);
        inputFrame.add(weightField);
        inputFrame.add(ageLabel);
        inputFrame.add(ageField);
        inputFrame.add(new JLabel()); // Empty cell for alignment
        inputFrame.add(submitButton);

        // Show the input frame
        inputFrame.setVisible(true);
    }

    // Database connection method
    private void connectToDatabase() {
        try {
            String url = "jdbc:mysql://localhost:3306/nutritiondb";
            String user = "root";
            String password = "Password@132";

            connection = DriverManager.getConnection(url, user, password);
            System.out.println("Connected to the database successfully!");

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Failed to connect to the database.", "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Store user data in the database
    private void storeUserData(double height, double weight, int age, double bmi) {
        try {
            String query = "INSERT INTO users (height, weight, age, bmi) VALUES (?, ?, ?, ?)";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setDouble(1, height);
            statement.setDouble(2, weight);
            statement.setInt(3, age);
            statement.setDouble(4, bmi);
            statement.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Failed to store data in the database.", "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Open a new GUI for diabetes input
    private void openDiabetesInputGUI(double bmi) {
        JFrame diabetesFrame = new JFrame("Diabetes Information");
        diabetesFrame.setSize(400, 200);
        diabetesFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        diabetesFrame.setLocationRelativeTo(null);
        diabetesFrame.setLayout(new GridLayout(3, 1, 5, 5));

        JLabel diabetesLabel = new JLabel("Do you have diabetes?");
        JCheckBox diabetesCheckBox = new JCheckBox();

        JButton submitButton = new JButton("Get Diet Plan");
        submitButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                hasDiabetes = diabetesCheckBox.isSelected();
                storeDiabetesInfo(hasDiabetes);
                diabetesFrame.dispose();

                // Fetch and display the diet plan
                String dietPlan = fetchDietPlan(bmi, hasDiabetes);
                displayDietPlan(dietPlan);
            }
        });

        // Add components to the frame
        diabetesFrame.add(diabetesLabel);
        diabetesFrame.add(diabetesCheckBox);
        diabetesFrame.add(submitButton);

        // Show the frame
        diabetesFrame.setVisible(true);
    }

    // Store diabetes information in the database
    private void storeDiabetesInfo(boolean hasDiabetes) {
        String query = "INSERT INTO diabetes (has_diabetes) VALUES (?)";
        
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setBoolean(1, hasDiabetes);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Failed to store diabetes information in the database: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Fetch diet plan from the database based on BMI and diabetes status
    private String fetchDietPlan(double bmi, boolean hasDiabetes) {
        StringBuilder dietPlan = new StringBuilder("7-Day Diet Plan:\n\n");
        try {
            String fetch = "";
            if (bmi < 18) {
                fetch = "<18.5";
            } else if (bmi < 25) {
                fetch = "<25";
            } else if (bmi < 30) {
                fetch = "<30";
            } else {
                fetch = ">=30";
            }

            String query = "SELECT * FROM dietplans WHERE bmi_category = ?";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, fetch);

            ResultSet resultSet = statement.executeQuery();
            String[] daysOfWeek = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};

            while (resultSet.next()) {
                for (int i = 0; i < 7; i++) {
                    String text = resultSet.getString("day" + (i + 1) + " Text");
                    if (hasDiabetes && text.contains("sugar")) {
                        text = text.replace("sugar", "sugar-free");
                    }
                    dietPlan.append(daysOfWeek[i]).append(": ").append(text).append("\n\n");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Failed to retrieve diet plan from the database.", "Database Error", JOptionPane.ERROR_MESSAGE);
        }
        return dietPlan.toString();
    }

    // Display the diet plan in a new output frame
    private void displayDietPlan(String dietPlan) {
        JFrame outputFrame = new JFrame("Your 7-Day Diet Plan");
        outputFrame.setSize(700, 700);
        outputFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        outputFrame.setLocationRelativeTo(null);

        JTextArea resultArea = new JTextArea(dietPlan);
        resultArea.setLineWrap(true);
        resultArea.setWrapStyleWord(true);
        resultArea.setEditable(false);
        resultArea.setFont(new Font("Arial", Font.PLAIN, 15));

        outputFrame.add(new JScrollPane(resultArea));
        outputFrame.setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(NutritionDietPlan::new);
    }
}
