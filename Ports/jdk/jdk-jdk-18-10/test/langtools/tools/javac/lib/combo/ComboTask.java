/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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

package combo;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TaskListener;
import com.sun.tools.javac.api.JavacTaskImpl;
import com.sun.tools.javac.util.Assert;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import combo.ComboParameter.Resolver;

import javax.lang.model.element.Element;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;

import java.io.IOException;
import java.io.Writer;
import java.net.URI;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * This class represents a compilation task associated with a combo test instance. This is a small
 * wrapper around {@link JavacTask} which allows for fluent setup style and which makes use of
 * the shared compilation context to speedup performances.
 */
public class ComboTask {

    /** Sources to be compiled in this task. */
    private List<JavaFileObject> sources = List.nil();

    /** Options associated with this task. */
    private List<String> options = List.nil();

    /** Diagnostic collector. */
    private DiagnosticCollector diagsCollector = new DiagnosticCollector();

    /** Output writer. */
    private Writer out;

    /** Listeners associated with this task. */
    private List<TaskListener> listeners = List.nil();

    /** Listener factories associated with this task. */
    private List<Function<Context, TaskListener>> listenerFactories = List.nil();

    /** Combo execution environment. */
    private ComboTestHelper<?>.Env env;

    ComboTask(ComboTestHelper<?>.Env env) {
        this.env = env;
    }

    /**
     * Add a new source to this task.
     */
    public ComboTask withSource(JavaFileObject comboSource) {
        sources = sources.prepend(comboSource);
        return this;
    }

    /**
     * Add a new template source with given name to this task; the template is replaced with
     * corresponding combo parameters (as defined in the combo test environment).
     */
    public ComboTask withSourceFromTemplate(String name, String template) {
        return withSource(new ComboTemplateSource(name, template));
    }

    /**
     * Add a new template source with default name ("Test") to this task; the template is replaced with
     * corresponding combo parameters (as defined in the combo test environment).
     */
    public ComboTask withSourceFromTemplate(String template) {
        return withSource(new ComboTemplateSource("Test", template));
    }

    /**
     * Add a new template source with given name to this task; the template is replaced with
     * corresponding combo parameters (as defined in the combo test environment). A custom resolver
     * is used to add combo parameter mappings to the current combo test environment.
     */
    public ComboTask withSourceFromTemplate(String name, String template, Resolver resolver) {
        return withSource(new ComboTemplateSource(name, template, resolver));
    }

    /**
     * Add a new template source with default name ("Test") to this task; the template is replaced with
     * corresponding combo parameters (as defined in the combo test environment). A custom resolver
     * is used to add combo parameter mappings to the current combo test environment.
     */
    public ComboTask withSourceFromTemplate(String template, Resolver resolver) {
        return withSource(new ComboTemplateSource("Test", template, resolver));
    }

    /**
     * Add a new option to this task.
     */
    public ComboTask withOption(String opt) {
        options = options.append(opt);
        return this;
    }

    /**
     * Add a set of options to this task.
     */
    public ComboTask withOptions(String[] opts) {
        for (String opt : opts) {
            options = options.append(opt);
        }
        return this;
    }

    /**
     * Add a set of options to this task.
     */
    public ComboTask withOptions(Iterable<? extends String> opts) {
        for (String opt : opts) {
            options = options.append(opt);
        }
        return this;
    }

    /**
     * Set the output writer associated with this task.
     */
    public ComboTask withWriter(Writer out) {
        this.out = out;
        return this;
    }

    /**
     * Add a task listener to this task.
     */
    public ComboTask withListener(TaskListener listener) {
        listeners = listeners.prepend(listener);
        return this;
    }

    /**
     * Add a task listener factory to this task.
     */
    public ComboTask withListenerFactory(Function<Context, TaskListener> factory) {
        listenerFactories = listenerFactories.prepend(factory);
        return this;
    }

    /**
     * Parse the sources associated with this task.
     */
    public void parse(Consumer<Result<Iterable<? extends CompilationUnitTree>>> c) {
        doRunTest(c, JavacTask::parse);
    }

    /**
     * Parse and analyzes the sources associated with this task.
     */
    public void analyze(Consumer<Result<Iterable<? extends Element>>> c) {
        doRunTest(c, JavacTask::analyze);
    }

    /**
     * Parse, analyze and perform code generation for the sources associated with this task.
     */
    public void generate(Consumer<Result<Iterable<? extends JavaFileObject>>> c) {
        doRunTest(c, JavacTask::generate);
    }

    private <V> void doRunTest(Consumer<Result<Iterable<? extends V>>> c,
                               Convertor<V> task2Data) {
        env.pool().getTask(out, env.fileManager(),
                diagsCollector, options, null, sources, task -> {
            try {
                for (TaskListener l : listeners) {
                    task.addTaskListener(l);
                }
                for (Function<Context, TaskListener> f : listenerFactories) {
                    task.addTaskListener(f.apply(((JavacTaskImpl) task).getContext()));
                }
                c.accept(new Result<>(task2Data.convert(task)));
                return null;
            } catch (IOException ex) {
                throw new AssertionError(ex);
            }
        });
    }

    public List<JavaFileObject> getSources() {
        return sources;
    }

    interface Convertor<V> {
        public Iterable<? extends V> convert(JavacTask task) throws IOException;
    }

    /**
     * This class represents an execution task. It allows the execution of one or more classes previously
     * added to a given class loader. This class uses reflection to execute any given static public method
     * in any given class. It's not restricted to the execution of the {@code main} method
     */
    public class ExecutionTask {
        private ClassLoader classLoader;
        private String methodName = "main";
        private Class<?>[] parameterTypes = new Class<?>[]{String[].class};
        private Object[] args = new String[0];
        private Consumer<Throwable> handler;
        private Class<?> c;

        private ExecutionTask(ClassLoader classLoader) {
            this.classLoader = classLoader;
        }

        /**
         * Set the name of the class to be loaded.
         */
        public ExecutionTask withClass(String className) {
            Assert.check(className != null, "class name value is null, impossible to proceed");
            try {
                c = classLoader.loadClass(className);
            } catch (Throwable t) {
                throw new IllegalStateException(t);
            }
            return this;
        }

        /**
         * Set the name of the method to be executed along with the parameter types to
         * reflectively obtain the method.
         */
        public ExecutionTask withMethod(String methodName, Class<?>... parameterTypes) {
            this.methodName = methodName;
            this.parameterTypes = parameterTypes;
            return this;
        }

        /**
         * Set the arguments to be passed to the method.
         */
        public ExecutionTask withArguments(Object... args) {
            this.args = args;
            return this;
        }

        /**
         * Set a handler to handle any exception thrown.
         */
        public ExecutionTask withHandler(Consumer<Throwable> handler) {
            this.handler = handler;
            return this;
        }

        /**
         * Executes the given method in the given class. Returns true if the execution was
         * successful, false otherwise.
         */
        public Object run() {
            try {
                java.lang.reflect.Method meth = c.getMethod(methodName, parameterTypes);
                meth.invoke(null, (Object)args);
                return true;
            } catch (Throwable t) {
                if (handler != null) {
                    handler.accept(t);
                }
                return false;
            }
        }
    }

    /**
     * This class is used to help clients accessing the results of a given compilation task.
     * Contains several helper methods to inspect diagnostics generated during the task execution.
     */
    public class Result<D> {

        /** The underlying compilation results. */
        private final D data;

        public Result(D data) {
            this.data = data;
        }

        public D get() {
            return data;
        }

        /**
         * Did this task generate any error diagnostics?
         */
        public boolean hasErrors() {
            return diagsCollector.diagsByKind.containsKey(Diagnostic.Kind.ERROR);
        }

        /**
         * Did this task generate any warning diagnostics?
         */
        public boolean hasWarnings() {
            return diagsCollector.diagsByKind.containsKey(Diagnostic.Kind.WARNING);
        }

        /**
         * Did this task generate any note diagnostics?
         */
        public boolean hasNotes() {
            return diagsCollector.diagsByKind.containsKey(Diagnostic.Kind.NOTE);
        }

        /**
         * Did this task generate any diagnostic with given key?
         */
        public boolean containsKey(String key) {
            return diagsCollector.diagsByKeys.containsKey(key);
        }

        /**
         * Retrieve the list of diagnostics of a given kind.
         */
        public List<Diagnostic<? extends JavaFileObject>> diagnosticsForKind(Diagnostic.Kind kind) {
            List<Diagnostic<? extends JavaFileObject>> diags = diagsCollector.diagsByKind.get(kind);
            return diags != null ? diags : List.nil();
        }

        /**
         * Retrieve the list of diagnostics with given key.
         */
        public List<Diagnostic<? extends JavaFileObject>> diagnosticsForKey(String key) {
            List<Diagnostic<? extends JavaFileObject>> diags = diagsCollector.diagsByKeys.get(key);
            return diags != null ? diags : List.nil();
        }

        /**
         * Dump useful info associated with this task.
         */
        public String compilationInfo() {
            return "instance#" + env.info().comboCount + ":[ options = " + options
                    + ", diagnostics = " + diagsCollector.diagsByKeys.keySet()
                    + ", dimensions = " + env.bindings
                    + ", sources = \n" + sources.stream().map(s -> {
                try {
                    return s.getCharContent(true);
                } catch (IOException ex) {
                    return "";
                }
            }).collect(Collectors.joining(",")) + "]";
        }
    }

    /**
     * This class represents a Java source file whose contents are defined in terms of a template
     * string. The holes in such template are expanded using corresponding combo parameter
     * instances which can be retrieved using a resolver object.
     */
    class ComboTemplateSource extends SimpleJavaFileObject {

        String source;
        Map<String, ComboParameter> localParametersCache = new HashMap<>();

        protected ComboTemplateSource(String name, String template) {
            this(name, template, null);
        }

        protected ComboTemplateSource(String name, String template, Resolver resolver) {
            super(URI.create("myfo:/" + env.info().comboCount + "/" + name + ".java"), Kind.SOURCE);
            source = ComboParameter.expandTemplate(template, pname -> resolveParameter(pname, resolver));
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return source;
        }

        /**
         * Combo parameter resolver function. First parameters are looked up in the global environment,
         * then the local environment is looked up as a fallback.
         */
        ComboParameter resolveParameter(String pname, Resolver resolver) {
            //first search the env
            ComboParameter parameter = env.parametersCache.get(pname);
            if (parameter == null) {
                //then lookup local cache
                parameter = localParametersCache.get(pname);
                if (parameter == null && resolver != null) {
                    //if still null and we have a custom resolution function, try that
                    parameter = resolver.lookup(pname);
                    if (parameter != null) {
                       //if a match was found, store it in the local cache to aviod redundant recomputation
                       localParametersCache.put(pname, parameter);
                    }
                }
            }
            return parameter;
        }
    }

    /**
     * Helper class to collect all diagnostic generated during the execution of a given compilation task.
     */
    class DiagnosticCollector implements DiagnosticListener<JavaFileObject> {

        Map<Diagnostic.Kind, List<Diagnostic<? extends JavaFileObject>>> diagsByKind = new HashMap<>();
        Map<String, List<Diagnostic<? extends JavaFileObject>>> diagsByKeys = new HashMap<>();

        public void report(Diagnostic<? extends JavaFileObject> diagnostic) {
            List<Diagnostic<? extends JavaFileObject>> diags =
                    diagsByKeys.getOrDefault(diagnostic.getCode(), List.nil());
            diagsByKeys.put(diagnostic.getCode(), diags.prepend(diagnostic));
            Diagnostic.Kind kind = diagnostic.getKind();
            diags = diagsByKind.getOrDefault(kind, List.nil());
            diagsByKind.put(kind, diags.prepend(diagnostic));
        }
    }
}
