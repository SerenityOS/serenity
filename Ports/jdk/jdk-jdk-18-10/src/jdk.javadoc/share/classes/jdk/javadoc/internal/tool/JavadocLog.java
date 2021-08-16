/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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

package jdk.javadoc.internal.tool;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;
import javax.tools.Diagnostic;
import javax.tools.Diagnostic.Kind;
import javax.tools.FileObject;
import javax.tools.ForwardingFileObject;
import javax.tools.JavaFileObject;

import com.sun.source.doctree.CommentTree;
import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.DocTypeTree;
import com.sun.source.doctree.ReferenceTree;
import com.sun.source.doctree.TextTree;
import com.sun.tools.javac.tree.DCTree;
import jdk.javadoc.doclet.Reporter;

import com.sun.tools.javac.tree.EndPosTable;
import com.sun.tools.javac.util.Context.Factory;
import com.sun.tools.javac.util.DiagnosticSource;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.DocSourcePositions;
import com.sun.source.util.DocTreePath;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.JCDiagnostic;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticFlag;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticType;
import com.sun.tools.javac.util.JavacMessages;
import com.sun.tools.javac.util.Log;

/**
 * Class for reporting diagnostics and other messages.
 *
 * The class leverages the javac support for reporting diagnostics, for stylistic consistency
 * of diagnostic messages and to avoid code duplication.
 *
 * The class is a subtype of javac's Log, and is primarily an adapter between
 * javadoc method signatures and the underlying javac methods. Within this class,
 * the methods call down to a core {@code report} method which hands off to
 * a similar method in the superclass ({@code Log.report}, which takes care
 * of reporting the diagnostic (unless it has been suppressed), displaying
 * the source line and a caret to indicate the position of the issue (if appropriate),
 * counting errors and warnings, and so on.
 *
 * In general, the underlying javac layer is more powerful, whereas the javadoc methods are
 * constrained by the public {@link jdk.javadoc.doclet.Doclet} API.
 *
 * In the underlying javac layer, the following abstractions are used:
 * <ul>
 *     <li>{@code DiagnosticType} -- error, warning, note, etc.
 *     <li>{@code DiagnosticSource} -- a file object and a cache of its content
 *     <li>{@code DiagnosticPosition} -- a tuple of values (start, pos, end) for the position of a diagnostic
 *     <li>{@code DiagnosticFlag} -- additional flags related to the diagnostic
 * </ul>
 *
 * The javadoc layer is defined by the methods on {@code Doclet.Reporter}, and by
 * assorted methods defined in this class for use by the javadoc tool.
 * The primary data types are:
 * <ul>
 *     <li>{@code Diagnostic.Kind} -- maps to {@code DiagnosticType} and {@code Set<DiagnosticFlag>}
 *     <li>{@code Element} -- maps to {@code DiagnosticSource} and {@code DiagnosticPosition}
 *     <li>{@code DocTreePath} -- maps to {@code DiagnosticSource} and {@code DiagnosticPosition}
 * </ul>
 *
 * The reporting methods in the javac layer primarily take pre-localized (key, args) pairs,
 * while the methods in the javadoc layer, especially the {@code Reporter} interface, take
 * localized strings. To accommodate this, "wrapper" resources are used, whose value is {@code {0}},
 * to pass the localized string down to javac. A side-effect is that clients using a
 * {@code DiagnosticListener} with a {@code DocumentationTask} cannot access the original resource
 * key for the localized message.
 * Given the limitations of the API, it is not possible to do any better.
 * The javac Annotation Processing API has the same problem.
 *
 * There is a slight disparity between javac's use of streams and javadoc's use of streams.
 * javac reports <b>all</b> diagnostics to the "error" stream, and provides a separate
 * "output" stream for expected output, such as command-line help or the output from options
 * like {@code -Xprint}. javadoc API, and {@code Reporter} in particular, does not specify
 * the use of streams, and provides no support for identifying or specifying streams. JDK-8267204.
 * The current implementation/workaround is to write errors and warnings to the "error"
 * stream and notes to the "output" stream.
 *
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 *
 * @see java.util.ResourceBundle
 * @see java.text.MessageFormat
 */
public class JavadocLog extends Log implements Reporter {
    /** The overall context for the documentation run. */
    private final Context context;

    /** The tool environment, providing access to the tool's utility classes and tables. */
    private ToolEnvironment toolEnv;

    /** The utility class to access the positions of items in doc comments. */
    private DocSourcePositions sourcePositions;

    /**
     * A memory-sensitive cache of recently used {@code DiagnosticSource} objects.
     */
    private final LinkedHashMap<JavaFileObject, SoftReference<DiagnosticSource>> diagSourceCache;

    /** Get the current javadoc log, which is also the compiler log. */
    public static JavadocLog instance0(Context context) {
        Log instance = context.get(logKey);
        if (!(instance instanceof JavadocLog l))
            throw new InternalError("no JavadocLog instance!");
        return l;
    }

    public static void preRegister(Context context,
                                   final String programName) {
        context.put(logKey, (Factory<Log>)c -> new JavadocLog(c, programName));
    }

    public static void preRegister(Context context, final String programName,
            final PrintWriter outWriter, final PrintWriter errWriter) {
        context.put(logKey, (Factory<Log>)c -> new JavadocLog(c, programName, outWriter, errWriter));
    }

    final String programName;

    private Locale locale;
    private final JavacMessages messages;
    private final JCDiagnostic.Factory javadocDiags;

    private static PrintWriter createPrintWriter(PrintStream ps, boolean autoflush) {
        return new PrintWriter(ps, autoflush) {
            // avoid closing system streams
            @Override
            public void close() {
                super.flush();
            }
        };
    }

    /**
     * Constructor
     * @param programName  Name of the program (for error messages).
     */
    public JavadocLog(Context context, String programName) {
        // use the current values of System.out, System.err, in case they have been redirected
        this(context, programName,
                createPrintWriter(System.out, false),
                createPrintWriter(System.err, true));
    }

    /**
     * Constructor
     * @param programName  Name of the program (for error messages).
     * @param outWriter    Stream for notices etc.
     * @param errWriter    Stream for errors and warnings
     */
    public JavadocLog(Context context, String programName, PrintWriter outWriter, PrintWriter errWriter) {
        super(context, outWriter, errWriter);
        messages = JavacMessages.instance(context);
        messages.add(locale -> ResourceBundle.getBundle("jdk.javadoc.internal.tool.resources.javadoc",
                                                         locale));
        javadocDiags = new JCDiagnostic.Factory(messages, "javadoc");
        this.programName = programName;
        this.context = context;
        locale = Locale.getDefault();

        diagSourceCache = new LinkedHashMap<>() {
            private static final int MAX_ENTRIES = 5;

            @Override
            protected boolean removeEldestEntry(Map.Entry<JavaFileObject, SoftReference<DiagnosticSource>> eldest) {
                return size() > MAX_ENTRIES;
            }
        };
    }

    @Override // Reporter
    public PrintWriter getStandardWriter() {
        return getWriter(Log.WriterKind.STDOUT);
    }

    @Override // Reporter
    public PrintWriter getDiagnosticWriter() {
        return getWriter(Log.WriterKind.STDERR);
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    /**
     * Returns the localized string from the tool's resource bundles.
     *
     * @param key the resource key
     * @param args arguments for the resource
     */
    String getText(String key, Object... args) {
        return messages.getLocalizedString(locale, key, args);
    }

    @Override // Reporter
    public void print(Kind kind, String message) {
        report(kind, null, null, message);
    }

    @Override // Reporter
    public void print(Diagnostic.Kind kind, DocTreePath path, String message) {
        DiagnosticType dt = getDiagnosticType(kind);
        Set<DiagnosticFlag> flags = getDiagnosticFlags(kind);
        DiagnosticSource ds = getDiagnosticSource(path);
        DiagnosticPosition dp = getDiagnosticPosition(path);
        report(dt, flags, ds, dp, message);
    }

    @Override // Reporter
    public void print(Diagnostic.Kind kind, DocTreePath path, int start, int pos, int end, String message) {
        if (!(start <= pos && pos <= end)) {
            throw new IllegalArgumentException("start:" + start + ",pos:" + pos + ",end:" + end);
        }

        DocTree t = path.getLeaf();
        String s = switch (t.getKind()) {
            case COMMENT -> ((CommentTree) t).getBody();
            case DOC_TYPE -> ((DocTypeTree) t).getText();
            case REFERENCE -> ((ReferenceTree) t).getSignature();
            case TEXT -> ((TextTree) t).getBody();
            default -> throw new IllegalArgumentException(t.getKind().toString());
        };

        if (start < 0 || end > s.length()) {
            throw new StringIndexOutOfBoundsException("start:" + start + ",pos:" + pos + ",end:" + end
                    + "; string length " + s.length());
        }

        DiagnosticType dt = getDiagnosticType(kind);
        Set<DiagnosticFlag> flags = getDiagnosticFlags(kind);
        DiagnosticSource ds = getDiagnosticSource(path);

        DCTree.DCDocComment docComment = (DCTree.DCDocComment) path.getDocComment();
        DCTree tree = (DCTree) path.getLeaf();
        // note: it is important to evaluate the offsets in the context of the position
        // within the comment text, and not in the context of the overall source text
        int sStart = (int) tree.getSourcePosition(docComment, start);
        int sPos = (int) tree.getSourcePosition(docComment, pos);
        int sEnd = (int) tree.getSourcePosition(docComment, end);
        DiagnosticPosition dp = createDiagnosticPosition(null, sStart, sPos, sEnd);

        report(dt, flags, ds, dp, message);
    }

    private int getSourcePos(DocTreePath path, int offset) {
        DCTree.DCDocComment docComment = (DCTree.DCDocComment) path.getDocComment();
        DCTree tree = (DCTree) path.getLeaf();
        return (int) tree.getSourcePosition(docComment, offset);
    }

    @Override  // Reporter
    public void print(Kind kind, Element element, String message) {
        DiagnosticType dt = getDiagnosticType(kind);
        Set<DiagnosticFlag> flags = getDiagnosticFlags(kind);
        DiagnosticSource ds = getDiagnosticSource(element);
        DiagnosticPosition dp = getDiagnosticPosition(element);
        report(dt, flags, ds, dp, message);
    }

    @Override // Reporter
    public void print(Kind kind, FileObject file, int start, int pos, int end, String message) throws IllegalArgumentException {
        DiagnosticType dt = getDiagnosticType(kind);
        Set<DiagnosticFlag> flags = getDiagnosticFlags(kind);
        // Although not required to do so, it is the case that any file object returned from the
        // javac impl of JavaFileManager will return an object that implements JavaFileObject.
        // See PathFileObject, which provides the primary impls of (Java)FileObject.
        JavaFileObject fo = file instanceof JavaFileObject _fo ? _fo : new WrappingJavaFileObject(file);
        DiagnosticSource ds = new DiagnosticSource(fo, this);
        DiagnosticPosition dp = createDiagnosticPosition(null, start, pos, end);
        report(dt, flags, ds, dp, message);
    }

    private class WrappingJavaFileObject
            extends ForwardingFileObject<FileObject> implements JavaFileObject {

        WrappingJavaFileObject(FileObject fo) {
            super(fo);
            assert !(fo instanceof JavaFileObject);
        }

        @Override
        public Kind getKind() {
            String name = fileObject.getName();
            return name.endsWith(Kind.HTML.extension)
                    ? JavaFileObject.Kind.HTML
                    : JavaFileObject.Kind.OTHER;
        }

        @Override
        public boolean isNameCompatible(String simpleName, Kind kind) {
            return false;
        }

        @Override
        public NestingKind getNestingKind() {
            return null;
        }

        @Override
        public Modifier getAccessLevel() {
            return null;
        }
    }

    /**
     * Prints an error message.
     *
     * @param message the message
     */
    public void printError(String message) {
        report(DiagnosticType.ERROR,null, null, message);
    }

    /**
     * Prints an error message for a given documentation tree node.
     *
     * @param path    the path for the documentation tree node
     * @param message the message
     */
    public void printError(DocTreePath path, String message) {
        DiagnosticSource ds = getDiagnosticSource(path);
        DiagnosticPosition dp = getDiagnosticPosition(path);
        report(DiagnosticType.ERROR, EnumSet.noneOf(DiagnosticFlag.class), ds, dp, message);
    }

    /**
     * Prints an error message for a given element.
     *
     * @param element the element
     * @param message the message
     */
    public void printError(Element element, String message) {
        DiagnosticSource ds = getDiagnosticSource(element);
        DiagnosticPosition dp = getDiagnosticPosition(element);
        report(DiagnosticType.ERROR, EnumSet.noneOf(DiagnosticFlag.class), ds, dp, message);
    }

    /**
     * Prints an error message.
     *
     * @param key the resource key for the message
     * @param args the arguments for the message
     */
    public void printErrorUsingKey(String key, Object... args) {
        printError(getText(key, args));
    }

    /**
     * Prints a warning message.
     *
     * @param message the message
     */
    public void printWarning(String message) {
        report(DiagnosticType.WARNING, null, null, message);
    }

    /**
     * Prints a warning message for a given documentation tree node.
     *
     * @param path    the path for the documentation tree node
     * @param message the message
     */
    public void printWarning(DocTreePath path, String message) {
        DiagnosticSource ds = getDiagnosticSource(path);
        DiagnosticPosition dp = getDiagnosticPosition(path);
        report(DiagnosticType.WARNING, EnumSet.noneOf(DiagnosticFlag.class), ds, dp, message);
    }

    /**
     * Prints a warning message for a given element.
     *
     * @param element the element
     * @param message the message
     */
    public void printWarning(Element element, String message) {
        DiagnosticSource ds = getDiagnosticSource(element);
        DiagnosticPosition dp = getDiagnosticPosition(element);
        report(DiagnosticType.WARNING, EnumSet.noneOf(DiagnosticFlag.class), ds, dp, message);
    }

    /**
     * Prints a warning message.
     *
     * @param key the resource key for the message
     * @param args the arguments for the message
     */
    public void printWarningUsingKey(String key, Object... args) {
        printWarning(getText(key, args));
    }

    /**
     * Prints a warning message for an element.
     *
     * @param element the element
     * @param key     the resource key for the message
     * @param args    the arguments for the message
     */
    public void printWarningUsingKey(Element element, String key, Object... args) {
        printWarning(element, getText(key, args));
    }

    /**
     * Prints a "notice" message to the standard writer.
     *
     * @param key  the resource key for the message
     * @param args the arguments for the message
     */
    public void noticeUsingKey(String key, Object... args) {
        printRawLines(getStandardWriter(), getText(key, args));
    }

    /**
     * Prints a "notice" message to the standard writer.
     *
     * @param message the message
     */
    public void notice(String message) {
        printRawLines(getStandardWriter(), message);
    }

    /**
     * Returns true if errors have been recorded.
     */
    public boolean hasErrors() {
        return nerrors != 0;
    }

    /**
     * Returns true if warnings have been recorded.
     */
    public boolean hasWarnings() {
        return nwarnings != 0;
    }

    /**
     * Prints the error and warning counts, if any, to the diagnostic writer.
     */
    public void printErrorWarningCounts() {
        printCount(nerrors, "main.error", "main.errors");
        printCount(nwarnings, "main.warning", "main.warnings");
    }

    private void printCount(int count, String singleKey, String pluralKey) {
        if (count > 0) {
            String message = getText(count > 1 ? pluralKey : singleKey, count);
            if (diagListener != null) {
                report(DiagnosticType.NOTE, null, null, message);
            } else {
                printRawLines(getDiagnosticWriter(), message);
            }
        }
    }

    /**
     * Reports a diagnostic message.
     *
     * @param kind    the kind of diagnostic
     * @param ds      the diagnostic source
     * @param dp      the diagnostic position
     * @param message the message
     */
    private void report(Diagnostic.Kind kind, DiagnosticSource ds, DiagnosticPosition dp, String message) {
        report(getDiagnosticType(kind), getDiagnosticFlags(kind), ds, dp, message);
    }

    /**
     * Reports a diagnostic message.
     *
     * @param dt      the diagnostic type
     * @param ds      the diagnostic source
     * @param dp      the diagnostic position
     * @param message the message
     */
    private void report(DiagnosticType dt, DiagnosticSource ds, DiagnosticPosition dp, String message) {
        report(dt, EnumSet.noneOf(DiagnosticFlag.class), ds, dp, message);
    }

    /**
     * Reports a diagnostic message, with diagnostic flags.
     * For javadoc, the only flag that is used is {@code MANDATORY_WARNING}, and only
     * because in principle the public API supports it via {@code Kind.MANDATORY_WARNING}.
     * javadoc itself does generate mandatory warnings.
     *
     * This is the primary low-level wrapper around the underlying {@code Log.report}.
     * Because we already have a localized message, we use wrapper resources (just {@code {0}})
     * to wrap the string. The current behavior is one wrapper per diagnostic type.
     * We could improve this by subtyping {@code DiagnosticInfo} to modify the resource key used.
     *
     * {@code Log} reports all diagnostics to the corresponding writer, which defaults
     * to the "error" stream, when using the two-stream constructor. That doesn't work
     * for javadoc, which has historically written notes to the "output" stream, because
     * the public API used by doclets does not provide for more detailed control.
     * Therefore, for now, javadoc continues to use the (deprecated) three-stream
     * constructor, with the {@code NOTE} stream set to the "output" stream.
     *
     * {@code Log} reports all notes with a "Note:" prefix. That's not good for the
     * standard doclet, which uses notes to report the various "progress" messages,
     * such as  "Generating class ...".  They can be written directly to the diagnostic
     * writer, but that bypasses low-level checks about whether to suppress notes,
     * and bypasses the diagnostic listener for API clients.
     * Overall, it's an over-constrained problem with no obvious good solution.
     *
     * Note: there is an intentional difference in behavior between the diagnostic source
     * being set to {@code null} (no source intended) and {@code NO_SOURCE} (no source available).
     *
     * @param dt      the diagnostic type
     * @param ds      the diagnostic source
     * @param dp      the diagnostic position
     * @param message the message
     */
    private void report(DiagnosticType dt, Set<DiagnosticFlag> flags, DiagnosticSource ds, DiagnosticPosition dp, String message) {
        report(javadocDiags.create(dt, null, flags, ds, dp, "message", message));
    }

    /**
     * Returns a diagnostic position for a documentation tree node.
     *
     * @param path the path for the documentation tree node
     * @return the diagnostic position
     */
    private DiagnosticPosition getDiagnosticPosition(DocTreePath path) {
        DocSourcePositions posns = getSourcePositions();
        CompilationUnitTree compUnit = path.getTreePath().getCompilationUnit();
        int start = (int) posns.getStartPosition(compUnit, path.getDocComment(), path.getLeaf());
        int end = (int) posns.getEndPosition(compUnit, path.getDocComment(), path.getLeaf());
        return createDiagnosticPosition(null, start, start, end);
    }

    /**
     * Returns a diagnostic position for an element, or {@code null} if the source
     * file is not available.
     *
     * @param element the element
     * @return the diagnostic position
     */
    private DiagnosticPosition getDiagnosticPosition(Element element) {
        ToolEnvironment toolEnv = getToolEnv();
        DocSourcePositions posns = getSourcePositions();
        TreePath tp = toolEnv.elementToTreePath.get(element);
        if (tp == null) {
            return null;
        }
        CompilationUnitTree compUnit = tp.getCompilationUnit();
        JCTree tree = (JCTree) tp.getLeaf();
        int start = (int) posns.getStartPosition(compUnit, tree);
        int pos = tree.getPreferredPosition();
        int end = (int) posns.getEndPosition(compUnit, tree);
        return createDiagnosticPosition(tree, start, pos, end);
    }

    /**
     * Creates a diagnostic position.
     *
     * @param tree the tree node, or null if no tree is applicable
     * @param start the start position
     * @param pos   the "preferred" position: this is used to position the caret in messages
     * @param end   the end position
     * @return the diagnostic position
     */
    private DiagnosticPosition createDiagnosticPosition(JCTree tree, int start, int pos, int end) {
        return new DiagnosticPosition() {
            @Override
            public JCTree getTree() {
                return tree;
            }

            @Override
            public int getStartPosition() {
                return start;
            }

            @Override
            public int getPreferredPosition() {
                return pos;
            }

            @Override
            public int getEndPosition(EndPosTable endPosTable) {
                return end;
            }
        };
    }

    /**
     * Returns the diagnostic type for a diagnostic kind.
     *
     * @param kind the diagnostic kind
     * @return the diagnostic type
     */
    private DiagnosticType getDiagnosticType(Diagnostic.Kind kind) {
        return switch (kind) {
            case ERROR -> DiagnosticType.ERROR;
            case WARNING, MANDATORY_WARNING -> DiagnosticType.WARNING;
            case NOTE -> DiagnosticType.NOTE;
            case OTHER -> DiagnosticType.FRAGMENT;
        };
    }

    /**
     * Returns the diagnostic flags for a diagnostic kind.
     * A diagnostic kind of {@code MANDATORY_WARNING} requires the {@code MANDATORY} flag.
     *
     * @param kind the diagnostic kind
     * @return the flags
     */
    private Set<DiagnosticFlag> getDiagnosticFlags(Diagnostic.Kind kind) {
        return kind == Kind.MANDATORY_WARNING
                ? EnumSet.of(DiagnosticFlag.MANDATORY)
                : EnumSet.noneOf(DiagnosticFlag.class);
    }

    /**
     * Returns the diagnostic source for an documentation tree node.
     *
     * @param path the path for the documentation tree node
     * @return the diagnostic source
     */
    private DiagnosticSource getDiagnosticSource(DocTreePath path) {
        return getDiagnosticSource(path.getTreePath().getCompilationUnit().getSourceFile());
    }

    /**
     * Returns the diagnostic source for an element, or {@code NO_SOURCE} if the
     * source file is not known (for example, if the element was read from a class file).
     *
     * @param element the element
     * @return the diagnostic source
     */
    private DiagnosticSource getDiagnosticSource(Element element) {
        TreePath tp = getToolEnv().elementToTreePath.get(element);
        return tp == null ? DiagnosticSource.NO_SOURCE
                : getDiagnosticSource(tp.getCompilationUnit().getSourceFile());
    }

    /**
     * Returns the diagnostic source for a file object.
     *
     * {@code DiagnosticSource} objects are moderately expensive because they maintain
     * an internal copy of the content, to provide the line map.
     * Therefore, we keep a small memory-sensitive cache of recently used objects.
     *
     * @param fo the file object
     * @return the diagnostic source
     */
    private DiagnosticSource getDiagnosticSource(JavaFileObject fo) {
        Reference<DiagnosticSource> ref = diagSourceCache.get(fo);
        DiagnosticSource ds = ref == null ? null : ref.get();
        if (ds == null) {
            ds = new DiagnosticSource(fo, this);
            diagSourceCache.put(fo, new SoftReference<>(ds));
        }
        return ds;
    }

    /**
     * Returns the object for computing source positions.
     *
     * The value is determined lazily because the tool environment is computed lazily.
     *
     * @return the object for computing source positions
     */
    private DocSourcePositions getSourcePositions() {
        if (sourcePositions == null) {
            sourcePositions = getToolEnv().docTrees.getSourcePositions();
        }
        return sourcePositions;
    }

    /**
     * Returns the tool environment.
     *
     * The value is determined lazily, because creating it eagerly disrupts
     * the overall initialization of objects in the context.
     *
     * @return the tool environment
     */
    private ToolEnvironment getToolEnv() {
        if (toolEnv == null) {
            toolEnv = ToolEnvironment.instance(context);
        }
        return toolEnv;
    }
}
