/*
 * @test /nodynamiccopyright/
 * @bug 6278240
 * @summary Ensure AnnotationTypeMismatchException is thrown when appropriate
 *          with reasonable foundType filled.
 * @library /tools/javac/lib
 * @modules jdk.compiler/com.sun.tools.javac.util
 * @build JavacTestingAbstractProcessor Processor
 * @compile/fail/ref=Source.out -XDaccessInternalAPI -XDrawDiagnostics -processor Processor Source.java
 */

@Gen(fileName="Generated",
     content=
"class Generated {\n" +
"    @Check(classValue=String.class,\n" +
"           intConstValue=false,\n" +
"           enumValue=\"a\",\n" +
"           incorrectAnnotationValue=@Deprecated,\n" +
"           incorrectArrayValue={1, \"a\"},\n" +
"           incorrectClassValue=get())\n" +
"    public static Class<?> get() {\n" +
"        return null;\n" +
"    }\n" +
"}\n")
class Source {
}
