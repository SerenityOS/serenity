/*
 * @test /nodynamiccopyright/
 * @bug 4901271
 * @summary java.lang.annotation.Target
 * @author gafter
 *
 * @compile/fail/ref=WrongTarget.out -XDrawDiagnostics  WrongTarget.java
 */

import static java.lang.annotation.ElementType.*;

@java.lang.annotation.Target({FIELD})
@interface foo {
}

@foo
public class WrongTarget {
}
