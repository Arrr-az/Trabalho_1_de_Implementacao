package app;

import java.net.*;

public class Processo implements Runnable {
    private static final String COORDINADOR_HOST = "localhost";
    private static final int COORDINADOR_PORT = 12345;

    private int id;
    private DatagramSocket socket;

    public Processo(int id) {
        this.id = id;
        try {
            this.socket = new DatagramSocket();
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            // Requisita acesso à região crítica
            enviarMensagem("REQUISICAO");

            // Aguarda resposta do coordenador
            byte[] buffer = new byte[256];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            socket.receive(packet);

            String mensagem = new String(packet.getData(), 0, packet.getLength());
            if (mensagem.equals("OK")) {
                System.out.println("Processo " + id + " recebeu OK.");

                // Usa a região crítica por 3 segundos
                Thread.sleep(3000);

                System.out.println("Processo " + id + " saindo da região crítica.");
                enviarMensagem("LIBERACAO");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void enviarMensagem(String mensagem) throws Exception {
        System.out.println("Processo " + id + " enviando " + mensagem + " ao coordenador.");
        byte[] buffer = mensagem.getBytes();
        InetAddress address = InetAddress.getByName(COORDINADOR_HOST);
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, COORDINADOR_PORT);
        socket.send(packet);
    }

    public static void main(String[] args) {
        for (int i = 1; i <= 5; i++) {
            Thread processo = new Thread(new Processo(i));
            processo.start();
        }
    }
}
