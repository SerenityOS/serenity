/*
 * @test    /nodynamiccopyright/
 * @bug     6341173 6341072
 * @summary Test presence of Messager methods
 * @author  Joseph D. Darcy
 * @library /tools/javac/lib
 * @modules java.compiler
 *          jdk.compiler
 * @build   JavacTestingAbstractProcessor
 * @compile MessagerBasics.java
 * @compile -processor MessagerBasics -proc:only MessagerBasics.java
 * @compile/fail/ref=MessagerBasics.out -XDrawDiagnostics -processor MessagerBasics -proc:only -AfinalError MessagerBasics.java
 * @compile -processor MessagerBasics MessagerBasics.java
 * @compile/fail/ref=MessagerBasics.out -XDrawDiagnostics -processor MessagerBasics -AfinalError MessagerBasics.java
 */

import java.util.Set;
import javax.annotation.processing.*;
import javax.lang.model.element.*;
import javax.lang.model.util.*;
import static javax.tools.Diagnostic.Kind.*;

@SupportedOptions("finalError")
public class MessagerBasics extends JavacTestingAbstractProcessor {
    public boolean process(Set<? extends TypeElement> annotations,
                           RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) {
            if (processingEnv.getOptions().containsKey("finalError"))
                messager.printMessage(ERROR,   "Does not compute");
            else {
                messager.printMessage(NOTE,    "Post no bills");
                messager.printMessage(WARNING, "Beware the ides of March!");
            }
        }
        return true;
    }
}
