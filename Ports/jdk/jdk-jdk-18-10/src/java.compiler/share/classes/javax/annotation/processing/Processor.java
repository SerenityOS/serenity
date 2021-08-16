/*
 * Copyright (c) 2005, 2020, Oracle and/or its affiliates. All rights reserved.
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

package javax.annotation.processing;

import java.util.Set;
import javax.lang.model.util.Elements;
import javax.lang.model.AnnotatedConstruct;
import javax.lang.model.element.*;
import javax.lang.model.SourceVersion;

/**
 * The interface for an annotation processor.
 *
 * <p>Annotation processing happens in a sequence of {@linkplain
 * javax.annotation.processing.RoundEnvironment rounds}.  On each
 * round, a processor may be asked to {@linkplain #process process} a
 * subset of the annotations found on the source and class files
 * produced by a prior round.  The inputs to the first round of
 * processing are the initial inputs to a run of the tool; these
 * initial inputs can be regarded as the output of a virtual zeroth
 * round of processing.  If a processor was asked to process on a
 * given round, it will be asked to process on subsequent rounds,
 * including the last round, even if there are no annotations for it
 * to process.  The tool infrastructure may also ask a processor to
 * process files generated implicitly by the tool's operation.
 *
 * <p> Each implementation of a {@code Processor} must provide a
 * public no-argument constructor to be used by tools to instantiate
 * the processor.  The tool infrastructure will interact with classes
 * implementing this interface as follows:
 *
 * <ol>
 *
 * <li>If an existing {@code Processor} object is not being used, to
 * create an instance of a processor the tool calls the no-arg
 * constructor of the processor class.
 *
 * <li>Next, the tool calls the {@link #init init} method with
 * an appropriate {@link ProcessingEnvironment}.
 *
 * <li>Afterwards, the tool calls {@link #getSupportedAnnotationTypes
 * getSupportedAnnotationTypes}, {@link #getSupportedOptions
 * getSupportedOptions}, and {@link #getSupportedSourceVersion
 * getSupportedSourceVersion}.  These methods are only called once per
 * run, not on each round.
 *
 * <li>As appropriate, the tool calls the {@link #process process}
 * method on the {@code Processor} object; a new {@code Processor}
 * object is <em>not</em> created for each round.
 *
 * </ol>
 *
 * If a processor object is created and used without the above
 * protocol being followed, then the processor's behavior is not
 * defined by this interface specification.
 *
 * <p> The tool uses a <i>discovery process</i> to find annotation
 * processors and decide whether or not they should be run.  By
 * configuring the tool, the set of potential processors can be
 * controlled.  For example, for a {@link javax.tools.JavaCompiler
 * JavaCompiler} the list of candidate processors to run can be
 * {@linkplain javax.tools.JavaCompiler.CompilationTask#setProcessors
 * set directly} or controlled by a {@linkplain
 * javax.tools.StandardLocation#ANNOTATION_PROCESSOR_PATH search path}
 * used for a {@linkplain java.util.ServiceLoader service-style}
 * lookup.  Other tool implementations may have different
 * configuration mechanisms, such as command line options; for
 * details, refer to the particular tool's documentation.  Which
 * processors the tool asks to {@linkplain #process run} is a function
 * of the interfaces of the annotations <em>{@linkplain
 * AnnotatedConstruct present}</em> on the {@linkplain
 * RoundEnvironment#getRootElements root elements}, what {@linkplain
 * #getSupportedAnnotationTypes annotation interfaces a processor
 * supports}, and whether or not a processor {@linkplain #process
 * claims the annotation interfaces it processes}.  A processor will
 * be asked to process a subset of the annotation interfaces it
 * supports, possibly an empty set.
 *
 * For a given round, the tool computes the set of annotation
 * interfaces that are present on the elements enclosed within the
 * root elements.  If there is at least one annotation interface
 * present, then as processors claim annotation interfaces, they are
 * removed from the set of unmatched annotation interfaces.  When the
 * set is empty or no more processors are available, the round has run
 * to completion.  If there are no annotation interfaces present,
 * annotation processing still occurs but only <i>universal
 * processors</i> which support processing all annotation interfaces,
 * {@code "*"}, can claim the (empty) set of annotation interfaces.
 *
 * <p>An annotation interface is considered present if there is at least
 * one annotation of that interface present on an element enclosed within
 * the root elements of a round. For this purpose, a type parameter is
 * considered to be enclosed by its {@linkplain
 * TypeParameterElement#getGenericElement generic
 * element}.

 * For this purpose, a package element is <em>not</em> considered to
 * enclose the top-level classes and interfaces within that
 * package. (A root element representing a package is created when a
 * {@code package-info} file is processed.) Likewise, for this
 * purpose, a module element is <em>not</em> considered to enclose the
 * packages within that module. (A root element representing a module
 * is created when a {@code module-info} file is processed.)
 *
 * Annotations on {@linkplain
 * java.lang.annotation.ElementType#TYPE_USE type uses}, as opposed to
 * annotations on elements, are ignored when computing whether or not
 * an annotation interface is present.
 *
 * <p>An annotation is <em>present</em> if it meets the definition of being
 * present given in {@link AnnotatedConstruct}. In brief, an
 * annotation is considered present for the purposes of discovery if
 * it is directly present or present via inheritance. An annotation is
 * <em>not</em> considered present by virtue of being wrapped by a
 * container annotation. Operationally, this is equivalent to an
 * annotation being present on an element if and only if it would be
 * included in the results of {@link
 * Elements#getAllAnnotationMirrors(Element)} called on that element. Since
 * annotations inside container annotations are not considered
 * present, to properly process {@linkplain
 * java.lang.annotation.Repeatable repeatable annotation interfaces},
 * processors are advised to include both the repeatable annotation
 * interface and its containing annotation interface in the set of {@linkplain
 * #getSupportedAnnotationTypes() supported annotation interfaces} of a
 * processor.
 *
 * <p>Note that if a processor supports {@code "*"} and returns {@code
 * true}, all annotations are claimed.  Therefore, a universal
 * processor being used to, for example, implement additional validity
 * checks should return {@code false} so as to not prevent other such
 * checkers from being able to run.
 *
 * <p>If a processor throws an uncaught exception, the tool may cease
 * other active annotation processors.  If a processor raises an
 * error, the current round will run to completion and the subsequent
 * round will indicate an {@linkplain RoundEnvironment#errorRaised
 * error was raised}.  Since annotation processors are run in a
 * cooperative environment, a processor should throw an uncaught
 * exception only in situations where no error recovery or reporting
 * is feasible.
 *
 * <p>The tool environment is not required to support annotation
 * processors that access environmental resources, either {@linkplain
 * RoundEnvironment per round} or {@linkplain ProcessingEnvironment
 * cross-round}, in a multi-threaded fashion.
 *
 * <p>If the methods that return configuration information about the
 * annotation processor return {@code null}, return other invalid
 * input, or throw an exception, the tool infrastructure must treat
 * this as an error condition.
 *
 * <p>To be robust when running in different tool implementations, an
 * annotation processor should have the following properties:
 *
 * <ol>
 *
 * <li>The result of processing a given input is not a function of the presence or absence
 * of other inputs (orthogonality).
 *
 * <li>Processing the same input produces the same output (consistency).
 *
 * <li>Processing input <i>A</i> followed by processing input <i>B</i>
 * is equivalent to processing <i>B</i> then <i>A</i>
 * (commutativity)
 *
 * <li>Processing an input does not rely on the presence of the output
 * of other annotation processors (independence)
 *
 * </ol>
 *
 * <p>The {@link Filer} interface discusses restrictions on how
 * processors can operate on files.
 *
 * @apiNote Implementors of this interface may find it convenient
 * to extend {@link AbstractProcessor} rather than implementing this
 * interface directly.
 *
 * @author Joseph D. Darcy
 * @author Scott Seligman
 * @author Peter von der Ah&eacute;
 * @since 1.6
 */
public interface Processor {
    /**
     * Returns the options recognized by this processor.  An
     * implementation of the processing tool must provide a way to
     * pass processor-specific options distinctly from options passed
     * to the tool itself, see {@link ProcessingEnvironment#getOptions
     * getOptions}.
     *
     * <p>Each string returned in the set must be a period separated
     * sequence of {@linkplain
     * javax.lang.model.SourceVersion#isIdentifier identifiers}:
     *
     * <blockquote>
     * <dl>
     * <dt><i>SupportedOptionString:</i>
     * <dd><i>Identifiers</i>
     *
     * <dt><i>Identifiers:</i>
     * <dd> <i>Identifier</i>
     * <dd> <i>Identifier</i> {@code .} <i>Identifiers</i>
     *
     * <dt><i>Identifier:</i>
     * <dd>Syntactic identifier, including keywords and literals
     * </dl>
     * </blockquote>
     *
     * <p> A tool might use this information to determine if any
     * options provided by a user are unrecognized by any processor,
     * in which case it may wish to report a warning.
     *
     * @return the options recognized by this processor or an
     *         empty set if none
     * @see javax.annotation.processing.SupportedOptions
     */
    Set<String> getSupportedOptions();

    /**
     * Returns the names of the annotation interfaces supported by this
     * processor.  An element of the result may be the canonical
     * (fully qualified) name of a supported annotation interface.
     * Alternately it may be of the form &quot;<code><i>name</i>.*</code>&quot;
     * representing the set of all annotation interfaces with canonical
     * names beginning with &quot;<code><i>name.</i></code>&quot;.
     *
     * In either of those cases, the name of the annotation interface can
     * be optionally preceded by a module name followed by a {@code
     * "/"} character. For example, if a processor supports {@code
     * "a.B"}, this can include multiple annotation interfaces named {@code
     * a.B} which reside in different modules. To only support {@code
     * a.B} in the {@code foo} module, instead use {@code "foo/a.B"}.
     *
     * If a module name is included, only an annotation in that module
     * is matched. In particular, if a module name is given in an
     * environment where modules are not supported, such as an
     * annotation processing environment configured for a {@linkplain
     * javax.annotation.processing.ProcessingEnvironment#getSourceVersion
     * source version} without modules, then the annotation interfaces with
     * a module name do <em>not</em> match.
     *
     * Finally, {@code "*"} by itself represents the set of all
     * annotation interfaces, including the empty set.  Note that a
     * processor should not claim {@code "*"} unless it is actually
     * processing all files; claiming unnecessary annotations may
     * cause a performance slowdown in some environments.
     *
     * <p>Each string returned in the set must be accepted by the
     * following grammar:
     *
     * <blockquote>
     * <dl>
     * <dt><i>SupportedAnnotationTypeString:</i>
     * <dd><i>ModulePrefix</i><sub><i>opt</i></sub> <i>TypeName</i> <i>DotStar</i><sub><i>opt</i></sub>
     * <dd><code>*</code>
     *
     * <dt><i>ModulePrefix:</i>
     * <dd><i>ModuleName</i> <code>/</code>
     *
     * <dt><i>DotStar:</i>
     * <dd><code>.</code> <code>*</code>
     * </dl>
     * </blockquote>
     *
     * where <i>TypeName</i> and <i>ModuleName</i> are as defined in
     * <cite>The Java Language Specification</cite>
     * ({@jls 6.5 Determining the Meaning of a Name}).
     *
     * @apiNote When running in an environment which supports modules,
     * processors are encouraged to include the module prefix when
     * describing their supported annotation interfaces. The method {@link
     * AbstractProcessor#getSupportedAnnotationTypes
     * AbstractProcessor.getSupportedAnnotationTypes} provides support
     * for stripping off the module prefix when running in an
     * environment without modules.
     *
     * @return the names of the annotation interfaces supported by this processor
     *          or an empty set if none
     * @see javax.annotation.processing.SupportedAnnotationTypes
     * @jls 3.8 Identifiers
     */
    Set<String> getSupportedAnnotationTypes();

    /**
     * {@return the latest source version supported by this annotation
     * processor}
     *
     * @see javax.annotation.processing.SupportedSourceVersion
     * @see ProcessingEnvironment#getSourceVersion
     */
    SourceVersion getSupportedSourceVersion();

    /**
     * Initializes the processor with the processing environment.
     *
     * @param processingEnv environment for facilities the tool framework
     * provides to the processor
     */
    void init(ProcessingEnvironment processingEnv);

    /**
     * Processes a set of annotation interfaces on type elements
     * originating from the prior round and returns whether or not
     * these annotation interfaces are claimed by this processor.  If {@code
     * true} is returned, the annotation interfaces are claimed and subsequent
     * processors will not be asked to process them; if {@code false}
     * is returned, the annotation interfaces are unclaimed and subsequent
     * processors may be asked to process them.  A processor may
     * always return the same boolean value or may vary the result
     * based on its own chosen criteria.
     *
     * <p>The input set will be empty if the processor supports {@code
     * "*"} and the root elements have no annotations.  A {@code
     * Processor} must gracefully handle an empty set of annotations.
     *
     * @param annotations the annotation interfaces requested to be processed
     * @param roundEnv  environment for information about the current and prior round
     * @return whether or not the set of annotation interfaces are claimed by this processor
     */
    boolean process(Set<? extends TypeElement> annotations,
                    RoundEnvironment roundEnv);

   /**
    * Returns to the tool infrastructure an iterable of suggested
    * completions to an annotation.  Since completions are being asked
    * for, the information provided about the annotation may be
    * incomplete, as if for a source code fragment. A processor may
    * return an empty iterable.  Annotation processors should focus
    * their efforts on providing completions for annotation members
    * with additional validity constraints known to the processor, for
    * example an {@code int} member whose value should lie between 1
    * and 10 or a string member that should be recognized by a known
    * grammar, such as a regular expression or a URL.
    *
    * <p>Since incomplete programs are being modeled, some of the
    * parameters may only have partial information or may be {@code
    * null}.  At least one of {@code element} and {@code userText}
    * must be non-{@code null}.  If {@code element} is non-{@code null},
    * {@code annotation} and {@code member} may be {@code
    * null}.  Processors may not throw a {@code NullPointerException}
    * if some parameters are {@code null}; if a processor has no
    * completions to offer based on the provided information, an
    * empty iterable can be returned.  The processor may also return
    * a single completion with an empty value string and a message
    * describing why there are no completions.
    *
    * <p>Completions are informative and may reflect additional
    * validity checks performed by annotation processors.  For
    * example, consider the simple annotation:
    *
    * <blockquote>
    * <pre>
    * &#064;MersennePrime {
    *    int value();
    * }
    * </pre>
    * </blockquote>
    *
    * (A Mersenne prime is prime number of the form
    * 2<sup><i>n</i></sup> - 1.) Given an {@code AnnotationMirror}
    * for this annotation interface, a list of all such primes in the
    * {@code int} range could be returned without examining any other
    * arguments to {@code getCompletions}:
    *
    * <blockquote>
    * <pre>
    * import static javax.annotation.processing.Completions.*;
    * ...
    * return List.of({@link Completions#of(String) of}(&quot;3&quot;),
    *                of(&quot;7&quot;),
    *                of(&quot;31&quot;),
    *                of(&quot;127&quot;),
    *                of(&quot;8191&quot;),
    *                of(&quot;131071&quot;),
    *                of(&quot;524287&quot;),
    *                of(&quot;2147483647&quot;));
    * </pre>
    * </blockquote>
    *
    * A more informative set of completions would include the number
    * of each prime:
    *
    * <blockquote>
    * <pre>
    * return List.of({@link Completions#of(String, String) of}(&quot;3&quot;,          &quot;M2&quot;),
    *                of(&quot;7&quot;,          &quot;M3&quot;),
    *                of(&quot;31&quot;,         &quot;M5&quot;),
    *                of(&quot;127&quot;,        &quot;M7&quot;),
    *                of(&quot;8191&quot;,       &quot;M13&quot;),
    *                of(&quot;131071&quot;,     &quot;M17&quot;),
    *                of(&quot;524287&quot;,     &quot;M19&quot;),
    *                of(&quot;2147483647&quot;, &quot;M31&quot;));
    * </pre>
    * </blockquote>
    *
    * However, if the {@code userText} is available, it can be checked
    * to see if only a subset of the Mersenne primes are valid.  For
    * example, if the user has typed
    *
    * <blockquote>
    * <code>
    * &#064;MersennePrime(1
    * </code>
    * </blockquote>
    *
    * the value of {@code userText} will be {@code "1"}; and only
    * two of the primes are possible completions:
    *
    * <blockquote>
    * <pre>
    * return Arrays.asList(of(&quot;127&quot;,        &quot;M7&quot;),
    *                      of(&quot;131071&quot;,     &quot;M17&quot;));
    * </pre>
    * </blockquote>
    *
    * Sometimes no valid completion is possible.  For example, there
    * is no in-range Mersenne prime starting with 9:
    *
    * <blockquote>
    * <code>
    * &#064;MersennePrime(9
    * </code>
    * </blockquote>
    *
    * An appropriate response in this case is to either return an
    * empty list of completions,
    *
    * <blockquote>
    * <pre>
    * return Collections.emptyList();
    * </pre>
    * </blockquote>
    *
    * or a single empty completion with a helpful message
    *
    * <blockquote>
    * <pre>
    * return Arrays.asList(of(&quot;&quot;, &quot;No in-range Mersenne primes start with 9&quot;));
    * </pre>
    * </blockquote>
    *
    * @param element the element being annotated
    * @param annotation the (perhaps partial) annotation being
    *                   applied to the element
    * @param member the annotation member to return possible completions for
    * @param userText source code text to be completed
    *
    * @return suggested completions to the annotation
    */
    Iterable<? extends Completion> getCompletions(Element element,
                                                  AnnotationMirror annotation,
                                                  ExecutableElement member,
                                                  String userText);
}
