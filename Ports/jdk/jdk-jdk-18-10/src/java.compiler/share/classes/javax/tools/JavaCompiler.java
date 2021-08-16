/*
 * Copyright (c) 2005, 2019, Oracle and/or its affiliates. All rights reserved.
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

package javax.tools;

import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Locale;
import java.util.concurrent.Callable;
import javax.annotation.processing.Processor;

/**
 * Interface to invoke Java programming language compilers from
 * programs.
 *
 * <p>The compiler might generate diagnostics during compilation (for
 * example, error messages).  If a diagnostic listener is provided,
 * the diagnostics will be supplied to the listener.  If no listener
 * is provided, the diagnostics will be formatted in an unspecified
 * format and written to the default output, which is {@code
 * System.err} unless otherwise specified.  Even if a diagnostic
 * listener is supplied, some diagnostics might not fit in a {@code
 * Diagnostic} and will be written to the default output.
 *
 * <p>A compiler tool has an associated standard file manager, which
 * is the file manager that is native to the tool (or built-in).  The
 * standard file manager can be obtained by calling {@linkplain
 * #getStandardFileManager getStandardFileManager}.
 *
 * <p>A compiler tool must function with any file manager as long as
 * any additional requirements as detailed in the methods below are
 * met.  If no file manager is provided, the compiler tool will use a
 * standard file manager such as the one returned by {@linkplain
 * #getStandardFileManager getStandardFileManager}.
 *
 * <p>An instance implementing this interface must conform to
 * <cite>The Java Language Specification</cite>
 * and generate class files conforming to
 * <cite>The Java Virtual Machine Specification</cite>.
 * The versions of these
 * specifications are defined in the {@linkplain Tool} interface.
 *
 * Additionally, an instance of this interface supporting {@link
 * javax.lang.model.SourceVersion#RELEASE_6 SourceVersion.RELEASE_6}
 * or higher must also support {@linkplain javax.annotation.processing
 * annotation processing}.
 *
 * <p>The compiler relies on two services: {@linkplain
 * DiagnosticListener diagnostic listener} and {@linkplain
 * JavaFileManager file manager}.  Although most classes and
 * interfaces in this package defines an API for compilers (and
 * tools in general) the interfaces {@linkplain DiagnosticListener},
 * {@linkplain JavaFileManager}, {@linkplain FileObject}, and
 * {@linkplain JavaFileObject} are not intended to be used in
 * applications.  Instead these interfaces are intended to be
 * implemented and used to provide customized services for a
 * compiler and thus defines an SPI for compilers.
 *
 * <p>There are a number of classes and interfaces in this package
 * which are designed to ease the implementation of the SPI to
 * customize the behavior of a compiler:
 *
 * <dl>
 *   <dt>{@link StandardJavaFileManager}</dt>
 *   <dd>
 *
 *     Every compiler which implements this interface provides a
 *     standard file manager for operating on regular {@linkplain
 *     java.io.File files}.  The StandardJavaFileManager interface
 *     defines additional methods for creating file objects from
 *     regular files.
 *
 *     <p>The standard file manager serves two purposes:
 *
 *     <ul>
 *       <li>basic building block for customizing how a compiler reads
 *       and writes files</li>
 *       <li>sharing between multiple compilation tasks</li>
 *     </ul>
 *
 *     <p>Reusing a file manager can potentially reduce overhead of
 *     scanning the file system and reading jar files.  Although there
 *     might be no reduction in overhead, a standard file manager must
 *     work with multiple sequential compilations making the following
 *     example a recommended coding pattern:
 *
 *     <pre>
 *       File[] files1 = ... ; // input for first compilation task
 *       File[] files2 = ... ; // input for second compilation task
 *
 *       JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
 *       StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);
 *
 *       {@code Iterable<? extends JavaFileObject>} compilationUnits1 =
 *           fileManager.getJavaFileObjectsFromFiles({@linkplain java.util.Arrays#asList Arrays.asList}(files1));
 *       compiler.getTask(null, fileManager, null, null, null, compilationUnits1).call();
 *
 *       {@code Iterable<? extends JavaFileObject>} compilationUnits2 =
 *           fileManager.getJavaFileObjects(files2); // use alternative method
 *       // reuse the same file manager to allow caching of jar files
 *       compiler.getTask(null, fileManager, null, null, null, compilationUnits2).call();
 *
 *       fileManager.close();</pre>
 *
 *   </dd>
 *
 *   <dt>{@link DiagnosticCollector}</dt>
 *   <dd>
 *     Used to collect diagnostics in a list, for example:
 *     <pre>
 *       {@code Iterable<? extends JavaFileObject>} compilationUnits = ...;
 *       JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
 *       {@code DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();}
 *       StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);
 *       compiler.getTask(null, fileManager, diagnostics, null, null, compilationUnits).call();
 *
 *       for ({@code Diagnostic<? extends JavaFileObject>} diagnostic : diagnostics.getDiagnostics())
 *           System.out.format("Error on line %d in %s%n",
 *                             diagnostic.getLineNumber(),
 *                             diagnostic.getSource().toUri());
 *
 *       fileManager.close();</pre>
 *   </dd>
 *
 *   <dt>
 *     {@link ForwardingJavaFileManager}, {@link ForwardingFileObject}, and
 *     {@link ForwardingJavaFileObject}
 *   </dt>
 *   <dd>
 *
 *     Subclassing is not available for overriding the behavior of a
 *     standard file manager as it is created by calling a method on a
 *     compiler, not by invoking a constructor.  Instead forwarding
 *     (or delegation) should be used.  These classes makes it easy to
 *     forward most calls to a given file manager or file object while
 *     allowing customizing behavior.  For example, consider how to
 *     log all calls to {@linkplain JavaFileManager#flush}:
 *
 *     <pre>
 *       final  Logger logger = ...;
 *       {@code Iterable<? extends JavaFileObject>} compilationUnits = ...;
 *       JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
 *       StandardJavaFileManager stdFileManager = compiler.getStandardFileManager(null, null, null);
 *       JavaFileManager fileManager = new ForwardingJavaFileManager(stdFileManager) {
 *           public void flush() throws IOException {
 *               logger.entering(StandardJavaFileManager.class.getName(), "flush");
 *               super.flush();
 *               logger.exiting(StandardJavaFileManager.class.getName(), "flush");
 *           }
 *       };
 *       compiler.getTask(null, fileManager, null, null, null, compilationUnits).call();</pre>
 *   </dd>
 *
 *   <dt>{@link SimpleJavaFileObject}</dt>
 *   <dd>
 *
 *     This class provides a basic file object implementation which
 *     can be used as building block for creating file objects.  For
 *     example, here is how to define a file object which represent
 *     source code stored in a string:
 *
 *     <pre>
 *       /**
 *        * A file object used to represent source coming from a string.
 *        {@code *}/
 *       public class JavaSourceFromString extends SimpleJavaFileObject {
 *           /**
 *            * The source code of this "file".
 *            {@code *}/
 *           final String code;
 *
 *           /**
 *            * Constructs a new JavaSourceFromString.
 *            * {@code @}param name the name of the compilation unit represented by this file object
 *            * {@code @}param code the source code for the compilation unit represented by this file object
 *            {@code *}/
 *           JavaSourceFromString(String name, String code) {
 *               super({@linkplain java.net.URI#create URI.create}("string:///" + name.replace('.','/') + Kind.SOURCE.extension),
 *                     Kind.SOURCE);
 *               this.code = code;
 *           }
 *
 *           {@code @}Override
 *           public CharSequence getCharContent(boolean ignoreEncodingErrors) {
 *               return code;
 *           }
 *       }</pre>
 *   </dd>
 * </dl>
 *
 * @author Peter von der Ah&eacute;
 * @author Jonathan Gibbons
 * @see DiagnosticListener
 * @see Diagnostic
 * @see JavaFileManager
 * @since 1.6
 */
public interface JavaCompiler extends Tool, OptionChecker {

    /**
     * Creates a future for a compilation task with the given
     * components and arguments.  The compilation might not have
     * completed as described in the CompilationTask interface.
     *
     * <p>If a file manager is provided, it must be able to handle all
     * locations defined in {@link StandardLocation}.
     *
     * <p>Note that annotation processing can process both the
     * compilation units of source code to be compiled, passed with
     * the {@code compilationUnits} parameter, as well as class
     * files, whose names are passed with the {@code classes}
     * parameter.
     *
     * @param out a Writer for additional output from the compiler;
     * use {@code System.err} if {@code null}
     * @param fileManager a file manager; if {@code null} use the
     * compiler's standard file manager
     * @param diagnosticListener a diagnostic listener; if {@code
     * null} use the compiler's default method for reporting
     * diagnostics
     * @param options compiler options, {@code null} means no options
     * @param classes names of classes to be processed by annotation
     * processing, {@code null} means no class names
     * @param compilationUnits the compilation units to compile, {@code
     * null} means no compilation units
     * @return an object representing the compilation
     * @throws RuntimeException if an unrecoverable error
     * occurred in a user supplied component.  The
     * {@linkplain Throwable#getCause() cause} will be the error in
     * user code.
     * @throws IllegalArgumentException if any of the options are invalid,
     * or if any of the given compilation units are of other kind than
     * {@linkplain JavaFileObject.Kind#SOURCE source}
     */
    CompilationTask getTask(Writer out,
                            JavaFileManager fileManager,
                            DiagnosticListener<? super JavaFileObject> diagnosticListener,
                            Iterable<String> options,
                            Iterable<String> classes,
                            Iterable<? extends JavaFileObject> compilationUnits);

    /**
     * Returns a new instance of the standard file manager implementation
     * for this tool.  The file manager will use the given diagnostic
     * listener for producing any non-fatal diagnostics.  Fatal errors
     * will be signaled with the appropriate exceptions.
     *
     * <p>The standard file manager will be automatically reopened if
     * it is accessed after calls to {@code flush} or {@code close}.
     * The standard file manager must be usable with other tools.
     *
     * @param diagnosticListener a diagnostic listener for non-fatal
     * diagnostics; if {@code null} use the compiler's default method
     * for reporting diagnostics
     * @param locale the locale to apply when formatting diagnostics;
     * {@code null} means the {@linkplain Locale#getDefault() default locale}.
     * @param charset the character set used for decoding bytes; if
     * {@code null} use the platform default
     * @return the standard file manager
     */
    StandardJavaFileManager getStandardFileManager(
        DiagnosticListener<? super JavaFileObject> diagnosticListener,
        Locale locale,
        Charset charset);

    /**
     * Interface representing a future for a compilation task.  The
     * compilation task has not yet started.  To start the task, call
     * the {@linkplain #call call} method.
     *
     * <p>Before calling the {@code call} method, additional aspects of the
     * task can be configured, for example, by calling the
     * {@linkplain #setProcessors setProcessors} method.
     */
    interface CompilationTask extends Callable<Boolean> {
        /**
         * Adds root modules to be taken into account during module
         * resolution.
         * Invalid module names may cause either
         * {@code IllegalArgumentException} to be thrown,
         * or diagnostics to be reported when the task is started.
         * @param moduleNames the names of the root modules
         * @throws IllegalArgumentException may be thrown for some
         *      invalid module names
         * @throws IllegalStateException if the task has started
         * @since 9
         */
        void addModules(Iterable<String> moduleNames);

        /**
         * Sets processors (for annotation processing).  This will
         * bypass the normal discovery mechanism.
         *
         * @param processors processors (for annotation processing)
         * @throws IllegalStateException if the task has started
         */
        void setProcessors(Iterable<? extends Processor> processors);

        /**
         * Sets the locale to be applied when formatting diagnostics and
         * other localized data.
         *
         * @param locale the locale to apply; {@code null} means apply no
         * locale
         * @throws IllegalStateException if the task has started
         */
        void setLocale(Locale locale);

        /**
         * Performs this compilation task.  The compilation may only
         * be performed once.  Subsequent calls to this method throw
         * {@code IllegalStateException}.
         *
         * @return true if and only all the files compiled without errors;
         * false otherwise
         *
         * @throws RuntimeException if an unrecoverable error occurred
         * in a user-supplied component.  The
         * {@linkplain Throwable#getCause() cause} will be the error
         * in user code.
         * @throws IllegalStateException if called more than once
         */
        @Override
        Boolean call();
    }
}
