/*
 * @test  /nodynamiccopyright/
 * @bug     6362067
 * @summary Messager methods do not print out source position information
 * @library /tools/javac/lib
 * @modules java.compiler
 *          jdk.compiler
 * @build   JavacTestingAbstractProcessor T6362067
 * @compile -processor T6362067 -proc:only T6362067.java
 * @compile/ref=T6362067.out -XDrawDiagnostics -processor T6362067 -proc:only T6362067.java
 */
import java.util.Set;
import javax.annotation.processing.*;
import javax.lang.model.element.*;
import static javax.tools.Diagnostic.Kind.*;

@Deprecated // convenient test annotations
@SuppressWarnings({""})
public class T6362067 extends JavacTestingAbstractProcessor {
    public boolean process(Set<? extends TypeElement> annos,
                           RoundEnvironment roundEnv) {

        for (Element e: roundEnv.getRootElements()) {
            messager.printMessage(NOTE, "note:elem", e);
            for (AnnotationMirror a: e.getAnnotationMirrors()) {
                messager.printMessage(NOTE, "note:anno", e, a);
                for (AnnotationValue v: a.getElementValues().values()) {
                    messager.printMessage(NOTE, "note:value", e, a, v);
                }
            }
        }

        if (roundEnv.processingOver())
            messager.printMessage(NOTE, "note:nopos");
        return true;
    }
}
