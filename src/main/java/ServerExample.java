/*
 * EchoServer와 EchoClient 실행 코드
 */
public class ServerExample {
    public static void main(String... args) {
        Server server = new Server(19000);
        try {
            server.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
