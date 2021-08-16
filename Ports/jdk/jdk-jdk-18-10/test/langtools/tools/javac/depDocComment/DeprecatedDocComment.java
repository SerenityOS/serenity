/**
 * @test  /nodynamiccopyright/
 * @bug 4241231 4785453
 * @summary Make sure the compiler scans for deprecated tag in legal
 * docComment only
 * @author Jing Qian
 *
 * @compile DeprecatedDocComment2.java
 * @compile/fail/ref=DeprecatedDocComment.out -XDrawDiagnostics -Werror -deprecation DeprecatedDocComment.java
 */

// WARNING: This file needs to be compiled with the -deprecation flag on.
// DeprecatedDocCommentTest2.java in test/tools/javac/depDocComment/
// should be compiled first before this file can be compiled. This is because
// the compiler *does not* issue deprecation warnings for a file currently
// being compiled.

// The test passes iff the compile issues deprecation warnings for
// deprecatedTest 1, 5, and 6; and fails with an unclosed comment error
// The test does not need to be run.

//import depDocComment.*;

public class DeprecatedDocComment {

    public static void main(String argv[]) {
      DeprecatedDocComment2.deprecatedTest1();
      DeprecatedDocComment2.deprecatedTest2();
      DeprecatedDocComment2.deprecatedTest3();
      DeprecatedDocComment2.deprecatedTest4();
      DeprecatedDocComment2.deprecatedTest5();
      DeprecatedDocComment2.deprecatedTest6();
      DeprecatedDocComment2.deprecatedTest7();
      DeprecatedDocComment2.deprecatedTest8();
    }

}
