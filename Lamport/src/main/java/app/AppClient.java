package app;

import java.io.*;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AppClient implements Runnable {
    private static final String HOST = "localhost"; // Host definido aqui
    private static final int PORT = 4444; // Porta definida aqui
    private String message;
    
    private float saldoConta = 0F;

    public AppClient(String message) {
        this.message = message;
    }

    @Override
    public void run() {
        try (Socket echoSocket = new Socket(HOST, PORT);
             PrintWriter out = new PrintWriter(echoSocket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(echoSocket.getInputStream()))) {

            out.println(message);  


            String response = in.readLine(); // Lê apenas a primeira resposta
            if (response != null) {
                saldoConta = Float.parseFloat(response);
                System.out.println("Resposta do servidor: " + saldoConta);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        ExecutorService executor = Executors.newFixedThreadPool(3); // Cria um pool de threads

        executor.submit(() -> new AppClient("deposito 500").run());
        executor.submit(() -> new AppClient("juros 5.0").run());
        executor.submit(() -> new AppClient("fazer nada").run());

        executor.shutdown(); // Encerra o pool de threads após a execução
    }
}