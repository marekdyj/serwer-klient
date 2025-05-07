import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class Server {
    private static final String CONFIG_FILE = "management.txt";
    private static int PORT = 0;
    private static int MAX_CLIENTS = 0;

    private static List<Question> questions = Collections.synchronizedList(new ArrayList<>());
    private static ExecutorService executor;

    public static void main(String[] args) throws IOException {
        loadConfig();
        loadQuestions("bazaPytan.txt");

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

    private static void loadQuestions(String fileName) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(fileName));
        String line;
        List<String> qLines = new ArrayList<>();
        while ((line = reader.readLine()) != null) {
            if (line.isEmpty()) continue;
            qLines.add(line);
            if (qLines.size() == 6) {
                questions.add(new Question(qLines));
                qLines.clear();
            }
        }
        reader.close();
    }

    static class Question {
        String questionText;
        String[] answers = new String[4];
        String correctAnswer;

        Question(List<String> lines) {
            questionText = lines.get(0);
            for (int i = 0; i < 4; i++) {
                answers[i] = lines.get(i + 1);
            }
            correctAnswer = lines.get(5).split(":")[1];
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
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
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
                    if(response.equalsIgnoreCase("exit")) {
                        break;
                    }
                }

                out.println("Twój wynik: " + score + "/" + questions.size());
                System.out.println("Użytkownik "+username+" zakończył test.");

                // Zapisz odpowiedzi
                synchronized (Server.class) {
                    BufferedWriter answersWriter = new BufferedWriter(new FileWriter("bazaOdpowiedzi.txt", true));
                    answersWriter.write(username + "\n");
                    for (String ans : studentAnswers) {
                        answersWriter.write(ans + "\n");
                    }
                    answersWriter.write(username+" koniec\n\n");
                    answersWriter.close();

                    BufferedWriter resultsWriter = new BufferedWriter(new FileWriter("wyniki.txt", true));
                    resultsWriter.write(username+" wynik: " + score + "/" + questions.size() + "\n");
                    resultsWriter.close();
                }

            } catch (IOException e) {
                System.out.println("Błąd: " + e.getMessage());
            }
        }
    }
}