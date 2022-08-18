/*
 * EchoServer와 EchoClient 실행 코드
 */
public class EchoServerExample {
    public static void main(String... args) {
        EchoServer server = new EchoServer(19000);
        try {
            server.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
