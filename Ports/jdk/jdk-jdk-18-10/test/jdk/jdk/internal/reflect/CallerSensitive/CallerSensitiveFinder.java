/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.tools.classfile.*;
import static com.sun.tools.classfile.ConstantPool.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.stream.Stream;

/*
 * @test
 * @bug 8010117
 * @summary Verify if CallerSensitive methods are annotated with
 *          CallerSensitive annotation
 * @modules jdk.jdeps/com.sun.tools.classfile jdk.jdeps/com.sun.tools.jdeps
 * @build CallerSensitiveFinder
 * @run main/othervm/timeout=900 CallerSensitiveFinder
 */
public class CallerSensitiveFinder {
    private static int numThreads = 3;
    private static boolean verbose = false;
    private final ExecutorService pool;

    public static void main(String[] args) throws Exception {
        Stream<Path> classes = null;
        String testclasses = System.getProperty("test.classes", ".");
        int i = 0;
        while (i < args.length) {
            String arg = args[i++];
            if (arg.equals("-v")) {
                verbose = true;
            } else {
                Path p = Paths.get(testclasses, arg);
                if (!p.toFile().exists()) {
                    throw new IllegalArgumentException(arg + " does not exist");
                }
                classes = Stream.of(p);
            }
        }

        if (classes == null) {
            classes = getPlatformClasses();
        }

        CallerSensitiveFinder csfinder = new CallerSensitiveFinder();
        List<String> errors = csfinder.run(classes);

        if (!errors.isEmpty()) {
            throw new RuntimeException(errors.size() +
                    " caller-sensitive methods are missing @CallerSensitive annotation");
        }
    }

    private final List<String> csMethodsMissingAnnotation = new CopyOnWriteArrayList<>();
    private final ReferenceFinder finder;
    public CallerSensitiveFinder() {
        this.finder = new ReferenceFinder(getFilter(), getVisitor());
        pool = Executors.newFixedThreadPool(numThreads);

    }

    private ReferenceFinder.Filter getFilter() {
        final String classname = "jdk/internal/reflect/Reflection";
        final String method = "getCallerClass";
        return new ReferenceFinder.Filter() {
            public boolean accept(ConstantPool cpool, CPRefInfo cpref) {
                try {
                    CONSTANT_NameAndType_info nat = cpref.getNameAndTypeInfo();
                    return cpref.getClassName().equals(classname) && nat.getName().equals(method);
                } catch (ConstantPoolException ex) {
                    throw new RuntimeException(ex);
                }
            }
        };
    }

    private ReferenceFinder.Visitor getVisitor() {
        return new ReferenceFinder.Visitor() {
            public void visit(ClassFile cf, Method m,  List<CPRefInfo> refs) {
                try {
                    // ignore jdk.unsupported/sun.reflect.Reflection.getCallerClass
                    // which is a "special" delegate to the internal getCallerClass
                    if (cf.getName().equals("sun/reflect/Reflection") &&
                        m.getName(cf.constant_pool).equals("getCallerClass"))
                        return;

                    String name = String.format("%s#%s %s", cf.getName(),
                                                m.getName(cf.constant_pool),
                                                m.descriptor.getValue(cf.constant_pool));
                    if (!CallerSensitiveFinder.isCallerSensitive(m, cf.constant_pool)) {
                        csMethodsMissingAnnotation.add(name);
                        System.err.println("Missing @CallerSensitive: " + name);
                    } else {
                        if (verbose) {
                            System.out.format("@CS  %s%n", name);
                        }
                    }
                } catch (ConstantPoolException ex) {
                    throw new RuntimeException(ex);
                }
            }
        };
    }

    public List<String> run(Stream<Path> classes)throws IOException, InterruptedException,
            ExecutionException, ConstantPoolException
    {
        classes.forEach(p -> pool.submit(getTask(p)));
        waitForCompletion();
        return csMethodsMissingAnnotation;
    }

    private static final String CALLER_SENSITIVE_ANNOTATION = "Ljdk/internal/reflect/CallerSensitive;";
    private static boolean isCallerSensitive(Method m, ConstantPool cp)
            throws ConstantPoolException
    {
        RuntimeAnnotations_attribute attr =
            (RuntimeAnnotations_attribute)m.attributes.get(Attribute.RuntimeVisibleAnnotations);
        if (attr != null) {
            for (int i = 0; i < attr.annotations.length; i++) {
                Annotation ann = attr.annotations[i];
                String annType = cp.getUTF8Value(ann.type_index);
                if (CALLER_SENSITIVE_ANNOTATION.equals(annType)) {
                    return true;
                }
            }
        }
        return false;
    }

    private final List<FutureTask<Void>> tasks = new ArrayList<>();

    /*
     * Each task parses the class file of the given path.
     * - parse constant pool to find matching method refs
     * - parse each method (caller)
     * - visit and find method references matching the given method name
     */
    private FutureTask<Void> getTask(Path p) {
        FutureTask<Void> task = new FutureTask<>(new Callable<>() {
            public Void call() throws Exception {
                try (InputStream is = Files.newInputStream(p)) {
                    finder.parse(ClassFile.read(is));
                } catch (IOException x) {
                    throw new UncheckedIOException(x);
                } catch (ConstantPoolException x) {
                    throw new RuntimeException(x);
                }
                return null;
            }
        });
        tasks.add(task);
        return task;
    }

    private void waitForCompletion() throws InterruptedException, ExecutionException {
        for (FutureTask<Void> t : tasks) {
            t.get();
        }
        if (tasks.isEmpty()) {
            throw new RuntimeException("No classes found, or specified.");
        }
        pool.shutdown();
        System.out.println("Parsed " + tasks.size() + " classfiles");
    }

    static Stream<Path> getPlatformClasses() throws IOException {
        Path home = Paths.get(System.getProperty("java.home"));

        // Either an exploded build or an image.
        File classes = home.resolve("modules").toFile();
        Path root = classes.isDirectory()
                        ? classes.toPath()
                        : FileSystems.getFileSystem(URI.create("jrt:/"))
                                     .getPath("/");

        try {
            return Files.walk(root)
                .filter(p -> p.getNameCount() > 1)
                .filter(p -> p.toString().endsWith(".class") &&
                             !p.toString().equals("module-info.class"));
        } catch (IOException x) {
            throw new UncheckedIOException(x);
        }
    }
}
