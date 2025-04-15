import javax.swing.*;
import java.sql.*;
import java.time.*;
import java.util.*;

class Feeding {
    int id;
    String petName;
    LocalTime feedingTime;
    String foodType;
    LocalDate scheduledDate;
    boolean isFed;

    public Feeding(int id, String petName, LocalTime feedingTime, String foodType, LocalDate scheduledDate, boolean isFed) {
        this.id = id;
        this.petName = petName;
        this.feedingTime = feedingTime;
        this.foodType = foodType;
        this.scheduledDate = scheduledDate;
        this.isFed = isFed;
    }
}

class DBManager {
    private Connection conn;

    public DBManager() throws SQLException {
        try {
            conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/petcare", "root", "");
        } catch (SQLException e) {
            System.out.println("Database connection failed.");
            throw e;
        }
    }

    public void addFeeding(String petName, LocalTime time, String foodType, LocalDate date) throws SQLException {
        String query = "INSERT INTO feeding_schedule (pet_name, feeding_time, food_type, scheduled_date) VALUES (?, ?, ?, ?)";
        PreparedStatement stmt = conn.prepareStatement(query);
        stmt.setString(1, petName);
        stmt.setTime(2, Time.valueOf(time));
        stmt.setString(3, foodType);
        stmt.setDate(4, java.sql.Date.valueOf(date));
        stmt.executeUpdate();
    }

    public void updateFeeding(int id, String petName, LocalTime time, String foodType, LocalDate date) throws SQLException {
        String query = "UPDATE feeding_schedule SET pet_name=?, feeding_time=?, food_type=?, scheduled_date=? WHERE id=?";
        PreparedStatement stmt = conn.prepareStatement(query);
        stmt.setString(1, petName);
        stmt.setTime(2, Time.valueOf(time));
        stmt.setString(3, foodType);
        stmt.setDate(4, java.sql.Date.valueOf(date));
        stmt.setInt(5, id);
        int updated = stmt.executeUpdate();
        if (updated == 0) {
            throw new SQLException("No record found with ID " + id);
        }
    }

    public void markAsFed(int id) throws SQLException {
        String query = "UPDATE feeding_schedule SET is_fed = true WHERE id = ?";
        PreparedStatement stmt = conn.prepareStatement(query);
        stmt.setInt(1, id);
        int updated = stmt.executeUpdate();
        if (updated == 0) {
            throw new SQLException("No feeding found with ID " + id);
        }
    }

    public List<Feeding> getTodaysFeedings() throws SQLException {
        List<Feeding> list = new ArrayList<>();
        String query = "SELECT * FROM feeding_schedule WHERE scheduled_date = ?";
        PreparedStatement stmt = conn.prepareStatement(query);
        stmt.setDate(1, java.sql.Date.valueOf(LocalDate.now()));
        ResultSet rs = stmt.executeQuery();

        while (rs.next()) {
            list.add(new Feeding(
                    rs.getInt("id"),
                    rs.getString("pet_name"),
                    rs.getTime("feeding_time").toLocalTime(),
                    rs.getString("food_type"),
                    rs.getDate("scheduled_date").toLocalDate(),
                    rs.getBoolean("is_fed")
            ));
        }
        return list;
    }

    public List<Feeding> getMissedFeedings() throws SQLException {
        List<Feeding> list = new ArrayList<>();
        String query = "SELECT * FROM feeding_schedule WHERE scheduled_date < ? AND is_fed = false";
        PreparedStatement stmt = conn.prepareStatement(query);
        stmt.setDate(1, java.sql.Date.valueOf(LocalDate.now()));
        ResultSet rs = stmt.executeQuery();

        while (rs.next()) {
            list.add(new Feeding(
                    rs.getInt("id"),
                    rs.getString("pet_name"),
                    rs.getTime("feeding_time").toLocalTime(),
                    rs.getString("food_type"),
                    rs.getDate("scheduled_date").toLocalDate(),
                    rs.getBoolean("is_fed")
            ));
        }
        return list;
    }

    public Map<String, Integer> getFeedingStats() throws SQLException {
        Map<String, Integer> stats = new HashMap<>();
        String query = "SELECT pet_name, COUNT(*) AS total FROM feeding_schedule WHERE is_fed = true GROUP BY pet_name";
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(query);

        while (rs.next()) {
            stats.put(rs.getString("pet_name"), rs.getInt("total"));
        }

        return stats;
    }
}

public class SmartPetFeedingSystem {
    public static void main(String[] args) {
        try {
            DBManager db = new DBManager();

            JFrame frame = new JFrame("Smart Pet Feeding System");
            JButton addBtn = new JButton("Add Feeding");
            JButton todayBtn = new JButton("Today's Feedings");
            JButton missedBtn = new JButton("Missed Feedings");
            JButton statsBtn = new JButton("Feeding Stats");
            JButton updateBtn = new JButton("Update Feeding Info");
            JButton fedBtn = new JButton("Mark as Fed");

            addBtn.setBounds(50, 30, 200, 30);
            todayBtn.setBounds(50, 70, 200, 30);
            missedBtn.setBounds(50, 110, 200, 30);
            statsBtn.setBounds(50, 150, 200, 30);
            updateBtn.setBounds(50, 190, 200, 30);
            fedBtn.setBounds(50, 230, 200, 30);

            frame.add(addBtn);
            frame.add(todayBtn);
            frame.add(missedBtn);
            frame.add(statsBtn);
            frame.add(updateBtn);
            frame.add(fedBtn);

            frame.setSize(320, 330);
            frame.setLayout(null);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setVisible(true);

        
            addBtn.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    try {
                        String pet = JOptionPane.showInputDialog("Pet Name:");
                        String timeStr = JOptionPane.showInputDialog("Feeding Time (HH:mm):");
                        String food = JOptionPane.showInputDialog("Food Type:");
                        String dateStr = JOptionPane.showInputDialog("Scheduled Date (YYYY-MM-DD):");

                        LocalTime time = LocalTime.parse(timeStr);
                        LocalDate date = LocalDate.parse(dateStr);

                        db.addFeeding(pet, time, food, date);
                        JOptionPane.showMessageDialog(frame, "Feeding schedule added!");
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(frame, "Error: Invalid input.");
                    }
                }
            });

            todayBtn.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    try {
                        List<Feeding> list = db.getTodaysFeedings();
                        StringBuilder sb = new StringBuilder("Today's Feedings:\n");
                        for (Feeding f : list) {
                            sb.append("ID: ").append(f.id).append(" - ")
                              .append(f.petName).append(" at ").append(f.feedingTime)
                              .append(" | Fed: ").append(f.isFed ? "Yes" : "No").append("\n");
                        }
                        JOptionPane.showMessageDialog(frame, sb.length() == 0 ? "No feedings today." : sb.toString());
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            });

            missedBtn.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    try {
                        List<Feeding> list = db.getMissedFeedings();
                        StringBuilder sb = new StringBuilder("Missed Feedings:\n");
                        for (Feeding f : list) {
                            sb.append("ID: ").append(f.id).append(" - ")
                              .append(f.petName).append(" on ").append(f.scheduledDate).append("\n");
                        }
                        JOptionPane.showMessageDialog(frame, sb.length() == 0 ? "No missed feedings." : sb.toString());
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            });

            statsBtn.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    try {
                        Map<String, Integer> stats = db.getFeedingStats();
                        StringBuilder sb = new StringBuilder("Feeding Stats:\n");
                        for (Map.Entry<String, Integer> entry : stats.entrySet()) {
                            sb.append(entry.getKey()).append(": ").append(entry.getValue()).append(" times\n");
                        }
                        JOptionPane.showMessageDialog(frame, sb.length() == 0 ? "No stats available." : sb.toString());
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            });

            updateBtn.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    try {
                        String idStr = JOptionPane.showInputDialog("Feeding ID to update:");
                        int id = Integer.parseInt(idStr);

                        String pet = JOptionPane.showInputDialog("New Pet Name:");
                        String timeStr = JOptionPane.showInputDialog("New Feeding Time (HH:mm):");
                        String food = JOptionPane.showInputDialog("New Food Type:");
                        String dateStr = JOptionPane.showInputDialog("New Scheduled Date (YYYY-MM-DD):");

                        LocalTime time = LocalTime.parse(timeStr);
                        LocalDate date = LocalDate.parse(dateStr);

                        db.updateFeeding(id, pet, time, food, date);
                        JOptionPane.showMessageDialog(frame, "Feeding info updated!");
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(frame, "Error updating feeding info: " + ex.getMessage());
                    }
                }
            });

            fedBtn.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    try {
                        String idStr = JOptionPane.showInputDialog("Enter Feeding ID to mark as fed:");
                        int id = Integer.parseInt(idStr);
                        db.markAsFed(id);
                        JOptionPane.showMessageDialog(frame, "Feeding marked as fed!");
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(frame, "Error: " + ex.getMessage());
                    }
                }
            });

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Startup error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}