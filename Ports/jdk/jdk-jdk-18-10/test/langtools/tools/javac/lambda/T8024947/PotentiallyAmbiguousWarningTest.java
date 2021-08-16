/*
 * @test /nodynamiccopyright/
 * @bug 8024947
 * @summary javac should issue the potentially ambiguous overload warning only
 * where the problem appears
 * @compile/fail/ref=PotentiallyAmbiguousWarningTest.out -XDrawDiagnostics -Werror -Xlint:overloads PotentiallyAmbiguousWarningTest.java
 */

import java.util.function.*;

public interface PotentiallyAmbiguousWarningTest {

    //a warning should be fired
    interface I1 {
        void foo(Consumer<Integer> c);
        void foo(IntConsumer c);
    }

    //a warning should be fired
    class C1 {
        void foo(Consumer<Integer> c) { }
        void foo(IntConsumer c) { }
    }

    interface I2 {
        void foo(Consumer<Integer> c);
    }

    //a warning should be fired, J1 is provoking the issue
    interface J1 extends I2 {
        void foo(IntConsumer c);
    }

    //no warning here, the issue is introduced in I1
    interface I3 extends I1 {}

    //no warning here, the issue is introduced in I1. I4 is just overriding an existing method
    interface I4 extends I1 {
        void foo(IntConsumer c);
    }

    class C2 {
        void foo(Consumer<Integer> c) { }
    }

    //a warning should be fired, D1 is provoking the issue
    class D1 extends C2 {
        void foo(IntConsumer c) { }
    }

    //a warning should be fired, C3 is provoking the issue
    class C3 implements I2 {
        public void foo(Consumer<Integer> c) { }
        public void foo(IntConsumer c) { }
    }

    //no warning here, the issue is introduced in C1
    class C4 extends C1 {}

    //no warning here, the issue is introduced in C1. C5 is just overriding an existing method
    class C5 extends C1 {
        void foo(IntConsumer c) {}
    }

    interface I5<T> {
        void foo(T c);
    }

    //a warning should be fired, J2 is provoking the issue
    interface J2 extends I5<IntConsumer> {
        void foo(Consumer<Integer> c);
    }
}
