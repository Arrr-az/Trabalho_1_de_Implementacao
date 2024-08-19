package app;

import java.io.*;
import java.net.*;
import java.util.*;

class AppServer {
    private int pid;
    private int pidLider;
    private List<Integer> outrosServidores;
    private ServerSocket serverSocket;

    public AppServer(int pid, List<Integer> outrosServidores) {
        this.pid = pid;
        this.outrosServidores = outrosServidores;
        this.pidLider = -1;
    }
    
    public static void main(String[] args) {
        List<Integer> todosServidores = Arrays.asList(1, 2, 3, 4, 5, 6);

        for (int pid : todosServidores) {
            new Thread(() -> {
                try {
                    new AppServer(pid, todosServidores).iniciarServidor();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }
    
    public int getPid() {
        return pid;
    }

    public int getPidLider() {
        return pidLider;
    }

    public void iniciarServidor() throws IOException {
        serverSocket = new ServerSocket(5000 + pid);
        System.out.println("Servidor " + pid + " iniciado na porta " + (5000 + pid));

        // Inicia a eleição assim que o servidor starta
        new ManipuladorDeEleicao(this).start();

        while (true) {
            Socket clienteSocket = serverSocket.accept();
            new ThreadCliente(clienteSocket, this).start();
        }
    }

    public synchronized void iniciarEleicao() {
        System.out.println("Servidor " + pid + " iniciando eleição...");
        boolean algumMaiorRespondeu = false;

        for (int pidServidor : outrosServidores) {
            if (pidServidor > pid) {
                try (Socket socket = new Socket("localhost", 5000 + pidServidor);
                     PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

                    out.println("ELEICAO " + pid);
                    algumMaiorRespondeu = true;
                } catch (IOException e) {
                    System.out.println("Servidor " + pidServidor + " não respondeu.");
                }
            }
        }

        if (!algumMaiorRespondeu) {
            // Se ninguém maior respondeu, avisa que é o líder
            definirLider(pid);
            anunciarLider();
        }
    }

    public synchronized void definirLider(int pidLider) {
        this.pidLider = pidLider;
        System.out.println("Servidor " + pid + " reconhece " + pidLider + " como líder.");
    }

    public synchronized void anunciarLider() {
        System.out.println("Servidor " + pid + " anuncia que é o líder.");
        for (int pidServidor : outrosServidores) {
            try (Socket socket = new Socket("localhost", 5000 + pidServidor);
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

                out.println("LIDER " + pid);
            } catch (IOException e) {
                System.out.println("Servidor " + pidServidor + " não respondeu ao anúncio de líder.");
            }
        }
    }

    public synchronized void verificarLider() {
        if (pidLider != -1) {
            try (Socket socket = new Socket("localhost", 5000 + pidLider);
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                out.println("ATIVO");
                String resposta = in.readLine();
                if (!"SIM".equals(resposta)) {
                    throw new IOException();
                }
            } catch (IOException e) {
                System.out.println("Líder " + pidLider + " caiu.");
                iniciarEleicao();
            }
        }
    }
}

class ManipuladorDeEleicao extends Thread {
    private final AppServer servidor;

    public ManipuladorDeEleicao(AppServer servidor) {
        this.servidor = servidor;
    }

    @Override
    public void run() {
        try {
            Thread.sleep(new Random().nextInt(10000)); // Tempo aleatório
            servidor.iniciarEleicao();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

class ThreadCliente extends Thread {
    private Socket socket;
    private AppServer servidor;

    public ThreadCliente(Socket socket, AppServer servidor) {
        this.socket = socket;
        this.servidor = servidor;
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            String mensagem = in.readLine();
            String[] partes = mensagem.split(" ");
            String comando = partes[0];

            switch (comando) {
                case "ELEICAO":
                    int pidRemetente = Integer.parseInt(partes[1]);
                    System.out.println("Servidor " + servidor.getPid() + " recebeu eleição de " + pidRemetente);
                    out.println("OK");
                    if (pidRemetente < servidor.getPid()) {
                        servidor.iniciarEleicao();
                    }
                    break;

                case "LIDER":
                    int pidLider = Integer.parseInt(partes[1]);
                    servidor.definirLider(pidLider);
                    break;

                case "ATIVO":
                    if (servidor.getPidLider() == servidor.getPid()) {
                        out.println("SIM");
                    }
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
