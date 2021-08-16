/*
 * @test /nodynamiccopyright/
 * @bug 7196163
 * @summary Twr with resource variables as lambda expressions and method references
 * @compile/fail/ref=TwrAndLambda.out -XDrawDiagnostics TwrAndLambda.java
 */

public class TwrAndLambda {

    public static void main(String... args) {

        // Lambda expression
        AutoCloseable v1 = () -> {};
        // Static method reference
        AutoCloseable v2 = TwrAndLambda::close1;
        // Instance method reference
        AutoCloseable v3 = new TwrAndLambda()::close2;
        // Lambda expression which is not AutoCloseable
        Runnable r1 = () -> {};
        // Static method reference which is not AutoCloseable
        Runnable r2 = TwrAndLambda::close1;
        // Instance method reference which is not AutoCloseable
        Runnable r3 = new TwrAndLambda()::close2;

        try (v1) {
        } catch(Exception e) {}
        try (v2) {
        } catch(Exception e) {}
        try (v3) {
        } catch(Exception e) {}
        try (r1) {
        } catch(Exception e) {}
        try (r2) {
        } catch(Exception e) {}
        try (r3) {
        } catch(Exception e) {}

        // lambda invocation
        I i = (x) -> { try(x) { } catch (Exception e) { } };
        i.m(v1);
        i.m(v2);
        i.m(v3);
        i.m(r1);
        i.m(r2);
        i.m(r3);
    }

    static interface I {
        public void m(AutoCloseable r);
    }

    public static void close1() { }

    public void close2() { }
}
