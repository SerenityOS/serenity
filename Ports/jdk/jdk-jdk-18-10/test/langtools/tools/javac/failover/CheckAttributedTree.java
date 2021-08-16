/*
 * Copyright (c) 2010, 2021, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 6970584 8006694 8062373 8129962
 * @summary assorted position errors in compiler syntax trees
 *  temporarily workaround combo tests are causing time out in several platforms
 * @library ../lib
 * @modules java.desktop
 *          jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.code
 *          jdk.compiler/com.sun.tools.javac.file
 *          jdk.compiler/com.sun.tools.javac.tree
 *          jdk.compiler/com.sun.tools.javac.util
 * @build combo.ComboTestHelper
 * @run main CheckAttributedTree -q -r -et ERRONEOUS .
 */

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

import javax.lang.model.element.Element;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import javax.tools.JavaFileObject;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskEvent.Kind;
import com.sun.source.util.TaskListener;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.EndPosTable;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCBreak;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.tree.JCTree.JCImport;
import com.sun.tools.javac.tree.TreeInfo;
import com.sun.tools.javac.tree.TreeScanner;
import com.sun.tools.javac.util.Pair;

import static com.sun.tools.javac.tree.JCTree.Tag.*;

import combo.ComboTestHelper;
import combo.ComboInstance;
import combo.ComboTestHelper.IgnoreMode;

/**
 * Utility and test program to check validity of tree positions for tree nodes.
 * The program can be run standalone, or as a jtreg test.  In standalone mode,
 * errors can be displayed in a gui viewer. For info on command line args,
 * run program with no args.
 *
 * <p>
 * jtreg: Note that by using the -r switch in the test description below, this test
 * will process all java files in the langtools/test directory, thus implicitly
 * covering any new language features that may be tested in this test suite.
 */

public class CheckAttributedTree {
    /**
     * Main entry point.
     * If test.src is set, program runs in jtreg mode, and will throw an Error
     * if any errors arise, otherwise System.exit will be used, unless the gui
     * viewer is being used. In jtreg mode, the default base directory for file
     * args is the value of ${test.src}. In jtreg mode, the -r option can be
     * given to change the default base directory to the root test directory.
     */
    public static void main(String... args) throws Exception {
        String testSrc = System.getProperty("test.src");
        File baseDir = (testSrc == null) ? null : new File(testSrc);
        boolean ok = new CheckAttributedTree().run(baseDir, args);
        if (!ok) {
            if (testSrc != null)  // jtreg mode
                throw new Error("failed");
            else
                System.exit(1);
        }
        System.err.println("total number of compilations " + totalNumberOfCompilations);
        System.err.println("number of failed compilations " + numberOfFailedCompilations);
    }

    static private int totalNumberOfCompilations = 0;
    static private int numberOfFailedCompilations = 0;

    /**
     * Run the program. A base directory can be provided for file arguments.
     * In jtreg mode, the -r option can be given to change the default base
     * directory to the test root directory. For other options, see usage().
     * @param baseDir base directory for any file arguments.
     * @param args command line args
     * @return true if successful or in gui mode
     */
    boolean run(File baseDir, String... args) throws Exception {
        if (args.length == 0) {
            usage(System.out);
            return true;
        }

        List<File> files = new ArrayList<File>();
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.equals("-encoding") && i + 1 < args.length)
                encoding = args[++i];
            else if (arg.equals("-gui"))
                gui = true;
            else if (arg.equals("-q"))
                quiet = true;
            else if (arg.equals("-v")) {
                verbose = true;
            }
            else if (arg.equals("-t") && i + 1 < args.length)
                tags.add(args[++i]);
            else if (arg.equals("-ef") && i + 1 < args.length)
                excludeFiles.add(new File(baseDir, args[++i]));
            else if (arg.equals("-et") && i + 1 < args.length)
                excludeTags.add(args[++i]);
            else if (arg.equals("-r")) {
                if (excludeFiles.size() > 0)
                    throw new Error("-r must be used before -ef");
                File d = baseDir;
                while (!new File(d, "TEST.ROOT").exists()) {
                    if (d == null)
                        throw new Error("cannot find TEST.ROOT");
                    d = d.getParentFile();
                }
                baseDir = d;
            }
            else if (arg.startsWith("-"))
                throw new Error("unknown option: " + arg);
            else {
                while (i < args.length)
                    files.add(new File(baseDir, args[i++]));
            }
        }

        ComboTestHelper<FileChecker> cth = new ComboTestHelper<>();
        cth.withIgnoreMode(IgnoreMode.IGNORE_ALL)
                .withFilter(FileChecker::checkFile)
                .withDimension("FILE", (x, file) -> x.file = file, getAllFiles(files))
                .run(FileChecker::new);

        if (fileCount.get() != 1)
            errWriter.println(fileCount + " files read");

        if (verbose) {
            System.out.println(errSWriter.toString());
        }

        return (gui || !cth.info().hasFailures());
    }

    File[] getAllFiles(List<File> roots) throws IOException {
        long now = System.currentTimeMillis();
        ArrayList<File> buf = new ArrayList<>();
        for (File file : roots) {
            Files.walkFileTree(file.toPath(), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    buf.add(file.toFile());
                    return FileVisitResult.CONTINUE;
                }
            });
        }
        long delta = System.currentTimeMillis() - now;
        System.err.println("All files = " + buf.size() + " " + delta);
        return buf.toArray(new File[buf.size()]);
    }

    /**
     * Print command line help.
     * @param out output stream
     */
    void usage(PrintStream out) {
        out.println("Usage:");
        out.println("  java CheckAttributedTree options... files...");
        out.println("");
        out.println("where options include:");
        out.println("-q        Quiet: don't report on inapplicable files");
        out.println("-gui      Display returns in a GUI viewer");
        out.println("-v        Verbose: report on files as they are being read");
        out.println("-t tag    Limit checks to tree nodes with this tag");
        out.println("          Can be repeated if desired");
        out.println("-ef file  Exclude file or directory");
        out.println("-et tag   Exclude tree nodes with given tag name");
        out.println("");
        out.println("files may be directories or files");
        out.println("directories will be scanned recursively");
        out.println("non java files, or java files which cannot be parsed, will be ignored");
        out.println("");
    }

    class FileChecker extends ComboInstance<FileChecker> {

        File file;

        boolean checkFile() {
            if (!file.exists()) {
                error("File not found: " + file);
                return false;
            }
            if (excludeFiles.contains(file)) {
                if (!quiet)
                    error("File " + file + " excluded");
                return false;
            }
            if (!file.getName().endsWith(".java")) {
                if (!quiet)
                    error("File " + file + " ignored");
                return false;
            }

            return true;
        }

        public void doWork() {
            if (!file.exists()) {
                error("File not found: " + file);
            }
            if (excludeFiles.contains(file)) {
                if (!quiet)
                    error("File " + file + " excluded");
                return;
            }
            if (!file.getName().endsWith(".java")) {
                if (!quiet)
                    error("File " + file + " ignored");
            }
            try {
                if (verbose)
                    errWriter.println(file);
                fileCount.incrementAndGet();
                NPETester p = new NPETester();
                readAndCheck(file, p::test);
            } catch (Throwable t) {
                if (!quiet) {
                    error("Error checking " + file + "\n" + t.getMessage());
                }
            }
        }

        /**
         * Read and check a file.
         * @param file the file to be read
         * @return the tree for the content of the file
         * @throws IOException if any IO errors occur
         * @throws AttributionException if any errors occur while analyzing the file
         */
        void readAndCheck(File file, BiConsumer<JCCompilationUnit, JCTree> c) throws IOException {
            Iterable<? extends JavaFileObject> files = fileManager().getJavaFileObjects(file);
            final List<Element> analyzedElems = new ArrayList<>();
            final List<CompilationUnitTree> trees = new ArrayList<>();
            totalNumberOfCompilations++;
            newCompilationTask()
                .withWriter(pw)
                    .withOption("--should-stop=ifError=ATTR")
                    .withOption("--should-stop=ifNoError=ATTR")
                    .withOption("-XDverboseCompilePolicy")
                    .withOption("-Xdoclint:none")
                    .withSource(files.iterator().next())
                    .withListener(new TaskListener() {
                        public void started(TaskEvent e) {
                            if (e.getKind() == TaskEvent.Kind.ANALYZE)
                            analyzedElems.add(e.getTypeElement());
                    }

                    public void finished(TaskEvent e) {
                        if (e.getKind() == Kind.PARSE)
                            trees.add(e.getCompilationUnit());
                    }
                }).analyze(res -> {
                Iterable<? extends Element> elems = res.get();
                if (elems.iterator().hasNext()) {
                    for (CompilationUnitTree t : trees) {
                       JCCompilationUnit cu = (JCCompilationUnit)t;
                       for (JCTree def : cu.defs) {
                           if (def.hasTag(CLASSDEF) &&
                                   analyzedElems.contains(((JCTree.JCClassDecl)def).sym)) {
                               c.accept(cu, def);
                           }
                       }
                    }
                } else {
                    numberOfFailedCompilations++;
                }
            });
        }

        /**
         * Report an error. When the program is complete, the program will either
         * exit or throw an Error if any errors have been reported.
         * @param msg the error message
         */
        void error(String msg) {
            System.err.println();
            System.err.println(msg);
            System.err.println();
            fail(msg);
        }

        /**
         * Main class for testing assertions concerning types/symbol
         * left uninitialized after attribution
         */
        private class NPETester extends TreeScanner {
            void test(JCCompilationUnit cut, JCTree tree) {
                sourcefile = cut.sourcefile;
                endPosTable = cut.endPositions;
                encl = new Info(tree, endPosTable);
                tree.accept(this);
            }

            @Override
            public void scan(JCTree tree) {
                if (tree == null ||
                        excludeTags.contains(treeUtil.nameFromTag(tree.getTag()))) {
                    return;
                }

                Info self = new Info(tree, endPosTable);
                if (mandatoryType(tree)) {
                    check(tree.type != null,
                            "'null' field 'type' found in tree ", self);
                    if (tree.type==null)
                        Thread.dumpStack();
                }

                Field errField = checkFields(tree);
                if (errField!=null) {
                    check(false,
                            "'null' field '" + errField.getName() + "' found in tree ", self);
                }

                Info prevEncl = encl;
                encl = self;
                tree.accept(this);
                encl = prevEncl;
            }

            private boolean mandatoryType(JCTree that) {
                return that instanceof JCTree.JCExpression ||
                        that.hasTag(VARDEF) ||
                        that.hasTag(METHODDEF) ||
                        that.hasTag(CLASSDEF);
            }

            private final List<String> excludedFields = Arrays.asList("varargsElement", "targetType");

            void check(boolean ok, String label, Info self) {
                if (!ok) {
                    if (gui) {
                        if (viewer == null)
                            viewer = new Viewer();
                        viewer.addEntry(sourcefile, label, encl, self);
                    }
                    error(label + self.toString() + " encl: " + encl.toString() +
                            " in file: " + sourcefile + "  " + self.tree);
                }
            }

            Field checkFields(JCTree t) {
                List<Field> fieldsToCheck = treeUtil.getFieldsOfType(t,
                        excludedFields,
                        Symbol.class,
                        Type.class);
                for (Field f : fieldsToCheck) {
                    try {
                        if (f.get(t) == null) {
                            return f;
                        }
                    }
                    catch (IllegalAccessException e) {
                        System.err.println("Cannot read field: " + f);
                        //swallow it
                    }
                }
                return null;
            }

            @Override
            public void visitImport(JCImport tree) { }

            @Override
            public void visitTopLevel(JCCompilationUnit tree) {
                scan(tree.defs);
            }

            @Override
            public void visitBreak(JCBreak tree) {
                if (tree.isValueBreak())
                    super.visitBreak(tree);
            }

            JavaFileObject sourcefile;
            EndPosTable endPosTable;
            Info encl;
        }
    }

    // See CR:  6982992 Tests CheckAttributedTree.java, JavacTreeScannerTest.java, and SourceTreeeScannerTest.java timeout
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);

    StringWriter errSWriter = new StringWriter();
    PrintWriter errWriter = new PrintWriter(errSWriter);

    /** Flag: don't report irrelevant files. */
    boolean quiet;
    /** Flag: show errors in GUI viewer. */
    boolean gui;
    /** The GUI viewer for errors. */
    Viewer viewer;
    /** Flag: report files as they are processed. */
    boolean verbose;
    /** Option: encoding for test files. */
    String encoding;
    /** The set of tags for tree nodes to be analyzed; if empty, all tree nodes
     * are analyzed. */
    Set<String> tags = new HashSet<String>();
    /** Set of files and directories to be excluded from analysis. */
    Set<File> excludeFiles = new HashSet<File>();
    /** Set of tag names to be excluded from analysis. */
    Set<String> excludeTags = new HashSet<String>();
    /** Utility class for trees */
    TreeUtil treeUtil = new TreeUtil();

    /**
     * Utility class providing easy access to position and other info for a tree node.
     */
    private class Info {
        Info() {
            tree = null;
            tag = ERRONEOUS;
            start = 0;
            pos = 0;
            end = Integer.MAX_VALUE;
        }

        Info(JCTree tree, EndPosTable endPosTable) {
            this.tree = tree;
            tag = tree.getTag();
            start = TreeInfo.getStartPos(tree);
            pos = tree.pos;
            end = TreeInfo.getEndPos(tree, endPosTable);
        }

        @Override
        public String toString() {
            return treeUtil.nameFromTag(tree.getTag()) + "[start:" + start + ",pos:" + pos + ",end:" + end + "]";
        }

        final JCTree tree;
        final JCTree.Tag tag;
        final int start;
        final int pos;
        final int end;
    }

    /**
     * Names for tree tags.
     */
    private static class TreeUtil {
        String nameFromTag(JCTree.Tag tag) {
            String name = tag.name();
            return (name == null) ? "??" : name;
        }

        List<Field> getFieldsOfType(JCTree t, List<String> excludeNames, Class<?>... types) {
            List<Field> buf = new ArrayList<Field>();
            for (Field f : t.getClass().getDeclaredFields()) {
                if (!excludeNames.contains(f.getName())) {
                    for (Class<?> type : types) {
                        if (type.isAssignableFrom(f.getType())) {
                            f.setAccessible(true);
                            buf.add(f);
                            break;
                        }
                    }
                }
            }
            return buf;
        }
    }

    /**
     * GUI viewer for issues found by TreePosTester. The viewer provides a drop
     * down list for selecting error conditions, a header area providing details
     * about an error, and a text area with the ranges of text highlighted as
     * appropriate.
     */
    private class Viewer extends JFrame {
        /**
         * Create a viewer.
         */
        Viewer() {
            initGUI();
        }

        /**
         * Add another entry to the list of errors.
         * @param file The file containing the error
         * @param check The condition that was being tested, and which failed
         * @param encl the enclosing tree node
         * @param self the tree node containing the error
         */
        void addEntry(JavaFileObject file, String check, Info encl, Info self) {
            Entry e = new Entry(file, check, encl, self);
            DefaultComboBoxModel m = (DefaultComboBoxModel) entries.getModel();
            m.addElement(e);
            if (m.getSize() == 1)
                entries.setSelectedItem(e);
        }

        /**
         * Initialize the GUI window.
         */
        private void initGUI() {
            JPanel head = new JPanel(new GridBagLayout());
            GridBagConstraints lc = new GridBagConstraints();
            GridBagConstraints fc = new GridBagConstraints();
            fc.anchor = GridBagConstraints.WEST;
            fc.fill = GridBagConstraints.HORIZONTAL;
            fc.gridwidth = GridBagConstraints.REMAINDER;

            entries = new JComboBox();
            entries.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    showEntry((Entry) entries.getSelectedItem());
                }
            });
            fc.insets.bottom = 10;
            head.add(entries, fc);
            fc.insets.bottom = 0;
            head.add(new JLabel("check:"), lc);
            head.add(checkField = createTextField(80), fc);
            fc.fill = GridBagConstraints.NONE;
            head.add(setBackground(new JLabel("encl:"), enclColor), lc);
            head.add(enclPanel = new InfoPanel(), fc);
            head.add(setBackground(new JLabel("self:"), selfColor), lc);
            head.add(selfPanel = new InfoPanel(), fc);
            add(head, BorderLayout.NORTH);

            body = new JTextArea();
            body.setFont(Font.decode(Font.MONOSPACED));
            body.addCaretListener(new CaretListener() {
                public void caretUpdate(CaretEvent e) {
                    int dot = e.getDot();
                    int mark = e.getMark();
                    if (dot == mark)
                        statusText.setText("dot: " + dot);
                    else
                        statusText.setText("dot: " + dot + ", mark:" + mark);
                }
            });
            JScrollPane p = new JScrollPane(body,
                    JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                    JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            p.setPreferredSize(new Dimension(640, 480));
            add(p, BorderLayout.CENTER);

            statusText = createTextField(80);
            add(statusText, BorderLayout.SOUTH);

            pack();
            setLocationRelativeTo(null); // centered on screen
            setVisible(true);
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        }

        /** Show an entry that has been selected. */
        private void showEntry(Entry e) {
            try {
                // update simple fields
                setTitle(e.file.getName());
                checkField.setText(e.check);
                enclPanel.setInfo(e.encl);
                selfPanel.setInfo(e.self);
                // show file text with highlights
                body.setText(e.file.getCharContent(true).toString());
                Highlighter highlighter = body.getHighlighter();
                highlighter.removeAllHighlights();
                addHighlight(highlighter, e.encl, enclColor);
                addHighlight(highlighter, e.self, selfColor);
                scroll(body, getMinPos(enclPanel.info, selfPanel.info));
            } catch (IOException ex) {
                body.setText("Cannot read " + e.file.getName() + ": " + e);
            }
        }

        /** Create a test field. */
        private JTextField createTextField(int width) {
            JTextField f = new JTextField(width);
            f.setEditable(false);
            f.setBorder(null);
            return f;
        }

        /** Add a highlighted region based on the positions in an Info object. */
        private void addHighlight(Highlighter h, Info info, Color c) {
            int start = info.start;
            int end = info.end;
            if (start == -1 && end == -1)
                return;
            if (start == -1)
                start = end;
            if (end == -1)
                end = start;
            try {
                h.addHighlight(info.start, info.end,
                        new DefaultHighlighter.DefaultHighlightPainter(c));
                if (info.pos != -1) {
                    Color c2 = new Color(c.getRed(), c.getGreen(), c.getBlue(), (int)(.4f * 255)); // 40%
                    h.addHighlight(info.pos, info.pos + 1,
                        new DefaultHighlighter.DefaultHighlightPainter(c2));
                }
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        }

        /** Get the minimum valid position in a set of info objects. */
        private int getMinPos(Info... values) {
            int i = Integer.MAX_VALUE;
            for (Info info: values) {
                if (info.start >= 0) i = Math.min(i, info.start);
                if (info.pos   >= 0) i = Math.min(i, info.pos);
                if (info.end   >= 0) i = Math.min(i, info.end);
            }
            return (i == Integer.MAX_VALUE) ? 0 : i;
        }

        /** Set the background on a component. */
        private JComponent setBackground(JComponent comp, Color c) {
            comp.setOpaque(true);
            comp.setBackground(c);
            return comp;
        }

        /** Scroll a text area to display a given position near the middle of the visible area. */
        private void scroll(final JTextArea t, final int pos) {
            // Using invokeLater appears to give text a chance to sort itself out
            // before the scroll happens; otherwise scrollRectToVisible doesn't work.
            // Maybe there's a better way to sync with the text...
            EventQueue.invokeLater(new Runnable() {
                public void run() {
                    try {
                        Rectangle r = t.modelToView(pos);
                        JScrollPane p = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, t);
                        r.y = Math.max(0, r.y - p.getHeight() * 2 / 5);
                        r.height += p.getHeight() * 4 / 5;
                        t.scrollRectToVisible(r);
                    } catch (BadLocationException ignore) {
                    }
                }
            });
        }

        private JComboBox entries;
        private JTextField checkField;
        private InfoPanel enclPanel;
        private InfoPanel selfPanel;
        private JTextArea body;
        private JTextField statusText;

        private Color selfColor = new Color(0.f, 1.f, 0.f, 0.2f); // 20% green
        private Color enclColor = new Color(1.f, 0.f, 0.f, 0.2f); // 20% red

        /** Panel to display an Info object. */
        private class InfoPanel extends JPanel {
            InfoPanel() {
                add(tagName = createTextField(20));
                add(new JLabel("start:"));
                add(addListener(start = createTextField(6)));
                add(new JLabel("pos:"));
                add(addListener(pos = createTextField(6)));
                add(new JLabel("end:"));
                add(addListener(end = createTextField(6)));
            }

            void setInfo(Info info) {
                this.info = info;
                tagName.setText(treeUtil.nameFromTag(info.tag));
                start.setText(String.valueOf(info.start));
                pos.setText(String.valueOf(info.pos));
                end.setText(String.valueOf(info.end));
            }

            JTextField addListener(final JTextField f) {
                f.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        body.setCaretPosition(Integer.valueOf(f.getText()));
                        body.getCaret().setVisible(true);
                    }
                });
                return f;
            }

            Info info;
            JTextField tagName;
            JTextField start;
            JTextField pos;
            JTextField end;
        }

        /** Object to record information about an error to be displayed. */
        private class Entry {
            Entry(JavaFileObject file, String check, Info encl, Info self) {
                this.file = file;
                this.check = check;
                this.encl = encl;
                this.self= self;
            }

            @Override
            public String toString() {
                return file.getName() + " " + check + " " + getMinPos(encl, self);
            }

            final JavaFileObject file;
            final String check;
            final Info encl;
            final Info self;
        }
    }

    /** Number of files that have been analyzed. */
    static AtomicInteger fileCount = new AtomicInteger();

}
