/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/**
 * @test
 * @bug 8072480 8203814
 * @summary Check the platform classpath contains the correct elements.
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.code
 *          jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.compiler/com.sun.tools.javac.platform
 *          jdk.compiler/com.sun.tools.javac.util
 *          jdk.jdeps/com.sun.tools.classfile
 *          jdk.jdeps/com.sun.tools.javap
 * @build toolbox.ToolBox ElementStructureTest
 * @run main ElementStructureTest
 */

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.tools.FileObject;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

import com.sun.source.util.JavacTask;
import com.sun.tools.classfile.ClassFile;
import com.sun.tools.classfile.ConstantPoolException;
import com.sun.tools.javac.api.JavacTaskImpl;
import com.sun.tools.javac.code.Symbol.CompletionFailure;
import com.sun.tools.javac.platform.PlatformProvider;

import toolbox.ToolBox;


/**To generate the hash values for version N, invoke this class like:
 *
 *     java ElementStructureTest generate-hashes $LANGTOOLS_DIR/make/data/symbols/include.list (<classes-for-N> N)+
 *
 * Where <classes-for-N> is the file produced by make/src/classes/build/tools/symbolgenerator/Probe.java.
 * So, to produce hashes for 6, 7 and 8, this command can be used:
 *
 *     java ElementStructureTest generate-hashes classes-6 6 classes-7 7 classes-8 8
 *
 * To inspect differences between the actual and expected output for version N, invoke this class like:
 *
 *     java ElementStructureTest generate-output $LANGTOOLS_DIR/make/data/symbols/include.list (<classes-for-N> N <actual-output-file> <expected-output-file>)+
 *
 * For example, to get the actual and expected output for 6 in /tmp/actual and /tmp/expected, respectively:
 *
 *     java ElementStructureTest generate-output $LANGTOOLS_DIR/make/data/symbols/include.list classes-6 6 /tmp/actual /tmp/expected
 */
public class ElementStructureTest {

    static final byte[] hash6 = new byte[] {
        (byte) 0x99, (byte) 0x34, (byte) 0x82, (byte) 0xCF,
        (byte) 0xE0, (byte) 0x53, (byte) 0xF3, (byte) 0x13,
        (byte) 0x4E, (byte) 0xCF, (byte) 0x49, (byte) 0x32,
        (byte) 0xB7, (byte) 0x52, (byte) 0x0F, (byte) 0x68
    };
    static final byte[] hash7 = new byte[] {
        (byte) 0x45, (byte) 0xCA, (byte) 0x83, (byte) 0xCD,
        (byte) 0x1A, (byte) 0x68, (byte) 0x57, (byte) 0x9C,
        (byte) 0x6F, (byte) 0x2D, (byte) 0xEB, (byte) 0x28,
        (byte) 0xAB, (byte) 0x05, (byte) 0x53, (byte) 0x6E
    };
    static final byte[] hash8 = new byte[] {
        (byte) 0x26, (byte) 0x8C, (byte) 0xFD, (byte) 0x61,
        (byte) 0x53, (byte) 0x00, (byte) 0x57, (byte) 0x10,
        (byte) 0x36, (byte) 0x2B, (byte) 0x92, (byte) 0x0B,
        (byte) 0xE1, (byte) 0x6A, (byte) 0xB5, (byte) 0xFD
    };

    final static Map<String, byte[]> version2Hash = new HashMap<>();

    static {
        version2Hash.put("6", hash6);
        version2Hash.put("7", hash7);
        version2Hash.put("8", hash8);
    }

    public static void main(String... args) throws Exception {
        if (args.length == 0) {
            new ElementStructureTest().doTest();
            return ;
        }
        switch (args[0]) {
            case "generate-hashes":
                new ElementStructureTest().generateHashes(args);
                break;
            case "generate-output":
                new ElementStructureTest().generateOutput(args);
                break;
            default:
                throw new IllegalStateException("Unrecognized request: " + args[0]);
        }
    }

    void doTest() throws Exception {
        for (PlatformProvider provider : ServiceLoader.load(PlatformProvider.class)) {
            for (String ver : provider.getSupportedPlatformNames()) {
                if (!version2Hash.containsKey(ver))
                    continue;
                try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); Writer output = new OutputStreamWriter(baos, "UTF-8")) {
                    run(output, ver);
                    output.close();
                    byte[] actual = MessageDigest.getInstance("MD5").digest(baos.toByteArray());
                    if (!Arrays.equals(version2Hash.get(ver), actual))
                        throw new AssertionError("Wrong hash: " + toHex(actual) + " for version: " + ver);
                }
            }
        }
    }

    void generateHashes(String... args) throws Exception {
        Predicate<String> ignoreList = constructAcceptIgnoreList(args[1]);
        for (int i = 2; i < args.length; i += 2) {
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); Writer output = new OutputStreamWriter(baos, "UTF-8")) {
                realClasses(args[i], ignoreList, output, args[i + 1]);
                output.close();
                System.err.println("version:" + args[i + 1] + "; " + toHex(MessageDigest.getInstance("MD5").digest(baos.toByteArray())));
            }
        }
    }

    void generateOutput(String... args) throws Exception {
        Predicate<String> ignoreList = constructAcceptIgnoreList(args[1]);
        for (int i = 2; i < args.length; i += 4) {
            try (Writer actual = Files.newBufferedWriter(Paths.get(args[i + 2]));
                 Writer expected = Files.newBufferedWriter(Paths.get(args[i + 3]))) {
                run(actual, args[i + 1]);
                realClasses(args[i], ignoreList, expected, args[i + 1]);
            }
        }
    }

    Predicate<String> constructAcceptIgnoreList(String fromFiles) throws IOException {
        StringBuilder acceptPattern = new StringBuilder();
        StringBuilder rejectPattern = new StringBuilder();
        for (String file : fromFiles.split(File.pathSeparator)) {
            try (Stream<String> lines = Files.lines(Paths.get(file))) {
                lines.forEach(line -> {
                    if (line.isEmpty())
                        return;
                    StringBuilder targetPattern;
                    switch (line.charAt(0)) {
                        case '+':
                            targetPattern = acceptPattern;
                            break;
                        case '-':
                            targetPattern = rejectPattern;
                            break;
                        default:
                            return ;
                    }
                    line = line.substring(1);
                    if (line.endsWith("/")) {
                        line += "[^/]*";
                    } else {
                        line += "|" + line + "$[^/]*";
                    }
                    line = line.replace("/", ".");
                    if (targetPattern.length() != 0)
                        targetPattern.append("|");
                    targetPattern.append(line);
                });
            }
        }
        Pattern accept = Pattern.compile(acceptPattern.toString());
        Pattern reject = Pattern.compile(rejectPattern.toString());

        return clazzName -> accept.matcher(clazzName).matches() && !reject.matcher(clazzName).matches();
    }

    private static String toHex(byte[] bytes) {
        StringBuilder hex = new StringBuilder();
        String delim = "";

        for (byte b : bytes) {
            hex.append(delim);
            hex.append(String.format("(byte) 0x%02X", b));
            delim = ", ";
        }

        return hex.toString();
    }

    void run(Writer output, String version) throws Exception {
        List<String> options = Arrays.asList("--release", version, "-classpath", "");
        List<ToolBox.JavaSource> files = Arrays.asList(new ToolBox.JavaSource("Test", ""));
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        JavacTaskImpl task = (JavacTaskImpl) compiler.getTask(null, null, null, options, null, files);

        task.analyze();

        JavaFileManager fm = task.getContext().get(JavaFileManager.class);

        for (String pack : packages(fm)) {
            PackageElement packEl = task.getElements().getPackageElement(pack);
            if (packEl == null) {
                throw new AssertionError("Cannot find package: " + pack);
            }
            new ExhaustiveElementScanner(task, output, p -> true).visit(packEl);
        }
    }

    void realClasses(String location, Predicate<String> acceptor, Writer output, String version) throws Exception {
        Path classes = Paths.get(location);
        Map<String, JavaFileObject> className2File = new HashMap<>();
        Map<JavaFileObject, String> file2ClassName = new HashMap<>();

        try (BufferedReader descIn = Files.newBufferedReader(classes)) {
            String classFileData;

            while ((classFileData = descIn.readLine()) != null) {
                ByteArrayOutputStream data = new ByteArrayOutputStream();
                for (int i = 0; i < classFileData.length(); i += 2) {
                    data.write(Integer.parseInt(classFileData.substring(i, i + 2), 16));
                }
                JavaFileObject file = new ByteArrayJavaFileObject(data.toByteArray());
                try (InputStream in = new ByteArrayInputStream(data.toByteArray())) {
                    String name = ClassFile.read(in).getName().replace("/", ".");
                    className2File.put(name, file);
                    file2ClassName.put(file, name);
                } catch (IOException | ConstantPoolException ex) {
                    throw new IllegalStateException(ex);
                }
            }
        }

        try (JavaFileManager fm = new TestFileManager(className2File, file2ClassName)) {
            JavacTaskImpl task = (JavacTaskImpl) ToolProvider.getSystemJavaCompiler().getTask(null, fm, null, Arrays.asList("-source", version), null, Arrays.asList(new ToolBox.JavaSource("Test", "")));
            task.parse();

            PACK: for (String pack : packages(fm)) {
                PackageElement packEl = task.getElements().getPackageElement(pack);
                assert packEl != null;
                new ExhaustiveElementScanner(task, output, acceptor).visit(packEl);
            }
        }
    }

    Set<String> packages(JavaFileManager fm) throws IOException {
        Set<String> packages = new TreeSet<>();
        EnumSet<Kind> kinds = EnumSet.of(JavaFileObject.Kind.CLASS, JavaFileObject.Kind.OTHER);

        for (JavaFileObject file : fm.list(StandardLocation.PLATFORM_CLASS_PATH, "", kinds, true)) {
            String binary = fm.inferBinaryName(StandardLocation.PLATFORM_CLASS_PATH, file);
            packages.add(binary.substring(0, binary.lastIndexOf('.')));
        }

        return packages;
    }

    final class ExhaustiveElementScanner implements ElementVisitor<Void, Void> {

        final JavacTask task;
        final Writer out;
        final Predicate<String> acceptType;

        public ExhaustiveElementScanner(JavacTask task, Writer out, Predicate<String> acceptType) {
            this.task = task;
            this.out = out;
            this.acceptType = acceptType;
        }

        @Override
        public Void visit(Element e, Void p) {
            return e.accept(this, p);
        }

        @Override
        public Void visit(Element e) {
            return e.accept(this, null);
        }

        private void write(TypeMirror type) throws IOException {
            try {
                out.write(type.toString()
                              .replace("java.lang.invoke.MethodHandle$PolymorphicSignature", "java.lang.invoke.MethodHandle.PolymorphicSignature")
                              .replace("javax.swing.JRootPane$DefaultAction", "javax.swing.JRootPane.DefaultAction")
                              .replace("javax.swing.plaf.metal.MetalFileChooserUI$DirectoryComboBoxRenderer", "javax.swing.plaf.metal.MetalFileChooserUI.DirectoryComboBoxRenderer")
                         );
            } catch (CompletionFailure cf) {
                out.write("cf");
            }
        }

        private void writeTypes(Iterable<? extends TypeMirror> types) throws IOException {
            String sep = "";

            for (TypeMirror type : types) {
                out.write(sep);
                write(type);
                sep = ", ";
            }
        }

        private void writeAnnotations(Iterable<? extends AnnotationMirror> annotations) throws IOException {
            for (AnnotationMirror ann : annotations) {
                out.write("@");
                write(ann.getAnnotationType());
                if (!ann.getElementValues().isEmpty()) {
                    out.write("(");
                    Map<ExecutableElement, AnnotationValue> valuesMap = new TreeMap<>((a1, a2) -> a1.getSimpleName().toString().compareTo(a2.getSimpleName().toString()));
                    valuesMap.putAll(ann.getElementValues());
                    for (Entry<? extends ExecutableElement, ? extends AnnotationValue> ev : valuesMap.entrySet()) {
                        out.write(ev.getKey().getSimpleName().toString());
                        out.write(" = ");
                        out.write(ev.getValue().toString());
                    }
                    out.write(")");
                }
            }
        }

        void analyzeElement(Element e) {
            try {
                write(e.asType());
                writeAnnotations(e.getAnnotationMirrors());
                out.write(e.getKind().toString());
                out.write(e.getModifiers().toString());
                out.write(e.getSimpleName().toString());
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        boolean acceptAccess(Element e) {
            return e.getModifiers().contains(Modifier.PUBLIC) || e.getModifiers().contains(Modifier.PROTECTED);
        }

        @Override
        public Void visitExecutable(ExecutableElement e, Void p) {
            if (!acceptAccess(e))
                return null;
            try {
                analyzeElement(e);
                out.write(String.valueOf(e.getDefaultValue()));
                for (VariableElement param : e.getParameters()) {
                    visit(param, p);
                }
                out.write(String.valueOf(typeMirrorTranslate(e.getReceiverType())));
                write(e.getReturnType());
                out.write(e.getSimpleName().toString());
                writeTypes(e.getThrownTypes());
                for (TypeParameterElement param : e.getTypeParameters()) {
                    visit(param, p);
                }
                out.write(String.valueOf(e.isDefault()));
                out.write(String.valueOf(e.isVarArgs()));
                out.write("\n");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            return null;
        }

        /**
         * Original implementation of getReceiverType returned null
         * for many cases where TypeKind.NONE was specified; translate
         * back to null to compare against old hashes.
         */
        private TypeMirror typeMirrorTranslate(TypeMirror type) {
            if (type.getKind() == javax.lang.model.type.TypeKind.NONE)
                return null;
            else
                return type;
        }

        @Override
        public Void visitPackage(PackageElement e, Void p) {
            List<Element> types = new ArrayList<>(e.getEnclosedElements());
            Collections.sort(types, (e1, e2) -> e1.getSimpleName().toString().compareTo(e2.getSimpleName().toString()));
            for (Element encl : types) {
                visit(encl, p);
            }
            return null;
        }

        @Override
        public Void visitType(TypeElement e, Void p) {
            if (!acceptAccess(e))
                return null;
            writeType(e);
            return null;
        }

        void writeType(TypeElement e) {
            if (!acceptType.test(task.getElements().getBinaryName(e).toString()))
                return ;
            try {
                analyzeElement(e);
                writeTypes(e.getInterfaces());
                out.write(e.getNestingKind().toString());
                out.write(e.getQualifiedName().toString());
                write(e.getSuperclass());
                for (TypeParameterElement param : e.getTypeParameters()) {
                    visit(param, null);
                }
                List<Element> defs = new ArrayList<>(e.getEnclosedElements()); //XXX: forcing ordering for members - not completely correct!
                Collections.sort(defs, (e1, e2) -> e1.toString().compareTo(e2.toString()));
                for (Element def : defs) {
                    visit(def, null);
                }
                out.write("\n");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        @Override
        public Void visitVariable(VariableElement e, Void p) {
            if (!acceptAccess(e))
                return null;
            try {
                analyzeElement(e);
                out.write(String.valueOf(e.getConstantValue()));
                out.write("\n");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            return null;
        }

        @Override
        public Void visitTypeParameter(TypeParameterElement e, Void p) {
            try {
                analyzeElement(e);
                out.write(e.getBounds().toString());
                out.write("\n");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            return null;
        }

        @Override
        public Void visitModule(ModuleElement e, Void p) {
            throw new IllegalStateException("Not supported yet.");
        }

        @Override
        public Void visitUnknown(Element e, Void p) {
            throw new IllegalStateException("Should not get here.");
        }

    }

    final class TestFileManager implements JavaFileManager {

        final Map<String, JavaFileObject> className2File;
        final Map<JavaFileObject, String> file2ClassName;

        public TestFileManager(Map<String, JavaFileObject> className2File, Map<JavaFileObject, String> file2ClassName) {
            this.className2File = className2File;
            this.file2ClassName = file2ClassName;
        }

        @Override
        public ClassLoader getClassLoader(Location location) {
            return new URLClassLoader(new URL[0]);
        }

        @Override
        public Iterable<JavaFileObject> list(Location location, String packageName, Set<Kind> kinds, boolean recurse) throws IOException {
            if (location != StandardLocation.PLATFORM_CLASS_PATH || !kinds.contains(Kind.CLASS))
                return Collections.emptyList();

            if (!packageName.isEmpty())
                packageName += ".";

            List<JavaFileObject> result = new ArrayList<>();

            for (Entry<String, JavaFileObject> e : className2File.entrySet()) {
                String currentPackage = e.getKey().substring(0, e.getKey().lastIndexOf(".") + 1);
                if (recurse ? currentPackage.startsWith(packageName) : packageName.equals(currentPackage))
                    result.add(e.getValue());
            }

            return result;
        }

        @Override
        public String inferBinaryName(Location location, JavaFileObject file) {
            return file2ClassName.get(file);
        }

        @Override
        public boolean isSameFile(FileObject a, FileObject b) {
            return a == b;
        }

        @Override
        public boolean handleOption(String current, Iterator<String> remaining) {
            return false;
        }

        @Override
        public boolean hasLocation(Location location) {
            return location == StandardLocation.PLATFORM_CLASS_PATH;
        }

        @Override
        public JavaFileObject getJavaFileForInput(Location location, String className, Kind kind) throws IOException {
            if (location != StandardLocation.PLATFORM_CLASS_PATH || kind != Kind.CLASS)
                return null;

            return className2File.get(className);
        }

        @Override
        public JavaFileObject getJavaFileForOutput(Location location, String className, Kind kind, FileObject sibling) throws IOException {
            throw new UnsupportedOperationException("");
        }

        @Override
        public FileObject getFileForInput(Location location, String packageName, String relativeName) throws IOException {
            return null;
        }

        @Override
        public FileObject getFileForOutput(Location location, String packageName, String relativeName, FileObject sibling) throws IOException {
            throw new UnsupportedOperationException("");
        }

        @Override
        public void flush() throws IOException {
        }

        @Override
        public void close() throws IOException {
        }

        @Override
        public int isSupportedOption(String option) {
            return -1;
        }

    }

    static class ByteArrayJavaFileObject implements JavaFileObject {

        private final byte[] data;

        public ByteArrayJavaFileObject(byte[] data) {
            this.data = data;
        }

        @Override
        public Kind getKind() {
            return Kind.CLASS;
        }

        @Override
        public boolean isNameCompatible(String simpleName, Kind kind) {
            return true;
        }

        @Override
        public NestingKind getNestingKind() {
            return null;
        }

        @Override
        public Modifier getAccessLevel() {
            return null;
        }

        @Override
        public URI toUri() {
            return null;
        }

        @Override
        public String getName() {
            return null;
        }

        @Override
        public InputStream openInputStream() throws IOException {
            return new ByteArrayInputStream(data);
        }

        @Override
        public OutputStream openOutputStream() throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public Reader openReader(boolean ignoreEncodingErrors) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public Writer openWriter() throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public long getLastModified() {
            return 0;
        }

        @Override
        public boolean delete() {
            throw new UnsupportedOperationException();
        }
    }

}
