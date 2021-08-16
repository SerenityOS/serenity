/**
 * @test /nodynamiccopyright/
 * @bug 8171332
 * @summary 8171332: NPE in MembersPhase.finishClass
 * @modules java.compiler
 *          jdk.compiler
 * @build Processor
 * @compile/fail/ref=Processor.out -XDrawDiagnostics -processor Processor Buggy.java
 */

import java.util.Set;
import javax.annotation.processing.*;
import javax.lang.model.element.TypeElement;

@SupportedAnnotationTypes("*")
public class Processor extends AbstractProcessor {
    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        return false;
    }
}
