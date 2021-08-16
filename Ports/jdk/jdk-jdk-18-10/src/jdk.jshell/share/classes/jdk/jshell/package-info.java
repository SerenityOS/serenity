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

/**
 * Provides interfaces for creating tools, such as a Read-Eval-Print Loop (REPL),
 * which interactively evaluate "snippets" of Java programming language code.
 * Where a "snippet" is a single expression, statement, or declaration.
 * This functionality can be used to enhance tools such as IDEs or can be
 * stand-alone.
 * <p>
 * {@link jdk.jshell.JShell} is the central class.  An instance of
 * <code>JShell</code> holds the evaluation state, which is both the current
 * set of source snippets and the execution state they have produced.
 * <p>
 * Each source snippet is represented by an instance of a subclass of
 * {@link jdk.jshell.Snippet}. For example, a statement is represented by an
 * instance of {@link jdk.jshell.StatementSnippet}, and a method declaration is
 * represented by an instance of {@link jdk.jshell.MethodSnippet}.
 * Snippets are created when
 * {@link jdk.jshell.JShell#eval(java.lang.String) JShell.eval(String)}
 * is invoked with an input which includes one or more snippets of code.
 * <p>
 * Any change to the compilation status of a snippet is reported with a
 * {@link jdk.jshell.SnippetEvent}.  There are three major kinds of
 * changes to the status of a snippet: it can be created with <code>eval</code>,
 * it can be dropped from the active source state with
 * {@link jdk.jshell.JShell#drop(jdk.jshell.Snippet)}, and it can have
 * its status updated as a result of a status change in another snippet.
 * For
 * example: given <code>js</code>, an instance of <code>JShell</code>, executing
 * <code>js.eval("int x = 5;")</code> will add the variable <code>x</code> to
 * the source state and will generate an event describing the creation of a
 * {@link jdk.jshell.VarSnippet} for <code>x</code>. Then executing
 * <code>js.eval("int timesx(int val) { return val * x; }")</code> will add
 * a method to the source state and will generate an event
 * describing the creation of a {@link jdk.jshell.MethodSnippet} for
 * <code>timesx</code>.
 * Assume that <code>varx</code> holds the snippet created by the first
 * call to <code>eval</code>, executing <code>js.drop(varx)</code> will
 * generate two events: one for changing the status of the
 * variable snippet to <code>DROPPED</code> and one for
 * updating the method snippet (which now has an unresolved reference to
 * <code>x</code>).
 * <p>
 * Of course, for any general application of the API, the input would not be
 * fixed strings, but would come from the user. Below is a very simplified
 * example of how the API might be used to implement a REPL.
 * <pre>
 * {@code
 *     import java.io.ByteArrayInputStream;
 *     import java.io.Console;
 *     import java.util.List;
 *     import jdk.jshell.*;
 *     import jdk.jshell.Snippet.Status;
 *
 *     class ExampleJShell {
 *         public static void main(String[] args) {
 *             Console console = System.console();
 *             try (JShell js = JShell.create()) {
 *                 do {
 *                     System.out.print("Enter some Java code: ");
 *                     String input = console.readLine();
 *                     if (input == null) {
 *                         break;
 *                     }
 *                     List<SnippetEvent> events = js.eval(input);
 *                     for (SnippetEvent e : events) {
 *                         StringBuilder sb = new StringBuilder();
 *                         if (e.causeSnippet() == null) {
 *                             //  We have a snippet creation event
 *                             switch (e.status()) {
 *                                 case VALID:
 *                                     sb.append("Successful ");
 *                                     break;
 *                                 case RECOVERABLE_DEFINED:
 *                                     sb.append("With unresolved references ");
 *                                     break;
 *                                 case RECOVERABLE_NOT_DEFINED:
 *                                     sb.append("Possibly reparable, failed  ");
 *                                     break;
 *                                 case REJECTED:
 *                                     sb.append("Failed ");
 *                                     break;
 *                             }
 *                             if (e.previousStatus() == Status.NONEXISTENT) {
 *                                 sb.append("addition");
 *                             } else {
 *                                 sb.append("modification");
 *                             }
 *                             sb.append(" of ");
 *                             sb.append(e.snippet().source());
 *                             System.out.println(sb);
 *                             if (e.value() != null) {
 *                                 System.out.printf("Value is: %s\n", e.value());
 *                             }
 *                             System.out.flush();
 *                         }
 *                     }
 *                 } while (true);
 *             }
 *             System.out.println("\nGoodbye");
 *         }
 *     }
 * }
 * </pre>
 * <p>
 * To register for status change events use
 * {@link jdk.jshell.JShell#onSnippetEvent(java.util.function.Consumer)}.
 * These events are only generated by <code>eval</code> and <code>drop</code>,
 * the return values of these methods are the list of events generated by that
 * call.  So, as in the example above, events can be used without registering
 * to receive events.
 * <p>
 * If you experiment with this example, you will see that failing to terminate
 * a statement or variable declaration with a semi-colon will simply fail.
 * An unfinished entry (for example a desired multi-line method) will also just
 * fail after one line.  The utilities in {@link jdk.jshell.SourceCodeAnalysis}
 * provide source boundary and completeness analysis to address cases like
 * those.  <code>SourceCodeAnalysis</code> also provides suggested completions
 * of input, as might be used in tab-completion.
 *
 * @since 9
 */


package jdk.jshell;

