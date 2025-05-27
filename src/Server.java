import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;

public class Server {
    private static final String CONFIG_FILE = "management.txt";
    private static int PORT = 0;
    private static int MAX_CLIENTS = 0;

    private static List<Question> questions = Collections.synchronizedList(new ArrayList<>());
    private static ExecutorService executor;

    private static final String DB_URL = "jdbc:mysql://localhost:3306/quiz";
    private static final String DB_USER = "root";
    private static final String DB_PASS = ""; // lub Twoje hasło

    public static void main(String[] args) throws IOException {
        loadConfig();
//        loadQuestions("bazaPytan.txt");
        loadQuestionsFromDB();

        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("Serwer uruchomiony...");

        while (true) {
            Socket clientSocket = serverSocket.accept();
            executor.execute(new ClientHandler(clientSocket));
        }
    }

    private static void loadConfig() throws IOException {
        List<String> configLines = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new FileReader(CONFIG_FILE));
        String line;
        for (int i = 0; i < 4; i++) {
            line = reader.readLine();
            if (line != null) {
                configLines.add(line);
            }
        }
        reader.close();
        PORT = Integer.parseInt(configLines.get(0).split(":")[1].trim());
        MAX_CLIENTS = Integer.parseInt(configLines.get(1).split(":")[1].trim());
        executor = Executors.newFixedThreadPool(MAX_CLIENTS);
    }

    private static void loadQuestionsFromDB() {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM questions");

            while (rs.next()) {
                questions.add(new Question(
                        rs.getInt("id"),
                        rs.getString("question_text"),
                        new String[]{
                                rs.getString("answer_a"),
                                rs.getString("answer_b"),
                                rs.getString("answer_c"),
                                rs.getString("answer_d")
                        },
                        rs.getString("correct_answer")
                ));
            }

        } catch (SQLException e) {
            System.out.println("Błąd podczas wczytywania pytań z bazy: " + e.getMessage());
        }
    }

    static class Question {
        String questionText;
        String[] answers = new String[4];
        String correctAnswer;
        int id;

        Question(int id, String questionText, String[] answers, String correctAnswer) {
            this.id = id;
            this.questionText = questionText;
            this.answers = answers;
            this.correctAnswer = correctAnswer;
        }

        String toSendFormat() {
            return questionText + "\n" + String.join("\n", answers);
        }
    }

    static class ClientHandler implements Runnable {
        private Socket socket;
        private static String username;

        ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                 Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
                try{
                    username = in.readLine();
                }
                catch(IOException e){
                    System.out.println("Błąd podczas odczytu nazwy użytkownika: " + e.getMessage());
                }
                System.out.println("Użytkownik " + username + " rozpoczął test.");

                int score = 0;
                List<String> studentAnswers = new ArrayList<>();
                for (Question q : questions) {
                    out.println(q.toSendFormat());

                    String response;
                    try {
                        response = in.readLine();
                    } catch (SocketTimeoutException e) {
                        response = "brak odpowiedzi";
                    }

                    studentAnswers.add(response);
                    if (response != null && response.equalsIgnoreCase(q.correctAnswer)) {
                        score++;
                    }
                    try (PreparedStatement stmt = conn.prepareStatement(
                            "INSERT INTO answers (username, question_id, user_answer) VALUES (?, ?, ?)")) {
                        stmt.setString(1, username);
                        stmt.setInt(2, q.id);
                        stmt.setString(3, response);
                        stmt.executeUpdate();
                    }
                    if(response.equalsIgnoreCase("exit")) {
                        break;
                    }
                }

                out.println("Twój wynik: " + score + "/" + questions.size());
                System.out.println("Użytkownik "+username+" zakończył test.");

                // Zapisz odpowiedzi
                synchronized (Server.class) {
//                    BufferedWriter answersWriter = new BufferedWriter(new FileWriter("bazaOdpowiedzi.txt", true));
//                    answersWriter.write(username + "\n");
//                    for (String ans : studentAnswers) {
//                        answersWriter.write(ans + "\n");
//                    }
//                    answersWriter.write(username+" koniec\n\n");
//                    answersWriter.close();
//
//                    BufferedWriter resultsWriter = new BufferedWriter(new FileWriter("wyniki.txt", true));
//                    resultsWriter.write(username+" wynik: " + score + "/" + questions.size() + "\n");
//                    resultsWriter.close();

                    try (PreparedStatement stmt = conn.prepareStatement(
                            "INSERT INTO results (username, score) VALUES (?, ?)")) {
                        stmt.setString(1, username);
                        stmt.setInt(2, score);
                        stmt.executeUpdate();
                    }
                }

            } catch (IOException e) {
                System.out.println("Błąd: " + e.getMessage());
            }catch (java.sql.SQLException s){
                System.out.println("Błąd: " + s.getMessage());
            }
        }
    }
}