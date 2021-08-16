/*
 * @test /nodynamiccopyright/
 * @bug 4531500
 * @summary Cascade of problems from duplicate class can cause compiler crash.
 * @author gafter
 *
 * @compile/fail/ref=DuplicateClass.out -XDrawDiagnostics DuplicateClass.java
 */

/**
 * Compiling this translation unit should fail; there is, after all, a
 * duplicate class.  Nonetheless, the compiler should not crash while
 * processing it.
 */
public class DuplicateClass {
    protected Object clone() {
        super.clone();
    }
}

public class DuplicateClass {}
