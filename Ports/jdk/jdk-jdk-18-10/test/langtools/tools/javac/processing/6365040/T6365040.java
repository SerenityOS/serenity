/*
 * @test /nodynamiccopyright/
 * @bug 6365040 6358129
 * @summary Test -processor foo,bar,baz
 * @author  Joseph D. Darcy
 * @library /tools/javac/lib
 * @modules java.compiler
 *          jdk.compiler
 * @build   JavacTestingAbstractProcessor
 * @compile ProcFoo.java
 * @compile ProcBar.java
 * @compile T6365040.java
 * @compile      -processor ProcFoo,ProcBar,T6365040  -proc:only T6365040.java
 * @compile      -processor T6365040                  -proc:only T6365040.java
 * @compile      -processor T6365040,NotThere,        -proc:only T6365040.java
 * @compile/fail/ref=T6365040.out -XDrawDiagnostics -processor NotThere -proc:only T6365040.java
 * @compile/fail/ref=T6365040.out -XDrawDiagnostics -processor NotThere,T6365040 -proc:only T6365040.java
 */

import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.TypeElement;
import static javax.tools.Diagnostic.Kind.*;

public class T6365040 extends JavacTestingAbstractProcessor {
    public boolean process(Set<? extends TypeElement> annotations,
                           RoundEnvironment roundEnvironment) {
        if (!roundEnvironment.processingOver())
            messager.printMessage(NOTE, "Hello from T6365040");
        return true;
    }
}
