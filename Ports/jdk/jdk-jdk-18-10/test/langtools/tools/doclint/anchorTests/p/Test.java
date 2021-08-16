/* @test /nodynamiccopyright/
 * @bug 8025246 8247957
 * @summary doclint is showing error on anchor already defined when it's not
 * @library ../..
 * @modules jdk.javadoc/jdk.javadoc.internal.doclint
 * @build DocLintTester
 * @run main DocLintTester -ref Test.out Test.java
 * @compile/fail/ref=Test.javac.out -XDrawDiagnostics -Werror -Xdoclint:all Test.java
 */

package p;

/**
 * <a id="dupTest">dupTest</a>
 * <a id="dupTest">dupTest again</a>
 *
 * <a id="dupTestField">dupTestField</a>
 * <a id="dupTestMethod">dupTestMethod</a>

 * <a id="okClass">okClass</a>
 * <a id="okField">okField</a>
 * <a id="okMethod">okMethod</a>
 */
public class Test {
    /** <a id="dupTestField">dupTestField again</a> */
    public int f;

    /** <a id="dupTestMethod">dupTestMethod again</a> */
    public void m() { }

    /**
     * <a id="dupNested">dupNested</a>
     * <a id="dupNested">dupNested again</a>
     * <a id="dupNestedField">dupNestedField</a>
     * <a id="dupNestedMethod">dupNestedMethod</a>
     *
     * <a id="okClass">okClass again</a>
     */
    public class Nested {
        /**
         * <a id="dupNestedField">dupNestedField</a>
         *
         * <a id="okField">okField again</a>
         */
        public int f;

        /**
         * <a id="dupNestedMethod">dupNestedMethod</a>
         *
         * <a id="okMethod">okMethod again</a>
         */
        public void m() { }
    }
}
