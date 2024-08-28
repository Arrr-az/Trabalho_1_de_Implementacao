package app;

import java.net.*;
import java.util.*;

public class Processador {
    private static final int PORTA = 12345;
    private static List<Requisicao> filaRequisicoes = new ArrayList<>();
    private static boolean regiaoCriticaOcupada = false;

    public static void main(String[] args) {
        try (DatagramSocket socket = new DatagramSocket(PORTA)) {
            System.out.println("Processador em execução...");

            while (true) {
                byte[] buffer = new byte[256];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                String mensagem = new String(packet.getData(), 0, packet.getLength());
                InetAddress enderecoProcesso = packet.getAddress();
                int portaProcesso = packet.getPort();
                
                String[] partes = mensagem.split(":");
                if (partes[0].equals("REQUISICAO")) {
                    int idSolicitante = Integer.parseInt(partes[1]);
                    String timestampSolicitante = partes[2];
                    processarRequisicao(idSolicitante, timestampSolicitante, enderecoProcesso, portaProcesso, socket);
                } else if (partes[0].equals("LIBERACAO")) {
                    System.out.println("Recebeu LIBERACAO de " + enderecoProcesso);
                    regiaoCriticaOcupada = false;
                    processarFila(socket);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void processarRequisicao(int id, String timestamp, InetAddress endereco, int porta, DatagramSocket socket) throws Exception {
        Requisicao requisicao = new Requisicao(id, timestamp, endereco, porta);
        if (!regiaoCriticaOcupada) {
            enviarMensagem(socket, endereco, porta, "OK");
            regiaoCriticaOcupada = true;
        } else {
            filaRequisicoes.add(requisicao);
            filaRequisicoes.sort(Comparator.comparing(r -> r.timestamp)); // Ordena por timestamp
            System.out.println("Enfileirou requisição de " + id);
        }
    }

    private static void processarFila(DatagramSocket socket) throws Exception {
        if (!filaRequisicoes.isEmpty()) {
            Requisicao requisicao = filaRequisicoes.remove(0);
            enviarMensagem(socket, requisicao.endereco, requisicao.porta, "OK");
            regiaoCriticaOcupada = true;
            System.out.println("Enviou OK para " + requisicao.id);
        }
    }

    private static void enviarMensagem(DatagramSocket socket, InetAddress endereco, int porta, String mensagem) throws Exception {
        byte[] buffer = mensagem.getBytes();
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, endereco, porta);
        socket.send(packet);
        System.out.println("Enviado " + mensagem + " para " + endereco + ":" + porta);
    }

    private static class Requisicao {
        int id;
        String timestamp;
        InetAddress endereco;
        int porta;

        Requisicao(int id, String timestamp, InetAddress endereco, int porta) {
            this.id = id;
            this.timestamp = timestamp;
            this.endereco = endereco;
            this.porta = porta;
        }
    }
}
