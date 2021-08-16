
/*
 * @test /nodynamiccopyright/
 * @bug 7190862 7109747
 * @summary javap shows an incorrect type for operands if the 'wide' prefix is used
 * @modules jdk.jdeps/com.sun.tools.javap
 */

import com.sun.source.util.JavacTask;
import com.sun.tools.javap.JavapFileManager;
import com.sun.tools.javap.JavapTask;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

public class T7190862 {

    enum TypeWideInstructionMap {
        INT("int", new String[]{"istore_w", "iload_w"}),
        LONG("long", new String[]{"lstore_w", "lload_w"}),
        FLOAT("float", new String[]{"fstore_w", "fload_w"}),
        DOUBLE("double", new String[]{"dstore_w", "dload_w"}),
        OBJECT("Object", new String[]{"astore_w", "aload_w"});

        String type;
        String[] instructions;

        TypeWideInstructionMap(String type, String[] instructions) {
            this.type = type;
            this.instructions = instructions;
        }
    }

    JavaSource source;

    public static void main(String[] args) {
        JavaCompiler comp = ToolProvider.getSystemJavaCompiler();
        new T7190862().run(comp);
    }

    private void run(JavaCompiler comp) {
        String code;
        for (TypeWideInstructionMap typeInstructionMap: TypeWideInstructionMap.values()) {
            if (typeInstructionMap != TypeWideInstructionMap.OBJECT) {
                code = createWideLocalSource(typeInstructionMap.type, 300);
            } else {
                code = createWideLocalSourceForObject(300);
            }
            source = new JavaSource(code);
            compile(comp);
            check(typeInstructionMap.instructions);
        }

        //an extra test for the iinc instruction
        code = createIincSource();
        source = new JavaSource(code);
        compile(comp);
        check(new String[]{"iinc_w"});
    }

    private void compile(JavaCompiler comp) {
        JavacTask ct = (JavacTask)comp.getTask(null, null, null, null, null, Arrays.asList(source));
        try {
            if (!ct.call()) {
                throw new AssertionError("Error thrown when compiling the following source:\n" + source.getCharContent(true));
            }
        } catch (Throwable ex) {
            throw new AssertionError("Error thrown when compiling the following source:\n" + source.getCharContent(true));
        }
    }

    private void check(String[] instructions) {
        String out = javap(Arrays.asList("-c"), Arrays.asList("Test.class"));
        for (String line: out.split(System.getProperty("line.separator"))) {
            line = line.trim();
            for (String instruction: instructions) {
                if (line.contains(instruction) && line.contains("#")) {
                    throw new Error("incorrect type for operands for instruction " + instruction);
                }
            }
        }
    }

    private String javap(List<String> args, List<String> classes) {
        DiagnosticCollector<JavaFileObject> dc = new DiagnosticCollector<JavaFileObject>();
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        JavaFileManager fm = JavapFileManager.create(dc, pw);
        JavapTask t = new JavapTask(pw, fm, dc, args, classes);
        if (t.run() != 0)
            throw new Error("javap failed unexpectedly");

        List<Diagnostic<? extends JavaFileObject>> diags = dc.getDiagnostics();
        for (Diagnostic<? extends JavaFileObject> d: diags) {
            if (d.getKind() == Diagnostic.Kind.ERROR)
                throw new Error(d.getMessage(Locale.ENGLISH));
        }
        return sw.toString();

    }

    private String createWideLocalSource(String type, int numberOfVars) {
        String result = "    " + type + " x0 = 0;\n";
        for (int i = 1; i < numberOfVars; i++) {
            result += "        " + type + " x" + i + " = x" + (i - 1) + " + 1;\n";
        }
        return result;
    }

    private String createWideLocalSourceForObject(int numberOfVars) {
        String result = "    Object x0 = new Object();\n";
        for (int i = 1; i < numberOfVars; i++) {
            result += "        Object x" + i + " = x0;\n";
        }
        return result;
    }

    private String createIincSource() {
        return "    int i = 0;\n"
                + "        i += 1;\n"
                + "        i += 51;\n"
                + "        i += 101;\n"
                + "        i += 151;\n";
    }

    class JavaSource extends SimpleJavaFileObject {

        String template = "class Test {\n" +
                          "    public static void main(String[] args)\n" +
                          "    {\n" +
                          "        #C" +
                          "    }\n" +
                          "}";

        String source;

        public JavaSource(String code) {
            super(URI.create("Test.java"), JavaFileObject.Kind.SOURCE);
            source = template.replaceAll("#C", code);
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return source;
        }
    }
}
