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

package jdk.internal.jshell.tool;

import jdk.jshell.SourceCodeAnalysis.Documentation;
import jdk.jshell.SourceCodeAnalysis.QualifiedNames;
import jdk.jshell.SourceCodeAnalysis.Suggestion;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;

import jdk.internal.shellsupport.doc.JavadocFormatter;
import jdk.internal.jshell.tool.StopDetectingInputStream.State;
import jdk.internal.misc.Signal;
import jdk.internal.misc.Signal.Handler;
import jdk.internal.org.jline.keymap.KeyMap;
import jdk.internal.org.jline.reader.Binding;
import jdk.internal.org.jline.reader.EOFError;
import jdk.internal.org.jline.reader.EndOfFileException;
import jdk.internal.org.jline.reader.History;
import jdk.internal.org.jline.reader.LineReader;
import jdk.internal.org.jline.reader.LineReader.Option;
import jdk.internal.org.jline.reader.Parser;
import jdk.internal.org.jline.reader.UserInterruptException;
import jdk.internal.org.jline.reader.Widget;
import jdk.internal.org.jline.reader.impl.LineReaderImpl;
import jdk.internal.org.jline.reader.impl.completer.ArgumentCompleter.ArgumentLine;
import jdk.internal.org.jline.reader.impl.history.DefaultHistory;
import jdk.internal.org.jline.terminal.impl.LineDisciplineTerminal;
import jdk.internal.org.jline.terminal.Attributes;
import jdk.internal.org.jline.terminal.Attributes.ControlChar;
import jdk.internal.org.jline.terminal.Attributes.LocalFlag;
import jdk.internal.org.jline.terminal.Size;
import jdk.internal.org.jline.terminal.Terminal;
import jdk.internal.org.jline.terminal.TerminalBuilder;
import jdk.internal.org.jline.utils.Display;
import jdk.internal.org.jline.utils.NonBlocking;
import jdk.internal.org.jline.utils.NonBlockingInputStreamImpl;
import jdk.internal.org.jline.utils.NonBlockingReader;
import jdk.jshell.ExpressionSnippet;
import jdk.jshell.Snippet;
import jdk.jshell.Snippet.SubKind;
import jdk.jshell.SourceCodeAnalysis.CompletionInfo;
import jdk.jshell.VarSnippet;

class ConsoleIOContext extends IOContext {

    private static final String HISTORY_LINE_PREFIX = "HISTORY_LINE_";

    final boolean allowIncompleteInputs;
    final JShellTool repl;
    final StopDetectingInputStream input;
    final Attributes originalAttributes;
    final LineReaderImpl in;
    final History userInputHistory = new DefaultHistory();
    final Instant historyLoad;

    String prefix = "";

    ConsoleIOContext(JShellTool repl, InputStream cmdin, PrintStream cmdout,
                     boolean interactive) throws Exception {
        this.repl = repl;
        Map<String, Object> variables = new HashMap<>();
        this.input = new StopDetectingInputStream(() -> repl.stop(),
                                                  ex -> repl.hard("Error on input: %s", ex));
        InputStream nonBlockingInput = new NonBlockingInputStreamImpl(null, input) {
            @Override
            public int readBuffered(byte[] b) throws IOException {
                return input.read(b);
            }
        };
        Terminal terminal;
        boolean allowIncompleteInputs = Boolean.getBoolean("jshell.test.allow.incomplete.inputs");
        Consumer<LineReaderImpl> setupReader = r -> {};
        if (cmdin != System.in) {
            if (System.getProperty("test.jdk") != null) {
                terminal = new TestTerminal(nonBlockingInput, cmdout);
            } else {
                Size size = null;
                terminal = new ProgrammaticInTerminal(nonBlockingInput, cmdout, interactive,
                                                      size);
                if (!interactive) {
                    setupReader = r -> r.unsetOpt(Option.BRACKETED_PASTE);
                    allowIncompleteInputs = true;
                }
            }
            input.setInputStream(cmdin);
        } else {
            terminal = TerminalBuilder.builder().inputStreamWrapper(in -> {
                input.setInputStream(in);
                return nonBlockingInput;
            }).build();
        }
        this.allowIncompleteInputs = allowIncompleteInputs;
        originalAttributes = terminal.getAttributes();
        Attributes noIntr = new Attributes(originalAttributes);
        noIntr.setControlChar(ControlChar.VINTR, 0);
        terminal.setAttributes(noIntr);
        terminal.enterRawMode();
        LineReaderImpl reader = new LineReaderImpl(terminal, "jshell", variables) {
            {
                //jline can handle the CONT signal on its own, but (currently) requires sun.misc for it
                try {
                    Signal.handle(new Signal("CONT"), new Handler() {
                        @Override public void handle(Signal sig) {
                            try {
                                handleSignal(jdk.internal.org.jline.terminal.Terminal.Signal.CONT);
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        }
                    });
                } catch (IllegalArgumentException ignored) {
                    //the CONT signal does not exist on this platform
                }
            }
            CompletionState completionState = new CompletionState();
            @Override
            protected boolean doComplete(CompletionType lst, boolean useMenu, boolean prefix) {
                return ConsoleIOContext.this.complete(completionState);
            }
            @Override
            public Binding readBinding(KeyMap<Binding> keys, KeyMap<Binding> local) {
                completionState.actionCount++;
                return super.readBinding(keys, local);
            }
            @Override
            protected boolean insertCloseParen() {
                Object oldIndent = getVariable(INDENTATION);
                try {
                    setVariable(INDENTATION, 0);
                    return super.insertCloseParen();
                } finally {
                    setVariable(INDENTATION, oldIndent);
                }
            }
            @Override
            protected boolean insertCloseSquare() {
                Object oldIndent = getVariable(INDENTATION);
                try {
                    setVariable(INDENTATION, 0);
                    return super.insertCloseSquare();
                } finally {
                    setVariable(INDENTATION, oldIndent);
                }
            }
        };

        setupReader.accept(reader);
        reader.setOpt(Option.DISABLE_EVENT_EXPANSION);

        reader.setParser((line, cursor, context) -> {
            if (!ConsoleIOContext.this.allowIncompleteInputs && !repl.isComplete(line)) {
                int pendingBraces = countPendingOpenBraces(line);
                throw new EOFError(cursor, cursor, line, null, pendingBraces, null);
            }
            return new ArgumentLine(line, cursor);
        });

        reader.getKeyMaps().get(LineReader.MAIN)
              .bind((Widget) () -> fixes(), FIXES_SHORTCUT);
        reader.getKeyMaps().get(LineReader.MAIN)
              .bind((Widget) () -> { throw new UserInterruptException(""); }, "\003");

        List<String> loadHistory = new ArrayList<>();
        Stream.of(repl.prefs.keys())
              .filter(key -> key.startsWith(HISTORY_LINE_PREFIX))
              .sorted()
              .map(key -> repl.prefs.get(key))
              .forEach(loadHistory::add);

        for (ListIterator<String> it = loadHistory.listIterator(); it.hasNext(); ) {
            String current = it.next();

            int trailingBackSlashes = countTrailintBackslashes(current);
            boolean continuation = trailingBackSlashes % 2 != 0;
            current = current.substring(0, current.length() - trailingBackSlashes / 2 - (continuation ? 1 : 0));
            if (continuation && it.hasNext()) {
                String next = it.next();
                it.remove();
                it.previous();
                current += "\n" + next;
            }

            it.set(current);
        }

        historyLoad = Instant.MIN;
        loadHistory.forEach(line -> reader.getHistory().add(historyLoad, line));

        in = reader;
    }

    @Override
    public String readLine(String firstLinePrompt, String continuationPrompt,
                           boolean firstLine, String prefix) throws IOException, InputInterruptedException {
        assert firstLine || allowIncompleteInputs;
        this.prefix = prefix;
        try {
            in.setVariable(LineReader.SECONDARY_PROMPT_PATTERN, continuationPrompt);
            return in.readLine(firstLine ? firstLinePrompt : continuationPrompt);
        } catch (UserInterruptException ex) {
            throw (InputInterruptedException) new InputInterruptedException().initCause(ex);
        } catch (EndOfFileException ex) {
            return null;
        }
    }

    @Override
    public boolean interactiveOutput() {
        return true;
    }

    @Override
    public Iterable<String> history(boolean currentSession) {
        return StreamSupport.stream(getHistory().spliterator(), false)
                            .filter(entry -> !currentSession || !historyLoad.equals(entry.time()))
                            .map(History.Entry::line)
                            .toList();
    }

    @Override
    public void close() throws IOException {
        //save history:
        for (String key : repl.prefs.keys()) {
            if (key.startsWith(HISTORY_LINE_PREFIX)) {
                repl.prefs.remove(key);
            }
        }
        Collection<String> savedHistory =
            StreamSupport.stream(in.getHistory().spliterator(), false)
                         .map(History.Entry::line)
                         .flatMap(this::toSplitEntries)
                         .toList();
        if (!savedHistory.isEmpty()) {
            int len = (int) Math.ceil(Math.log10(savedHistory.size()+1));
            String format = HISTORY_LINE_PREFIX + "%0" + len + "d";
            int index = 0;
            for (String historyLine : savedHistory) {
                repl.prefs.put(String.format(format, index++), historyLine);
            }
        }
        repl.prefs.flush();
        try {
            in.getTerminal().setAttributes(originalAttributes);
            in.getTerminal().close();
        } catch (Exception ex) {
            throw new IOException(ex);
        }
        input.shutdown();
    }

    private Stream<String> toSplitEntries(String entry) {
        String[] lines = entry.split("\n");
        List<String> result = new ArrayList<>();

        for (int i = 0; i < lines.length; i++) {
            StringBuilder historyLine = new StringBuilder(lines[i]);
            int trailingBackSlashes = countTrailintBackslashes(historyLine);
            for (int j = 0; j < trailingBackSlashes; j++) {
                historyLine.append("\\");
            }
            if (i + 1 < lines.length) {
                historyLine.append("\\");
            }
            result.add(historyLine.toString());
        }

        return result.stream();
    }

    private int countTrailintBackslashes(CharSequence text) {
        int count = 0;

        for (int i = text.length() - 1; i >= 0; i--) {
            if (text.charAt(i) == '\\') {
                count++;
            } else {
                break;
            }
        }

        return count;
    }

    @Override
    public void setIndent(int indent) {
        in.variable(LineReader.INDENTATION, indent);
    }

    private static final String FIXES_SHORTCUT = "\033\133\132"; //Shift-TAB

    private static final String LINE_SEPARATOR = System.getProperty("line.separator");
    private static final String LINE_SEPARATORS2 = LINE_SEPARATOR + LINE_SEPARATOR;

    /*XXX:*/private static final int AUTOPRINT_THRESHOLD = 100;
    @SuppressWarnings("fallthrough")
    private boolean complete(CompletionState completionState) {
        //The completion has multiple states (invoked by subsequent presses of <tab>).
        //On the first invocation in a given sequence, all steps are precomputed
        //and placed into the todo list (completionState.todo). The todo list is
        //then followed on both the first and subsequent completion invocations:
        try {
            String text = in.getBuffer().toString();
            int cursor = in.getBuffer().cursor();

            List<CompletionTask> todo = completionState.todo;

            if (todo.isEmpty() || completionState.actionCount != 1) {
                ConsoleIOContextTestSupport.willComputeCompletion();
                int[] anchor = new int[] {-1};
                List<Suggestion> suggestions;
                List<String> doc;
                boolean command = prefix.isEmpty() && text.startsWith("/");
                if (command) {
                    suggestions = repl.commandCompletionSuggestions(text, cursor, anchor);
                    doc = repl.commandDocumentation(text, cursor, true);
                } else {
                    int prefixLength = prefix.length();
                    suggestions = repl.analysis.completionSuggestions(prefix + text, cursor + prefixLength, anchor);
                    anchor[0] -= prefixLength;
                    doc = repl.analysis.documentation(prefix + text, cursor + prefix.length(), false)
                                       .stream()
                                       .map(Documentation::signature)
                                       .toList();
                }
                long smartCount = suggestions.stream().filter(Suggestion::matchesType).count();
                boolean hasSmart = smartCount > 0 && smartCount <= /*in.getAutoprintThreshold()*/AUTOPRINT_THRESHOLD;
                boolean hasBoth = hasSmart &&
                                  suggestions.stream()
                                             .map(s -> s.matchesType())
                                             .distinct()
                                             .count() == 2;
                boolean tooManyItems = suggestions.size() > /*in.getAutoprintThreshold()*/AUTOPRINT_THRESHOLD;
                CompletionTask ordinaryCompletion;
                List<? extends CharSequence> ordinaryCompletionToShow;

                if (hasBoth) {
                    ordinaryCompletionToShow =
                        suggestions.stream()
                                   .filter(Suggestion::matchesType)
                                   .map(Suggestion::continuation)
                                   .distinct()
                                   .toList();
                } else {
                    ordinaryCompletionToShow =
                        suggestions.stream()
                                   .map(Suggestion::continuation)
                                   .distinct()
                                   .toList();
                }

                if (ordinaryCompletionToShow.isEmpty()) {
                    ordinaryCompletion = new ContinueCompletionTask();
                } else {
                    Optional<String> prefixOpt =
                            suggestions.stream()
                                       .map(Suggestion::continuation)
                                       .reduce(ConsoleIOContext::commonPrefix);

                    String prefix =
                            prefixOpt.orElse("").substring(cursor - anchor[0]);

                    if (!prefix.isEmpty() && !command) {
                        //the completion will fill in the prefix, which will invalidate
                        //the documentation, avoid adding documentation tasks into the
                        //todo list:
                        doc = List.of();
                    }

                    ordinaryCompletion =
                            new OrdinaryCompletionTask(ordinaryCompletionToShow,
                                                       prefix,
                                                       !command && !doc.isEmpty(),
                                                       hasBoth);
                }

                CompletionTask allCompletion = new AllSuggestionsCompletionTask(suggestions, anchor[0]);

                todo = new ArrayList<>();

                //the main decission tree:
                if (command) {
                    CompletionTask shortDocumentation = new CommandSynopsisTask(doc);
                    CompletionTask fullDocumentation = new CommandFullDocumentationTask(todo);

                    if (!doc.isEmpty()) {
                        if (tooManyItems) {
                            todo.add(new NoopCompletionTask());
                            todo.add(allCompletion);
                        } else {
                            todo.add(ordinaryCompletion);
                        }
                        todo.add(shortDocumentation);
                        todo.add(fullDocumentation);
                    } else {
                        todo.add(new NoSuchCommandCompletionTask());
                    }
                } else {
                    if (doc.isEmpty()) {
                        if (hasSmart) {
                            todo.add(ordinaryCompletion);
                        } else if (tooManyItems) {
                            todo.add(new NoopCompletionTask());
                        }
                        if (!hasSmart || hasBoth) {
                            todo.add(allCompletion);
                        }
                    } else {
                        CompletionTask shortDocumentation = new ExpressionSignaturesTask(doc);
                        CompletionTask fullDocumentation = new ExpressionJavadocTask(todo);

                        if (hasSmart) {
                            todo.add(ordinaryCompletion);
                        }
                        todo.add(shortDocumentation);
                        if (!hasSmart || hasBoth) {
                            todo.add(allCompletion);
                        }
                        if (tooManyItems) {
                            todo.add(todo.size() - 1, fullDocumentation);
                        } else {
                            todo.add(fullDocumentation);
                        }
                    }
                }
            }

            boolean success = false;
            boolean repaint = true;

            OUTER: while (!todo.isEmpty()) {
                CompletionTask.Result result = todo.remove(0).perform(text, cursor);

                switch (result) {
                    case CONTINUE:
                        break;
                    case SKIP_NOREPAINT:
                        repaint = false;
                    case SKIP:
                        todo.clear();
                        //intentional fall-through
                    case FINISH:
                        success = true;
                        //intentional fall-through
                    case NO_DATA:
                        if (!todo.isEmpty()) {
                            in.getTerminal().writer().println();
                            in.getTerminal().writer().println(todo.get(0).description());
                        }
                        break OUTER;
                }
            }

            completionState.actionCount = 0;
            completionState.todo = todo;

            if (repaint) {
                in.redrawLine();
                in.flush();
            }

            return success;
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private CompletionTask.Result doPrintFullDocumentation(List<CompletionTask> todo, List<String> doc, boolean command) {
        if (doc != null && !doc.isEmpty()) {
            Terminal term = in.getTerminal();
            int pageHeight = term.getHeight() - NEEDED_LINES;
            List<CompletionTask> thisTODO = new ArrayList<>();

            for (Iterator<String> docIt = doc.iterator(); docIt.hasNext(); ) {
                String currentDoc = docIt.next();
                String[] lines = currentDoc.split("\n");
                int firstLine = 0;

                while (firstLine < lines.length) {
                    boolean first = firstLine == 0;
                    String[] thisPageLines =
                            Arrays.copyOfRange(lines,
                                               firstLine,
                                               Math.min(firstLine + pageHeight, lines.length));

                    thisTODO.add(new CompletionTask() {
                        @Override
                        public String description() {
                            String key =  !first ? "jshell.console.see.next.page"
                                                 : command ? "jshell.console.see.next.command.doc"
                                                           : "jshell.console.see.next.javadoc";

                            return repl.getResourceString(key);
                        }

                        @Override
                        public Result perform(String text, int cursor) throws IOException {
                            in.getTerminal().writer().println();
                            for (String line : thisPageLines) {
                                in.getTerminal().writer().println(line);
                            }
                            return Result.FINISH;
                        }
                    });

                    firstLine += pageHeight;
                }
            }

            todo.addAll(0, thisTODO);

            return CompletionTask.Result.CONTINUE;
        }

        return CompletionTask.Result.FINISH;
    }
    //where:
        private static final int NEEDED_LINES = 4;

    private static String commonPrefix(String str1, String str2) {
        for (int i = 0; i < str2.length(); i++) {
            if (!str1.startsWith(str2.substring(0, i + 1))) {
                return str2.substring(0, i);
            }
        }

        return str2;
    }

    private interface CompletionTask {
        public String description();
        public Result perform(String text, int cursor) throws IOException;

        enum Result {
            NO_DATA,
            CONTINUE,
            FINISH,
            SKIP,
            SKIP_NOREPAINT;
        }
    }

    private final class NoopCompletionTask implements CompletionTask {

        @Override
        public String description() {
            throw new UnsupportedOperationException("Should not get here.");
        }

        @Override
        public Result perform(String text, int cursor) throws IOException {
            return Result.FINISH;
        }

    }

    private final class NoSuchCommandCompletionTask implements CompletionTask {

        @Override
        public String description() {
            throw new UnsupportedOperationException("Should not get here.");
        }

        @Override
        public Result perform(String text, int cursor) throws IOException {
            in.getTerminal().writer().println();
            in.getTerminal().writer().println(repl.getResourceString("jshell.console.no.such.command"));
            in.getTerminal().writer().println();
            return Result.SKIP;
        }

    }

    private final class OrdinaryCompletionTask implements CompletionTask {
        private final List<? extends CharSequence> toShow;
        private final String prefix;
        private final boolean cont;
        private final boolean showSmart;

        public OrdinaryCompletionTask(List<? extends CharSequence> toShow,
                                      String prefix,
                                      boolean cont,
                                      boolean showSmart) {
            this.toShow = toShow;
            this.prefix = prefix;
            this.cont = cont;
            this.showSmart = showSmart;
        }

        @Override
        public String description() {
            throw new UnsupportedOperationException("Should not get here.");
        }

        @Override
        public Result perform(String text, int cursor) throws IOException {
            in.putString(prefix);

            boolean showItems = toShow.size() > 1 || showSmart;

            if (showItems) {
                in.getTerminal().writer().println();
                printColumns(toShow);
            }

            if (!prefix.isEmpty())
                return showItems ? Result.FINISH : Result.SKIP_NOREPAINT;

            return cont ? Result.CONTINUE : Result.FINISH;
        }

    }

    private final class AllSuggestionsCompletionTask implements CompletionTask {
        private final List<Suggestion> suggestions;
        private final int anchor;

        public AllSuggestionsCompletionTask(List<Suggestion> suggestions,
                                            int anchor) {
            this.suggestions = suggestions;
            this.anchor = anchor;
        }

        @Override
        public String description() {
            if (suggestions.size() <= /*in.getAutoprintThreshold()*/AUTOPRINT_THRESHOLD) {
                return repl.getResourceString("jshell.console.completion.all.completions");
            } else {
                return repl.messageFormat("jshell.console.completion.all.completions.number", suggestions.size());
            }
        }

        @Override
        public Result perform(String text, int cursor) throws IOException {
            List<String> candidates =
                    suggestions.stream()
                               .map(Suggestion::continuation)
                               .distinct()
                               .toList();

            Optional<String> prefix =
                    candidates.stream()
                              .reduce(ConsoleIOContext::commonPrefix);

            String prefixStr = prefix.map(str -> str.substring(cursor - anchor)).orElse("");
            in.putString(prefixStr);
            if (candidates.size() > 1) {
                in.getTerminal().writer().println();
                printColumns(candidates);
            }
            return suggestions.isEmpty() ? Result.NO_DATA : Result.FINISH;
        }

    }

    private void printColumns(List<? extends CharSequence> candidates) {
        if (candidates.isEmpty()) return ;
        int size = candidates.stream().mapToInt(CharSequence::length).max().getAsInt() + 3;
        int columns = in.getTerminal().getWidth() / size;
        int c = 0;
        for (CharSequence cand : candidates) {
            in.getTerminal().writer().print(cand);
            for (int s = cand.length(); s < size; s++) {
                in.getTerminal().writer().print(" ");
            }
            if (++c == columns) {
                in.getTerminal().writer().println();
                c = 0;
            }
        }
        if (c != 0) {
            in.getTerminal().writer().println();
        }
    }

    private final class CommandSynopsisTask implements CompletionTask {

        private final List<String> synopsis;

        public CommandSynopsisTask(List<String> synposis) {
            this.synopsis = synposis;
        }

        @Override
        public String description() {
            return repl.getResourceString("jshell.console.see.synopsis");
        }

        @Override
        public Result perform(String text, int cursor) throws IOException {
//            try {
                in.getTerminal().writer().println();
                in.getTerminal().writer().println(synopsis.stream()
                                   .map(l -> l.replaceAll("\n", LINE_SEPARATOR))
                                   .collect(Collectors.joining(LINE_SEPARATORS2)));
//            } catch (IOException ex) {
//                throw new IllegalStateException(ex);
//            }
            return Result.FINISH;
        }

    }

    private final class CommandFullDocumentationTask implements CompletionTask {

        private final List<CompletionTask> todo;

        public CommandFullDocumentationTask(List<CompletionTask> todo) {
            this.todo = todo;
        }

        @Override
        public String description() {
            return repl.getResourceString("jshell.console.see.full.documentation");
        }

        @Override
        public Result perform(String text, int cursor) throws IOException {
            List<String> fullDoc = repl.commandDocumentation(text, cursor, false);
            return doPrintFullDocumentation(todo, fullDoc, true);
        }

    }

    private final class ExpressionSignaturesTask implements CompletionTask {

        private final List<String> doc;

        public ExpressionSignaturesTask(List<String> doc) {
            this.doc = doc;
        }

        @Override
        public String description() {
            throw new UnsupportedOperationException("Should not get here.");
        }

        @Override
        public Result perform(String text, int cursor) throws IOException {
            in.getTerminal().writer().println();
            in.getTerminal().writer().println(repl.getResourceString("jshell.console.completion.current.signatures"));
            in.getTerminal().writer().println(doc.stream().collect(Collectors.joining(LINE_SEPARATOR)));
            return Result.FINISH;
        }

    }

    private final class ExpressionJavadocTask implements CompletionTask {

        private final List<CompletionTask> todo;

        public ExpressionJavadocTask(List<CompletionTask> todo) {
            this.todo = todo;
        }

        @Override
        public String description() {
            return repl.getResourceString("jshell.console.see.documentation");
        }

        @Override
        public Result perform(String text, int cursor) throws IOException {
            //schedule showing javadoc:
            Terminal term = in.getTerminal();
            JavadocFormatter formatter = new JavadocFormatter(term.getWidth(),
                                                              true);
            Function<Documentation, String> convertor = d -> formatter.formatJavadoc(d.signature(), d.javadoc()) +
                             (d.javadoc() == null ? repl.messageFormat("jshell.console.no.javadoc")
                                                  : "");
            List<String> doc = repl.analysis.documentation(prefix + text, cursor + prefix.length(), true)
                                            .stream()
                                            .map(convertor)
                                            .toList();
            return doPrintFullDocumentation(todo, doc, false);
        }

    }

    private static class ContinueCompletionTask implements ConsoleIOContext.CompletionTask {

        @Override
        public String description() {
            throw new UnsupportedOperationException("Should not get here.");
        }

        @Override
        public CompletionTask.Result perform(String text, int cursor) throws IOException {
            return CompletionTask.Result.CONTINUE;
        }
    }

    @Override
    public boolean terminalEditorRunning() {
        Terminal terminal = in.getTerminal();
        return !terminal.getAttributes().getLocalFlag(LocalFlag.ICANON);
    }

    @Override
    public void suspend() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void beforeUserCode() {
        synchronized (this) {
            inputBytes = null;
        }
        input.setState(State.BUFFER);
    }

    @Override
    public void afterUserCode() {
        input.setState(State.WAIT);
    }

    @Override
    public void replaceLastHistoryEntry(String source) {
        var it = in.getHistory().iterator();
        while (it.hasNext()) {
            it.next();
        }
        it.remove();
        in.getHistory().add(source);
        in.getHistory().resetIndex();
    }

    private static final long ESCAPE_TIMEOUT = 100;

    private boolean fixes() {
        try {
            int c = in.getTerminal().reader().read();

            if (c == (-1)) {
                return true; //TODO: true or false???
            }

            for (FixComputer computer : FIX_COMPUTERS) {
                if (computer.shortcut == c) {
                    fixes(computer);
                    return true; //TODO: true of false???
                }
            }

            readOutRemainingEscape(c);

            in.beep();
            in.getTerminal().writer().println();
            in.getTerminal().writer().println(repl.getResourceString("jshell.fix.wrong.shortcut"));
            in.redrawLine();
            in.flush();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return true;
    }

    private void readOutRemainingEscape(int c) throws IOException {
        if (c == '\033') {
            //escape, consume waiting input:
            NonBlockingReader inp = in.getTerminal().reader();

            while (inp.peek(ESCAPE_TIMEOUT) > 0) {
                inp.read();
            }
        }
    }

    //compute possible options/Fixes based on the selected FixComputer, present them to the user,
    //and perform the selected one:
    private void fixes(FixComputer computer) {
        String input = prefix + in.getBuffer().toString();
        int cursor = prefix.length() + in.getBuffer().cursor();
        FixResult candidates = computer.compute(repl, input, cursor);

        try {
            final boolean printError = candidates.error != null && !candidates.error.isEmpty();
            if (printError) {
                in.getTerminal().writer().println(candidates.error);
            }
            if (candidates.fixes.isEmpty()) {
                in.beep();
                if (printError) {
                    in.redrawLine();
                    in.flush();
                }
            } else if (candidates.fixes.size() == 1 && !computer.showMenu) {
                if (printError) {
                    in.redrawLine();
                    in.flush();
                }
                candidates.fixes.get(0).perform(in);
            } else {
                List<Fix> fixes = new ArrayList<>(candidates.fixes);
                fixes.add(0, new Fix() {
                    @Override
                    public String displayName() {
                        return repl.messageFormat("jshell.console.do.nothing");
                    }

                    @Override
                    public void perform(LineReaderImpl in) throws IOException {
                        in.redrawLine();
                    }
                });

                Map<Character, Fix> char2Fix = new HashMap<>();
                in.getTerminal().writer().println();
                for (int i = 0; i < fixes.size(); i++) {
                    Fix fix = fixes.get(i);
                    char2Fix.put((char) ('0' + i), fix);
                    in.getTerminal().writer().println("" + i + ": " + fixes.get(i).displayName());
                }
                in.getTerminal().writer().print(repl.messageFormat("jshell.console.choice"));
                in.flush();
                int read;

                read = in.readCharacter();

                Fix fix = char2Fix.get((char) read);

                if (fix == null) {
                    in.beep();
                    fix = fixes.get(0);
                }

                in.getTerminal().writer().println();

                fix.perform(in);

                in.flush();
            }
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private byte[] inputBytes;
    private int inputBytesPointer;

    @Override
    public synchronized int readUserInput() throws IOException {
        while (inputBytes == null || inputBytes.length <= inputBytesPointer) {
            History prevHistory = in.getHistory();
            boolean prevDisableCr = Display.DISABLE_CR;
            Parser prevParser = in.getParser();

            try {
                in.setParser((line, cursor, context) -> new ArgumentLine(line, cursor));
                input.setState(State.WAIT);
                Display.DISABLE_CR = true;
                in.setHistory(userInputHistory);
                inputBytes = (in.readLine("") + System.getProperty("line.separator")).getBytes();
                inputBytesPointer = 0;
            } catch (UserInterruptException ex) {
                throw new InterruptedIOException();
            } finally {
                in.setParser(prevParser);
                in.setHistory(prevHistory);
                input.setState(State.BUFFER);
                Display.DISABLE_CR = prevDisableCr;
            }
        }
        return inputBytes[inputBytesPointer++];
    }

    private int countPendingOpenBraces(String code) {
        int pendingBraces = 0;
        com.sun.tools.javac.util.Context ctx =
                new com.sun.tools.javac.util.Context();
        SimpleJavaFileObject source = new SimpleJavaFileObject(URI.create("mem://snippet"),
                                                               JavaFileObject.Kind.SOURCE) {
            @Override
            public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
                return code;
            }
        };
        ctx.put(DiagnosticListener.class, d -> {});
        com.sun.tools.javac.util.Log.instance(ctx).useSource(source);
        com.sun.tools.javac.parser.ScannerFactory scannerFactory =
                com.sun.tools.javac.parser.ScannerFactory.instance(ctx);
        com.sun.tools.javac.parser.Scanner scanner =
                scannerFactory.newScanner(code, false);

        while (true) {
            switch (scanner.token().kind) {
                case LBRACE: pendingBraces++; break;
                case RBRACE: pendingBraces--; break;
                case EOF: return pendingBraces;
            }
            scanner.nextToken();
        }
    }

    /**
     * A possible action which the user can choose to perform.
     */
    public interface Fix {
        /**
         * A name that should be shown to the user.
         */
        public String displayName();
        /**
         * Perform the given action.
         */
        public void perform(LineReaderImpl in) throws IOException;
    }

    /**
     * A factory for {@link Fix}es.
     */
    public abstract static class FixComputer {
        private final char shortcut;
        private final boolean showMenu;

        /**
         * Construct a new FixComputer. {@code shortcut} defines the key which should trigger this FixComputer.
         * If {@code showMenu} is {@code false}, and this computer returns exactly one {@code Fix},
         * no options will be show to the user, and the given {@code Fix} will be performed.
         */
        public FixComputer(char shortcut, boolean showMenu) {
            this.shortcut = shortcut;
            this.showMenu = showMenu;
        }

        /**
         * Compute possible actions for the given code.
         */
        public abstract FixResult compute(JShellTool repl, String code, int cursor);
    }

    /**
     * A list of {@code Fix}es with a possible error that should be shown to the user.
     */
    public static class FixResult {
        public final List<Fix> fixes;
        public final String error;

        public FixResult(List<Fix> fixes, String error) {
            this.fixes = fixes;
            this.error = error;
        }
    }

    private static final FixComputer[] FIX_COMPUTERS = new FixComputer[] {
        new FixComputer('v', false) { //compute "Introduce variable" Fix:
            private void performToVar(LineReaderImpl in, String type) throws IOException {
                in.redrawLine();
                in.getBuffer().cursor(0);
                in.putString(type + "  = ");
                in.getBuffer().cursor(in.getBuffer().cursor() - 3);
                in.flush();
            }

            @Override
            public FixResult compute(JShellTool repl, String code, int cursor) {
                String type = repl.analysis.analyzeType(code, cursor);
                if (type == null) {
                    return new FixResult(Collections.emptyList(), null);
                }
                List<Fix> fixes = new ArrayList<>();
                fixes.add(new Fix() {
                    @Override
                    public String displayName() {
                        return repl.messageFormat("jshell.console.create.variable");
                    }

                    @Override
                    public void perform(LineReaderImpl in) throws IOException {
                        performToVar(in, type);
                    }
                });
                int idx = type.lastIndexOf(".");
                if (idx > 0) {
                    String stype = type.substring(idx + 1);
                    QualifiedNames res = repl.analysis.listQualifiedNames(stype, stype.length());
                    if (res.isUpToDate() && res.getNames().contains(type)
                            && !res.isResolvable()) {
                        fixes.add(new Fix() {
                            @Override
                            public String displayName() {
                                return "import: " + type + ". " +
                                        repl.messageFormat("jshell.console.create.variable");
                            }

                            @Override
                            public void perform(LineReaderImpl in) throws IOException {
                                repl.processSource("import " + type + ";");
                                in.getTerminal().writer().println("Imported: " + type);
                                performToVar(in, stype);
                            }
                        });
                    }
                }
                return new FixResult(fixes, null);
            }
        },
        new FixComputer('m', false) { //compute "Introduce method" Fix:
            private void performToMethod(LineReaderImpl in, String type, String code) throws IOException {
                in.redrawLine();
                if (!code.trim().endsWith(";")) {
                    in.putString(";");
                }
                in.putString(" }");
                in.getBuffer().cursor(0);
                String afterCursor = type.equals("void")
                        ? "() { "
                        : "() { return ";
                in.putString(type + " " + afterCursor);
                // position the cursor where the method name should be entered (before parens)
                in.getBuffer().cursor(in.getBuffer().cursor() - afterCursor.length());
                in.flush();
            }

            private FixResult reject(JShellTool repl, String messageKey) {
                return new FixResult(Collections.emptyList(), repl.messageFormat(messageKey));
            }

            @Override
            public FixResult compute(JShellTool repl, String code, int cursor) {
                final String codeToCursor = code.substring(0, cursor);
                final String type;
                final CompletionInfo ci = repl.analysis.analyzeCompletion(codeToCursor);
                if (!ci.remaining().isEmpty()) {
                    return reject(repl, "jshell.console.exprstmt");
                }
                switch (ci.completeness()) {
                    case COMPLETE:
                    case COMPLETE_WITH_SEMI:
                    case CONSIDERED_INCOMPLETE:
                        break;
                    case EMPTY:
                        return reject(repl, "jshell.console.empty");
                    case DEFINITELY_INCOMPLETE:
                    case UNKNOWN:
                    default:
                        return reject(repl, "jshell.console.erroneous");
                }
                List<Snippet> snl = repl.analysis.sourceToSnippets(ci.source());
                if (snl.size() != 1) {
                    return reject(repl, "jshell.console.erroneous");
                }
                Snippet sn = snl.get(0);
                switch (sn.kind()) {
                    case EXPRESSION:
                        type = ((ExpressionSnippet) sn).typeName();
                        break;
                    case STATEMENT:
                        type = "void";
                        break;
                    case VAR:
                        if (sn.subKind() != SubKind.TEMP_VAR_EXPRESSION_SUBKIND) {
                            // only valid var is an expression turned into a temp var
                            return reject(repl, "jshell.console.exprstmt");
                        }
                        type = ((VarSnippet) sn).typeName();
                        break;
                    case IMPORT:
                    case METHOD:
                    case TYPE_DECL:
                        return reject(repl, "jshell.console.exprstmt");
                    case ERRONEOUS:
                    default:
                        return reject(repl, "jshell.console.erroneous");
                }
                List<Fix> fixes = new ArrayList<>();
                fixes.add(new Fix() {
                    @Override
                    public String displayName() {
                        return repl.messageFormat("jshell.console.create.method");
                    }

                    @Override
                    public void perform(LineReaderImpl in) throws IOException {
                        performToMethod(in, type, codeToCursor);
                    }
                });
                int idx = type.lastIndexOf(".");
                if (idx > 0) {
                    String stype = type.substring(idx + 1);
                    QualifiedNames res = repl.analysis.listQualifiedNames(stype, stype.length());
                    if (res.isUpToDate() && res.getNames().contains(type)
                            && !res.isResolvable()) {
                        fixes.add(new Fix() {
                            @Override
                            public String displayName() {
                                return "import: " + type + ". " +
                                        repl.messageFormat("jshell.console.create.method");
                            }

                            @Override
                            public void perform(LineReaderImpl in) throws IOException {
                                repl.processSource("import " + type + ";");
                                in.getTerminal().writer().println("Imported: " + type);
                                performToMethod(in, stype, codeToCursor);
                            }
                        });
                    }
                }
                return new FixResult(fixes, null);
            }
        },
        new FixComputer('i', true) { //compute "Add import" Fixes:
            @Override
            public FixResult compute(JShellTool repl, String code, int cursor) {
                QualifiedNames res = repl.analysis.listQualifiedNames(code, cursor);
                List<Fix> fixes = new ArrayList<>();
                for (String fqn : res.getNames()) {
                    fixes.add(new Fix() {
                        @Override
                        public String displayName() {
                            return "import: " + fqn;
                        }

                        @Override
                        public void perform(LineReaderImpl in) throws IOException {
                            repl.processSource("import " + fqn + ";");
                            in.getTerminal().writer().println("Imported: " + fqn);
                            in.redrawLine();
                        }
                    });
                }
                if (res.isResolvable()) {
                    return new FixResult(Collections.emptyList(),
                            repl.messageFormat("jshell.console.resolvable"));
                } else {
                    String error = "";
                    if (fixes.isEmpty()) {
                        error = repl.messageFormat("jshell.console.no.candidate");
                    }
                    if (!res.isUpToDate()) {
                        error += repl.messageFormat("jshell.console.incomplete");
                    }
                    return new FixResult(fixes, error);
                }
            }
        }
    };

    private History getHistory() {
        return in.getHistory();
    }

    private static class ProgrammaticInTerminal extends LineDisciplineTerminal {

        protected static final int DEFAULT_HEIGHT = 24;

        private final NonBlockingReader inputReader;
        private final Size bufferSize;

        public ProgrammaticInTerminal(InputStream input, OutputStream output,
                                       boolean interactive, Size size) throws Exception {
            this(input, output, interactive ? "ansi" : "dumb",
                 size != null ? size : new Size(80, DEFAULT_HEIGHT),
                 size != null ? size
                              : interactive ? new Size(80, DEFAULT_HEIGHT)
                                            : new Size(Integer.MAX_VALUE - 1, DEFAULT_HEIGHT));
        }

        protected ProgrammaticInTerminal(InputStream input, OutputStream output,
                                         String terminal, Size size, Size bufferSize) throws Exception {
            super("non-system-in", terminal, output, Charset.forName("UTF-8"));
            this.inputReader = NonBlocking.nonBlocking(getName(), input, encoding());
            Attributes a = new Attributes(getAttributes());
            a.setLocalFlag(LocalFlag.ECHO, false);
            setAttributes(attributes);
            setSize(size);
            this.bufferSize = bufferSize;
        }

        @Override
        public NonBlockingReader reader() {
            return inputReader;
        }

        @Override
        protected void doClose() throws IOException {
            super.doClose();
            slaveInput.close();
            inputReader.close();
        }

        @Override
        public Size getBufferSize() {
            return bufferSize;
        }
    }

    private static final class TestTerminal extends ProgrammaticInTerminal {
        private static Size computeSize() {
            int h = DEFAULT_HEIGHT;
            try {
                String hp = System.getProperty("test.terminal.height");
                if (hp != null && !hp.isEmpty() && System.getProperty("test.jdk") != null) {
                    h = Integer.parseInt(hp);
                }
            } catch (Throwable ex) {
                // ignore
            }
            return new Size(80, h);
        }
        public TestTerminal(InputStream input, OutputStream output) throws Exception {
            this(input, output, computeSize());
        }
        private TestTerminal(InputStream input, OutputStream output, Size size) throws Exception {
            super(input, output, "ansi", size, size);
        }
    }

    private static final class CompletionState {
        /**The number of actions since the last completion invocation. Will be 1 when completion is
         * invoked immediately after the last completion invocation.*/
        public int actionCount;
        /**Precomputed completion actions. Should only be reused if actionCount == 1.*/
        public List<CompletionTask> todo = Collections.emptyList();
    }

}
