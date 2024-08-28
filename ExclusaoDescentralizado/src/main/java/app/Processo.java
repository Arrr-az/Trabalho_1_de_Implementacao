package app;

import java.net.*;
import java.util.*;
import java.text.SimpleDateFormat;

public class Processo implements Runnable {
    private static final int PORTA = 12345;
    private static final String IP_BROADCAST = "255.255.255.255";
    private static final int TEMPO_CRITICO = 2000; // Tempo de uso da região crítica em milissegundos

    private DatagramSocket socket;
    private int id;
    private boolean naRegiaoCritica = false;
    private List<Requisicao> fila = new LinkedList<>();
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    public Processo(int id) {
        this.id = id;
        try {
            this.socket = new DatagramSocket();
            this.socket.setBroadcast(true);
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            // Solicita acesso à região crítica
            requisitarAcesso();

            // Processa mensagens recebidas
            while (true) {
                byte[] buffer = new byte[256];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                String mensagem = new String(packet.getData(), 0, packet.getLength());
                processarMensagem(mensagem, packet.getAddress(), packet.getPort());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            socket.close();
        }
    }

    private void requisitarAcesso() throws Exception {
        String mensagem = "REQUISICAO:" + id + ":" + sdf.format(new Date());
        enviarMensagem(mensagem);
    }

    private void processarMensagem(String mensagem, InetAddress endereco, int porta) throws Exception {
        String[] partes = mensagem.split(":");
        if (partes[0].equals("REQUISICAO")) {
            int idSolicitante = Integer.parseInt(partes[1]);
            String timestampSolicitante = partes[2];

            synchronized (this) {
                if (naRegiaoCritica) {
                    fila.add(new Requisicao(idSolicitante, timestampSolicitante, endereco, porta));
                    System.out.println("Processo " + id + " enfileirou requisição de " + idSolicitante);
                } else {
                    enviarMensagem("OK", endereco, porta);
                }
            }
        } else if (partes[0].equals("OK")) {
            System.out.println("Processo " + id + " recebeu OK.");
            synchronized (this) {
                if (!naRegiaoCritica) {
                    entrarNaRegiaoCritica();
                }
            }
        } else if (partes[0].equals("LIBERACAO")) {
            System.out.println("Processo " + id + " recebeu LIBERACAO.");
            synchronized (this) {
                processarFila();
            }
        }
    }

    private void entrarNaRegiaoCritica() throws Exception {
        naRegiaoCritica = true;
        System.out.println("Processo " + id + " entrou na região crítica.");
        Thread.sleep(TEMPO_CRITICO);
        System.out.println("Processo " + id + " saindo da região crítica.");
        naRegiaoCritica = false;
        enviarMensagem("LIBERACAO");
    }

    private void processarFila() throws Exception {
        if (!fila.isEmpty()) {
            Requisicao requisicao = fila.remove(0);
            enviarMensagem("OK", requisicao.endereco, requisicao.porta);
            System.out.println("Processo " + id + " enviou OK para " + requisicao.id);
        }
    }

    private void enviarMensagem(String mensagem) throws Exception {
        byte[] buffer = mensagem.getBytes();
        InetAddress enderecoBroadcast = InetAddress.getByName(IP_BROADCAST);
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, enderecoBroadcast, PORTA);
        socket.send(packet);
        System.out.println("Processo " + id + " enviando " + mensagem + " para o broadcast.");
    }

    private void enviarMensagem(String mensagem, InetAddress endereco, int porta) throws Exception {
        byte[] buffer = mensagem.getBytes();
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, endereco, porta);
        socket.send(packet);
        System.out.println("Processo " + id + " enviando " + mensagem + " para " + endereco + ":" + porta);
    }

    public static void main(String[] args) {
        for (int i = 1; i <= 5; i++) {
            new Thread(new Processo(i)).start();
        }
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
