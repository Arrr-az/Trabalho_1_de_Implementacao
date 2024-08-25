package app;

import java.net.*;

public class Processo implements Runnable {
    private int id;
    private Token token;
    private InetAddress enderecoProximo;
    private int portaProximo;
    private DatagramSocket socket;

    public Processo(int id, InetAddress enderecoProximo, int portaProximo) {
        this.id = id;
        this.enderecoProximo = enderecoProximo;
        this.portaProximo = portaProximo;
        this.token = new Token(id == 1); // Setando manualmente que o token começa com o primeiro processo
        try {
            this.socket = new DatagramSocket(50000 + id);
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        while (true) {
            try {
                if (token.possuiToken()) {
                    // Entra na região crítica
                    System.out.println("Processo " + id + " entrou na região crítica.");
                    Thread.sleep(3000); // Usa a região crítica por 3 segundos

                    // Sai da região crítica e passa o token pro próximo processo
                    System.out.println("Processo " + id + " saindo da região crítica.");
                    enviarToken();
                }
                else {
                    // Espera receber o token
                    byte[] buffer = new byte[256];
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);

                    String mensagem = new String(packet.getData(), 0, packet.getLength());
                    if (mensagem.equals("TOKEN")) {
                        token.setToken(true);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void enviarToken() throws Exception {
        token.setToken(false); // Remove o token do processo atual
        String mensagem = "TOKEN";
        byte[] buffer = mensagem.getBytes();
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, enderecoProximo, portaProximo);
        socket.send(packet);
        System.out.println("Processo " + id + " passou o token para o próximo processo.");
    }

    public static void main(String[] args) throws UnknownHostException {
        // Anel com 3 processos
        InetAddress endereco1 = InetAddress.getByName("localhost");
        InetAddress endereco2 = InetAddress.getByName("localhost");
        InetAddress endereco3 = InetAddress.getByName("localhost");

        Processo p1 = new Processo(1, endereco2, 50002);
        Processo p2 = new Processo(2, endereco3, 50003);
        Processo p3 = new Processo(3, endereco1, 50001);

        new Thread(p1).start();
        new Thread(p2).start();
        new Thread(p3).start();
    }
}