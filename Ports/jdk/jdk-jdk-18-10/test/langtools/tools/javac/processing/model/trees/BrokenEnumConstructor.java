/*
 * @test /nodynamiccopyright/
 * @bug 8047347
 * @summary Verify that an explicit (incorrect) super constructor invocation in enums is not cleared
 *          by JavacProcessingEnvironment
 * @library /tools/javac/lib
 * @modules jdk.compiler
 * @build JavacTestingAbstractProcessor OnDemandAttribution
 * @compile/process/fail/ref=BrokenEnumConstructor.out -XDrawDiagnostics -processor OnDemandAttribution BrokenEnumConstructor.java
 */

public enum BrokenEnumConstructor {

    A;

    BrokenEnumConstructor() {super(); /*illegal*/}
}
