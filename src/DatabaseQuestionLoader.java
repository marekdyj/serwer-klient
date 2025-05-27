import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DatabaseQuestionLoader {
    private static final String URL = "jdbc:mysql://localhost:3306/quiz";
    private static final String USER = "root";
    private static final String PASSWORD = "";
    private static List<Question> questions = Collections.synchronizedList(new ArrayList<>());
    private static final String QUESTIONS_FILE = "bazaPytan.txt";
    private static void loadQuestions(String fileName) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(fileName));
        String line;
        List<String> qLines = new ArrayList<>();
        int id=1;
        while ((line = reader.readLine()) != null) {
            if (line.isEmpty()) continue;
            qLines.add(line);
            if (qLines.size() == 6) {
                questions.add(new Question(qLines, id++));
                qLines.clear();
            }
        }
        reader.close();
    }
    static class Question {
        String questionText;
        String[] answers = new String[4];
        String correctAnswer;
        int id;

                Question(List<String> lines,int id) {
            questionText = lines.get(0);
            for (int i = 0; i < 4; i++) {
                answers[i] = lines.get(i + 1);
            }
            correctAnswer = lines.get(5).split(":")[1];
            this.id=id;
        }


        String toSendFormat() {
            return questionText + "\n" + String.join("\n", answers);
        }
    }
    public static void main(String[] args) throws IOException,java.sql.SQLException {
        loadQuestions(QUESTIONS_FILE);
        Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
        for(Question question : questions) {
            try (PreparedStatement stmt = conn.prepareStatement("INSERT INTO questions (id,question_text,answer_a,answer_b,answer_c,answer_d,correct_answer) VALUES (?,?,?, ?, ?,?,?)")) {
                stmt.setInt(1, question.id);
                stmt.setString(2, question.questionText);
                stmt.setString(3, question.answers[0]);
                stmt.setString(4, question.answers[1]);
                stmt.setString(5, question.answers[2]);
                stmt.setString(6, question.answers[3]);
                stmt.setString(7, question.correctAnswer);
                stmt.executeUpdate();
                System.out.println(stmt.toString());
            }
        }

    }
}
