/*
 * @test /nodynamiccopyright/
 * @bug 8007096
 * @summary DocLint parsing problems with some comments
 * @modules jdk.javadoc/jdk.javadoc.internal.doclint
 * @build DocLintTester
 * @run main DocLintTester -Xmsgs:-html EndWithIdentifierTest.java
 * @run main DocLintTester -Xmsgs -ref EndWithIdentifierTest.out EndWithIdentifierTest.java
 * @author jlahoda
 */

/**@deprecated*/
public class EndWithIdentifierTest {

    /**{@link*/
    private void unfinishedInlineTagName() {}

    /**@see List*/
    private void endsWithIdentifier() {}

    /**&amp*/
    private void entityName() {}

    /**<a*/
    private void tag() {}

    /**</a*/
    private void tagEnd() {}

    /**<a name*/
    private void attribute() {}
}

