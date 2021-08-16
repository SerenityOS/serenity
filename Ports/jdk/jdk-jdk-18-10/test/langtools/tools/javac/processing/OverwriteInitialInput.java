/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8067747
 * @summary Verify the correct Filer behavior w.r.t. initial inputs
 *          (should throw FilerException when overwriting initial inputs).
 * @library /tools/lib /tools/javac/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 * @build toolbox.ToolBox toolbox.JavacTask toolbox.Task
 * @build OverwriteInitialInput JavacTestingAbstractProcessor
 * @run main OverwriteInitialInput
 */

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

import javax.annotation.processing.FilerException;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedOptions;
import javax.lang.model.element.TypeElement;
import javax.tools.StandardLocation;

import toolbox.JavacTask;
import toolbox.Task;
import toolbox.ToolBox;

@SupportedOptions({OverwriteInitialInput.EXPECT_EXCEPTION_OPTION,
                   OverwriteInitialInput.TEST_SOURCE
                  })
public class OverwriteInitialInput extends JavacTestingAbstractProcessor {

    public static final String EXPECT_EXCEPTION_OPTION = "exception";
    public static final String TEST_SOURCE = "testSource";

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) {
            if (processingEnv.getOptions().containsKey(EXPECT_EXCEPTION_OPTION)) {
                try (Writer w = processingEnv.getFiler().createSourceFile("Test").openWriter()) {
                    throw new AssertionError("Expected IOException not seen.");
                } catch (FilerException ex) {
                    //expected
                } catch (IOException ex) {
                    throw new IllegalStateException(ex);
                }
                try (OutputStream out = processingEnv.getFiler().createClassFile("Test").openOutputStream()) {
                    throw new AssertionError("Expected IOException not seen.");
                } catch (FilerException ex) {
                    //expected
                } catch (IOException ex) {
                    throw new IllegalStateException(ex);
                }
                if (processingEnv.getOptions().containsKey(TEST_SOURCE)) {
                    try (OutputStream out = processingEnv.getFiler().createResource(StandardLocation.SOURCE_OUTPUT, "", "Test.java").openOutputStream()) {
                        throw new AssertionError("Expected IOException not seen.");
                    } catch (FilerException ex) {
                        //expected
                    } catch (IOException ex) {
                        throw new IllegalStateException(ex);
                    }
                } else {
                    try (OutputStream out = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "", "Test2.class").openOutputStream()) {
                        throw new AssertionError("Expected IOException not seen.");
                    } catch (FilerException ex) {
                        //expected
                    } catch (IOException ex) {
                        throw new IllegalStateException(ex);
                    }
                }
            } else {
                try (Writer w = processingEnv.getFiler().createSourceFile("Test").openWriter()) {
                    w.append("public class Test {}");
                } catch (IOException ex) {
                    throw new IllegalStateException(ex);
                }
                try (OutputStream out = processingEnv.getFiler().createClassFile("Test2").openOutputStream()) {
                    try (ToolBox.MemoryFileManager mfm = new ToolBox.MemoryFileManager()) {
                        ToolBox tb = new ToolBox();
                        new JavacTask(tb)
                          .sources("public class Test2 {}")
                          .fileManager(mfm)
                          .run()
                          .writeAll();

                        out.write(mfm.getFileBytes(StandardLocation.CLASS_OUTPUT, "Test2"));
                    }
                } catch (IOException ex) {
                    throw new IllegalStateException(ex);
                }
            }
        }

        return false;
    }

    public static void main(String... args) throws IOException {
        new OverwriteInitialInput().run();
    }

    void run() throws IOException {
        run(Task.Mode.API);
        run(Task.Mode.CMDLINE);
    }

    void run(Task.Mode mode) throws IOException {
        ToolBox tb = new ToolBox();
        Path path = Paths.get("output");
        if (Files.isDirectory(path))
            tb.cleanDirectory(path);
        Files.deleteIfExists(path);
        tb.createDirectories(path);
        Path thisSource = Paths.get(System.getProperty("test.src"), "OverwriteInitialInput.java");
        new JavacTask(tb, mode).options("-processor", "OverwriteInitialInput",
                                        "-d", path.toString(),
                                        "-XDaccessInternalAPI=true")
                               .files(thisSource)
                               .run()
                               .writeAll();
        new JavacTask(tb, mode).options("-processor", "OverwriteInitialInput",
                                        "-d", path.toString(),
                                        "-A" + EXPECT_EXCEPTION_OPTION,
                                        "-A" + TEST_SOURCE,
                                        "-XDaccessInternalAPI=true")
                               .files(thisSource, path.resolve("Test.java"))
                               .run()
                               .writeAll();
        new JavacTask(tb, mode).options("-processor", "OverwriteInitialInput",
                                        "-classpath", path.toString(),
                                        "-processorpath", System.getProperty("test.class.path"),
                                        "-d", path.toString(),
                                        "-A" + EXPECT_EXCEPTION_OPTION,
                                        "-XDaccessInternalAPI=true")
                               .classes("Test", "Test2")
                               .run()
                               .writeAll();
    }

}
