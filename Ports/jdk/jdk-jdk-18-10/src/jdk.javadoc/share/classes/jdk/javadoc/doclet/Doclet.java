/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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

package jdk.javadoc.doclet;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.lang.model.SourceVersion;

/**
 * The user doclet must implement this interface, as described in the
 * <a href="package-summary.html#package-description">package description</a>.
 * Each implementation of a Doclet must provide a public no-argument constructor
 * to be used by tools to instantiate the doclet. The tool infrastructure will
 * interact with classes implementing this interface as follows:
 * <ol>
 * <li> The tool will create an instance of a doclet using the no-arg constructor
 *  of the doclet class.
 * <li> Next, the tool calls the {@link #init init} method with an appropriate locale
 *  and reporter.
 * <li> Afterwards, the tool calls {@link #getSupportedOptions getSupportedOptions},
 * and {@link #getSupportedSourceVersion getSupportedSourceVersion}.
 * These methods are only called once.
 * <li> As appropriate, the tool calls the {@link #run run} method on the doclet
 * object, giving it a DocletEnvironment object, from which the doclet can determine
 * the elements to be included in the documentation.
 * </ol>
 * <p>
 * If a doclet object is created and used without the above protocol being followed,
 * then the doclet's behavior is not defined by this interface specification.
 * <p> To start the doclet, pass {@code -doclet} followed by the fully-qualified
 * name of the entry point class (i.e. the implementation of this interface) on
 * the javadoc tool command line.
 *
 * @since 9
 */
public interface Doclet {

    /**
     * Initializes this doclet with the given locale and error reporter.
     * This locale will be used by the reporter and the doclet components.
     *
     * @param locale the locale to be used
     * @param reporter the reporter to be used
     */
    void init(Locale locale, Reporter reporter);

    /**
     * Returns a name identifying the doclet. A name is a simple identifier
     * without white spaces, as defined in <cite>The Java Language Specification</cite>,
     * section 6.2 "Names and Identifiers".
     *
     * @return name of the Doclet
     */
    String getName();

    /**
     * Returns all the supported options.
     *
     * @return a set containing all the supported options, an empty set if none
     */
    Set<? extends Option> getSupportedOptions();

    /**
     * Returns the version of the Java Programming Language supported
     * by this doclet.
     *
     * @return  the language version supported by this doclet, usually
     * the latest version
     */
    SourceVersion getSupportedSourceVersion();

    /**
     * The entry point of the doclet. Further processing will commence as
     * instructed by this method.
     *
     * @param environment from which essential information can be extracted
     * @return true on success
     */
    boolean run(DocletEnvironment environment);

    /**
     * An encapsulation of option name, aliases, parameters and descriptions
     * as used by the Doclet.
     */
    interface Option {
        /**
         * Returns the number of arguments, this option will consume.
         * @return number of consumed arguments
         */
        int getArgumentCount();

        /**
         * Returns the description of the option. For instance, the option "group", would
         * return the synopsis of the option such as, "groups the documents".
         * @return description if set, otherwise an empty String
         */
        String getDescription();

        /**
         * Returns the option kind.
         * @return the kind of this option
         */
        Option.Kind getKind();

        /**
         * Returns the list of names that may be used to identify the option. For instance, the
         * list could be {@code ["-classpath", "--class-path"]} for the
         * option "-classpath", with an alias "--class-path".
         * @return the names of the option
         */
        List<String> getNames();

        /**
         * Returns the parameters of the option. For instance "name &lt;p1&gt;:&lt;p2&gt;.."
         * @return parameters if set, otherwise an empty String
         */
        String getParameters();

        /**
         * Processes the option and arguments as needed. This method will
         * be invoked if the given option name matches the option.
         * @param option the option
         * @param arguments a list encapsulating the arguments
         * @return true if operation succeeded, false otherwise
         */
        boolean process(String option, List<String> arguments);

        /**
         * The kind of an option.
         */
        enum Kind {
            /** An extended option, such as those prefixed with {@code -X}. */
            EXTENDED,
            /** A standard option. */
            STANDARD,
            /** An implementation-reserved option. */
            OTHER;
        }
    }
}
