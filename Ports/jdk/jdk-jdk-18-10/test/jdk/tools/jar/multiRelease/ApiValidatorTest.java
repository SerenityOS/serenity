/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 # @bug 8196748
 * @summary Tests for API validator.
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          jdk.compiler
 *          jdk.jartool
 * @build jdk.test.lib.Utils
 *        jdk.test.lib.Asserts
 *        jdk.test.lib.JDKToolFinder
 *        jdk.test.lib.JDKToolLauncher
 *        jdk.test.lib.Platform
 *        jdk.test.lib.process.*
 *        MRTestBase
 * @run testng/timeout=1200 ApiValidatorTest
 */

import jdk.test.lib.process.OutputAnalyzer;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class ApiValidatorTest extends MRTestBase {

    static final Pattern MODULE_PATTERN = Pattern.compile("module (\\w+)");
    static final Pattern CLASS_PATTERN = Pattern.compile("package (\\w+).*public class (\\w+)");

    private Path root;
    private Path classes;

    @BeforeMethod
    void testInit(Method method) {
        root = Paths.get(method.getName());
        classes = root.resolve("classes");
    }

    @Test(dataProvider = "signatureChange")
    public void changeMethodSignature(String sigBase, String sigV10,
                                      boolean isAcceptable) throws Throwable {

        String METHOD_SIG = "#SIG";
        String classTemplate =
                "public class C { \n" +
                        "    " + METHOD_SIG + "{ throw new RuntimeException(); };\n" +
                        "}\n";
        String base = classTemplate.replace(METHOD_SIG, sigBase);
        String v10 = classTemplate.replace(METHOD_SIG, sigV10);

        compileTemplate(classes.resolve("base"), base);
        compileTemplate(classes.resolve("v10"), v10);

        String jarfile = root.resolve("test.jar").toString();
        OutputAnalyzer result = jar("cf", jarfile,
                "-C", classes.resolve("base").toString(), ".",
                "--release", "10", "-C", classes.resolve("v10").toString(),
                ".");

        String failureMessage = "contains a class with different api from earlier version";
        checkResult(result, isAcceptable, failureMessage);
        if (isAcceptable) result.shouldBeEmptyIgnoreVMWarnings();


        Path malformed = root.resolve("zip").resolve("test.jar");
        zip(malformed,
            Map.entry("", classes.resolve("base")),
            Map.entry("META-INF/versions/10", classes.resolve("v10")));

        result = validateJar(malformed.toString(), isAcceptable, failureMessage);
        if (isAcceptable) result.shouldBeEmptyIgnoreVMWarnings();
    }

    @DataProvider
    Object[][] signatureChange() {
        return new Object[][]{
                {"public int m()", "protected int m()", false},
                {"protected int m()", "public int m()", false},
                {"public int m()", "int m()", false},
                {"protected int m()", "private int m()", false},
                {"private int m()", "int m()", true},
                {"int m()", "private int m()", true},
                {"int m()", "private int m(boolean b)", true},
                {"public int m()", "public int m(int i)", false},
                {"public int m()", "public int k()", false},
                {"public int m()", "private int k()", false},
// @ignore JDK-8172147   {"public int m()", "public boolean m()", false},
// @ignore JDK-8172147   {"public boolean", "public Boolean", false},
// @ignore JDK-8172147   {"public <T> T", "public <T extends String> T", false},
        };
    }

    @Test(dataProvider = "publicAPI")
    public void introducingPublicMembers(String publicAPI) throws Throwable {
        String API = "#API";
        String classTemplate =
                "public class C { \n" +
                        "    " + API + "\n" +
                        "    public void method(){ };\n" +
                        "}\n";
        String base = classTemplate.replace(API, "");
        String v10 = classTemplate.replace(API, publicAPI);

        compileTemplate(classes.resolve("base"), base);
        compileTemplate(classes.resolve("v10"), v10);

        String failureMessage = "contains a class with different api from earlier version";

        String jarfile = root.resolve("test.jar").toString();
        jar("cf", jarfile, "-C", classes.resolve("base").toString(), ".",
                "--release", "10", "-C", classes.resolve("v10").toString(), ".")
                .shouldNotHaveExitValue(SUCCESS)
                .shouldContain(failureMessage);

        Path malformed = root.resolve("zip").resolve("test.jar");
        zip(malformed,
            Map.entry("", classes.resolve("base")),
            Map.entry("META-INF/versions/10", classes.resolve("v10")));

        validateJar(malformed.toString(), false, failureMessage);
    }

    @DataProvider
    Object[][] publicAPI() {
        return new Object[][]{
// @ignore JDK-8172148  {"protected class Inner { public void m(){ } } "}, // protected inner class
// @ignore JDK-8172148  {"public class Inner { public void m(){ } }"},  // public inner class
// @ignore JDK-8172148  {"public enum E { A; }"},  // public enum
                {"public void m(){ }"}, // public method
                {"protected void m(){ }"}, // protected method
        };
    }

    @Test(dataProvider = "privateAPI")
    public void introducingPrivateMembers(String privateAPI) throws Throwable {
        String API = "#API";
        String classTemplate =
                "public class C { \n" +
                        "    " + API + "\n" +
                        "    public void method(){ };\n" +
                        "}\n";
        String base = classTemplate.replace(API, "");
        String v10 = classTemplate.replace(API, privateAPI);

        compileTemplate(classes.resolve("base"), base);
        compileTemplate(classes.resolve("v10"), v10);

        String jarfile = root.resolve("test.jar").toString();
        jar("cf", jarfile, "-C", classes.resolve("base").toString(), ".",
                "--release", "10", "-C", classes.resolve("v10").toString(), ".")
                .shouldHaveExitValue(SUCCESS);
        validateJar(jarfile);
        // add release
        jar("uf", jarfile,
                "--release", "11", "-C", classes.resolve("v10").toString(), ".")
                .shouldHaveExitValue(SUCCESS);
        validateJar(jarfile);
        // replace release
        jar("uf", jarfile,
                "--release", "11", "-C", classes.resolve("v10").toString(), ".")
                .shouldHaveExitValue(SUCCESS);
        validateJar(jarfile);
    }

    @DataProvider
    Object[][] privateAPI() {
        return new Object[][]{
                {"private class Inner { public void m(){ } } "}, // private inner class
                {"class Inner { public void m(){ } }"},  // package private inner class
                {"enum E { A; }"},  // package private enum
                // Local class and private method
                {"private void m(){ class Inner { public void m(){} } Inner i = null; }"},
                {"void m(){ }"}, // package private method
        };
    }

    private void compileTemplate(Path classes, String template) throws Throwable {
        Path classSourceFile = Files.createDirectories(
                classes.getParent().resolve("src").resolve(classes.getFileName()))
                .resolve("C.java");
        Files.write(classSourceFile, template.getBytes());
        javac(classes, classSourceFile);
    }

     /* Modular multi-release checks */

    @Test
    public void moduleNameHasChanged() throws Throwable {

        compileModule(classes.resolve("base"), "module A { }");
        compileModule(classes.resolve("v10"), "module B { }");

        String failureMessage = "incorrect name";

        String jarfile = root.resolve("test.jar").toString();
        jar("cf", jarfile, "-C", classes.resolve("base").toString(), ".",
                "--release", "10", "-C", classes.resolve("v10").toString(), ".")
                .shouldNotHaveExitValue(SUCCESS)
                .shouldContain(failureMessage);

        Path malformed = root.resolve("zip").resolve("test.jar");
        zip(malformed,
            Map.entry("", classes.resolve("base")),
            Map.entry("META-INF/versions/10", classes.resolve("v10")));

        validateJar(malformed.toString(), false, failureMessage);

        // update module-info release
        jar("cf", jarfile, "-C", classes.resolve("base").toString(), ".",
                "--release", "10", "-C", classes.resolve("base").toString(), ".")
                .shouldHaveExitValue(SUCCESS);
        validateJar(jarfile);

        jar("uf", jarfile,
                "--release", "10", "-C", classes.resolve("v10").toString(), ".")
                .shouldNotHaveExitValue(SUCCESS)
                .shouldContain(failureMessage);
    }

    //    @Test @ignore 8173370
    public void moduleBecomeOpen() throws Throwable {

        compileModule(classes.resolve("base"), "module A { }");
        compileModule(classes.resolve("v10"), "open module A { }");

        String jarfile = root.resolve("test.jar").toString();
        jar("cf", jarfile, "-C", classes.resolve("base").toString(), ".",
                "--release", "10", "-C", classes.resolve("v10").toString(), ".")
                .shouldNotHaveExitValue(SUCCESS)
                .shouldContain("FIX ME");

        // update module-info release
        jar("cf", jarfile, "-C", classes.resolve("base").toString(), ".",
                "--release", "10", "-C", classes.resolve("base").toString(), ".")
                .shouldHaveExitValue(SUCCESS);
        validateJar(jarfile);
        jar("uf", jarfile,
                "--release", "10", "-C", classes.resolve("v10").toString(), ".")
                .shouldNotHaveExitValue(SUCCESS)
                .shouldContain("FIX ME");
    }

    @Test
    public void moduleRequires() throws Throwable {

        String BASE_VERSION_DIRECTIVE = "requires jdk.compiler;";
        // add transitive flag
        moduleDirectivesCase(BASE_VERSION_DIRECTIVE,
                "requires transitive jdk.compiler;",
                false,
                "contains additional \"requires transitive\"");
        // remove requires
        moduleDirectivesCase(BASE_VERSION_DIRECTIVE,
                "",
                true,
                "");
        // add requires
        moduleDirectivesCase(BASE_VERSION_DIRECTIVE,
                "requires jdk.compiler; requires jdk.jartool;",
                true,
                "");
        // add requires transitive
        moduleDirectivesCase(BASE_VERSION_DIRECTIVE,
                "requires jdk.compiler; requires transitive jdk.jartool;",
                false,
                "contains additional \"requires transitive\"");
    }

    @Test
    public void moduleExports() throws Throwable {

        String BASE_VERSION_DIRECTIVE = "exports pkg1; exports pkg2 to jdk.compiler;";
        // add export
        moduleDirectivesCase(BASE_VERSION_DIRECTIVE,
                BASE_VERSION_DIRECTIVE + " exports pkg3;",
                false,
                "contains different \"exports\"");
        // change exports to qualified exports
        moduleDirectivesCase(BASE_VERSION_DIRECTIVE,
                "exports pkg1 to jdk.compiler; exports pkg2;",
                false,
                "contains different \"exports\"");
        // remove exports
        moduleDirectivesCase(BASE_VERSION_DIRECTIVE,
                "exports pkg1;",
                false,
                "contains different \"exports\"");
        // add qualified exports
        moduleDirectivesCase(BASE_VERSION_DIRECTIVE,
                BASE_VERSION_DIRECTIVE + " exports pkg3 to jdk.compiler;",
                false,
                "contains different \"exports\"");
    }

    @Test
    public void moduleOpens() throws Throwable {

        String BASE_VERSION_DIRECTIVE = "opens pkg1; opens pkg2 to jdk.compiler;";
        // add opens
        moduleDirectivesCase(BASE_VERSION_DIRECTIVE,
                BASE_VERSION_DIRECTIVE + " opens pkg3;",
                false,
                "contains different \"opens\"");
        // change opens to qualified opens
        moduleDirectivesCase(BASE_VERSION_DIRECTIVE,
                "opens pkg1 to jdk.compiler; opens pkg2;",
                false,
                "contains different \"opens\"");
        // remove opens
        moduleDirectivesCase(BASE_VERSION_DIRECTIVE,
                "opens pkg1;",
                false,
                "contains different \"opens\"");
        // add qualified opens
        moduleDirectivesCase(BASE_VERSION_DIRECTIVE,
                BASE_VERSION_DIRECTIVE + " opens pkg3 to jdk.compiler;",
                false,
                "contains different \"opens\"");
    }

    @Test
    public void moduleProvides() throws Throwable {

        String BASE_VERSION_DIRECTIVE = "provides pkg1.A with pkg1.A;";
        // add provides
        moduleDirectivesCase(BASE_VERSION_DIRECTIVE,
                BASE_VERSION_DIRECTIVE + " provides pkg2.B with pkg2.B;",
                false,
                "contains different \"provides\"");
        // change service impl
        moduleDirectivesCase(BASE_VERSION_DIRECTIVE,
                "provides pkg1.A with pkg2.B;",
                false,
                "contains different \"provides\"");
        // remove provides
        moduleDirectivesCase(BASE_VERSION_DIRECTIVE,
                "",
                false,
                "contains different \"provides\"");
    }

    @Test
    public void moduleUses() throws Throwable {

        String BASE_VERSION_DIRECTIVE = "uses pkg1.A;";
        // add
        moduleDirectivesCase(BASE_VERSION_DIRECTIVE,
                BASE_VERSION_DIRECTIVE + " uses pkg2.B;",
                true,
                "");
        // replace
        moduleDirectivesCase(BASE_VERSION_DIRECTIVE,
                "uses pkg2.B;",
                true,
                "");
        // remove
        moduleDirectivesCase(BASE_VERSION_DIRECTIVE,
                "",
                true,
                "");
    }

    private void moduleDirectivesCase(String baseDirectives,
                                      String versionedDirectives,
                                      boolean expectSuccess,
                                      String expectedMessage) throws Throwable {
        String[] moduleClasses = {
                "package pkg1; public class A { }",
                "package pkg2; public class B extends pkg1.A { }",
                "package pkg3; public class C extends pkg2.B { }"};
        compileModule(classes.resolve("base"),
                "module A { " + baseDirectives + " }",
                moduleClasses);
        compileModule(classes.resolve("v10"),
                "module A { " + versionedDirectives + " }",
                moduleClasses);

        String jarfile = root.resolve("test.jar").toString();
        OutputAnalyzer output = jar("cf", jarfile,
                "-C", classes.resolve("base").toString(), ".",
                "--release", "10", "-C", classes.resolve("v10").toString(), ".");
        checkResult(output, expectSuccess, expectedMessage);

        Path malformed = root.resolve("zip").resolve("test.jar");
        zip(malformed,
            Map.entry("", classes.resolve("base")),
            Map.entry("META-INF/versions/10", classes.resolve("v10")));

        validateJar(malformed.toString(), expectSuccess, expectedMessage);
    }

    private void compileModule(Path classes, String moduleSource,
                               String... classSources) throws Throwable {
        Matcher moduleMatcher = MODULE_PATTERN.matcher(moduleSource);
        moduleMatcher.find();
        String name = moduleMatcher.group(1);
        Path moduleinfo = Files.createDirectories(
                classes.getParent().resolve("src").resolve(name))
                .resolve("module-info.java");
        Files.write(moduleinfo, moduleSource.getBytes());

        Path[] sourceFiles = new Path[classSources.length + 1];
        sourceFiles[0] = moduleinfo;

        for (int i = 0; i < classSources.length; i++) {
            String classSource = classSources[i];
            Matcher classMatcher = CLASS_PATTERN.matcher(classSource);
            classMatcher.find();
            String packageName = classMatcher.group(1);
            String className = classMatcher.group(2);

            Path packagePath = moduleinfo.getParent()
                    .resolve(packageName.replace('.', '/'));
            Path sourceFile = Files.createDirectories(packagePath)
                    .resolve(className + ".java");
            Files.write(sourceFile, classSource.getBytes());

            sourceFiles[i + 1] = sourceFile;
        }

        javac(classes, sourceFiles);
    }

    @SafeVarargs
    private void zip(Path file, Map.Entry<String, Path>... copies) throws IOException {
        Files.createDirectories(file.getParent());
        Files.deleteIfExists(file);
        try (FileSystem zipfs = FileSystems.newFileSystem(file, Map.of("create", "true"))) {
            for (var entry : copies) {
                Path dstDir = zipfs.getPath(entry.getKey());
                Path srcDir = entry.getValue();

                Files.createDirectories(dstDir);

                try (Stream<Path> stream = Files.walk(srcDir)) {
                    stream.filter(Files::isRegularFile).forEach(srcFile -> {
                        try {
                            Path relativePath = srcDir.relativize(srcFile);
                            Path dst = dstDir.resolve(relativePath.toString());
                            Path dstParent = dst.getParent();
                            if (dstParent != null)
                                Files.createDirectories(dstParent);
                            Files.copy(srcFile, dst);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
                }
            }
        }
    }

    private static OutputAnalyzer checkResult(OutputAnalyzer result, boolean isAcceptable, String failureMessage) {
        if (isAcceptable) {
            result.shouldHaveExitValue(SUCCESS);
        } else {
            result.shouldNotHaveExitValue(SUCCESS)
                    .shouldContain(failureMessage);
        }

        return result;
    }

    private OutputAnalyzer validateJar(String jarFile) throws Throwable {
        return validateJar(jarFile, true, "");
    }

    private OutputAnalyzer validateJar(String jarFile, boolean shouldSucceed, String failureMessage) throws Throwable {
        return checkResult(jar("--validate", "--file", jarFile), shouldSucceed, failureMessage);
    }
}
