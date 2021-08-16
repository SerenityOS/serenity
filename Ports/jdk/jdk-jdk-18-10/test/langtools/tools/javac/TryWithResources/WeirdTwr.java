/*
 * @test /nodynamiccopyright/
 * @bug 6911256 6964740
 * @author Joseph D. Darcy
 * @summary Strange TWRs
 * @compile WeirdTwr.java
 * @run main WeirdTwr
 */

public class WeirdTwr implements AutoCloseable {
    private static int closeCount = 0;
    public static void main(String... args) {
        try(WeirdTwr r1 = new WeirdTwr(); WeirdTwr r2 = r1) {
            if (r1 != r2)
                throw new RuntimeException("Unexpected inequality.");
        }
        if (closeCount != 2)
            throw new RuntimeException("bad closeCount" + closeCount);
    }

    public void close() {
        closeCount++;
    }
}
