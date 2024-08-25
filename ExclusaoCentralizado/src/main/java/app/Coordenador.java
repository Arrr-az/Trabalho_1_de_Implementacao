package app;

import java.net.*;
import java.util.LinkedList;
import java.util.Queue;

public class Coordenador {
    private static final int PORT = 12345;
    private static Queue<ProcessoInfo> fila = new LinkedList<>();
    private static boolean regiaoCriticaOcupada = false;

    private static class ProcessoInfo {
        InetAddress endereco;
        int porta;

        ProcessoInfo(InetAddress endereco, int porta) {
            this.endereco = endereco;
            this.porta = porta;
        }
    }

    public static void main(String[] args) {
        try (DatagramSocket socket = new DatagramSocket(PORT)) {
            System.out.println("Coordenador em execução...");

            while (true) {
                byte[] buffer = new byte[256];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                String mensagem = new String(packet.getData(), 0, packet.getLength());
                InetAddress enderecoProcesso = packet.getAddress();
                int portaProcesso = packet.getPort();

                if (mensagem.equals("REQUISICAO")) {
                    if (regiaoCriticaOcupada) {
                        fila.add(new ProcessoInfo(enderecoProcesso, portaProcesso));
                    }
                    else {
                        regiaoCriticaOcupada = true;
                        enviarOK(socket, enderecoProcesso, portaProcesso);
                    }
                }
                else if (mensagem.equals("LIBERACAO")) {
                    regiaoCriticaOcupada = false;

                    if (!fila.isEmpty()) {
                        ProcessoInfo proximoProcesso = fila.poll();
                        enviarOK(socket, proximoProcesso.endereco, proximoProcesso.porta);
                        regiaoCriticaOcupada = true;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void enviarOK(DatagramSocket socket, InetAddress enderecoProcesso, int portaProcesso) throws Exception {
        String resposta = "OK";
        byte[] respostaBuffer = resposta.getBytes();
        DatagramPacket respostaPacket = new DatagramPacket(respostaBuffer, respostaBuffer.length, enderecoProcesso, portaProcesso);
        socket.send(respostaPacket);
    }
}
