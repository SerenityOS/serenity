/*
 * @test /nodynamiccopyright/
 * @bug 8004832
 * @summary Add new doclint package
 * @modules jdk.javadoc/jdk.javadoc.internal.doclint
 * @build DocLintTester
 * @run main DocLintTester -ref AccessTest.protected.out AccessTest.java
 * @run main DocLintTester -Xmsgs -ref AccessTest.private.out AccessTest.java
 * @run main DocLintTester -Xmsgs:syntax -ref AccessTest.private.out AccessTest.java
 * @run main DocLintTester -Xmsgs:syntax/public -ref AccessTest.public.out AccessTest.java
 * @run main DocLintTester -Xmsgs:syntax/protected -ref AccessTest.protected.out AccessTest.java
 * @run main DocLintTester -Xmsgs:syntax/package -ref AccessTest.package.out AccessTest.java
 * @run main DocLintTester -Xmsgs:syntax/private -ref AccessTest.private.out AccessTest.java
 * @run main DocLintTester -Xmsgs:all,-syntax AccessTest.java
 * @run main DocLintTester -Xmsgs:all,-syntax/public AccessTest.java
 * @run main DocLintTester -Xmsgs:all,-syntax/protected -ref AccessTest.public.out AccessTest.java
 * @run main DocLintTester -Xmsgs:all,-syntax/package -ref AccessTest.protected.out AccessTest.java
 * @run main DocLintTester -Xmsgs:all,-syntax/private -ref AccessTest.package.out AccessTest.java
 */

/** */
public class AccessTest {
    /**
     * public a < b
     */
    public void public_syntax_error() { }

    /**
     * protected a < b
     */
    protected void protected_syntax_error() { }

    /**
     * package-private a < b
     */
    void syntax_error() { }

    /**
     * private a < b
     */
    private void private_syntax_error() { }
}

/** */
class AccessTest2 {
    /**
     * public a < b
     */
    public void public_syntax_error() { }

    /**
     * protected a < b
     */
    protected void protected_syntax_error() { }

    /**
     * package-private a < b
     */
    void syntax_error() { }

    /**
     * private a < b
     */
    private void private_syntax_error() { }
}

