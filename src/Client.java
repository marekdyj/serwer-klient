import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Client {
    private static final String CONFIG_FILE = "management.txt";
    private static int PORT = 0;
    private static String HOST = "";
    private static int TIME_LIMIT = 0; // seconds
    private static String USERNAME = "";

    public static void main(String[] args) {
        try {
            loadConfig();
        } catch (IOException e) {
            System.out.println("Błąd podczas ładowania konfiguracji: " + e.getMessage());
            return;
        }
        try (Socket socket = new Socket(HOST, PORT);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             Scanner scanner = new Scanner(System.in)) {
            System.out.println("Podaj swój nr albumu");
            USERNAME = scanner.nextLine();
            if (USERNAME.isEmpty()) {
                System.out.println("Nie podano nr albumu. Nazwa użytkownika: 'student'");
                USERNAME = "student"; // Ustaw domyślną nazwę użytkownika
            }
            out.println(USERNAME); // Wysyłamy nazwę użytkownika do serwera
            System.out.println("Aby przerwać test, wpisz 'exit' w dowolnym momencie.");


            String line;
            while ((line = in.readLine()) != null) {
                if (line.startsWith("Twój wynik:")) {
                    System.out.println(line);
                    break;
                }
                System.out.println(line);

                String answer = null;
                // Po odebraniu 5 linii pytań, pobierz odpowiedź
                if (line.startsWith("D:")) {
                    System.out.print("Wpisz odpowiedź (A/B/C/D, wiele odpowiedzi wpisz w kolejności alfabetycznej bez odstępów): ");
                    // Rozpoczynamy odliczanie czasu
                    long startTime = System.currentTimeMillis();

                    while ((System.currentTimeMillis() - startTime) < TIME_LIMIT * 1000) {
                        if (System.in.available() > 0) { // Sprawdź, czy użytkownik wpisał coś
                            answer = scanner.nextLine().trim().toUpperCase();
                            break;
                        }
                    }
                    if (answer == null || answer.isEmpty()) {
                        answer = "brak odpowiedzi"; // Ustaw domyślną odpowiedź, jeśli czas minął
                        System.out.println("Czas na odpowiedź minął!");
                    }

                    out.println(answer); // Wyślij odpowiedź do serwera
                }
            }

        } catch (IOException e) {
            System.out.println("Błąd: " + e.getMessage());
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
        HOST = configLines.get(3).split(":")[1].trim();
        TIME_LIMIT = Integer.parseInt(configLines.get(2).split(":")[1].trim());
    }
}
