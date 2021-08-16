/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Collection;
import java.util.List;

/**
 * Provides analysis utilities for source code input.
 * Optional functionality that provides for a richer interactive experience.
 * Includes completion analysis:
 * Is the input a complete snippet of code?
 * Do I need to prompt for more input?
 * Would adding a semicolon make it complete?
 * Is there more than one snippet?
 * etc.
 * Also includes completion suggestions, as might be used in tab-completion.
 *
 * @since 9
 */
public abstract class SourceCodeAnalysis {

    /**
     * Given an input string, find the first snippet of code (one statement,
     * definition, import, or expression) and evaluate if it is complete.
     * @param input the input source string
     * @return a CompletionInfo instance with location and completeness info
     */
    public abstract CompletionInfo analyzeCompletion(String input);

    /**
     * Compute possible follow-ups for the given input.
     * Uses information from the current {@code JShell} state, including
     * type information, to filter the suggestions.
     * @param input the user input, so far
     * @param cursor the current position of the cursors in the given {@code input} text
     * @param anchor outgoing parameter - when an option will be completed, the text between
     *               the anchor and cursor will be deleted and replaced with the given option
     * @return list of candidate continuations of the given input.
     */
    public abstract List<Suggestion> completionSuggestions(String input, int cursor, int[] anchor);

    /**
     * Compute documentation for the given user's input. Multiple {@code Documentation} objects may
     * be returned when multiple elements match the user's input (like for overloaded methods).
     * @param input the snippet the user wrote so far
     * @param cursor the current position of the cursors in the given {@code input} text
     * @param computeJavadoc true if the javadoc for the given input should be computed in
     *                       addition to the signature
     * @return the documentations for the given user's input, if multiple elements match the input,
     *         multiple {@code Documentation} objects are returned.
     */
    public abstract List<Documentation> documentation(String input, int cursor, boolean computeJavadoc);

    /**
     * Infer the type of the given expression. The expression spans from the beginning of {@code code}
     * to the given {@code cursor} position. Returns null if the type of the expression cannot
     * be inferred.
     *
     * @param code the expression for which the type should be inferred
     * @param cursor current cursor position in the given code
     * @return the inferred type, or null if it cannot be inferred
     */
    public abstract String analyzeType(String code, int cursor);

    /**
     * List qualified names known for the simple name in the given code immediately
     * to the left of the given cursor position. The qualified names are gathered by inspecting the
     * classpath used by eval (see {@link JShell#addToClasspath(java.lang.String)}).
     *
     * @param code the expression for which the candidate qualified names should be computed
     * @param cursor current cursor position in the given code
     * @return the known qualified names
     */
    public abstract QualifiedNames listQualifiedNames(String code, int cursor);

    /**
     * Returns the wrapper information for the {@code Snippet}. The wrapper changes as
     * the environment changes, so calls to this method at different times may
     * yield different results.
     *
     * @param snippet the {@code Snippet} from which to retrieve the wrapper
     * @return information on the wrapper
     */
    public abstract SnippetWrapper wrapper(Snippet snippet);

    /**
     * Returns the wrapper information for the snippet within the
     * input source string.
     * <p>
     * Wrapper information for malformed and incomplete
     * snippets also generate wrappers. The list is in snippet encounter
     * order. The wrapper changes as the environment changes, so calls to this
     * method at different times may yield different results.
     * <p>
     * The input should be
     * exactly one complete snippet of source code, that is, one expression,
     * statement, variable declaration, method declaration, class declaration,
     * or import.
     * To break arbitrary input into individual complete snippets, use
     * {@link SourceCodeAnalysis#analyzeCompletion(String)}.
     * <p>
     * The wrapper may not match that returned by
     * {@link SourceCodeAnalysis#wrapper(Snippet) wrapper(Snippet)},
     * were the source converted to a {@code Snippet}.
     *
     * @param input the source input from which to generate wrappers
     * @return a list of wrapper information
     */
    public abstract List<SnippetWrapper> wrappers(String input);

    /**
     * Converts the source code of a snippet into a {@link Snippet} object (or
     * list of {@code Snippet} objects in the case of some var declarations,
     * e.g.: int x, y, z;).
     * Does not install the snippets: declarations are not
     * accessible by other snippets; imports are not added.
     * Does not execute the snippets.
     * <p>
     * Queries may be done on the {@code Snippet} object. The {@link Snippet#id()}
     * will be {@code "*UNASSOCIATED*"}.
     * The returned snippets are not associated with the
     * {@link JShell} instance, so attempts to pass them to {@code JShell}
     * methods will throw an {@code IllegalArgumentException}.
     * They will not appear in queries for snippets --
     * for example, {@link JShell#snippets() }.
     * <p>
     * Restrictions on the input are as in {@link JShell#eval}.
     * <p>
     * Only preliminary compilation is performed, sufficient to build the
     * {@code Snippet}.  Snippets known to be erroneous, are returned as
     * {@link ErroneousSnippet}, other snippets may or may not be in error.
     *
     * @param input The input String to convert
     * @return usually a singleton list of Snippet, but may be empty or multiple
     * @throws IllegalStateException if the {@code JShell} instance is closed.
     */
    public abstract List<Snippet> sourceToSnippets(String input);

    /**
     * Returns a collection of {@code Snippet}s which might need updating if the
     * given {@code Snippet} is updated. The returned collection is designed to
     * be inclusive and may include many false positives.
     *
     * @param snippet the {@code Snippet} whose dependents are requested
     * @return the collection of dependents
     */
    public abstract Collection<Snippet> dependents(Snippet snippet);

    /**
     * Internal only constructor
     */
    SourceCodeAnalysis() {}

    /**
     * The result of {@code analyzeCompletion(String input)}.
     * Describes the completeness of the first snippet in the given input.
     */
    public interface CompletionInfo {

        /**
         * The analyzed completeness of the input.
         *
         * @return an enum describing the completeness of the input string.
         */
        Completeness completeness();

        /**
         * Input remaining after the complete part of the source.
         *
         * @return the portion of the input string that remains after the
         * complete Snippet
         */
        String remaining();

        /**
         * Source code for the first Snippet of code input. For example, first
         * statement, or first method declaration. Trailing semicolons will be
         * added, as needed.
         *
         * @return the source of the first encountered Snippet
         */
        String source();
    }

    /**
     * Describes the completeness of the given input.
     */
    public enum Completeness {
        /**
         * The input is a complete source snippet (declaration or statement) as is.
         */
        COMPLETE(true),

        /**
         * With this addition of a semicolon the input is a complete source snippet.
         * This will only be returned when the end of input is encountered.
         */
        COMPLETE_WITH_SEMI(true),

        /**
         * There must be further source beyond the given input in order for it
         * to be complete.  A semicolon would not complete it.
         * This will only be returned when the end of input is encountered.
         */
        DEFINITELY_INCOMPLETE(false),

        /**
         * A statement with a trailing (non-terminated) empty statement.
         * Though technically it would be a complete statement
         * with the addition of a semicolon, it is rare
         * that that assumption is the desired behavior.
         * The input is considered incomplete.  Comments and white-space are
         * still considered empty.
         */
        CONSIDERED_INCOMPLETE(false),


        /**
         * An empty input.
         * The input is considered incomplete.  Comments and white-space are
         * still considered empty.
         */
        EMPTY(false),

        /**
         * The completeness of the input could not be determined because it
         * contains errors. Error detection is not a goal of completeness
         * analysis, however errors interfered with determining its completeness.
         * The input is considered complete because evaluating is the best
         * mechanism to get error information.
         */
        UNKNOWN(true);

        private final boolean isComplete;

        Completeness(boolean isComplete) {
            this.isComplete = isComplete;
        }

        /**
         * Indicates whether the first snippet of source is complete.
         * For example, "{@code x=}" is not
         * complete, but "{@code x=2}" is complete, even though a subsequent line could
         * make it "{@code x=2+2}". Already erroneous code is marked complete.
         *
         * @return {@code true} if the input is or begins a complete Snippet;
         * otherwise {@code false}
         */
        public boolean isComplete() {
            return isComplete;
        }
    }

    /**
     * A candidate for continuation of the given user's input.
     */
    public interface Suggestion {

        /**
         * The candidate continuation of the given user's input.
         *
         * @return the continuation string
         */
        String continuation();

        /**
         * Indicates whether input continuation matches the target type and is thus
         * more likely to be the desired continuation. A matching continuation is
         * preferred.
         *
         * @return {@code true} if this suggested continuation matches the
         * target type; otherwise {@code false}
         */
        boolean matchesType();
    }

    /**
     * A documentation for a candidate for continuation of the given user's input.
     */
    public interface Documentation {

        /**
         * The signature of the given element.
         *
         * @return the signature
         */
        String signature();

        /**
         * The javadoc of the given element.
         *
         * @return the javadoc, or null if not found or not requested
         */
        String javadoc();
    }

    /**
     * List of possible qualified names.
     */
    public static final class QualifiedNames {

        private final List<String> names;
        private final int simpleNameLength;
        private final boolean upToDate;
        private final boolean resolvable;

        QualifiedNames(List<String> names, int simpleNameLength, boolean upToDate, boolean resolvable) {
            this.names = names;
            this.simpleNameLength = simpleNameLength;
            this.upToDate = upToDate;
            this.resolvable = resolvable;
        }

        /**
         * Known qualified names for the given simple name in the original code.
         *
         * @return known qualified names
         */
        public List<String> getNames() {
            return names;
        }

        /**
         * The length of the simple name in the original code for which the
         * qualified names where gathered.
         *
         * @return the length of the simple name; -1 if there is no name immediately left to the cursor for
         *         which the candidates could be computed
         */
        public int getSimpleNameLength() {
            return simpleNameLength;
        }

        /**
         * Indicates whether the result is based on up-to-date data. The
         * {@link SourceCodeAnalysis#listQualifiedNames(java.lang.String, int) listQualifiedNames}
         * method may return before the classpath is fully inspected, in which case this method will
         * return {@code false}. If the result is based on a fully inspected classpath, this method
         * will return {@code true}.
         *
         * @return {@code true} if the result is based on up-to-date data;
         * otherwise {@code false}
         */
        public boolean isUpToDate() {
            return upToDate;
        }

        /**
         * Indicates whether the given simple name in the original code refers
         * to a resolvable element.
         *
         * @return {@code true} if the given simple name in the original code
         * refers to a resolvable element; otherwise {@code false}
         */
        public boolean isResolvable() {
            return resolvable;
        }

    }

    /**
     * The wrapping of a snippet of Java source into valid top-level Java
     * source. The wrapping will always either be an import or include a
     * synthetic class at the top-level. If a synthetic class is generated, it
     * will be proceeded by the package and import declarations, and may contain
     * synthetic class members.
     * <p>
     * This interface, in addition to the mapped form, provides the context and
     * position mapping information.
     */
    public interface SnippetWrapper {

        /**
         * Returns the input that is wrapped. For
         * {@link SourceCodeAnalysis#wrappers(java.lang.String) wrappers(String)},
         * this is the source of the snippet within the input. A variable
         * declaration of {@code N} variables will map to {@code N} wrappers
         * with the source separated.
         * <p>
         * For {@link SourceCodeAnalysis#wrapper(Snippet) wrapper(Snippet)},
         * this is {@link Snippet#source() }.
         *
         * @return the input source corresponding to the wrapper.
         */
        String source();

        /**
         * Returns a Java class definition that wraps the
         * {@link SnippetWrapper#source()} or, if an import, the import source.
         * <p>
         * If the input is not a valid Snippet, this will not be a valid
         * class/import definition.
         * <p>
         * The source may be divided and mapped to different locations within
         * the wrapped source.
         *
         * @return the source wrapped into top-level Java code
         */
        String wrapped();

        /**
         * Returns the fully qualified class name of the
         * {@link SnippetWrapper#wrapped() } class.
         * For erroneous input, a best guess is returned.
         *
         * @return the name of the synthetic wrapped class; if an import, the
         * name is not defined
         */
        String fullClassName();

        /**
         * Returns the {@link Snippet.Kind} of the
         * {@link SnippetWrapper#source()}.
         *
         * @return an enum representing the general kind of snippet.
         */
        Snippet.Kind kind();

        /**
         * Maps character position within the source to character position
         * within the wrapped.
         *
         * @param pos the position in {@link SnippetWrapper#source()}
         * @return the corresponding position in
         * {@link SnippetWrapper#wrapped() }
         */
        int sourceToWrappedPosition(int pos);

        /**
         * Maps character position within the wrapped to character position
         * within the source.
         *
         * @param pos the position in {@link SnippetWrapper#wrapped()}
         * @return the corresponding position in
         * {@link SnippetWrapper#source() }
         */
        int wrappedToSourcePosition(int pos);
    }
}
