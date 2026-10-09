import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

public class FakeCli {

    public static void main(String[] args) throws Exception {
        PrintStream out = new PrintStream(System.out, true, StandardCharsets.UTF_8);
        PrintStream err = new PrintStream(System.err, true, StandardCharsets.UTF_8);
        out.println("{\"schema\": 1, \"event\": \"info\", \"message\": \"a\u00e7\u00e3o \u00b7 ok\"}");
        out.println("texto livre");
        out.print("{\"schema\": 1, \"event\": \"pro");
        out.flush();
        Thread.sleep(100);
        out.println("gress\", \"fraction\": 0.5}");
        err.println("aviso no stderr");
        for (String arg : args) {
            out.println(arg);
        }
        out.println("{\"schema\": 1, \"event\": \"result\", \"ok\": false}");
        out.print("sem quebra");
        out.flush();
        System.exit(3);
    }
}
