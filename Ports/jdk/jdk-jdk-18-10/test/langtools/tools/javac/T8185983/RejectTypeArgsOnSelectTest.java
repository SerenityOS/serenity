/*
 * @test  /nodynamiccopyright/
 * @bug 8185983
 * @summary Javac should reject TypeArguments on field access expression
 * @compile/fail/ref=RejectTypeArgsOnSelectTest.out -XDrawDiagnostics RejectTypeArgsOnSelectTest.java
 */

import java.util.*;

class RejectTypeArgsOnSelectTest {
    Iterator<RejectTypeArgsOnSelectTest> nullIter = Collections.<RejectTypeArgsOnSelectTest>EMPTY_LIST.iterator();
}
