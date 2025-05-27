import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;


public class QuizDatabaseSetup {
    private static final String URL = "jdbc:mysql://localhost:3306/?useSSL=false&serverTimezone=UTC";
    private static final String DB_NAME = "quiz";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    public static void main(String[] args) {
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             Statement stmt = conn.createStatement()) {

            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS " + DB_NAME);
            System.out.println("Baza 'quiz' utworzona lub już istnieje.");

            stmt.execute("USE " + DB_NAME);

            String createQuestions = "CREATE TABLE IF NOT EXISTS questions (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "question_text TEXT NOT NULL," +
                    "answer_a VARCHAR(255)," +
                    "answer_b VARCHAR(255)," +
                    "answer_c VARCHAR(255)," +
                    "answer_d VARCHAR(255)," +
                    "correct_answer CHAR(4)" +
                    ")";
            stmt.executeUpdate(createQuestions);
            System.out.println("Tabela 'questions' utworzona lub już istnieje.");

            String createAnswers = "CREATE TABLE IF NOT EXISTS answers (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "username VARCHAR(50)," +
                    "question_id INT," +
                    "user_answer VARCHAR(15)," +
                    "FOREIGN KEY (question_id) REFERENCES questions(id)" +
                    ")";
            stmt.executeUpdate(createAnswers);
            System.out.println("Tabela 'answers' utworzona lub już istnieje.");

            String createResults = "CREATE TABLE IF NOT EXISTS results (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "username VARCHAR(50)," +
                    "score INT" +
                    ")";
            stmt.executeUpdate(createResults);
            System.out.println("Tabela 'results' utworzona lub już istnieje.");

            System.out.println("Inicjalizacja bazy danych zakończona.");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
