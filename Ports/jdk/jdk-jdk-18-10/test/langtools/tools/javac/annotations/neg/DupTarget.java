/*
 * @test /nodynamiccopyright/
 * @bug 4901271
 * @summary <at>Target
 * @author gafter
 *
 * @compile/fail/ref=DupTarget.out -XDrawDiagnostics  DupTarget.java
 */

import static java.lang.annotation.ElementType.*;

@java.lang.annotation.Target({TYPE, FIELD, PACKAGE, FIELD})
@interface DupTarget {
}
