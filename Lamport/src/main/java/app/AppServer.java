package app;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AppServer {
    private static final int PORT_NUMBER = 4444;
    private static float VALOR_CONTA = 1000F;
    
    private static List<PrintWriter> clientWriters = Collections.synchronizedList(new ArrayList<>());
    
    private static int timestamp = 0; // Relógio de Lamport
    private static List<Mensagem> messageQueue = Collections.synchronizedList(new ArrayList<>()); // Fila de mensagens
    
    public static void main(String[] args) throws IOException {

        try (ServerSocket serverSocket = new ServerSocket(PORT_NUMBER)) {
            System.out.println("Servidor aguardando conexões na porta " + PORT_NUMBER);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Cliente conectado: " + clientSocket.getRemoteSocketAddress());

                new Thread(() -> {
                    try {
                        BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                        PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                        clientWriters.add(out);
                                                
                        String inputLine;
                        while ( ( inputLine = in.readLine() ) != null ) {
                            
                            if (inputLine.startsWith("deposito")) {
                                float valorDeposito = Float.parseFloat(inputLine.split(" ")[1]);
                                synchronized (AppServer.class) {
                                    VALOR_CONTA += valorDeposito;
                                }
                                System.out.println("Valor alterado: " + VALOR_CONTA);
                                enviarSaldoParaClientes();
                            }
                            else if (inputLine.startsWith("juros")) {
                                float taxaJuros = Float.parseFloat(inputLine.split(" ")[1]);
                                synchronized (AppServer.class) {
                                    VALOR_CONTA *= (1 + taxaJuros / 100); // Aplicar juros
                                }
                                System.out.println("Valor alterado: " + VALOR_CONTA);
                                enviarSaldoParaClientes();
                            }
                            else {
                                // Lidar com mensagens ou comandos fora do padrão ("deposito", "juros")
                                System.out.println("Comando desconhecido: " + inputLine);
                            }
                        }
                        
                        timestamp++; // Incrementa o relógio
                        messageQueue.add(new Mensagem(inputLine, timestamp, clientSocket.getRemoteSocketAddress()));
                        processQueue();

                        clientSocket.close();
                        System.out.println("Cliente desconectado: " + clientSocket.getRemoteSocketAddress());
                        
                        clientWriters.remove(out);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }).start();
            }
        } catch (IOException e) {
            System.err.println("Erro ao iniciar o servidor: " + e.getMessage());
            System.exit(1);
        }
    }
    
    private static void enviarSaldoParaClientes() {
        synchronized (clientWriters) {
            for (PrintWriter writer : clientWriters) {
                writer.println(VALOR_CONTA);
            }
        }
    }
    
    private static void processQueue() {
        synchronized (messageQueue) {
            messageQueue.sort((m1, m2) -> Integer.compare(m1.timestamp, m2.timestamp));

            if (!messageQueue.isEmpty()) {
                Mensagem mensagem = messageQueue.remove(0);
                System.out.println("Processando mensagem: " + mensagem);

                boolean houveOperacao = false; // Flag para indicar se houve operação

                if (mensagem.conteudo.startsWith("deposito")) {
                    float valorDeposito = Float.parseFloat(mensagem.conteudo.split(" ")[1]);
                    synchronized (AppServer.class) {
                        VALOR_CONTA += valorDeposito;
                    }
                    houveOperacao = true;
                } else if (mensagem.conteudo.startsWith("juros")) {
                    float taxaJuros = Float.parseFloat(mensagem.conteudo.split(" ")[1]);
                    synchronized (AppServer.class) {
                        VALOR_CONTA *= (1 + taxaJuros / 100);
                    }
                    houveOperacao = true;
                }

                if (houveOperacao) {
                    enviarSaldoParaClientes(); 
                }
            }
        }
    }
    
    private static class Mensagem {
        String conteudo;
        int timestamp;
        SocketAddress remetente; 

        public Mensagem(String content, int timestamp, SocketAddress remetente) {
            this.conteudo = content;
            this.timestamp = timestamp;
            this.remetente = remetente;
        }

        @Override
        public String toString() {
            return conteudo + " (timestamp: " + timestamp + ", sender: " + remetente + ")";
        }
    }
}