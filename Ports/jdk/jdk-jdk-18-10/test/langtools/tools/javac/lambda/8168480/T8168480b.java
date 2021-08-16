/*
 * @test /nodynamiccopyright/
 * @bug 8168480
 * @summary Speculative attribution of lambda causes NPE in Flow
 * @compile/fail/ref=T8168480b.out -XDrawDiagnostics T8168480b.java
 */

import java.util.function.Supplier;

class T8168480b {
   Supplier<Runnable> ssr = () -> () -> { while (true); System.err.println("Hello"); };
}
