/* @test /nodynamiccopyright/
 * @bug 7129225
 * @summary import xxx.* isn't handled correctly by annotation processing
 * @library /tools/javac/lib
 * @modules java.compiler
 *          jdk.compiler
 * @build JavacTestingAbstractProcessor
 * @compile/fail/ref=NegTest.out -XDrawDiagnostics TestImportStar.java
 * @compile Anno.java AnnoProcessor.java
 * @compile/fail/ref=TestImportStar.out -XDrawDiagnostics -processor AnnoProcessor -proc:only TestImportStar.java
 */

 //The @compile/fail... verifies that the fix doesn't break the normal compilation of import xxx.*
 //The @comple/ref... verifies the fix fixes the bug

import xxx.*;

@Anno
public class TestImportStar {
}
