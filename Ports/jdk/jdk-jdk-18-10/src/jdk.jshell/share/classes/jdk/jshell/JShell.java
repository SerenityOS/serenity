/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package jdk.jshell;

import jdk.jshell.spi.ExecutionControl;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.PrintStream;
import java.net.InetAddress;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import javax.tools.StandardJavaFileManager;
import jdk.internal.jshell.debug.InternalDebugControl;
import jdk.jshell.Snippet.Status;
import jdk.jshell.spi.ExecutionControl.EngineTerminationException;
import jdk.jshell.spi.ExecutionControl.ExecutionControlException;
import jdk.jshell.spi.ExecutionControlProvider;
import jdk.jshell.spi.ExecutionEnv;
import static jdk.jshell.Util.expunge;

/**
 * The JShell evaluation state engine.  This is the central class in the JShell
 * API.  A {@code JShell} instance holds the evolving compilation and
 * execution state.  The state is changed with the instance methods
 * {@link jdk.jshell.JShell#eval(java.lang.String) eval(String)},
 * {@link jdk.jshell.JShell#drop(jdk.jshell.Snippet) drop(Snippet)} and
 * {@link jdk.jshell.JShell#addToClasspath(java.lang.String) addToClasspath(String)}.
 * The majority of methods query the state.
 * A {@code JShell} instance also allows registering for events with
 * {@link jdk.jshell.JShell#onSnippetEvent(java.util.function.Consumer) onSnippetEvent(Consumer)}
 * and {@link jdk.jshell.JShell#onShutdown(java.util.function.Consumer) onShutdown(Consumer)}, which
 * are unregistered with
 * {@link jdk.jshell.JShell#unsubscribe(jdk.jshell.JShell.Subscription) unsubscribe(Subscription)}.
 * Access to the source analysis utilities is via
 * {@link jdk.jshell.JShell#sourceCodeAnalysis()}.
 * When complete the instance should be closed to free resources --
 * {@link jdk.jshell.JShell#close()}.
 * <p>
 * An instance of {@code JShell} is created with
 * {@code JShell.create()}.
 * <p>
 * This class is not thread safe, except as noted, all access should be through
 * a single thread.
 *
 * @author Robert Field
 * @since 9
 */
public class JShell implements AutoCloseable {

    final SnippetMaps maps;
    final KeyMap keyMap;
    final OuterWrapMap outerMap;
    final TaskFactory taskFactory;
    final InputStream in;
    final PrintStream out;
    final PrintStream err;
    final Supplier<String> tempVariableNameGenerator;
    final BiFunction<Snippet, Integer, String> idGenerator;
    final List<String> extraRemoteVMOptions;
    final List<String> extraCompilerOptions;
    final Function<StandardJavaFileManager, StandardJavaFileManager> fileManagerMapping;

    private int nextKeyIndex = 1;

    final Eval eval;
    final ClassTracker classTracker;
    private final Map<Subscription, Consumer<JShell>> shutdownListeners = new HashMap<>();
    private final Map<Subscription, Consumer<SnippetEvent>> keyStatusListeners = new HashMap<>();
    private boolean closed = false;

    private final ExecutionControl executionControl;
    private SourceCodeAnalysisImpl sourceCodeAnalysis = null;

    private static final String L10N_RB_NAME    = "jdk.jshell.resources.l10n";
    private static ResourceBundle outputRB  = null;

    JShell(Builder b) throws IllegalStateException {
        this.in = b.in;
        this.out = b.out;
        this.err = b.err;
        this.tempVariableNameGenerator = b.tempVariableNameGenerator;
        this.idGenerator = b.idGenerator;
        this.extraRemoteVMOptions = b.extraRemoteVMOptions;
        this.extraCompilerOptions = b.extraCompilerOptions;
        this.fileManagerMapping = b.fileManagerMapping;
        try {
            if (b.executionControlProvider != null) {
                executionControl = b.executionControlProvider.generate(new ExecutionEnvImpl(),
                        b.executionControlParameters == null
                                ? b.executionControlProvider.defaultParameters()
                                : b.executionControlParameters);
            } else {
                String loopback = InetAddress.getLoopbackAddress().getHostAddress();
                String spec = b.executionControlSpec == null
                        ? "failover:0(jdi:hostname(" + loopback + ")),"
                          + "1(jdi:launch(true)), 2(jdi)"
                        : b.executionControlSpec;
                executionControl = ExecutionControl.generate(new ExecutionEnvImpl(), spec);
            }
        } catch (Throwable ex) {
            throw new IllegalStateException("Launching JShell execution engine threw: " + ex.getMessage(), ex);
        }

        this.maps = new SnippetMaps(this);
        this.keyMap = new KeyMap(this);
        this.outerMap = new OuterWrapMap(this);
        this.taskFactory = new TaskFactory(this);
        this.eval = new Eval(this);
        this.classTracker = new ClassTracker();
    }

    /**
     * Builder for {@code JShell} instances.
     * Create custom instances of {@code JShell} by using the setter
     * methods on this class.  After zero or more of these, use the
     * {@link #build()} method to create a {@code JShell} instance.
     * These can all be chained. For example, setting the remote output and
     * error streams:
     * <pre>
     * {@code
     *     JShell myShell =
     *       JShell.builder()
     *         .out(myOutStream)
     *         .err(myErrStream)
     *         .build(); } </pre>
     * If no special set-up is needed, just use
     * {@code JShell.builder().build()} or the short-cut equivalent
     * {@code JShell.create()}.
     */
    public static class Builder {

        InputStream in = new ByteArrayInputStream(new byte[0]);
        PrintStream out = System.out;
        PrintStream err = System.err;
        Supplier<String> tempVariableNameGenerator = null;
        BiFunction<Snippet, Integer, String> idGenerator = null;
        List<String> extraRemoteVMOptions = new ArrayList<>();
        List<String> extraCompilerOptions = new ArrayList<>();
        ExecutionControlProvider executionControlProvider;
        Map<String,String> executionControlParameters;
        String executionControlSpec;
        Function<StandardJavaFileManager, StandardJavaFileManager> fileManagerMapping;

        Builder() { }

        /**
         * Sets the input for the running evaluation (it's {@code System.in}). Note:
         * applications that use {@code System.in} for snippet or other
         * user input cannot use {@code System.in} as the input stream for
         * the remote process.
         * <p>
         * The {@code read} method of the {@code InputStream} may throw the {@link InterruptedIOException}
         * to signal the user canceled the input. The currently running snippet will be automatically
         * {@link JShell#stop() stopped}.
         * <p>
         * The default, if this is not set, is to provide an empty input stream
         * -- {@code new ByteArrayInputStream(new byte[0])}.
         *
         * @param in the {@code InputStream} to be channelled to
         * {@code System.in} in the remote execution process
         * @return the {@code Builder} instance (for use in chained
         * initialization)
         */
        public Builder in(InputStream in) {
            this.in = in;
            return this;
        }

        /**
         * Sets the output for the running evaluation (it's {@code System.out}).
         * The controlling process and
         * the remote process can share {@code System.out}.
         * <p>
         * The default, if this is not set, is {@code System.out}.
         *
         * @param out the {@code PrintStream} to be channelled to
         * {@code System.out} in the remote execution process
         * @return the {@code Builder} instance (for use in chained
         * initialization)
         */
        public Builder out(PrintStream out) {
            this.out = out;
            return this;
        }

        /**
         * Sets the error output for the running evaluation (it's
         * {@code System.err}). The controlling process and the remote
         * process can share {@code System.err}.
         * <p>
         * The default, if this is not set, is {@code System.err}.
         *
         * @param err the {@code PrintStream} to be channelled to
         * {@code System.err} in the remote execution process
         * @return the {@code Builder} instance (for use in chained
         * initialization)
         */
        public Builder err(PrintStream err) {
            this.err = err;
            return this;
        }

        /**
         * Sets a generator of temp variable names for
         * {@link jdk.jshell.VarSnippet} of
         * {@link jdk.jshell.Snippet.SubKind#TEMP_VAR_EXPRESSION_SUBKIND}.
         * <p>
         * Do not use this method unless you have explicit need for it.
         * <p>
         * The generator will be used for newly created VarSnippet
         * instances. The name of a variable is queried with
         * {@link jdk.jshell.VarSnippet#name()}.
         * <p>
         * The callback is sent during the processing of the snippet, the
         * JShell state is not stable. No calls whatsoever on the
         * {@code JShell} instance may be made from the callback.
         * <p>
         * The generated name must be unique within active snippets.
         * <p>
         * The default behavior (if this is not set or {@code generator}
         * is null) is to generate the name as a sequential number with a
         * prefixing dollar sign ("$").
         *
         * @param generator the {@code Supplier} to generate the temporary
         * variable name string or {@code null}
         * @return the {@code Builder} instance (for use in chained
         * initialization)
         */
        public Builder tempVariableNameGenerator(Supplier<String> generator) {
            this.tempVariableNameGenerator = generator;
            return this;
        }

        /**
         * Sets the generator of identifying names for Snippets.
         * <p>
         * Do not use this method unless you have explicit need for it.
         * <p>
         * The generator will be used for newly created Snippet instances. The
         * identifying name (id) is accessed with
         * {@link jdk.jshell.Snippet#id()} and can be seen in the
         * {@code StackTraceElement.getFileName()} for a
         * {@link jdk.jshell.EvalException} and
         * {@link jdk.jshell.UnresolvedReferenceException}.
         * <p>
         * The inputs to the generator are the {@link jdk.jshell.Snippet} and an
         * integer. The integer will be the same for two Snippets which would
         * overwrite one-another, but otherwise is unique.
         * <p>
         * The callback is sent during the processing of the snippet and the
         * Snippet and the state as a whole are not stable. No calls to change
         * system state (including Snippet state) should be made. Queries of
         * Snippet may be made except to {@link jdk.jshell.Snippet#id()}. No
         * calls on the {@code JShell} instance may be made from the
         * callback, except to
         * {@link #status(jdk.jshell.Snippet) status(Snippet)}.
         * <p>
         * The default behavior (if this is not set or {@code generator}
         * is null) is to generate the id as the integer converted to a string.
         *
         * @param generator the {@code BiFunction} to generate the id
         * string or {@code null}
         * @return the {@code Builder} instance (for use in chained
         * initialization)
         */
        public Builder idGenerator(BiFunction<Snippet, Integer, String> generator) {
            this.idGenerator = generator;
            return this;
        }

        /**
         * Sets additional VM options for launching the VM.
         *
         * @param options The options for the remote VM
         * @return the {@code Builder} instance (for use in chained
         * initialization)
         */
        public Builder remoteVMOptions(String... options) {
            this.extraRemoteVMOptions.addAll(Arrays.asList(options));
            return this;
        }

        /**
         * Adds compiler options.  These additional options will be used on
         * parsing, analysis, and code generation calls to the compiler.
         * Options which interfere with results are not supported and have
         * undefined effects on JShell's operation.
         *
         * @param options the addition options for compiler invocations
         * @return the {@code Builder} instance (for use in chained
         * initialization)
         */
        public Builder compilerOptions(String... options) {
            this.extraCompilerOptions.addAll(Arrays.asList(options));
            return this;
        }

        /**
         * Sets the custom engine for execution. Snippet execution will be
         * provided by the {@link ExecutionControl} instance selected by the
         * specified execution control spec.
         * Use, at most, one of these overloaded {@code executionEngine} builder
         * methods.
         *
         * @param executionControlSpec the execution control spec,
         * which is documented in the {@link jdk.jshell.spi}
         * package documentation.
         * @return the {@code Builder} instance (for use in chained
         * initialization)
         */
        public Builder executionEngine(String executionControlSpec) {
            this.executionControlSpec = executionControlSpec;
            return this;
        }

        /**
         * Sets the custom engine for execution. Snippet execution will be
         * provided by the specified {@link ExecutionControl} instance.
         * Use, at most, one of these overloaded {@code executionEngine} builder
         * methods.
         *
         * @param executionControlProvider the provider to supply the execution
         * engine
         * @param executionControlParameters the parameters to the provider, or
         * {@code null} for default parameters
         * @return the {@code Builder} instance (for use in chained
         * initialization)
         */
        public Builder executionEngine(ExecutionControlProvider executionControlProvider,
                Map<String,String> executionControlParameters) {
            this.executionControlProvider = executionControlProvider;
            this.executionControlParameters = executionControlParameters;
            return this;
        }

        /**
         * Configure the {@code FileManager} to be used by compilation and
         * source analysis.
         * If not set or passed null, the compiler's standard file manager will
         * be used (identity mapping).
         * For use in special applications where the compiler's normal file
         * handling needs to be overridden.  See the file manager APIs for more
         * information.
         * The file manager input enables forwarding file managers, if this
         * is not needed, the incoming file manager can be ignored (constant
         * function).
         *
         * @param mapping a function that given the compiler's standard file
         * manager, returns a file manager to use
         * @return the {@code Builder} instance (for use in chained
         * initialization)
         */
        public Builder fileManager(Function<StandardJavaFileManager, StandardJavaFileManager> mapping) {
            this.fileManagerMapping = mapping;
            return this;
        }

        /**
         * Builds a JShell state engine. This is the entry-point to all JShell
         * functionality. This creates a remote process for execution. It is
         * thus important to close the returned instance.
         *
         * @throws IllegalStateException if the {@code JShell} instance could not be created.
         * @return the state engine
         */
        public JShell build() throws IllegalStateException {
            return new JShell(this);
        }
    }

    // --- public API ---

    /**
     * Create a new JShell state engine.
     * That is, create an instance of {@code JShell}.
     * <p>
     * Equivalent to {@link JShell#builder() JShell.builder()}{@link JShell.Builder#build() .build()}.
     * @throws IllegalStateException if the {@code JShell} instance could not be created.
     * @return an instance of {@code JShell}.
     */
    public static JShell create() throws IllegalStateException {
        return builder().build();
    }

    /**
     * Factory method for {@code JShell.Builder} which, in-turn, is used
     * for creating instances of {@code JShell}.
     * Create a default instance of {@code JShell} with
     * {@code JShell.builder().build()}. For more construction options
     * see {@link jdk.jshell.JShell.Builder}.
     * @return an instance of {@code Builder}.
     * @see jdk.jshell.JShell.Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Access to source code analysis functionality.
     * An instance of {@code JShell} will always return the same
     * {@code SourceCodeAnalysis} instance from
     * {@code sourceCodeAnalysis()}.
     * @return an instance of {@link SourceCodeAnalysis SourceCodeAnalysis}
     * which can be used for source analysis such as completion detection and
     * completion suggestions.
     */
    public SourceCodeAnalysis sourceCodeAnalysis() {
        if (sourceCodeAnalysis == null) {
            sourceCodeAnalysis = new SourceCodeAnalysisImpl(this);
        }
        return sourceCodeAnalysis;
    }

    /**
     * Evaluate the input String, including definition and/or execution, if
     * applicable. The input is checked for errors, unless the errors can be
     * deferred (as is the case with some unresolvedDependencies references),
     * errors will abort evaluation.
     * <p>
     * The input should be
     * exactly one complete snippet of source code, that is, one expression,
     * statement, variable declaration, method declaration, class declaration,
     * or import.
     * To break arbitrary input into individual complete snippets, use
     * {@link SourceCodeAnalysis#analyzeCompletion(String)}.
     * <p>
     * For imports, the import is added.  Classes, interfaces. methods,
     * and variables are defined.  The initializer of variables, statements,
     * and expressions are executed.
     * The modifiers public, protected, private, static, and final are not
     * allowed on op-level declarations and are ignored with a warning.
     * Synchronized, native, abstract, and default top-level methods are not
     * allowed and are errors.
     * If a previous definition of a declaration is overwritten then there will
     * be an event showing its status changed to OVERWRITTEN, this will not
     * occur for dropped, rejected, or already overwritten declarations.
     * <p>
     * If execution environment is out of process, as is the default case, then
     * if the evaluated code
     * causes the execution environment to terminate, this {@code JShell}
     * instance will be closed but the calling process and VM remain valid.
     * @param input The input String to evaluate
     * @return the list of events directly or indirectly caused by this evaluation.
     * @throws IllegalStateException if this {@code JShell} instance is closed.
     * @see SourceCodeAnalysis#analyzeCompletion(String)
     * @see JShell#onShutdown(java.util.function.Consumer)
     */
    public List<SnippetEvent> eval(String input) throws IllegalStateException {
        SourceCodeAnalysisImpl a = sourceCodeAnalysis;
        if (a != null) {
            a.suspendIndexing();
        }
        try {
            checkIfAlive();
            List<SnippetEvent> events = eval.eval(input);
            events.forEach(this::notifyKeyStatusEvent);
            return Collections.unmodifiableList(events);
        } finally {
            if (a != null) {
                a.resumeIndexing();
            }
        }
    }

    /**
     * Remove a declaration from the state.  That is, if the snippet is an
     * {@linkplain jdk.jshell.Snippet.Status#isActive() active}
     * {@linkplain jdk.jshell.PersistentSnippet persistent} snippet, remove the
     * snippet and update the JShell evaluation state accordingly.
     * For all active snippets, change the {@linkplain #status status} to
     * {@link jdk.jshell.Snippet.Status#DROPPED DROPPED}.
     * @param snippet The snippet to remove
     * @return The list of events from updating declarations dependent on the
     * dropped snippet.
     * @throws IllegalStateException if this {@code JShell} instance is closed.
     * @throws IllegalArgumentException if the snippet is not associated with
     * this {@code JShell} instance.
     */
    public List<SnippetEvent> drop(Snippet snippet) throws IllegalStateException {
        checkIfAlive();
        checkValidSnippet(snippet);
        List<SnippetEvent> events = eval.drop(snippet);
        events.forEach(this::notifyKeyStatusEvent);
        return Collections.unmodifiableList(events);
    }

    /**
     * The specified path is added to the end of the classpath used in eval().
     * Note that the unnamed package is not accessible from the package in which
     * {@link JShell#eval(String)} code is placed.
     * @param path the path to add to the classpath.
     * @throws IllegalStateException if this {@code JShell} instance is closed.
     */
    public void addToClasspath(String path) {
        checkIfAlive();
        // Compiler
        taskFactory.addToClasspath(path);
        // Runtime
        try {
            executionControl().addToClasspath(path);
        } catch (ExecutionControlException ex) {
            debug(ex, "on addToClasspath(" + path + ")");
        }
        if (sourceCodeAnalysis != null) {
            sourceCodeAnalysis.classpathChanged();
        }
    }

    /**
     * Attempt to stop currently running evaluation. When called while
     * the {@link #eval(java.lang.String) } method is running and the
     * user's code being executed, an attempt will be made to stop user's code.
     * Note that typically this method needs to be called from a different thread
     * than the one running the {@code eval} method.
     * <p>
     * If the {@link #eval(java.lang.String) } method is not running, does nothing.
     * <p>
     * The attempt to stop the user's code may fail in some case, which may include
     * when the execution is blocked on an I/O operation, or when the user's code is
     * catching the {@link ThreadDeath} exception.
     */
    public void stop() {
        if (executionControl != null) {
            try {
                executionControl.stop();
            } catch (ExecutionControlException ex) {
                debug(ex, "on stop()");
            }
        }
    }

    /**
     * Close this state engine. Frees resources. Should be called when this
     * state engine is no longer needed.
     */
    @Override
    public void close() {
        closeDown();
    }

    /**
     * Return all snippets.
     * @return the snippets for all current snippets in id order.
     */
    public Stream<Snippet> snippets() {
        return maps.snippetList().stream();
    }

    /**
     * Returns the active variable snippets.
     * This convenience method is equivalent to {@code snippets()} filtered for
     * {@link jdk.jshell.Snippet.Status#isActive() status(snippet).isActive()}
     * {@code && snippet.kind() == Kind.VARIABLE}
     * and cast to {@code VarSnippet}.
     * @return the active declared variables.
     */
    public Stream<VarSnippet> variables() {
        return snippets()
                     .filter(sn -> status(sn).isActive() && sn.kind() == Snippet.Kind.VAR)
                     .map(sn -> (VarSnippet) sn);
    }

    /**
     * Returns the active method snippets.
     * This convenience method is equivalent to {@code snippets()} filtered for
     * {@link jdk.jshell.Snippet.Status#isActive() status(snippet).isActive()}
     * {@code && snippet.kind() == Kind.METHOD}
     * and cast to MethodSnippet.
     * @return the active declared methods.
     */
    public Stream<MethodSnippet> methods() {
        return snippets()
                     .filter(sn -> status(sn).isActive() && sn.kind() == Snippet.Kind.METHOD)
                     .map(sn -> (MethodSnippet)sn);
    }

    /**
     * Returns the active type declaration (class, interface, annotation type, and enum) snippets.
     * This convenience method is equivalent to {@code snippets()} filtered for
     * {@link jdk.jshell.Snippet.Status#isActive() status(snippet).isActive()}
     * {@code && snippet.kind() == Kind.TYPE_DECL}
     * and cast to TypeDeclSnippet.
     * @return the active declared type declarations.
     */
    public Stream<TypeDeclSnippet> types() {
        return snippets()
                .filter(sn -> status(sn).isActive() && sn.kind() == Snippet.Kind.TYPE_DECL)
                .map(sn -> (TypeDeclSnippet) sn);
    }

    /**
     * Returns the active import snippets.
     * This convenience method is equivalent to {@code snippets()} filtered for
     * {@link jdk.jshell.Snippet.Status#isActive() status(snippet).isActive()}
     * {@code && snippet.kind() == Kind.IMPORT}
     * and cast to ImportSnippet.
     * @return the active declared import declarations.
     */
    public Stream<ImportSnippet> imports() {
        return snippets()
                .filter(sn -> status(sn).isActive() && sn.kind() == Snippet.Kind.IMPORT)
                .map(sn -> (ImportSnippet) sn);
    }

    /**
     * Return the status of the snippet.
     * This is updated either because of an explicit {@code eval()} call or
     * an automatic update triggered by a dependency.
     * @param snippet the {@code Snippet} to look up
     * @return the status corresponding to this snippet
     * @throws IllegalStateException if this {@code JShell} instance is closed.
     * @throws IllegalArgumentException if the snippet is not associated with
     * this {@code JShell} instance.
     */
    public Status status(Snippet snippet) {
        return checkValidSnippet(snippet).status();
    }

    /**
     * Return the diagnostics of the most recent evaluation of the snippet.
     * The evaluation can either because of an explicit {@code eval()} call or
     * an automatic update triggered by a dependency.
     * @param snippet the {@code Snippet} to look up
     * @return the diagnostics corresponding to this snippet.  This does not
     * include unresolvedDependencies references reported in {@code unresolvedDependencies()}.
     * @throws IllegalStateException if this {@code JShell} instance is closed.
     * @throws IllegalArgumentException if the snippet is not associated with
     * this {@code JShell} instance.
     */
    public Stream<Diag> diagnostics(Snippet snippet) {
        return checkValidSnippet(snippet).diagnostics().stream();
    }

    /**
     * For {@link jdk.jshell.Snippet.Status#RECOVERABLE_DEFINED RECOVERABLE_DEFINED} or
     * {@link jdk.jshell.Snippet.Status#RECOVERABLE_NOT_DEFINED RECOVERABLE_NOT_DEFINED}
     * declarations, the names of current unresolved dependencies for
     * the snippet.
     * The returned value of this method, for a given method may change when an
     * {@code eval()} or {@code drop()} of another snippet causes
     * an update of a dependency.
     * @param snippet the declaration {@code Snippet} to look up
     * @return a stream of symbol names that are currently unresolvedDependencies.
     * @throws IllegalStateException if this {@code JShell} instance is closed.
     * @throws IllegalArgumentException if the snippet is not associated with
     * this {@code JShell} instance.
     */
    public Stream<String> unresolvedDependencies(DeclarationSnippet snippet) {
        return checkValidSnippet(snippet).unresolved().stream();
    }

    /**
     * Get the current value of a variable.
     * @param snippet the variable Snippet whose value is queried.
     * @return the current value of the variable referenced by snippet.
     * @throws IllegalStateException if this {@code JShell} instance is closed.
     * @throws IllegalArgumentException if the snippet is not associated with
     * this {@code JShell} instance.
     * @throws IllegalArgumentException if the variable's status is anything but
     * {@link jdk.jshell.Snippet.Status#VALID}.
     */
    public String varValue(VarSnippet snippet) throws IllegalStateException {
        checkIfAlive();
        checkValidSnippet(snippet);
        if (snippet.status() != Status.VALID) {
            throw new IllegalArgumentException(
                    messageFormat("jshell.exc.var.not.valid",  snippet, snippet.status()));
        }
        String value;
        try {
            value = executionControl().varValue(snippet.classFullName(), snippet.name());
        } catch (EngineTerminationException ex) {
            throw new IllegalStateException(ex.getMessage());
        } catch (ExecutionControlException ex) {
            debug(ex, "In varValue()");
            return "[" + ex.getMessage() + "]";
        }
        return expunge(value);
    }

    /**
     * Register a callback to be called when the Status of a snippet changes.
     * Each call adds a new subscription.
     * @param listener Action to perform when the Status changes.
     * @return A token which can be used to {@linkplain JShell#unsubscribe unsubscribe} this subscription.
     * @throws IllegalStateException if this {@code JShell} instance is closed.
     */
    public Subscription onSnippetEvent(Consumer<SnippetEvent> listener)
            throws IllegalStateException {
        return onX(keyStatusListeners, listener);
    }

    /**
     * Register a callback to be called when this JShell instance terminates.
     * This occurs either because the client process has ended (e.g. called System.exit(0))
     * or the connection has been shutdown, as by close().
     * Each call adds a new subscription.
     * @param listener Action to perform when the state terminates.
     * @return A token which can be used to {@linkplain JShell#unsubscribe unsubscribe} this subscription.
     * @throws IllegalStateException if this JShell instance is closed
     */
    public Subscription onShutdown(Consumer<JShell> listener)
            throws IllegalStateException {
        return onX(shutdownListeners, listener);
    }

    /**
     * Cancel a callback subscription.
     * @param token The token corresponding to the subscription to be unsubscribed.
     */
    public void unsubscribe(Subscription token) {
        synchronized (this) {
            token.remover.accept(token);
        }
    }

    /**
     * Subscription is a token for referring to subscriptions so they can
     * be {@linkplain JShell#unsubscribe unsubscribed}.
     */
    public class Subscription {

        Consumer<Subscription> remover;

        Subscription(Consumer<Subscription> remover) {
            this.remover = remover;
        }
    }

    /**
     * Provide the environment for a execution engine.
     */
    class ExecutionEnvImpl implements ExecutionEnv {

        @Override
        public InputStream userIn() {
            return in;
        }

        @Override
        public PrintStream userOut() {
            return out;
        }

        @Override
        public PrintStream userErr() {
            return err;
        }

        @Override
        public List<String> extraRemoteVMOptions() {
            return extraRemoteVMOptions;
        }

        @Override
        public void closeDown() {
            JShell.this.closeDown();
        }

    }

    // --- private / package-private implementation support ---

    ExecutionControl executionControl() {
        return executionControl;
    }

    void debug(int flags, String format, Object... args) {
        InternalDebugControl.debug(this, err, flags, format, args);
    }

    void debug(Throwable ex, String where) {
        InternalDebugControl.debug(this, err, ex, where);
    }

    /**
     * Generate the next key index, indicating a unique snippet signature.
     *
     * @return the next key index
     */
    int nextKeyIndex() {
        return nextKeyIndex++;
    }

    private synchronized <T> Subscription onX(Map<Subscription, Consumer<T>> map, Consumer<T> listener)
            throws IllegalStateException {
        Objects.requireNonNull(listener);
        checkIfAlive();
        Subscription token = new Subscription(map::remove);
        map.put(token, listener);
        return token;
    }

    private synchronized void notifyKeyStatusEvent(SnippetEvent event) {
        keyStatusListeners.values().forEach(l -> l.accept(event));
    }

    private synchronized void notifyShutdownEvent(JShell state) {
        shutdownListeners.values().forEach(l -> l.accept(state));
    }

    void closeDown() {
        if (!closed) {
            // Send only once
            closed = true;
            try {
                notifyShutdownEvent(this);
            } catch (Throwable thr) {
                // Don't care about dying exceptions
            }
            try {
                executionControl().close();
            } catch (Throwable ex) {
                // don't care about exceptions on close
            }
            if (sourceCodeAnalysis != null) {
                sourceCodeAnalysis.close();
            }
            InternalDebugControl.release(this);
        }
    }

    /**
     * Check if this JShell has been closed
     * @throws IllegalStateException if it is closed
     */
    void checkIfAlive()  throws IllegalStateException {
        if (closed) {
            throw new IllegalStateException(messageFormat("jshell.exc.closed", this));
        }
    }

    /**
     * Check a Snippet parameter coming from the API user
     * @param sn the Snippet to check
     * @throws NullPointerException if Snippet parameter is null
     * @throws IllegalArgumentException if Snippet is not from this JShell
     * @return the input Snippet (for chained calls)
     */
    private Snippet checkValidSnippet(Snippet sn) {
        if (sn == null) {
            throw new NullPointerException(messageFormat("jshell.exc.null"));
        } else {
            if (sn.key().state() != this || sn.id() == Snippet.UNASSOCIATED_ID) {
                throw new IllegalArgumentException(messageFormat("jshell.exc.alien", sn.toString()));
            }
            return sn;
        }
    }

    /**
     * Format using resource bundle look-up using MessageFormat
     *
     * @param key the resource key
     * @param args
     */
    String messageFormat(String key, Object... args) {
        if (outputRB == null) {
            try {
                outputRB = ResourceBundle.getBundle(L10N_RB_NAME);
            } catch (MissingResourceException mre) {
                throw new InternalError("Cannot find ResourceBundle: " + L10N_RB_NAME);
            }
        }
        String s;
        try {
            s = outputRB.getString(key);
        } catch (MissingResourceException mre) {
            throw new InternalError("Missing resource: " + key + " in " + L10N_RB_NAME);
        }
        return MessageFormat.format(s, args);
    }

}
