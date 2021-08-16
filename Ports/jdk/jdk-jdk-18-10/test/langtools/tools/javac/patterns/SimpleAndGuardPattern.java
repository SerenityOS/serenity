/**
 * @test
 * @compile --enable-preview -source ${jdk.version} -doe SimpleAndGuardPattern.java
 * @run main/othervm --enable-preview SimpleAndGuardPattern
 */

import java.util.List;
import java.util.Objects;

public class SimpleAndGuardPattern {

    public static void main(String... args) throws Throwable {
        if (!Objects.equals(4, simple("test"))) {
            throw new IllegalStateException();
        }
        if (!Objects.equals(4, simple("TEST"))) {
            throw new IllegalStateException();
        }
        if (!Objects.equals(-1, simple("other"))) {
            throw new IllegalStateException();
        }
    }

    private static int simple(Object o) throws Throwable {
        return switch (o) {
            case String s && s.equalsIgnoreCase("test") -> s.length();
            default -> -1;
        };
    }

}
