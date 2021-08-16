/*
 * @test /nodynamiccopyright/
 * @bug 8066843
 * @summary Annotation processors should be able to print multiple errors at the same location.
 * @library /tools/javac/lib
 * @modules jdk.compiler
 * @build JavacTestingAbstractProcessor TestMultipleErrors
 * @compile/fail/ref=TestMultipleErrors.out -XDrawDiagnostics -processor TestMultipleErrors TestMultipleErrors.java
 */

import java.util.*;
import javax.annotation.processing.*;
import javax.lang.model.element.*;
import javax.tools.Diagnostic.Kind;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;

public class TestMultipleErrors extends JavacTestingAbstractProcessor {
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element root : roundEnv.getRootElements()) {
            processingEnv.getMessager().printMessage(Kind.ERROR, "error1", root);
            processingEnv.getMessager().printMessage(Kind.ERROR, "error2", root);

            Trees trees = Trees.instance(processingEnv);
            TreePath path = trees.getPath(root);

            trees.printMessage(Kind.ERROR, "error3", path.getLeaf(), path.getCompilationUnit());
            trees.printMessage(Kind.ERROR, "error4", path.getLeaf(), path.getCompilationUnit());
        }
        return true;
    }
}
