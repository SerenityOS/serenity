/*
 * @test /nodynamiccopyright/
 * @bug 8187487
 * @summary crash with duplicate class name
 * @compile/fail/ref=CrashWithDuplicateClassNamesTest.out -XDrawDiagnostics CrashWithDuplicateClassNamesTest.java
 */

class CrashWithDuplicateClassNamesTest {
    static class C1$C2 {}

    static class C1 {
        static class C2 {}
    }

    static class C3 {
        static class C4 {}
    }

    static class C3$C4 {}
}
