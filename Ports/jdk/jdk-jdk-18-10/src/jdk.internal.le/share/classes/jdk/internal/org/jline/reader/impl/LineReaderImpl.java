/*
 * Copyright (c) 2002-2020, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package jdk.internal.org.jline.reader.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Flushable;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.lang.reflect.Constructor;
import java.time.Instant;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import jdk.internal.org.jline.keymap.BindingReader;
import jdk.internal.org.jline.keymap.KeyMap;
import jdk.internal.org.jline.reader.*;
import jdk.internal.org.jline.reader.Parser.ParseContext;
import jdk.internal.org.jline.reader.impl.history.DefaultHistory;
import jdk.internal.org.jline.terminal.*;
import jdk.internal.org.jline.terminal.Attributes.ControlChar;
import jdk.internal.org.jline.terminal.Terminal.Signal;
import jdk.internal.org.jline.terminal.Terminal.SignalHandler;
import jdk.internal.org.jline.terminal.impl.AbstractWindowsTerminal;
import jdk.internal.org.jline.utils.AttributedString;
import jdk.internal.org.jline.utils.AttributedStringBuilder;
import jdk.internal.org.jline.utils.AttributedStyle;
import jdk.internal.org.jline.utils.Curses;
import jdk.internal.org.jline.utils.Display;
import jdk.internal.org.jline.utils.InfoCmp.Capability;
import jdk.internal.org.jline.utils.Levenshtein;
import jdk.internal.org.jline.utils.Log;
import jdk.internal.org.jline.utils.Status;
import jdk.internal.org.jline.utils.WCWidth;

import static jdk.internal.org.jline.keymap.KeyMap.alt;
import static jdk.internal.org.jline.keymap.KeyMap.ctrl;
import static jdk.internal.org.jline.keymap.KeyMap.del;
import static jdk.internal.org.jline.keymap.KeyMap.esc;
import static jdk.internal.org.jline.keymap.KeyMap.range;
import static jdk.internal.org.jline.keymap.KeyMap.translate;

/**
 * A reader for terminal applications. It supports custom tab-completion,
 * saveable command history, and command line editing.
 *
 * @author <a href="mailto:mwp1@cornell.edu">Marc Prud'hommeaux</a>
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 * @author <a href="mailto:gnodet@gmail.com">Guillaume Nodet</a>
 */
@SuppressWarnings("StatementWithEmptyBody")
public class LineReaderImpl implements LineReader, Flushable
{
    public static final char NULL_MASK = 0;

    public static final int TAB_WIDTH = 4;


    public static final String DEFAULT_WORDCHARS = "*?_-.[]~=/&;!#$%^(){}<>";
    public static final String DEFAULT_REMOVE_SUFFIX_CHARS = " \t\n;&|";
    public static final String DEFAULT_COMMENT_BEGIN = "#";
    public static final String DEFAULT_SEARCH_TERMINATORS = "\033\012";
    public static final String DEFAULT_BELL_STYLE = "";
    public static final int    DEFAULT_LIST_MAX = 100;
    public static final int    DEFAULT_ERRORS = 2;
    public static final long   DEFAULT_BLINK_MATCHING_PAREN = 500L;
    public static final long   DEFAULT_AMBIGUOUS_BINDING = 1000L;
    public static final String DEFAULT_SECONDARY_PROMPT_PATTERN = "%M> ";
    public static final String DEFAULT_OTHERS_GROUP_NAME = "others";
    public static final String DEFAULT_ORIGINAL_GROUP_NAME = "original";
    public static final String DEFAULT_COMPLETION_STYLE_STARTING = "36";    // cyan
    public static final String DEFAULT_COMPLETION_STYLE_DESCRIPTION = "90"; // dark gray
    public static final String DEFAULT_COMPLETION_STYLE_GROUP = "35;1";     // magenta
    public static final String DEFAULT_COMPLETION_STYLE_SELECTION = "7";    // inverted
    public static final int    DEFAULT_INDENTATION = 0;
    public static final int    DEFAULT_FEATURES_MAX_BUFFER_SIZE = 1000;

    private static final int MIN_ROWS = 3;

    public static final String BRACKETED_PASTE_ON = "\033[?2004h";
    public static final String BRACKETED_PASTE_OFF = "\033[?2004l";
    public static final String BRACKETED_PASTE_BEGIN = "\033[200~";
    public static final String BRACKETED_PASTE_END = "\033[201~";

    public static final String FOCUS_IN_SEQ = "\033[I";
    public static final String FOCUS_OUT_SEQ = "\033[O";

    /**
     * Possible states in which the current readline operation may be in.
     */
    protected enum State {
        /**
         * The user is just typing away
         */
        NORMAL,
        /**
         * readLine should exit and return the buffer content
         */
        DONE,
        /**
         * readLine should exit and return empty String
         */
        IGNORE,
        /**
         * readLine should exit and throw an EOFException
         */
        EOF,
        /**
         * readLine should exit and throw an UserInterruptException
         */
        INTERRUPT
    }

    protected enum ViMoveMode {
        NORMAL,
        YANK,
        DELETE,
        CHANGE
    }

    protected enum BellType {
        NONE,
        AUDIBLE,
        VISIBLE
    }

    //
    // Constructor variables
    //

    /** The terminal to use */
    protected final Terminal terminal;
    /** The application name */
    protected final String appName;
    /** The terminal keys mapping */
    protected final Map<String, KeyMap<Binding>> keyMaps;

    //
    // Configuration
    //
    protected final Map<String, Object> variables;
    protected History history = new DefaultHistory();
    protected Completer completer = null;
    protected Highlighter highlighter = new DefaultHighlighter();
    protected Parser parser = new DefaultParser();
    protected Expander expander = new DefaultExpander();

    //
    // State variables
    //

    protected final Map<Option, Boolean> options = new HashMap<>();

    protected final Buffer buf = new BufferImpl();
    protected String tailTip = "";
    protected SuggestionType autosuggestion = SuggestionType.NONE;

    protected final Size size = new Size();

    protected AttributedString prompt = AttributedString.EMPTY;
    protected AttributedString rightPrompt = AttributedString.EMPTY;

    protected MaskingCallback maskingCallback;

    protected Map<Integer, String> modifiedHistory = new HashMap<>();
    protected Buffer historyBuffer = null;
    protected CharSequence searchBuffer;
    protected StringBuffer searchTerm = null;
    protected boolean searchFailing;
    protected boolean searchBackward;
    protected int searchIndex = -1;
    protected boolean doAutosuggestion;


    // Reading buffers
    protected final BindingReader bindingReader;


    /**
     * VI character find
     */
    protected int findChar;
    protected int findDir;
    protected int findTailAdd;
    /**
     * VI history string search
     */
    private int searchDir;
    private String searchString;

    /**
     * Region state
     */
    protected int regionMark;
    protected RegionType regionActive;

    private boolean forceChar;
    private boolean forceLine;

    /**
     * The vi yank buffer
     */
    protected String yankBuffer = "";

    protected ViMoveMode viMoveMode = ViMoveMode.NORMAL;

    protected KillRing killRing = new KillRing();

    protected UndoTree<Buffer> undo = new UndoTree<>(this::setBuffer);
    protected boolean isUndo;

    /**
     * State lock
     */
    protected final ReentrantLock lock = new ReentrantLock();
    /*
     * Current internal state of the line reader
     */
    protected State   state = State.DONE;
    protected final AtomicBoolean startedReading = new AtomicBoolean();
    protected boolean reading;

    protected Supplier<AttributedString> post;

    protected Map<String, Widget> builtinWidgets;
    protected Map<String, Widget> widgets;

    protected int count;
    protected int mult;
    protected int universal = 4;
    protected int repeatCount;
    protected boolean isArgDigit;

    protected ParsedLine parsedLine;

    protected boolean skipRedisplay;
    protected Display display;

    protected boolean overTyping = false;

    protected String keyMap;

    protected int smallTerminalOffset = 0;
    /*
     * accept-and-infer-next-history, accept-and-hold & accept-line-and-down-history
     */
    protected boolean nextCommandFromHistory = false;
    protected int nextHistoryId = -1;

    /*
     * execute commands from commandsBuffer
     */
    protected List<String> commandsBuffer = new ArrayList<>();

    public LineReaderImpl(Terminal terminal) throws IOException {
        this(terminal, null, null);
    }

    public LineReaderImpl(Terminal terminal, String appName) throws IOException {
        this(terminal, appName, null);
    }

    public LineReaderImpl(Terminal terminal, String appName, Map<String, Object> variables) {
        Objects.requireNonNull(terminal, "terminal can not be null");
        this.terminal = terminal;
        if (appName == null) {
            appName = "JLine";
        }
        this.appName = appName;
        if (variables != null) {
            this.variables = variables;
        } else {
            this.variables = new HashMap<>();
        }
        this.keyMaps = defaultKeyMaps();

        builtinWidgets = builtinWidgets();
        widgets = new HashMap<>(builtinWidgets);
        bindingReader = new BindingReader(terminal.reader());
        doDisplay();
    }

    public Terminal getTerminal() {
        return terminal;
    }

    public String getAppName() {
        return appName;
    }

    public Map<String, KeyMap<Binding>> getKeyMaps() {
        return keyMaps;
    }

    public KeyMap<Binding> getKeys() {
        return keyMaps.get(keyMap);
    }

    @Override
    public Map<String, Widget> getWidgets() {
        return widgets;
    }

    @Override
    public Map<String, Widget> getBuiltinWidgets() {
        return Collections.unmodifiableMap(builtinWidgets);
    }

    @Override
    public Buffer getBuffer() {
        return buf;
    }

    @Override
    public void setAutosuggestion(SuggestionType type) {
        this.autosuggestion = type;
    }

    @Override
    public SuggestionType getAutosuggestion() {
        return autosuggestion;
    }

    @Override
    public String getTailTip() {
        return tailTip;
    }

    @Override
    public void setTailTip(String tailTip) {
        this.tailTip = tailTip;
    }

    @Override
    public void runMacro(String macro) {
        bindingReader.runMacro(macro);
    }

    @Override
    public MouseEvent readMouseEvent() {
        return terminal.readMouseEvent(bindingReader::readCharacter);
    }

    /**
     * Set the completer.
     *
     * @param completer the completer to use
     */
    public void setCompleter(Completer completer) {
        this.completer = completer;
    }

    /**
     * Returns the completer.
     *
     * @return the completer
     */
    public Completer getCompleter() {
        return completer;
    }

    //
    // History
    //

    public void setHistory(final History history) {
        Objects.requireNonNull(history);
        this.history = history;
    }

    public History getHistory() {
        return history;
    }

    //
    // Highlighter
    //

    public void setHighlighter(Highlighter highlighter) {
        this.highlighter = highlighter;
    }

    public Highlighter getHighlighter() {
        return highlighter;
    }

    public Parser getParser() {
        return parser;
    }

    public void setParser(Parser parser) {
        this.parser = parser;
    }

    @Override
    public Expander getExpander() {
        return expander;
    }

    public void setExpander(Expander expander) {
        this.expander = expander;
    }

    //
    // Line Reading
    //

    /**
     * Read the next line and return the contents of the buffer.
     *
     * @return          A line that is read from the terminal, can never be null.
     */
    public String readLine() throws UserInterruptException, EndOfFileException {
        return readLine(null, null, (MaskingCallback) null, null);
    }

    /**
     * Read the next line with the specified character mask. If null, then
     * characters will be echoed. If 0, then no characters will be echoed.
     *
     * @param mask      The mask character, <code>null</code> or <code>0</code>.
     * @return          A line that is read from the terminal, can never be null.
     */
    public String readLine(Character mask) throws UserInterruptException, EndOfFileException {
        return readLine(null, null, mask, null);
    }

    /**
     * Read a line from the <i>in</i> {@link InputStream}, and return the line
     * (without any trailing newlines).
     *
     * @param prompt    The prompt to issue to the terminal, may be null.
     * @return          A line that is read from the terminal, can never be null.
     */
    public String readLine(String prompt) throws UserInterruptException, EndOfFileException {
        return readLine(prompt, null, (MaskingCallback) null, null);
    }

    /**
     * Read a line from the <i>in</i> {@link InputStream}, and return the line
     * (without any trailing newlines).
     *
     * @param prompt    The prompt to issue to the terminal, may be null.
     * @param mask      The mask character, <code>null</code> or <code>0</code>.
     * @return          A line that is read from the terminal, can never be null.
     */
    public String readLine(String prompt, Character mask) throws UserInterruptException, EndOfFileException {
        return readLine(prompt, null, mask, null);
    }

    /**
     * Read a line from the <i>in</i> {@link InputStream}, and return the line
     * (without any trailing newlines).
     *
     * @param prompt    The prompt to issue to the terminal, may be null.
     * @param mask      The mask character, <code>null</code> or <code>0</code>.
     * @param buffer    A string that will be set for editing.
     * @return          A line that is read from the terminal, can never be null.
     */
    public String readLine(String prompt, Character mask, String buffer) throws UserInterruptException, EndOfFileException {
        return readLine(prompt, null, mask, buffer);
    }

    /**
     * Read a line from the <i>in</i> {@link InputStream}, and return the line
     * (without any trailing newlines).
     *
     * @param prompt      The prompt to issue to the terminal, may be null.
     * @param rightPrompt The prompt to issue to the right of the terminal, may be null.
     * @param mask        The mask character, <code>null</code> or <code>0</code>.
     * @param buffer      A string that will be set for editing.
     * @return            A line that is read from the terminal, can never be null.
     */
    public String readLine(String prompt, String rightPrompt, Character mask, String buffer) throws UserInterruptException, EndOfFileException {
        return readLine(prompt, rightPrompt, mask != null ? new SimpleMaskingCallback(mask) : null, buffer);
    }

    /**
     * Read a line from the <i>in</i> {@link InputStream}, and return the line
     * (without any trailing newlines).
     *
     * @param prompt          The prompt to issue to the terminal, may be null.
     * @param rightPrompt     The prompt to issue to the right of the terminal, may be null.
     * @param maskingCallback The callback used to mask parts of the edited line.
     * @param buffer          A string that will be set for editing.
     * @return                A line that is read from the terminal, can never be null.
     */
    public String readLine(String prompt, String rightPrompt, MaskingCallback maskingCallback, String buffer) throws UserInterruptException, EndOfFileException {
        // prompt may be null
        // maskingCallback may be null
        // buffer may be null
        if (!commandsBuffer.isEmpty()) {
            String cmd = commandsBuffer.remove(0);
            boolean done = false;
            do {
                try {
                    parser.parse(cmd, cmd.length() + 1, ParseContext.ACCEPT_LINE);
                    done = true;
                } catch (EOFError e) {
                    if (commandsBuffer.isEmpty()) {
                        throw new IllegalArgumentException("Incompleted command: \n" + cmd);
                    }
                    cmd += "\n";
                    cmd += commandsBuffer.remove(0);
                } catch (SyntaxError e) {
                    done = true;
                } catch (Exception e) {
                    commandsBuffer.clear();
                    throw new IllegalArgumentException(e.getMessage());
                }
            } while (!done);
            AttributedStringBuilder sb = new AttributedStringBuilder();
            sb.styled(AttributedStyle::bold, cmd);
            sb.toAttributedString().println(terminal);
            terminal.flush();
            return finish(cmd);
        }

        if (!startedReading.compareAndSet(false, true)) {
            throw new IllegalStateException();
        }

        Thread readLineThread = Thread.currentThread();
        SignalHandler previousIntrHandler = null;
        SignalHandler previousWinchHandler = null;
        SignalHandler previousContHandler = null;
        Attributes originalAttributes = null;
        boolean dumb = isTerminalDumb();
        try {

            this.maskingCallback = maskingCallback;

            /*
             * This is the accumulator for VI-mode repeat count. That is, while in
             * move mode, if you type 30x it will delete 30 characters. This is
             * where the "30" is accumulated until the command is struck.
             */
            repeatCount = 0;
            mult = 1;
            regionActive = RegionType.NONE;
            regionMark = -1;

            smallTerminalOffset = 0;

            state = State.NORMAL;

            modifiedHistory.clear();

            setPrompt(prompt);
            setRightPrompt(rightPrompt);
            buf.clear();
            if (buffer != null) {
                buf.write(buffer);
            }
            if (nextCommandFromHistory && nextHistoryId > 0) {
                if (history.size() > nextHistoryId) {
                    history.moveTo(nextHistoryId);
                } else {
                    history.moveTo(history.last());
                }
                buf.write(history.current());
            } else {
                nextHistoryId = -1;
            }
            nextCommandFromHistory = false;
            undo.clear();
            parsedLine = null;
            keyMap = MAIN;

            if (history != null) {
                history.attach(this);
            }

            try {
                lock.lock();

                this.reading = true;

                previousIntrHandler = terminal.handle(Signal.INT, signal -> readLineThread.interrupt());
                previousWinchHandler = terminal.handle(Signal.WINCH, this::handleSignal);
                previousContHandler = terminal.handle(Signal.CONT, this::handleSignal);
                originalAttributes = terminal.enterRawMode();

                doDisplay();

                // Move into application mode
                if (!dumb) {
                    terminal.puts(Capability.keypad_xmit);
                    if (isSet(Option.AUTO_FRESH_LINE))
                        callWidget(FRESH_LINE);
                    if (isSet(Option.MOUSE))
                        terminal.trackMouse(Terminal.MouseTracking.Normal);
                    if (isSet(Option.BRACKETED_PASTE))
                        terminal.writer().write(BRACKETED_PASTE_ON);
                } else {
                    // For dumb terminals, we need to make sure that CR are ignored
                    Attributes attr = new Attributes(originalAttributes);
                    attr.setInputFlag(Attributes.InputFlag.IGNCR, true);
                    terminal.setAttributes(attr);
                }

                callWidget(CALLBACK_INIT);

                undo.newState(buf.copy());

                // Draw initial prompt
                redrawLine();
                redisplay();
            } finally {
                lock.unlock();
            }

            while (true) {

                KeyMap<Binding> local = null;
                if (isInViCmdMode() && regionActive != RegionType.NONE) {
                    local = keyMaps.get(VISUAL);
                }
                Binding o = readBinding(getKeys(), local);
                if (o == null) {
                    throw new EndOfFileException();
                }
                Log.trace("Binding: ", o);
                if (buf.length() == 0 && getLastBinding().charAt(0) == originalAttributes.getControlChar(ControlChar.VEOF)) {
                    throw new EndOfFileException();
                }

                // If this is still false after handling the binding, then
                // we reset our repeatCount to 0.
                isArgDigit = false;
                // Every command that can be repeated a specified number
                // of times, needs to know how many times to repeat, so
                // we figure that out here.
                count = ((repeatCount == 0) ? 1 : repeatCount) * mult;
                // Reset undo/redo flag
                isUndo = false;
                // Reset region after a paste
                if (regionActive == RegionType.PASTE) {
                    regionActive = RegionType.NONE;
                }

                try {
                    lock.lock();
                    // Get executable widget
                    Buffer copy = buf.length() <= getInt(FEATURES_MAX_BUFFER_SIZE, DEFAULT_FEATURES_MAX_BUFFER_SIZE) ? buf.copy() : null;
                    Widget w = getWidget(o);
                    if (!w.apply()) {
                        beep();
                    }
                    if (!isUndo && copy != null && buf.length() <= getInt(FEATURES_MAX_BUFFER_SIZE, DEFAULT_FEATURES_MAX_BUFFER_SIZE)
                            && !copy.toString().equals(buf.toString())) {
                        undo.newState(buf.copy());
                    }

                    switch (state) {
                        case DONE:
                            return finishBuffer();
                        case IGNORE:
                            return "";
                        case EOF:
                            throw new EndOfFileException();
                        case INTERRUPT:
                            throw new UserInterruptException(buf.toString());
                    }

                    if (!isArgDigit) {
                        /*
                         * If the operation performed wasn't a vi argument
                         * digit, then clear out the current repeatCount;
                         */
                        repeatCount = 0;
                        mult = 1;
                    }

                    if (!dumb) {
                        redisplay();
                    }
                } finally {
                    lock.unlock();
                }
            }
        } catch (IOError e) {
            if (e.getCause() instanceof InterruptedIOException) {
                throw new UserInterruptException(buf.toString());
            } else {
                throw e;
            }
        }
        finally {
            try {
                lock.lock();

                this.reading = false;

                cleanup();
                if (originalAttributes != null) {
                    terminal.setAttributes(originalAttributes);
                }
                if (previousIntrHandler != null) {
                    terminal.handle(Signal.INT, previousIntrHandler);
                }
                if (previousWinchHandler != null) {
                    terminal.handle(Signal.WINCH, previousWinchHandler);
                }
                if (previousContHandler != null) {
                    terminal.handle(Signal.CONT, previousContHandler);
                }
            } finally {
                lock.unlock();
            }
            startedReading.set(false);
        }
    }

    private boolean isTerminalDumb() {
        return Terminal.TYPE_DUMB.equals(terminal.getType())
                || Terminal.TYPE_DUMB_COLOR.equals(terminal.getType());
    }

    private void doDisplay() {
        // Cache terminal size for the duration of the call to readLine()
        // It will eventually be updated with WINCH signals
        size.copy(terminal.getBufferSize());

        display = new Display(terminal, false);
        if (size.getRows() == 0 || size.getColumns() == 0) {
            display.resize(1, Integer.MAX_VALUE);
        } else {
            display.resize(size.getRows(), size.getColumns());
        }
        if (isSet(Option.DELAY_LINE_WRAP))
            display.setDelayLineWrap(true);
    }

    @Override
    public void printAbove(String str) {
        try {
            lock.lock();

            boolean reading = this.reading;
            if (reading) {
                display.update(Collections.emptyList(), 0);
            }
            if (str.endsWith("\n") || str.endsWith("\n\033[m") || str.endsWith("\n\033[0m")) {
                terminal.writer().print(str);
            } else {
                terminal.writer().println(str);
            }
            if (reading) {
                redisplay(false);
            }
            terminal.flush();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void printAbove(AttributedString str) {
        printAbove(str.toAnsi(terminal));
    }

    @Override
    public boolean isReading() {
        try {
            lock.lock();
            return reading;
        } finally {
            lock.unlock();
        }
    }

    /* Make sure we position the cursor on column 0 */
    protected boolean freshLine() {
        boolean wrapAtEol = terminal.getBooleanCapability(Capability.auto_right_margin);
        boolean delayedWrapAtEol = wrapAtEol && terminal.getBooleanCapability(Capability.eat_newline_glitch);
        AttributedStringBuilder sb = new AttributedStringBuilder();
        sb.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.BLACK + AttributedStyle.BRIGHT));
        sb.append("~");
        sb.style(AttributedStyle.DEFAULT);
        if (!wrapAtEol || delayedWrapAtEol) {
            for (int i = 0; i < size.getColumns() - 1; i++) {
                sb.append(" ");
            }
            sb.append(KeyMap.key(terminal, Capability.carriage_return));
            sb.append(" ");
            sb.append(KeyMap.key(terminal, Capability.carriage_return));
        } else {
            // Given the terminal will wrap automatically,
            // we need to print one less than needed.
            // This means that the last character will not
            // be overwritten, and that's why we're using
            // a clr_eol first if possible.
            String el = terminal.getStringCapability(Capability.clr_eol);
            if (el != null) {
                Curses.tputs(sb, el);
            }
            for (int i = 0; i < size.getColumns() - 2; i++) {
                sb.append(" ");
            }
            sb.append(KeyMap.key(terminal, Capability.carriage_return));
            sb.append(" ");
            sb.append(KeyMap.key(terminal, Capability.carriage_return));
        }
        sb.print(terminal);
        return true;
    }

    @Override
    public void callWidget(String name) {
        try {
            lock.lock();
            if (!reading) {
                throw new IllegalStateException("Widgets can only be called during a `readLine` call");
            }
            try {
                Widget w;
                if (name.startsWith(".")) {
                    w = builtinWidgets.get(name.substring(1));
                } else {
                    w = widgets.get(name);
                }
                if (w != null) {
                    w.apply();
                }
            } catch (Throwable t) {
                Log.debug("Error executing widget '", name, "'", t);
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Clear the line and redraw it.
     * @return <code>true</code>
     */
    public boolean redrawLine() {
        display.reset();
        return true;
    }

    /**
     * Write out the specified string to the buffer and the output stream.
     * @param str the char sequence to write in the buffer
     */
    public void putString(final CharSequence str) {
        buf.write(str, overTyping);
    }

    /**
     * Flush the terminal output stream. This is important for printout out single
     * characters (like a buf.backspace or keyboard) that we want the terminal to
     * handle immediately.
     */
    public void flush() {
        terminal.flush();
    }

    public boolean isKeyMap(String name) {
        return keyMap.equals(name);
    }

    /**
     * Read a character from the terminal.
     *
     * @return the character, or -1 if an EOF is received.
     */
    public int readCharacter() {
        if (lock.isHeldByCurrentThread()) {
            try {
                lock.unlock();
                return bindingReader.readCharacter();
            } finally {
                lock.lock();
            }
        } else {
            return bindingReader.readCharacter();
        }
    }

    public int peekCharacter(long timeout) {
        return bindingReader.peekCharacter(timeout);
    }

    protected <T> T doReadBinding(KeyMap<T> keys, KeyMap<T> local) {
        if (lock.isHeldByCurrentThread()) {
            try {
                lock.unlock();
                return bindingReader.readBinding(keys, local);
            } finally {
                lock.lock();
            }
        } else {
            return bindingReader.readBinding(keys, local);
        }
    }

    protected String doReadStringUntil(String sequence) {
        if (lock.isHeldByCurrentThread()) {
            try {
                lock.unlock();
                return bindingReader.readStringUntil(sequence);
            } finally {
                lock.lock();
            }
        } else {
            return bindingReader.readStringUntil(sequence);
        }
    }

    /**
     * Read from the input stream and decode an operation from the key map.
     *
     * The input stream will be read character by character until a matching
     * binding can be found.  Characters that can't possibly be matched to
     * any binding will be discarded.
     *
     * @param keys the KeyMap to use for decoding the input stream
     * @return the decoded binding or <code>null</code> if the end of
     *         stream has been reached
     */
    public Binding readBinding(KeyMap<Binding> keys) {
        return readBinding(keys, null);
    }

    public Binding readBinding(KeyMap<Binding> keys, KeyMap<Binding> local) {
        Binding o = doReadBinding(keys, local);
        /*
         * The kill ring keeps record of whether or not the
         * previous command was a yank or a kill. We reset
         * that state here if needed.
         */
        if (o instanceof Reference) {
            String ref = ((Reference) o).name();
            if (!YANK_POP.equals(ref) && !YANK.equals(ref)) {
                killRing.resetLastYank();
            }
            if (!KILL_LINE.equals(ref) && !KILL_WHOLE_LINE.equals(ref)
                    && !BACKWARD_KILL_WORD.equals(ref) && !KILL_WORD.equals(ref)) {
                killRing.resetLastKill();
            }
        }
        return o;
    }

    @Override
    public ParsedLine getParsedLine() {
        return parsedLine;
    }

    @Override
    public String getLastBinding() {
        return bindingReader.getLastBinding();
    }

    @Override
    public String getSearchTerm() {
        return searchTerm != null ? searchTerm.toString() : null;
    }

    @Override
    public RegionType getRegionActive() {
        return regionActive;
    }

    @Override
    public int getRegionMark() {
        return regionMark;
    }

    //
    // Key Bindings
    //

    /**
     * Sets the current keymap by name. Supported keymaps are "emacs",
     * "viins", "vicmd".
     * @param name The name of the keymap to switch to
     * @return true if the keymap was set, or false if the keymap is
     *    not recognized.
     */
    public boolean setKeyMap(String name) {
        KeyMap<Binding> map = keyMaps.get(name);
        if (map == null) {
            return false;
        }
        this.keyMap = name;
        if (reading) {
            callWidget(CALLBACK_KEYMAP);
        }
        return true;
    }

    /**
     * Returns the name of the current key mapping.
     * @return the name of the key mapping. This will be the canonical name
     *   of the current mode of the key map and may not reflect the name that
     *   was used with {@link #setKeyMap(String)}.
     */
    public String getKeyMap() {
        return keyMap;
    }

    @Override
    public LineReader variable(String name, Object value) {
        variables.put(name, value);
        return this;
    }

    @Override
    public Map<String, Object> getVariables() {
        return variables;
    }

    @Override
    public Object getVariable(String name) {
        return variables.get(name);
    }

    @Override
    public void setVariable(String name, Object value) {
        variables.put(name, value);
    }

    @Override
    public LineReader option(Option option, boolean value) {
        options.put(option, value);
        return this;
    }

    @Override
    public boolean isSet(Option option) {
        Boolean b = options.get(option);
        return b != null ? b : option.isDef();
    }

    @Override
    public void setOpt(Option option) {
        options.put(option, Boolean.TRUE);
    }

    @Override
    public void unsetOpt(Option option) {
        options.put(option, Boolean.FALSE);
    }

    @Override
    public void addCommandsInBuffer(Collection<String> commands) {
        commandsBuffer.addAll(commands);
    }

    @Override
    public void editAndAddInBuffer(File file) throws Exception {
        Constructor<?> ctor = Class.forName("org.jline.builtins.Nano").getConstructor(Terminal.class, File.class);
        Editor editor = (Editor) ctor.newInstance(terminal, new File(file.getParent()));
        editor.setRestricted(true);
        editor.open(Arrays.asList(file.getName()));
        editor.run();
        BufferedReader br = new BufferedReader(new FileReader(file));
        String line;
        commandsBuffer.clear();
        while ((line = br.readLine()) != null) {
            commandsBuffer.add(line);
        }
        br.close();
    }

    //
    // Widget implementation
    //

    /**
     * Clear the buffer and add its contents to the history.
     *
     * @return the former contents of the buffer.
     */
    protected String finishBuffer() {
        return finish(buf.toString());
    }

    protected String finish(String str) {
        String historyLine = str;

        if (!isSet(Option.DISABLE_EVENT_EXPANSION)) {
            StringBuilder sb = new StringBuilder();
            boolean escaped = false;
            for (int i = 0; i < str.length(); i++) {
                char ch = str.charAt(i);
                if (escaped) {
                    escaped = false;
                    if (ch != '\n') {
                        sb.append(ch);
                    }
                } else if (parser.isEscapeChar(ch)) {
                    escaped = true;
                } else {
                    sb.append(ch);
                }
            }
            str = sb.toString();
        }

        if (maskingCallback != null) {
            historyLine = maskingCallback.history(historyLine);
        }

        // we only add it to the history if the buffer is not empty
        if (historyLine != null && historyLine.length() > 0 ) {
            history.add(Instant.now(), historyLine);
        }
        return str;
    }

    protected void handleSignal(Signal signal) {
        doAutosuggestion = false;
        if (signal == Signal.WINCH) {
            Status status = Status.getStatus(terminal, false);
            if (status != null) {
                status.hardReset();
            }
            size.copy(terminal.getBufferSize());
            display.resize(size.getRows(), size.getColumns());
            // restores prompt but also prevents scrolling in consoleZ, see #492
            // redrawLine();
            redisplay();
        }
        else if (signal == Signal.CONT) {
            terminal.enterRawMode();
            size.copy(terminal.getBufferSize());
            display.resize(size.getRows(), size.getColumns());
            terminal.puts(Capability.keypad_xmit);
            redrawLine();
            redisplay();
        }
    }

    @SuppressWarnings("unchecked")
    protected Widget getWidget(Object binding) {
        Widget w;
        if (binding instanceof Widget) {
            w = (Widget) binding;
        } else if (binding instanceof Macro) {
            String macro = ((Macro) binding).getSequence();
            w = () -> {
                bindingReader.runMacro(macro);
                return true;
            };
        } else if (binding instanceof Reference) {
            String name = ((Reference) binding).name();
            w = widgets.get(name);
            if (w == null) {
                w = () -> {
                    post = () -> new AttributedString("No such widget `" + name + "'");
                    return false;
                };
            }
        } else {
            w = () -> {
                post = () -> new AttributedString("Unsupported widget");
                return false;
            };
        }
        return w;
    }

    //
    // Helper methods
    //

    public void setPrompt(final String prompt) {
        this.prompt = (prompt == null ? AttributedString.EMPTY
                       : expandPromptPattern(prompt, 0, "", 0));
    }

    public void setRightPrompt(final String rightPrompt) {
        this.rightPrompt = (rightPrompt == null ? AttributedString.EMPTY
                            : expandPromptPattern(rightPrompt, 0, "", 0));
    }

    protected void setBuffer(Buffer buffer) {
        buf.copyFrom(buffer);
    }

    /**
     * Set the current buffer's content to the specified {@link String}. The
     * visual terminal will be modified to show the current buffer.
     *
     * @param buffer the new contents of the buffer.
     */
    protected void setBuffer(final String buffer) {
        buf.clear();
        buf.write(buffer);
    }

    /**
     * This method is calling while doing a delete-to ("d"), change-to ("c"),
     * or yank-to ("y") and it filters out only those movement operations
     * that are allowable during those operations. Any operation that isn't
     * allow drops you back into movement mode.
     *
     * @param op The incoming operation to remap
     * @return The remaped operation
     */
    protected String viDeleteChangeYankToRemap (String op) {
        switch (op) {
            case SEND_BREAK:
            case BACKWARD_CHAR:
            case FORWARD_CHAR:
            case END_OF_LINE:
            case VI_MATCH_BRACKET:
            case VI_DIGIT_OR_BEGINNING_OF_LINE:
            case NEG_ARGUMENT:
            case DIGIT_ARGUMENT:
            case VI_BACKWARD_CHAR:
            case VI_BACKWARD_WORD:
            case VI_FORWARD_CHAR:
            case VI_FORWARD_WORD:
            case VI_FORWARD_WORD_END:
            case VI_FIRST_NON_BLANK:
            case VI_GOTO_COLUMN:
            case VI_DELETE:
            case VI_YANK:
            case VI_CHANGE:
            case VI_FIND_NEXT_CHAR:
            case VI_FIND_NEXT_CHAR_SKIP:
            case VI_FIND_PREV_CHAR:
            case VI_FIND_PREV_CHAR_SKIP:
            case VI_REPEAT_FIND:
            case VI_REV_REPEAT_FIND:
                return op;

            default:
                return VI_CMD_MODE;
        }
    }

    protected int switchCase(int ch) {
        if (Character.isUpperCase(ch)) {
            return Character.toLowerCase(ch);
        } else if (Character.isLowerCase(ch)) {
            return Character.toUpperCase(ch);
        } else {
            return ch;
        }
    }

    /**
     * @return true if line reader is in the middle of doing a change-to
     *   delete-to or yank-to.
     */
    protected boolean isInViMoveOperation() {
        return viMoveMode != ViMoveMode.NORMAL;
    }

    protected boolean isInViChangeOperation() {
        return viMoveMode == ViMoveMode.CHANGE;
    }

    protected boolean isInViCmdMode() {
        return VICMD.equals(keyMap);
    }


    //
    // Movement
    //

    protected boolean viForwardChar() {
        if (count < 0) {
            return callNeg(this::viBackwardChar);
        }
        int lim = findeol();
        if (isInViCmdMode() && !isInViMoveOperation()) {
            lim--;
        }
        if (buf.cursor() >= lim) {
            return false;
        }
        while (count-- > 0 && buf.cursor() < lim) {
            buf.move(1);
        }
        return true;
    }

    protected boolean viBackwardChar() {
        if (count < 0) {
            return callNeg(this::viForwardChar);
        }
        int lim = findbol();
        if (buf.cursor() == lim) {
            return false;
        }
        while (count-- > 0 && buf.cursor() > 0) {
            buf.move(-1);
            if (buf.currChar() == '\n') {
                buf.move(1);
                break;
            }
        }
        return true;
    }


    //
    // Word movement
    //

    protected boolean forwardWord() {
        if (count < 0) {
            return callNeg(this::backwardWord);
        }
        while (count-- > 0) {
            while (buf.cursor() < buf.length() && isWord(buf.currChar())) {
                buf.move(1);
            }
            if (isInViChangeOperation() && count == 0) {
                break;
            }
            while (buf.cursor() < buf.length() && !isWord(buf.currChar())) {
                buf.move(1);
            }
        }
        return true;
    }

    protected boolean viForwardWord() {
        if (count < 0) {
            return callNeg(this::backwardWord);
        }
        while (count-- > 0) {
            if (isViAlphaNum(buf.currChar())) {
                while (buf.cursor() < buf.length() && isViAlphaNum(buf.currChar())) {
                    buf.move(1);
                }
            } else {
                while (buf.cursor() < buf.length()
                        && !isViAlphaNum(buf.currChar())
                        && !isWhitespace(buf.currChar())) {
                    buf.move(1);
                }
            }
            if (isInViChangeOperation() && count == 0) {
                return true;
            }
            int nl = buf.currChar() == '\n' ? 1 : 0;
            while (buf.cursor() < buf.length()
                    && nl < 2
                    && isWhitespace(buf.currChar())) {
                buf.move(1);
                nl += buf.currChar() == '\n' ? 1 : 0;
            }
        }
        return true;
    }

    protected boolean viForwardBlankWord() {
        if (count < 0) {
            return callNeg(this::viBackwardBlankWord);
        }
        while (count-- > 0) {
            while (buf.cursor() < buf.length() && !isWhitespace(buf.currChar())) {
                buf.move(1);
            }
            if (isInViChangeOperation() && count == 0) {
                return true;
            }
            int nl = buf.currChar() == '\n' ? 1 : 0;
            while (buf.cursor() < buf.length()
                    && nl < 2
                    && isWhitespace(buf.currChar())) {
                buf.move(1);
                nl += buf.currChar() == '\n' ? 1 : 0;
            }
        }
        return true;
    }

    protected boolean emacsForwardWord() {
        if (count < 0) {
            return callNeg(this::emacsBackwardWord);
        }
        while (count-- > 0) {
            while (buf.cursor() < buf.length() && !isWord(buf.currChar())) {
                buf.move(1);
            }
            if (isInViChangeOperation() && count == 0) {
                return true;
            }
            while (buf.cursor() < buf.length() && isWord(buf.currChar())) {
                buf.move(1);
            }
        }
        return true;
    }

    protected boolean viForwardBlankWordEnd() {
        if (count < 0) {
            return false;
        }
        while (count-- > 0) {
            while (buf.cursor() < buf.length()) {
                buf.move(1);
                if (!isWhitespace(buf.currChar())) {
                    break;
                }
            }
            while (buf.cursor() < buf.length()) {
                buf.move(1);
                if (isWhitespace(buf.currChar())) {
                    break;
                }
            }
        }
        return true;
    }

    protected boolean viForwardWordEnd() {
        if (count < 0) {
            return callNeg(this::backwardWord);
        }
        while (count-- > 0) {
            while (buf.cursor() < buf.length()) {
                if (!isWhitespace(buf.nextChar())) {
                    break;
                }
                buf.move(1);
            }
            if (buf.cursor() < buf.length()) {
                if (isViAlphaNum(buf.nextChar())) {
                    buf.move(1);
                    while (buf.cursor() < buf.length() && isViAlphaNum(buf.nextChar())) {
                        buf.move(1);
                    }
                } else {
                    buf.move(1);
                    while (buf.cursor() < buf.length() && !isViAlphaNum(buf.nextChar()) && !isWhitespace(buf.nextChar())) {
                        buf.move(1);
                    }
                }
            }
        }
        if (buf.cursor() < buf.length() && isInViMoveOperation()) {
            buf.move(1);
        }
        return true;
    }

    protected boolean backwardWord() {
        if (count < 0) {
            return callNeg(this::forwardWord);
        }
        while (count-- > 0) {
            while (buf.cursor() > 0 && !isWord(buf.atChar(buf.cursor() - 1))) {
                buf.move(-1);
            }
            while (buf.cursor() > 0 && isWord(buf.atChar(buf.cursor() - 1))) {
                buf.move(-1);
            }
        }
        return true;
    }

    protected boolean viBackwardWord() {
        if (count < 0) {
            return callNeg(this::backwardWord);
        }
        while (count-- > 0) {
            int nl = 0;
            while (buf.cursor() > 0) {
                buf.move(-1);
                if (!isWhitespace(buf.currChar())) {
                    break;
                }
                nl += buf.currChar() == '\n' ? 1 : 0;
                if (nl == 2) {
                    buf.move(1);
                    break;
                }
            }
            if (buf.cursor() > 0) {
                if (isViAlphaNum(buf.currChar())) {
                    while (buf.cursor() > 0) {
                        if (!isViAlphaNum(buf.prevChar())) {
                            break;
                        }
                        buf.move(-1);
                    }
                } else {
                    while (buf.cursor() > 0) {
                        if (isViAlphaNum(buf.prevChar()) || isWhitespace(buf.prevChar())) {
                            break;
                        }
                        buf.move(-1);
                    }
                }
            }
        }
        return true;
    }

    protected boolean viBackwardBlankWord() {
        if (count < 0) {
            return callNeg(this::viForwardBlankWord);
        }
        while (count-- > 0) {
            while (buf.cursor() > 0) {
                buf.move(-1);
                if (!isWhitespace(buf.currChar())) {
                    break;
                }
            }
            while (buf.cursor() > 0) {
                buf.move(-1);
                if (isWhitespace(buf.currChar())) {
                    break;
                }
            }
        }
        return true;
    }

    protected boolean viBackwardWordEnd() {
        if (count < 0) {
            return callNeg(this::viForwardWordEnd);
        }
        while (count-- > 0 && buf.cursor() > 1) {
            int start;
            if (isViAlphaNum(buf.currChar())) {
                start = 1;
            } else if (!isWhitespace(buf.currChar())) {
                start = 2;
            } else {
                start = 0;
            }
            while (buf.cursor() > 0) {
                boolean same = (start != 1) && isWhitespace(buf.currChar());
                if (start != 0) {
                    same |= isViAlphaNum(buf.currChar());
                }
                if (same == (start == 2)) {
                    break;
                }
                buf.move(-1);
            }
            while (buf.cursor() > 0 && isWhitespace(buf.currChar())) {
                buf.move(-1);
            }
        }
        return true;
    }

    protected boolean viBackwardBlankWordEnd() {
        if (count < 0) {
            return callNeg(this::viForwardBlankWordEnd);
        }
        while (count-- > 0) {
            while (buf.cursor() > 0 && !isWhitespace(buf.currChar())) {
                buf.move(-1);
            }
            while (buf.cursor() > 0 && isWhitespace(buf.currChar())) {
                buf.move(-1);
            }
        }
        return true;
    }

    protected boolean emacsBackwardWord() {
        if (count < 0) {
            return callNeg(this::emacsForwardWord);
        }
        while (count-- > 0) {
            while (buf.cursor() > 0) {
                buf.move(-1);
                if (isWord(buf.currChar())) {
                    break;
                }
            }
            while (buf.cursor() > 0) {
                buf.move(-1);
                if (!isWord(buf.currChar())) {
                    break;
                }
            }
        }
        return true;
    }

    protected boolean backwardDeleteWord() {
        if (count < 0) {
            return callNeg(this::deleteWord);
        }
        int cursor = buf.cursor();
        while (count-- > 0) {
            while (cursor > 0 && !isWord(buf.atChar(cursor - 1))) {
                cursor--;
            }
            while (cursor > 0 && isWord(buf.atChar(cursor - 1))) {
                cursor--;
            }
        }
        buf.backspace(buf.cursor() - cursor);
        return true;
    }

    protected boolean viBackwardKillWord() {
        if (count < 0) {
            return false;
        }
        int lim = findbol();
        int x = buf.cursor();
        while (count-- > 0) {
            while (x > lim && isWhitespace(buf.atChar(x - 1))) {
                x--;
            }
            if (x > lim) {
                if (isViAlphaNum(buf.atChar(x - 1))) {
                    while (x > lim && isViAlphaNum(buf.atChar(x - 1))) {
                        x--;
                    }
                } else {
                    while (x > lim && !isViAlphaNum(buf.atChar(x - 1)) && !isWhitespace(buf.atChar(x - 1))) {
                        x--;
                    }
                }
            }
        }
        killRing.addBackwards(buf.substring(x, buf.cursor()));
        buf.backspace(buf.cursor() - x);
        return true;
    }

    protected boolean backwardKillWord() {
        if (count < 0) {
            return callNeg(this::killWord);
        }
        int x = buf.cursor();
        while (count-- > 0) {
            while (x > 0 && !isWord(buf.atChar(x - 1))) {
                x--;
            }
            while (x > 0 && isWord(buf.atChar(x - 1))) {
                x--;
            }
        }
        killRing.addBackwards(buf.substring(x, buf.cursor()));
        buf.backspace(buf.cursor() - x);
        return true;
    }

    protected boolean copyPrevWord() {
        if (count <= 0) {
            return false;
        }
        int t1, t0 = buf.cursor();
        while (true) {
            t1 = t0;
            while (t0 > 0 && !isWord(buf.atChar(t0 - 1))) {
                t0--;
            }
            while (t0 > 0 && isWord(buf.atChar(t0 - 1))) {
                t0--;
            }
            if (--count == 0) {
                break;
            }
            if (t0 == 0) {
                return false;
            }
        }
        buf.write(buf.substring(t0, t1));
        return true;
    }

    protected boolean upCaseWord() {
        int count = Math.abs(this.count);
        int cursor = buf.cursor();
        while (count-- > 0) {
            while (buf.cursor() < buf.length() && !isWord(buf.currChar())) {
                buf.move(1);
            }
            while (buf.cursor() < buf.length() && isWord(buf.currChar())) {
                buf.currChar(Character.toUpperCase(buf.currChar()));
                buf.move(1);
            }
        }
        if (this.count < 0) {
            buf.cursor(cursor);
        }
        return true;
    }

    protected boolean downCaseWord() {
        int count = Math.abs(this.count);
        int cursor = buf.cursor();
        while (count-- > 0) {
            while (buf.cursor() < buf.length() && !isWord(buf.currChar())) {
                buf.move(1);
            }
            while (buf.cursor() < buf.length() && isWord(buf.currChar())) {
                buf.currChar(Character.toLowerCase(buf.currChar()));
                buf.move(1);
            }
        }
        if (this.count < 0) {
            buf.cursor(cursor);
        }
        return true;
    }

    protected boolean capitalizeWord() {
        int count = Math.abs(this.count);
        int cursor = buf.cursor();
        while (count-- > 0) {
            boolean first = true;
            while (buf.cursor() < buf.length() && !isWord(buf.currChar())) {
                buf.move(1);
            }
            while (buf.cursor() < buf.length() && isWord(buf.currChar()) && !isAlpha(buf.currChar())) {
                buf.move(1);
            }
            while (buf.cursor() < buf.length() && isWord(buf.currChar())) {
                buf.currChar(first
                        ? Character.toUpperCase(buf.currChar())
                        : Character.toLowerCase(buf.currChar()));
                buf.move(1);
                first = false;
            }
        }
        if (this.count < 0) {
            buf.cursor(cursor);
        }
        return true;
    }

    protected boolean deleteWord() {
        if (count < 0) {
            return callNeg(this::backwardDeleteWord);
        }
        int x = buf.cursor();
        while (count-- > 0) {
            while (x < buf.length() && !isWord(buf.atChar(x))) {
                x++;
            }
            while (x < buf.length() && isWord(buf.atChar(x))) {
                x++;
            }
        }
        buf.delete(x - buf.cursor());
        return true;
    }

    protected boolean killWord() {
        if (count < 0) {
            return callNeg(this::backwardKillWord);
        }
        int x = buf.cursor();
        while (count-- > 0) {
            while (x < buf.length() && !isWord(buf.atChar(x))) {
                x++;
            }
            while (x < buf.length() && isWord(buf.atChar(x))) {
                x++;
            }
        }
        killRing.add(buf.substring(buf.cursor(), x));
        buf.delete(x - buf.cursor());
        return true;
    }

    protected boolean transposeWords() {
        int lstart = buf.cursor() - 1;
        int lend = buf.cursor();
        while (buf.atChar(lstart) != 0 && buf.atChar(lstart) != '\n') {
            lstart--;
        }
        lstart++;
        while (buf.atChar(lend) != 0 && buf.atChar(lend) != '\n') {
            lend++;
        }
        if (lend - lstart < 2) {
            return false;
        }
        int words = 0;
        boolean inWord = false;
        if (!isDelimiter(buf.atChar(lstart))) {
            words++;
            inWord = true;
        }
        for (int i = lstart; i < lend; i++) {
            if (isDelimiter(buf.atChar(i))) {
                inWord = false;
            } else {
                if (!inWord) {
                    words++;
                }
                inWord = true;
            }
        }
        if (words < 2) {
            return false;
        }
        // TODO: use isWord instead of isDelimiter
        boolean neg = this.count < 0;
        for (int count = Math.max(this.count, -this.count); count > 0; --count) {
            int sta1, end1, sta2, end2;
            // Compute current word boundaries
            sta1 = buf.cursor();
            while (sta1 > lstart && !isDelimiter(buf.atChar(sta1 - 1))) {
                sta1--;
            }
            end1 = sta1;
            while (end1 < lend && !isDelimiter(buf.atChar(++end1)));
            if (neg) {
                end2 = sta1 - 1;
                while (end2 > lstart && isDelimiter(buf.atChar(end2 - 1))) {
                    end2--;
                }
                if (end2 < lstart) {
                    // No word before, use the word after
                    sta2 = end1;
                    while (isDelimiter(buf.atChar(++sta2)));
                    end2 = sta2;
                    while (end2 < lend && !isDelimiter(buf.atChar(++end2)));
                } else {
                    sta2 = end2;
                    while (sta2 > lstart && !isDelimiter(buf.atChar(sta2 - 1))) {
                        sta2--;
                    }
                }
            } else {
                sta2 = end1;
                while (sta2 < lend && isDelimiter(buf.atChar(++sta2)));
                if (sta2 == lend) {
                    // No word after, use the word before
                    end2 = sta1;
                    while (isDelimiter(buf.atChar(end2 - 1))) {
                        end2--;
                    }
                    sta2 = end2;
                    while (sta2 > lstart && !isDelimiter(buf.atChar(sta2 - 1))) {
                        sta2--;
                    }
                } else {
                    end2 = sta2;
                    while (end2 < lend && !isDelimiter(buf.atChar(++end2))) ;
                }
            }
            if (sta1 < sta2) {
                String res = buf.substring(0, sta1) + buf.substring(sta2, end2)
                        + buf.substring(end1, sta2) + buf.substring(sta1, end1)
                        + buf.substring(end2);
                buf.clear();
                buf.write(res);
                buf.cursor(neg ? end1 : end2);
            } else {
                String res = buf.substring(0, sta2) + buf.substring(sta1, end1)
                        + buf.substring(end2, sta1) + buf.substring(sta2, end2)
                        + buf.substring(end1);
                buf.clear();
                buf.write(res);
                buf.cursor(neg ? end2 : end1);
            }
        }
        return true;
    }

    private int findbol() {
        int x = buf.cursor();
        while (x > 0 && buf.atChar(x - 1) != '\n') {
            x--;
        }
        return x;
    }

    private int findeol() {
        int x = buf.cursor();
        while (x < buf.length() && buf.atChar(x) != '\n') {
            x++;
        }
        return x;
    }

    protected boolean insertComment() {
        return doInsertComment(false);
    }

    protected boolean viInsertComment() {
        return doInsertComment(true);
    }

    protected boolean doInsertComment(boolean isViMode) {
        String comment = getString(COMMENT_BEGIN, DEFAULT_COMMENT_BEGIN);
        beginningOfLine();
        putString(comment);
        if (isViMode) {
            setKeyMap(VIINS);
        }
        return acceptLine();
    }

    protected boolean viFindNextChar() {
        if ((findChar = vigetkey()) > 0) {
            findDir = 1;
            findTailAdd = 0;
            return vifindchar(false);
        }
        return false;
    }

    protected boolean viFindPrevChar() {
        if ((findChar = vigetkey()) > 0) {
            findDir = -1;
            findTailAdd = 0;
            return vifindchar(false);
        }
        return false;
    }

    protected boolean viFindNextCharSkip() {
        if ((findChar = vigetkey()) > 0) {
            findDir = 1;
            findTailAdd = -1;
            return vifindchar(false);
        }
        return false;
    }

    protected boolean viFindPrevCharSkip() {
        if ((findChar = vigetkey()) > 0) {
            findDir = -1;
            findTailAdd = 1;
            return vifindchar(false);
        }
        return false;
    }

    protected boolean viRepeatFind() {
        return vifindchar(true);
    }

    protected boolean viRevRepeatFind() {
        if (count < 0) {
            return callNeg(() -> vifindchar(true));
        }
        findTailAdd = -findTailAdd;
        findDir = -findDir;
        boolean ret = vifindchar(true);
        findTailAdd = -findTailAdd;
        findDir = -findDir;
        return ret;
    }

    private int vigetkey() {
        int ch = readCharacter();
        KeyMap<Binding> km = keyMaps.get(MAIN);
        if (km != null) {
            Binding b = km.getBound(new String(Character.toChars(ch)));
            if (b instanceof Reference) {
                String func = ((Reference) b).name();
                if (SEND_BREAK.equals(func)) {
                    return -1;
                }
            }
        }
        return ch;
    }

    private boolean vifindchar(boolean repeat) {
        if (findDir == 0) {
            return false;
        }
        if (count < 0) {
            return callNeg(this::viRevRepeatFind);
        }
        if (repeat && findTailAdd != 0) {
            if (findDir > 0) {
                if (buf.cursor() < buf.length() && buf.nextChar() == findChar) {
                    buf.move(1);
                }
            } else {
                if (buf.cursor() > 0 && buf.prevChar() == findChar) {
                    buf.move(-1);
                }
            }
        }
        int cursor = buf.cursor();
        while (count-- > 0) {
            do {
                buf.move(findDir);
            } while (buf.cursor() > 0 && buf.cursor() < buf.length()
                    && buf.currChar() != findChar
                    && buf.currChar() != '\n');
            if (buf.cursor() <= 0 || buf.cursor() >= buf.length()
                    || buf.currChar() == '\n') {
                buf.cursor(cursor);
                return false;
            }
        }
        if (findTailAdd != 0) {
            buf.move(findTailAdd);
        }
        if (findDir == 1 && isInViMoveOperation()) {
            buf.move(1);
        }
        return true;
    }

    private boolean callNeg(Widget widget) {
        this.count = -this.count;
        boolean ret = widget.apply();
        this.count = -this.count;
        return ret;
    }

    /**
     * Implements vi search ("/" or "?").
     *
     * @return <code>true</code> if the search was successful
     */
    protected boolean viHistorySearchForward() {
        searchDir = 1;
        searchIndex = 0;
        return getViSearchString() && viRepeatSearch();
    }

    protected boolean viHistorySearchBackward() {
        searchDir = -1;
        searchIndex = history.size() - 1;
        return getViSearchString() && viRepeatSearch();
    }

    protected boolean viRepeatSearch() {
        if (searchDir == 0) {
            return false;
        }
        int si = searchDir < 0
                ? searchBackwards(searchString, searchIndex, false)
                : searchForwards(searchString, searchIndex, false);
        if (si == -1 || si == history.index()) {
            return false;
        }
        searchIndex = si;

        /*
         * Show the match.
         */
        buf.clear();
        history.moveTo(searchIndex);
        buf.write(history.get(searchIndex));
        if (VICMD.equals(keyMap)) {
            buf.move(-1);
        }
        return true;
    }

    protected boolean viRevRepeatSearch() {
        boolean ret;
        searchDir = -searchDir;
        ret = viRepeatSearch();
        searchDir = -searchDir;
        return ret;
    }

    private boolean getViSearchString() {
        if (searchDir == 0) {
            return false;
        }
        String searchPrompt = searchDir < 0 ? "?" : "/";
        Buffer searchBuffer = new BufferImpl();

        KeyMap<Binding> keyMap = keyMaps.get(MAIN);
        if (keyMap == null) {
            keyMap = keyMaps.get(SAFE);
        }
        while (true) {
            post = () -> new AttributedString(searchPrompt + searchBuffer.toString() + "_");
            redisplay();
            Binding b = doReadBinding(keyMap, null);
            if (b instanceof Reference) {
                String func = ((Reference) b).name();
                switch (func) {
                    case SEND_BREAK:
                        post = null;
                        return false;
                    case ACCEPT_LINE:
                    case VI_CMD_MODE:
                        searchString = searchBuffer.toString();
                        post = null;
                        return true;
                    case MAGIC_SPACE:
                        searchBuffer.write(' ');
                        break;
                    case REDISPLAY:
                        redisplay();
                        break;
                    case CLEAR_SCREEN:
                        clearScreen();
                        break;
                    case SELF_INSERT:
                        searchBuffer.write(getLastBinding());
                        break;
                    case SELF_INSERT_UNMETA:
                        if (getLastBinding().charAt(0) == '\u001b') {
                            String s = getLastBinding().substring(1);
                            if ("\r".equals(s)) {
                                s = "\n";
                            }
                            searchBuffer.write(s);
                        }
                        break;
                    case BACKWARD_DELETE_CHAR:
                    case VI_BACKWARD_DELETE_CHAR:
                        if (searchBuffer.length() > 0) {
                            searchBuffer.backspace();
                        }
                        break;
                    case BACKWARD_KILL_WORD:
                    case VI_BACKWARD_KILL_WORD:
                        if (searchBuffer.length() > 0 && !isWhitespace(searchBuffer.prevChar())) {
                            searchBuffer.backspace();
                        }
                        if (searchBuffer.length() > 0 && isWhitespace(searchBuffer.prevChar())) {
                            searchBuffer.backspace();
                        }
                        break;
                    case QUOTED_INSERT:
                    case VI_QUOTED_INSERT:
                        int c = readCharacter();
                        if (c >= 0) {
                            searchBuffer.write(c);
                        } else {
                            beep();
                        }
                        break;
                    default:
                        beep();
                        break;
                }
            }
        }
    }

    protected boolean insertCloseCurly() {
        return insertClose("}");
    }

    protected boolean insertCloseParen() {
        return insertClose(")");
    }

    protected boolean insertCloseSquare() {
        return insertClose("]");
    }

    protected boolean insertClose(String s) {
        putString(s);

        long blink = getLong(BLINK_MATCHING_PAREN, DEFAULT_BLINK_MATCHING_PAREN);
        if (blink <= 0) {
            removeIndentation();
            return true;
        }

        int closePosition = buf.cursor();

        buf.move(-1);
        doViMatchBracket();
        redisplay();

        peekCharacter(blink);
        int blinkPosition = buf.cursor();
        buf.cursor(closePosition);

        if (blinkPosition != closePosition - 1) {
            removeIndentation();
        }
        return true;
    }

    private void removeIndentation() {
        int indent = getInt(INDENTATION, DEFAULT_INDENTATION);
        if (indent > 0) {
            buf.move(-1);
            for (int i = 0; i < indent; i++) {
                buf.move(-1);
                if (buf.currChar() == ' ') {
                    buf.delete();
                } else {
                    buf.move(1);
                    break;
                }
            }
            buf.move(1);
        }
    }

    protected boolean viMatchBracket() {
        return doViMatchBracket();
    }

    protected boolean undefinedKey() {
        return false;
    }

    /**
     * Implements vi style bracket matching ("%" command). The matching
     * bracket for the current bracket type that you are sitting on is matched.
     *
     * @return true if it worked, false if the cursor was not on a bracket
     *   character or if there was no matching bracket.
     */
    protected boolean doViMatchBracket() {
        int pos        = buf.cursor();

        if (pos == buf.length()) {
            return false;
        }

        int type       = getBracketType(buf.atChar(pos));
        int move       = (type < 0) ? -1 : 1;
        int count      = 1;

        if (type == 0)
            return false;

        while (count > 0) {
            pos += move;

            // Fell off the start or end.
            if (pos < 0 || pos >= buf.length()) {
                return false;
            }

            int curType = getBracketType(buf.atChar(pos));
            if (curType == type) {
                ++count;
            }
            else if (curType == -type) {
                --count;
            }
        }

        /*
         * Slight adjustment for delete-to, yank-to, change-to to ensure
         * that the matching paren is consumed
         */
        if (move > 0 && isInViMoveOperation())
            ++pos;

        buf.cursor(pos);
        return true;
    }

    /**
     * Given a character determines what type of bracket it is (paren,
     * square, curly, or none).
     * @param ch The character to check
     * @return 1 is square, 2 curly, 3 parent, or zero for none.  The value
     *   will be negated if it is the closing form of the bracket.
     */
    protected int getBracketType (int ch) {
        switch (ch) {
            case '[': return  1;
            case ']': return -1;
            case '{': return  2;
            case '}': return -2;
            case '(': return  3;
            case ')': return -3;
            default:
                return 0;
        }
    }

    /**
     * Performs character transpose. The character prior to the cursor and the
     * character under the cursor are swapped and the cursor is advanced one.
     * Do not cross line breaks.
     * @return true
     */
    protected boolean transposeChars() {
        int lstart = buf.cursor() - 1;
        int lend = buf.cursor();
        while (buf.atChar(lstart) != 0 && buf.atChar(lstart) != '\n') {
            lstart--;
        }
        lstart++;
        while (buf.atChar(lend) != 0 && buf.atChar(lend) != '\n') {
            lend++;
        }
        if (lend - lstart < 2) {
            return false;
        }
        boolean neg = this.count < 0;
        for (int count = Math.max(this.count, -this.count); count > 0; --count) {
            while (buf.cursor() <= lstart) {
                buf.move(1);
            }
            while (buf.cursor() >= lend) {
                buf.move(-1);
            }
            int c = buf.currChar();
            buf.currChar(buf.prevChar());
            buf.move(-1);
            buf.currChar(c);
            buf.move(neg ? 0 : 2);
        }
        return true;
    }

    protected boolean undo() {
        isUndo = true;
        if (undo.canUndo()) {
            undo.undo();
            return true;
        }
        return false;
    }

    protected boolean redo() {
        isUndo = true;
        if (undo.canRedo()) {
            undo.redo();
            return true;
        }
        return false;
    }

    protected boolean sendBreak() {
        if (searchTerm == null) {
            buf.clear();
            println();
            redrawLine();
//            state = State.INTERRUPT;
            return false;
        }
        return true;
    }

    protected boolean backwardChar() {
        return buf.move(-count) != 0;
    }

    protected boolean forwardChar() {
        return buf.move(count) != 0;
    }

    protected boolean viDigitOrBeginningOfLine() {
        if (repeatCount > 0) {
            return digitArgument();
        } else {
            return beginningOfLine();
        }
    }

    protected boolean universalArgument() {
        mult *= universal;
        isArgDigit = true;
        return true;
    }

    protected boolean argumentBase() {
        if (repeatCount > 0 && repeatCount < 32) {
            universal = repeatCount;
            isArgDigit = true;
            return true;
        } else {
            return false;
        }
    }

    protected boolean negArgument() {
        mult *= -1;
        isArgDigit = true;
        return true;
    }

    protected boolean digitArgument() {
        String s = getLastBinding();
        repeatCount = (repeatCount * 10) + s.charAt(s.length() - 1) - '0';
        isArgDigit = true;
        return true;
    }

    protected boolean viDelete() {
        int cursorStart = buf.cursor();
        Binding o = readBinding(getKeys());
        if (o instanceof Reference) {
            // TODO: be smarter on how to get the vi range
            String op = viDeleteChangeYankToRemap(((Reference) o).name());
            // This is a weird special case. In vi
            // "dd" deletes the current line. So if we
            // get a delete-to, followed by a delete-to,
            // we delete the line.
            if (VI_DELETE.equals(op)) {
                killWholeLine();
            } else {
                viMoveMode = ViMoveMode.DELETE;
                Widget widget = widgets.get(op);
                if (widget != null && !widget.apply()) {
                    viMoveMode = ViMoveMode.NORMAL;
                    return false;
                }
                viMoveMode = ViMoveMode.NORMAL;
            }
            return viDeleteTo(cursorStart, buf.cursor());
        } else {
            pushBackBinding();
            return false;
        }
    }

    protected boolean viYankTo() {
        int cursorStart = buf.cursor();
        Binding o = readBinding(getKeys());
        if (o instanceof Reference) {
            // TODO: be smarter on how to get the vi range
            String op = viDeleteChangeYankToRemap(((Reference) o).name());
            // Similar to delete-to, a "yy" yanks the whole line.
            if (VI_YANK.equals(op)) {
                yankBuffer = buf.toString();
                return true;
            } else {
                viMoveMode = ViMoveMode.YANK;
                Widget widget = widgets.get(op);
                if (widget != null && !widget.apply()) {
                    return false;
                }
                viMoveMode = ViMoveMode.NORMAL;
            }
            return viYankTo(cursorStart, buf.cursor());
        } else {
            pushBackBinding();
            return false;
        }
    }

    protected boolean viYankWholeLine() {
        int s, e;
        int p = buf.cursor();
        while (buf.move(-1) == -1 && buf.prevChar() != '\n') ;
        s = buf.cursor();
        for (int i = 0; i < repeatCount; i++) {
            while (buf.move(1) == 1 && buf.prevChar() != '\n') ;
        }
        e = buf.cursor();
        yankBuffer = buf.substring(s, e);
        if (!yankBuffer.endsWith("\n")) {
            yankBuffer += "\n";
        }
        buf.cursor(p);
        return true;
    }

    protected boolean viChange() {
        int cursorStart = buf.cursor();
        Binding o = readBinding(getKeys());
        if (o instanceof Reference) {
            // TODO: be smarter on how to get the vi range
            String op = viDeleteChangeYankToRemap(((Reference) o).name());
            // change whole line
            if (VI_CHANGE.equals(op)) {
                killWholeLine();
            } else {
                viMoveMode = ViMoveMode.CHANGE;
                Widget widget = widgets.get(op);
                if (widget != null && !widget.apply()) {
                    viMoveMode = ViMoveMode.NORMAL;
                    return false;
                }
                viMoveMode = ViMoveMode.NORMAL;
            }
            boolean res = viChange(cursorStart, buf.cursor());
            setKeyMap(VIINS);
            return res;
        } else {
            pushBackBinding();
            return false;
        }
    }

    /*
    protected int getViRange(Reference cmd, ViMoveMode mode) {
        Buffer buffer = buf.copy();
        int oldMark = mark;
        int pos = buf.cursor();
        String bind = getLastBinding();

        if (visual != 0) {
            if (buf.length() == 0) {
                return -1;
            }
            pos = mark;
            v
        } else {
            viMoveMode = mode;
            mark = -1;
            Binding b = doReadBinding(getKeys(), keyMaps.get(VIOPP));
            if (b == null || new Reference(SEND_BREAK).equals(b)) {
                viMoveMode = ViMoveMode.NORMAL;
                mark = oldMark;
                return -1;
            }
            if (cmd.equals(b)) {
                doViLineRange();
            }
            Widget w = getWidget(b);
            if (w )
            if (b instanceof Reference) {

            }
        }

    }
    */

    protected void cleanup() {
        if (isSet(Option.ERASE_LINE_ON_FINISH)) {
            Buffer oldBuffer = buf.copy();
            AttributedString oldPrompt = prompt;
            buf.clear();
            prompt = new AttributedString("");
            doCleanup(false);
            prompt = oldPrompt;
            buf.copyFrom(oldBuffer);
        } else {
            doCleanup(true);
        }
    }

    protected void doCleanup(boolean nl) {
        buf.cursor(buf.length());
        post = null;
        if (size.getColumns() > 0 || size.getRows() > 0) {
            doAutosuggestion = false;
            redisplay(false);
            if (nl) {
                println();
            }
            terminal.puts(Capability.keypad_local);
            terminal.trackMouse(Terminal.MouseTracking.Off);
            if (isSet(Option.BRACKETED_PASTE))
                terminal.writer().write(BRACKETED_PASTE_OFF);
            flush();
        }
        history.moveToEnd();
    }

    protected boolean historyIncrementalSearchForward() {
        return doSearchHistory(false);
    }

    protected boolean historyIncrementalSearchBackward() {
        return doSearchHistory(true);
    }

    static class Pair<U,V> {
        final U u; final V v;
        public Pair(U u, V v) {
            this.u = u;
            this.v = v;
        }
        public U getU() {
            return u;
        }
        public V getV() {
            return v;
        }
    }

    protected boolean doSearchHistory(boolean backward) {
        if (history.isEmpty()) {
            return false;
        }

        KeyMap<Binding> terminators = new KeyMap<>();
        getString(SEARCH_TERMINATORS, DEFAULT_SEARCH_TERMINATORS)
                .codePoints().forEach(c -> bind(terminators, ACCEPT_LINE, new String(Character.toChars(c))));

        Buffer originalBuffer = buf.copy();
        searchIndex = -1;
        searchTerm = new StringBuffer();
        searchBackward = backward;
        searchFailing = false;
        post = () -> new AttributedString((searchFailing ? "failing" + " " : "")
                        + (searchBackward ? "bck-i-search" : "fwd-i-search")
                        + ": " + searchTerm + "_");

        redisplay();
        try {
            while (true) {
                int prevSearchIndex = searchIndex;
                Binding operation = readBinding(getKeys(), terminators);
                String ref = (operation instanceof Reference) ? ((Reference) operation).name() : "";
                boolean next = false;
                switch (ref) {
                    case SEND_BREAK:
                        beep();
                        buf.copyFrom(originalBuffer);
                        return true;
                    case HISTORY_INCREMENTAL_SEARCH_BACKWARD:
                        searchBackward = true;
                        next = true;
                        break;
                    case HISTORY_INCREMENTAL_SEARCH_FORWARD:
                        searchBackward = false;
                        next = true;
                        break;
                    case BACKWARD_DELETE_CHAR:
                        if (searchTerm.length() > 0) {
                            searchTerm.deleteCharAt(searchTerm.length() - 1);
                        }
                        break;
                    case SELF_INSERT:
                        searchTerm.append(getLastBinding());
                        break;
                    default:
                        // Set buffer and cursor position to the found string.
                        if (searchIndex != -1) {
                            history.moveTo(searchIndex);
                        }
                        pushBackBinding();
                        return true;
                }

                // print the search status
                String pattern = doGetSearchPattern();
                if (pattern.length() == 0) {
                    buf.copyFrom(originalBuffer);
                    searchFailing = false;
                } else {
                    boolean caseInsensitive = isSet(Option.CASE_INSENSITIVE_SEARCH);
                    Pattern pat = Pattern.compile(pattern, caseInsensitive ? Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
                                                                           : Pattern.UNICODE_CASE);
                    Pair<Integer, Integer> pair = null;
                    if (searchBackward) {
                        boolean nextOnly = next;
                        pair = matches(pat, buf.toString(), searchIndex).stream()
                                .filter(p -> nextOnly ? p.v < buf.cursor() : p.v <= buf.cursor())
                                .max(Comparator.comparing(Pair::getV))
                                .orElse(null);
                        if (pair == null) {
                            pair = StreamSupport.stream(
                                    Spliterators.spliteratorUnknownSize(history.reverseIterator(searchIndex < 0 ? history.last() : searchIndex - 1), Spliterator.ORDERED), false)
                                    .flatMap(e -> matches(pat, e.line(), e.index()).stream())
                                    .findFirst()
                                    .orElse(null);
                        }
                    } else {
                        boolean nextOnly = next;
                        pair = matches(pat, buf.toString(), searchIndex).stream()
                                .filter(p -> nextOnly ? p.v > buf.cursor() : p.v >= buf.cursor())
                                .min(Comparator.comparing(Pair::getV))
                                .orElse(null);
                        if (pair == null) {
                            pair = StreamSupport.stream(
                                    Spliterators.spliteratorUnknownSize(history.iterator((searchIndex < 0 ? history.last() : searchIndex) + 1), Spliterator.ORDERED), false)
                                    .flatMap(e -> matches(pat, e.line(), e.index()).stream())
                                    .findFirst()
                                    .orElse(null);
                            if (pair == null && searchIndex >= 0) {
                                pair = matches(pat, originalBuffer.toString(), -1).stream()
                                        .min(Comparator.comparing(Pair::getV))
                                        .orElse(null);
                            }
                        }
                    }
                    if (pair != null) {
                        searchIndex = pair.u;
                        buf.clear();
                        if (searchIndex >= 0) {
                            buf.write(history.get(searchIndex));
                        } else {
                            buf.write(originalBuffer.toString());
                        }
                        buf.cursor(pair.v);
                        searchFailing = false;
                    } else {
                        searchFailing = true;
                        beep();
                    }
                }
                redisplay();
            }
        } catch (IOError e) {
            // Ignore Ctrl+C interrupts and just exit the loop
            if (!(e.getCause() instanceof InterruptedException)) {
                throw e;
            }
            return true;
        } finally {
            searchTerm = null;
            searchIndex = -1;
            post = null;
        }
    }

    private List<Pair<Integer, Integer>> matches(Pattern p, String line, int index) {
        List<Pair<Integer, Integer>> starts = new ArrayList<>();
        Matcher m = p.matcher(line);
        while (m.find()) {
            starts.add(new Pair<>(index, m.start()));
        }
        return starts;
   }

    private String doGetSearchPattern() {
        StringBuilder sb = new StringBuilder();
        boolean inQuote = false;
        for (int i = 0; i < searchTerm.length(); i++) {
            char c = searchTerm.charAt(i);
            if (Character.isLowerCase(c)) {
                if (inQuote) {
                    sb.append("\\E");
                    inQuote = false;
                }
                sb.append("[").append(Character.toLowerCase(c)).append(Character.toUpperCase(c)).append("]");
            } else {
                if (!inQuote) {
                    sb.append("\\Q");
                    inQuote = true;
                }
                sb.append(c);
            }
        }
        if (inQuote) {
            sb.append("\\E");
        }
        return sb.toString();
    }

    private void pushBackBinding() {
        pushBackBinding(false);
    }

    private void pushBackBinding(boolean skip) {
        String s = getLastBinding();
        if (s != null) {
            bindingReader.runMacro(s);
            skipRedisplay = skip;
        }
    }

    protected boolean historySearchForward() {
        if (historyBuffer == null || buf.length() == 0
                || !buf.toString().equals(history.current())) {
            historyBuffer = buf.copy();
            searchBuffer = getFirstWord();
        }
        int index = history.index() + 1;

        if (index < history.last() + 1) {
            int searchIndex = searchForwards(searchBuffer.toString(), index, true);
            if (searchIndex == -1) {
                history.moveToEnd();
                if (!buf.toString().equals(historyBuffer.toString())) {
                    setBuffer(historyBuffer.toString());
                    historyBuffer = null;
                } else {
                    return false;
                }
            } else {
                // Maintain cursor position while searching.
                if (history.moveTo(searchIndex)) {
                    setBuffer(history.current());
                } else {
                    history.moveToEnd();
                    setBuffer(historyBuffer.toString());
                    return false;
                }
            }
        } else {
            history.moveToEnd();
            if (!buf.toString().equals(historyBuffer.toString())) {
                setBuffer(historyBuffer.toString());
                historyBuffer = null;
            } else {
                return false;
            }
        }
        return true;
    }

    private CharSequence getFirstWord() {
        String s = buf.toString();
        int i = 0;
        while (i < s.length() && !Character.isWhitespace(s.charAt(i))) {
            i++;
        }
        return s.substring(0, i);
    }

    protected boolean historySearchBackward() {
        if (historyBuffer == null || buf.length() == 0
                || !buf.toString().equals(history.current())) {
            historyBuffer = buf.copy();
            searchBuffer = getFirstWord();
        }
        int searchIndex = searchBackwards(searchBuffer.toString(), history.index(), true);

        if (searchIndex == -1) {
            return false;
        } else {
            // Maintain cursor position while searching.
            if (history.moveTo(searchIndex)) {
                setBuffer(history.current());
            } else {
                return false;
            }
        }
        return true;
    }

    //
    // History search
    //
    /**
     * Search backward in history from a given position.
     *
     * @param searchTerm substring to search for.
     * @param startIndex the index from which on to search
     * @return index where this substring has been found, or -1 else.
     */
    public int searchBackwards(String searchTerm, int startIndex) {
        return searchBackwards(searchTerm, startIndex, false);
    }

    /**
     * Search backwards in history from the current position.
     *
     * @param searchTerm substring to search for.
     * @return index where the substring has been found, or -1 else.
     */
    public int searchBackwards(String searchTerm) {
        return searchBackwards(searchTerm, history.index(), false);
    }

    public int searchBackwards(String searchTerm, int startIndex, boolean startsWith) {
        boolean caseInsensitive = isSet(Option.CASE_INSENSITIVE_SEARCH);
        if (caseInsensitive) {
            searchTerm = searchTerm.toLowerCase();
        }
        ListIterator<History.Entry> it = history.iterator(startIndex);
        while (it.hasPrevious()) {
            History.Entry e = it.previous();
            String line = e.line();
            if (caseInsensitive) {
                line = line.toLowerCase();
            }
            int idx = line.indexOf(searchTerm);
            if ((startsWith && idx == 0) || (!startsWith && idx >= 0)) {
                return e.index();
            }
        }
        return -1;
    }

    public int searchForwards(String searchTerm, int startIndex, boolean startsWith) {
        boolean caseInsensitive = isSet(Option.CASE_INSENSITIVE_SEARCH);
        if (caseInsensitive) {
            searchTerm = searchTerm.toLowerCase();
        }
        if (startIndex > history.last()) {
            startIndex = history.last();
        }
        ListIterator<History.Entry> it = history.iterator(startIndex);
        if (searchIndex != -1 && it.hasNext()) {
            it.next();
        }
        while (it.hasNext()) {
            History.Entry e = it.next();
            String line = e.line();
            if (caseInsensitive) {
                line = line.toLowerCase();
            }
            int idx = line.indexOf(searchTerm);
            if ((startsWith && idx == 0) || (!startsWith && idx >= 0)) {
                return e.index();
            }
        }
        return -1;
    }

    /**
     * Search forward in history from a given position.
     *
     * @param searchTerm substring to search for.
     * @param startIndex the index from which on to search
     * @return index where this substring has been found, or -1 else.
     */
    public int searchForwards(String searchTerm, int startIndex) {
        return searchForwards(searchTerm, startIndex, false);
    }
    /**
     * Search forwards in history from the current position.
     *
     * @param searchTerm substring to search for.
     * @return index where the substring has been found, or -1 else.
     */
    public int searchForwards(String searchTerm) {
        return searchForwards(searchTerm, history.index());
    }

    protected boolean quit() {
        getBuffer().clear();
        return acceptLine();
    }

    protected boolean acceptAndHold() {
        nextCommandFromHistory = false;
        acceptLine();
        if (!buf.toString().isEmpty()) {
            nextHistoryId = Integer.MAX_VALUE;
            nextCommandFromHistory = true;
        }
        return nextCommandFromHistory;
    }

    protected boolean acceptLineAndDownHistory() {
        nextCommandFromHistory = false;
        acceptLine();
        if (nextHistoryId < 0) {
            nextHistoryId = history.index();
        }
        if (history.size() > nextHistoryId + 1) {
            nextHistoryId++;
            nextCommandFromHistory = true;
        }
        return nextCommandFromHistory;
    }

    protected boolean acceptAndInferNextHistory() {
        nextCommandFromHistory = false;
        acceptLine();
        if (!buf.toString().isEmpty()) {
            nextHistoryId = searchBackwards(buf.toString(), history.last());
            if (nextHistoryId >= 0 && history.size() > nextHistoryId + 1) {
                nextHistoryId++;
                nextCommandFromHistory = true;
            }
        }
        return nextCommandFromHistory;
    }

    protected boolean acceptLine() {
        parsedLine = null;
        int curPos = 0;
        if (!isSet(Option.DISABLE_EVENT_EXPANSION)) {
            try {
                String str = buf.toString();
                String exp = expander.expandHistory(history, str);
                if (!exp.equals(str)) {
                    buf.clear();
                    buf.write(exp);
                    if (isSet(Option.HISTORY_VERIFY)) {
                        return true;
                    }
                }
            } catch (IllegalArgumentException e) {
                // Ignore
            }
        }
        try {
            curPos = buf.cursor();
            parsedLine = parser.parse(buf.toString(), buf.cursor(), ParseContext.ACCEPT_LINE);
        } catch (EOFError e) {
            StringBuilder sb = new StringBuilder("\n");
            indention(e.getOpenBrackets(), sb);
            int curMove = sb.length();
            if (isSet(Option.INSERT_BRACKET) && e.getOpenBrackets() > 1 && e.getNextClosingBracket() != null) {
                sb.append('\n');
                indention(e.getOpenBrackets() - 1, sb);
                sb.append(e.getNextClosingBracket());
            }
            buf.write(sb.toString());
            buf.cursor(curPos + curMove);
            return true;
        } catch (SyntaxError e) {
            // do nothing
        }
        callWidget(CALLBACK_FINISH);
        state = State.DONE;
        return true;
    }

    void indention(int nb, StringBuilder sb) {
        int indent = getInt(INDENTATION, DEFAULT_INDENTATION)*nb;
        for (int i = 0; i < indent; i++) {
            sb.append(' ');
        }
    }

    protected boolean selfInsert() {
        for (int count = this.count; count > 0; count--) {
            putString(getLastBinding());
        }
        return true;
    }

    protected boolean selfInsertUnmeta() {
        if (getLastBinding().charAt(0) == '\u001b') {
            String s = getLastBinding().substring(1);
            if ("\r".equals(s)) {
                s = "\n";
            }
            for (int count = this.count; count > 0; count--) {
                putString(s);
            }
            return true;
        } else {
            return false;
        }
    }

    protected boolean overwriteMode() {
        overTyping = !overTyping;
        return true;
    }


    //
    // History Control
    //

    protected boolean beginningOfBufferOrHistory() {
        if (findbol() != 0) {
            buf.cursor(0);
            return true;
        } else {
            return beginningOfHistory();
        }
    }

    protected boolean beginningOfHistory() {
        if (history.moveToFirst()) {
            setBuffer(history.current());
            return true;
        } else {
            return false;
        }
    }

    protected boolean endOfBufferOrHistory() {
        if (findeol() != buf.length()) {
            buf.cursor(buf.length());
            return true;
        } else {
            return endOfHistory();
        }
    }

    protected boolean endOfHistory() {
        if (history.moveToLast()) {
            setBuffer(history.current());
            return true;
        } else {
            return false;
        }
    }

    protected boolean beginningOfLineHist() {
        if (count < 0) {
            return callNeg(this::endOfLineHist);
        }
        while (count-- > 0) {
            int bol = findbol();
            if (bol != buf.cursor()) {
                buf.cursor(bol);
            } else {
                moveHistory(false);
                buf.cursor(0);
            }
        }
        return true;
    }

    protected boolean endOfLineHist() {
        if (count < 0) {
            return callNeg(this::beginningOfLineHist);
        }
        while (count-- > 0) {
            int eol = findeol();
            if (eol != buf.cursor()) {
                buf.cursor(eol);
            } else {
                moveHistory(true);
            }
        }
        return true;
    }

    protected boolean upHistory() {
        while (count-- > 0) {
            if (!moveHistory(false)) {
                return !isSet(Option.HISTORY_BEEP);
            }
        }
        return true;
    }

    protected boolean downHistory() {
        while (count-- > 0) {
            if (!moveHistory(true)) {
                return !isSet(Option.HISTORY_BEEP);
            }
        }
        return true;
    }

    protected boolean viUpLineOrHistory() {
        return upLine()
                || upHistory() && viFirstNonBlank();
    }

    protected boolean viDownLineOrHistory() {
        return downLine()
                || downHistory() && viFirstNonBlank();
    }

    protected boolean upLine() {
        return buf.up();
    }

    protected boolean downLine() {
        return buf.down();
    }

    protected boolean upLineOrHistory() {
        return upLine() || upHistory();
    }

    protected boolean upLineOrSearch() {
        return upLine() || historySearchBackward();
    }

    protected boolean downLineOrHistory() {
        return downLine() || downHistory();
    }

    protected boolean downLineOrSearch() {
        return downLine() || historySearchForward();
    }

    protected boolean viCmdMode() {
        // If we are re-entering move mode from an
        // aborted yank-to, delete-to, change-to then
        // don't move the cursor back. The cursor is
        // only move on an explicit entry to movement
        // mode.
        if (state == State.NORMAL) {
            buf.move(-1);
        }
        return setKeyMap(VICMD);
    }

    protected boolean viInsert() {
        return setKeyMap(VIINS);
    }

    protected boolean viAddNext() {
        buf.move(1);
        return setKeyMap(VIINS);
    }

    protected boolean viAddEol() {
        return endOfLine() && setKeyMap(VIINS);
    }

    protected boolean emacsEditingMode() {
        return setKeyMap(EMACS);
    }

    protected boolean viChangeWholeLine() {
        return viFirstNonBlank() && viChangeEol();
    }

    protected boolean viChangeEol() {
        return viChange(buf.cursor(), buf.length())
                && setKeyMap(VIINS);
    }

    protected boolean viKillEol() {
        int eol = findeol();
        if (buf.cursor() == eol) {
            return false;
        }
        killRing.add(buf.substring(buf.cursor(), eol));
        buf.delete(eol - buf.cursor());
        return true;
    }

    protected boolean quotedInsert() {
        int c = readCharacter();
        while (count-- > 0) {
            putString(new String(Character.toChars(c)));
        }
        return true;
    }

    protected boolean viJoin() {
        if (buf.down()) {
            while (buf.move(-1) == -1 && buf.prevChar() != '\n') ;
            buf.backspace();
            buf.write(' ');
            buf.move(-1);
            return true;
        }
        return false;
    }

    protected boolean viKillWholeLine() {
        return killWholeLine() && setKeyMap(VIINS);
    }

    protected boolean viInsertBol() {
        return beginningOfLine() && setKeyMap(VIINS);
    }

    protected boolean backwardDeleteChar() {
        if (count < 0) {
            return callNeg(this::deleteChar);
        }
        if (buf.cursor() == 0) {
            return false;
        }
        buf.backspace(count);
        return true;
    }

    protected boolean viFirstNonBlank() {
        beginningOfLine();
        while (buf.cursor() < buf.length() && isWhitespace(buf.currChar())) {
            buf.move(1);
        }
        return true;
    }

    protected boolean viBeginningOfLine() {
        buf.cursor(findbol());
        return true;
    }

    protected boolean viEndOfLine() {
        if (count < 0) {
            return false;
        }
        while (count-- > 0) {
            buf.cursor(findeol() + 1);
        }
        buf.move(-1);
        return true;
    }

    protected boolean beginningOfLine() {
        while (count-- > 0) {
            while (buf.move(-1) == -1 && buf.prevChar() != '\n') ;
        }
        return true;
    }

    protected boolean endOfLine() {
        while (count-- > 0) {
            while (buf.move(1) == 1 && buf.currChar() != '\n') ;
        }
        return true;
    }

    protected boolean deleteChar() {
        if (count < 0) {
            return callNeg(this::backwardDeleteChar);
        }
        if (buf.cursor() == buf.length()) {
            return false;
        }
        buf.delete(count);
        return true;
    }

    /**
     * Deletes the previous character from the cursor position
     * @return <code>true</code> if it succeeded, <code>false</code> otherwise
     */
    protected boolean viBackwardDeleteChar() {
        for (int i = 0; i < count; i++) {
            if (!buf.backspace()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Deletes the character you are sitting on and sucks the rest of
     * the line in from the right.
     * @return <code>true</code> if it succeeded, <code>false</code> otherwise
     */
    protected boolean viDeleteChar() {
        for (int i = 0; i < count; i++) {
            if (!buf.delete()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Switches the case of the current character from upper to lower
     * or lower to upper as necessary and advances the cursor one
     * position to the right.
     * @return <code>true</code> if it succeeded, <code>false</code> otherwise
     */
    protected boolean viSwapCase() {
        for (int i = 0; i < count; i++) {
            if (buf.cursor() < buf.length()) {
                int ch = buf.atChar(buf.cursor());
                ch = switchCase(ch);
                buf.currChar(ch);
                buf.move(1);
            } else {
                return false;
            }
        }
        return true;
    }

    /**
     * Implements the vi change character command (in move-mode "r"
     * followed by the character to change to).
     * @return <code>true</code> if it succeeded, <code>false</code> otherwise
     */
    protected boolean viReplaceChars() {
        int c = readCharacter();
        // EOF, ESC, or CTRL-C aborts.
        if (c < 0 || c == '\033' || c == '\003') {
            return true;
        }

        for (int i = 0; i < count; i++) {
            if (buf.currChar((char) c)) {
                if (i < count - 1) {
                    buf.move(1);
                }
            } else {
                return false;
            }
        }
        return true;
    }

    protected boolean viChange(int startPos, int endPos) {
        return doViDeleteOrChange(startPos, endPos, true);
    }

    protected boolean viDeleteTo(int startPos, int endPos) {
        return doViDeleteOrChange(startPos, endPos, false);
    }

    /**
     * Performs the vi "delete-to" action, deleting characters between a given
     * span of the input line.
     * @param startPos The start position
     * @param endPos The end position.
     * @param isChange If true, then the delete is part of a change operationg
     *    (e.g. "c$" is change-to-end-of line, so we first must delete to end
     *    of line to start the change
     * @return <code>true</code> if it succeeded, <code>false</code> otherwise
     */
    protected boolean doViDeleteOrChange(int startPos, int endPos, boolean isChange) {
        if (startPos == endPos) {
            return true;
        }

        if (endPos < startPos) {
            int tmp = endPos;
            endPos = startPos;
            startPos = tmp;
        }

        buf.cursor(startPos);
        buf.delete(endPos - startPos);

        // If we are doing a delete operation (e.g. "d$") then don't leave the
        // cursor dangling off the end. In reality the "isChange" flag is silly
        // what is really happening is that if we are in "move-mode" then the
        // cursor can't be moved off the end of the line, but in "edit-mode" it
        // is ok, but I have no easy way of knowing which mode we are in.
        if (! isChange && startPos > 0 && startPos == buf.length()) {
            buf.move(-1);
        }
        return true;
    }

    /**
     * Implement the "vi" yank-to operation.  This operation allows you
     * to yank the contents of the current line based upon a move operation,
     * for example "yw" yanks the current word, "3yw" yanks 3 words, etc.
     *
     * @param startPos The starting position from which to yank
     * @param endPos The ending position to which to yank
     * @return <code>true</code> if the yank succeeded
     */
    protected boolean viYankTo(int startPos, int endPos) {
        int cursorPos = startPos;

        if (endPos < startPos) {
            int tmp = endPos;
            endPos = startPos;
            startPos = tmp;
        }

        if (startPos == endPos) {
            yankBuffer = "";
            return true;
        }

        yankBuffer = buf.substring(startPos, endPos);

        /*
         * It was a movement command that moved the cursor to find the
         * end position, so put the cursor back where it started.
         */
        buf.cursor(cursorPos);
        return true;
    }

    protected boolean viOpenLineAbove() {
        while (buf.move(-1) == -1 && buf.prevChar() != '\n') ;
        buf.write('\n');
        buf.move(-1);
        return setKeyMap(VIINS);
    }

    protected boolean viOpenLineBelow() {
        while (buf.move(1) == 1 && buf.currChar() != '\n') ;
        buf.write('\n');
        return setKeyMap(VIINS);
    }

    /**
     * Pasts the yank buffer to the right of the current cursor position
     * and moves the cursor to the end of the pasted region.
     * @return <code>true</code>
     */
    protected boolean viPutAfter() {
        if (yankBuffer.indexOf('\n') >= 0) {
            while (buf.move(1) == 1 && buf.currChar() != '\n');
            buf.move(1);
            putString(yankBuffer);
            buf.move(- yankBuffer.length());
        } else if (yankBuffer.length () != 0) {
            if (buf.cursor() < buf.length()) {
                buf.move(1);
            }
            for (int i = 0; i < count; i++) {
                putString(yankBuffer);
            }
            buf.move(-1);
        }
        return true;
    }

    protected boolean viPutBefore() {
        if (yankBuffer.indexOf('\n') >= 0) {
            while (buf.move(-1) == -1 && buf.prevChar() != '\n');
            putString(yankBuffer);
            buf.move(- yankBuffer.length());
        } else if (yankBuffer.length () != 0) {
            if (buf.cursor() > 0) {
                buf.move(-1);
            }
            for (int i = 0; i < count; i++) {
                putString(yankBuffer);
            }
            buf.move(-1);
        }
        return true;
    }

    protected boolean doLowercaseVersion() {
        bindingReader.runMacro(getLastBinding().toLowerCase());
        return true;
    }

    protected boolean setMarkCommand() {
        if (count < 0) {
            regionActive = RegionType.NONE;
            return true;
        }
        regionMark = buf.cursor();
        regionActive = RegionType.CHAR;
        return true;
    }

    protected boolean exchangePointAndMark() {
        if (count == 0) {
            regionActive = RegionType.CHAR;
            return true;
        }
        int x = regionMark;
        regionMark = buf.cursor();
        buf.cursor(x);
        if (buf.cursor() > buf.length()) {
            buf.cursor(buf.length());
        }
        if (count > 0) {
            regionActive = RegionType.CHAR;
        }
        return true;
    }

    protected boolean visualMode() {
        if (isInViMoveOperation()) {
            isArgDigit = true;
            forceLine = false;
            forceChar = true;
            return true;
        }
        if (regionActive == RegionType.NONE) {
            regionMark = buf.cursor();
            regionActive = RegionType.CHAR;
        } else if (regionActive == RegionType.CHAR) {
            regionActive = RegionType.NONE;
        } else if (regionActive == RegionType.LINE) {
            regionActive = RegionType.CHAR;
        }
        return true;
    }

    protected boolean visualLineMode() {
        if (isInViMoveOperation()) {
            isArgDigit = true;
            forceLine = true;
            forceChar = false;
            return true;
        }
        if (regionActive == RegionType.NONE) {
            regionMark = buf.cursor();
            regionActive = RegionType.LINE;
        } else if (regionActive == RegionType.CHAR) {
            regionActive = RegionType.LINE;
        } else if (regionActive == RegionType.LINE) {
            regionActive = RegionType.NONE;
        }
        return true;
    }

    protected boolean deactivateRegion() {
        regionActive = RegionType.NONE;
        return true;
    }

    protected boolean whatCursorPosition() {
        post = () -> {
            AttributedStringBuilder sb = new AttributedStringBuilder();
            if (buf.cursor() < buf.length()) {
                int c = buf.currChar();
                sb.append("Char: ");
                if (c == ' ') {
                    sb.append("SPC");
                } else if (c == '\n') {
                    sb.append("LFD");
                } else if (c < 32) {
                    sb.append('^');
                    sb.append((char) (c + 'A' - 1));
                } else if (c == 127) {
                    sb.append("^?");
                } else {
                    sb.append((char) c);
                }
                sb.append(" (");
                sb.append("0").append(Integer.toOctalString(c)).append(" ");
                sb.append(Integer.toString(c)).append(" ");
                sb.append("0x").append(Integer.toHexString(c)).append(" ");
                sb.append(")");
            } else {
                sb.append("EOF");
            }
            sb.append("   ");
            sb.append("point ");
            sb.append(Integer.toString(buf.cursor() + 1));
            sb.append(" of ");
            sb.append(Integer.toString(buf.length() + 1));
            sb.append(" (");
            sb.append(Integer.toString(buf.length() == 0 ? 100 : ((100 * buf.cursor()) / buf.length())));
            sb.append("%)");
            sb.append("   ");
            sb.append("column ");
            sb.append(Integer.toString(buf.cursor() - findbol()));
            return sb.toAttributedString();
        };
        return true;
    }

    protected boolean editAndExecute() {
        boolean out = true;
        File file = null;
        try {
            file = File.createTempFile("jline-execute-", null);
            FileWriter writer = new FileWriter(file);
            writer.write(buf.toString());
            writer.close();
            editAndAddInBuffer(file);
        } catch (Exception e) {
            e.printStackTrace(terminal.writer());
            out = false;
        } finally {
            state = State.IGNORE;
            if (file != null && file.exists()) {
                file.delete();
            }
        }
        return out;
    }

    protected Map<String, Widget> builtinWidgets() {
        Map<String, Widget> widgets = new HashMap<>();
        addBuiltinWidget(widgets, ACCEPT_AND_INFER_NEXT_HISTORY, this::acceptAndInferNextHistory);
        addBuiltinWidget(widgets, ACCEPT_AND_HOLD, this::acceptAndHold);
        addBuiltinWidget(widgets, ACCEPT_LINE, this::acceptLine);
        addBuiltinWidget(widgets, ACCEPT_LINE_AND_DOWN_HISTORY, this::acceptLineAndDownHistory);
        addBuiltinWidget(widgets, ARGUMENT_BASE, this::argumentBase);
        addBuiltinWidget(widgets, BACKWARD_CHAR, this::backwardChar);
        addBuiltinWidget(widgets, BACKWARD_DELETE_CHAR, this::backwardDeleteChar);
        addBuiltinWidget(widgets, BACKWARD_DELETE_WORD, this::backwardDeleteWord);
        addBuiltinWidget(widgets, BACKWARD_KILL_LINE, this::backwardKillLine);
        addBuiltinWidget(widgets, BACKWARD_KILL_WORD, this::backwardKillWord);
        addBuiltinWidget(widgets, BACKWARD_WORD, this::backwardWord);
        addBuiltinWidget(widgets, BEEP, this::beep);
        addBuiltinWidget(widgets, BEGINNING_OF_BUFFER_OR_HISTORY, this::beginningOfBufferOrHistory);
        addBuiltinWidget(widgets, BEGINNING_OF_HISTORY, this::beginningOfHistory);
        addBuiltinWidget(widgets, BEGINNING_OF_LINE, this::beginningOfLine);
        addBuiltinWidget(widgets, BEGINNING_OF_LINE_HIST, this::beginningOfLineHist);
        addBuiltinWidget(widgets, CAPITALIZE_WORD, this::capitalizeWord);
        addBuiltinWidget(widgets, CLEAR, this::clear);
        addBuiltinWidget(widgets, CLEAR_SCREEN, this::clearScreen);
        addBuiltinWidget(widgets, COMPLETE_PREFIX, this::completePrefix);
        addBuiltinWidget(widgets, COMPLETE_WORD, this::completeWord);
        addBuiltinWidget(widgets, COPY_PREV_WORD, this::copyPrevWord);
        addBuiltinWidget(widgets, COPY_REGION_AS_KILL, this::copyRegionAsKill);
        addBuiltinWidget(widgets, DELETE_CHAR, this::deleteChar);
        addBuiltinWidget(widgets, DELETE_CHAR_OR_LIST, this::deleteCharOrList);
        addBuiltinWidget(widgets, DELETE_WORD, this::deleteWord);
        addBuiltinWidget(widgets, DIGIT_ARGUMENT, this::digitArgument);
        addBuiltinWidget(widgets, DO_LOWERCASE_VERSION, this::doLowercaseVersion);
        addBuiltinWidget(widgets, DOWN_CASE_WORD, this::downCaseWord);
        addBuiltinWidget(widgets, DOWN_LINE, this::downLine);
        addBuiltinWidget(widgets, DOWN_LINE_OR_HISTORY, this::downLineOrHistory);
        addBuiltinWidget(widgets, DOWN_LINE_OR_SEARCH, this::downLineOrSearch);
        addBuiltinWidget(widgets, DOWN_HISTORY, this::downHistory);
        addBuiltinWidget(widgets, EDIT_AND_EXECUTE_COMMAND, this::editAndExecute);
        addBuiltinWidget(widgets, EMACS_EDITING_MODE, this::emacsEditingMode);
        addBuiltinWidget(widgets, EMACS_BACKWARD_WORD, this::emacsBackwardWord);
        addBuiltinWidget(widgets, EMACS_FORWARD_WORD, this::emacsForwardWord);
        addBuiltinWidget(widgets, END_OF_BUFFER_OR_HISTORY, this::endOfBufferOrHistory);
        addBuiltinWidget(widgets, END_OF_HISTORY, this::endOfHistory);
        addBuiltinWidget(widgets, END_OF_LINE, this::endOfLine);
        addBuiltinWidget(widgets, END_OF_LINE_HIST, this::endOfLineHist);
        addBuiltinWidget(widgets, EXCHANGE_POINT_AND_MARK, this::exchangePointAndMark);
        addBuiltinWidget(widgets, EXPAND_HISTORY, this::expandHistory);
        addBuiltinWidget(widgets, EXPAND_OR_COMPLETE, this::expandOrComplete);
        addBuiltinWidget(widgets, EXPAND_OR_COMPLETE_PREFIX, this::expandOrCompletePrefix);
        addBuiltinWidget(widgets, EXPAND_WORD, this::expandWord);
        addBuiltinWidget(widgets, FRESH_LINE, this::freshLine);
        addBuiltinWidget(widgets, FORWARD_CHAR, this::forwardChar);
        addBuiltinWidget(widgets, FORWARD_WORD, this::forwardWord);
        addBuiltinWidget(widgets, HISTORY_INCREMENTAL_SEARCH_BACKWARD, this::historyIncrementalSearchBackward);
        addBuiltinWidget(widgets, HISTORY_INCREMENTAL_SEARCH_FORWARD, this::historyIncrementalSearchForward);
        addBuiltinWidget(widgets, HISTORY_SEARCH_BACKWARD, this::historySearchBackward);
        addBuiltinWidget(widgets, HISTORY_SEARCH_FORWARD, this::historySearchForward);
        addBuiltinWidget(widgets, INSERT_CLOSE_CURLY, this::insertCloseCurly);
        addBuiltinWidget(widgets, INSERT_CLOSE_PAREN, this::insertCloseParen);
        addBuiltinWidget(widgets, INSERT_CLOSE_SQUARE, this::insertCloseSquare);
        addBuiltinWidget(widgets, INSERT_COMMENT, this::insertComment);
        addBuiltinWidget(widgets, KILL_BUFFER, this::killBuffer);
        addBuiltinWidget(widgets, KILL_LINE, this::killLine);
        addBuiltinWidget(widgets, KILL_REGION, this::killRegion);
        addBuiltinWidget(widgets, KILL_WHOLE_LINE, this::killWholeLine);
        addBuiltinWidget(widgets, KILL_WORD, this::killWord);
        addBuiltinWidget(widgets, LIST_CHOICES, this::listChoices);
        addBuiltinWidget(widgets, MENU_COMPLETE, this::menuComplete);
        addBuiltinWidget(widgets, MENU_EXPAND_OR_COMPLETE, this::menuExpandOrComplete);
        addBuiltinWidget(widgets, NEG_ARGUMENT, this::negArgument);
        addBuiltinWidget(widgets, OVERWRITE_MODE, this::overwriteMode);
//        addBuiltinWidget(widgets, QUIT, this::quit);
        addBuiltinWidget(widgets, QUOTED_INSERT, this::quotedInsert);
        addBuiltinWidget(widgets, REDISPLAY, this::redisplay);
        addBuiltinWidget(widgets, REDRAW_LINE, this::redrawLine);
        addBuiltinWidget(widgets, REDO, this::redo);
        addBuiltinWidget(widgets, SELF_INSERT, this::selfInsert);
        addBuiltinWidget(widgets, SELF_INSERT_UNMETA, this::selfInsertUnmeta);
        addBuiltinWidget(widgets, SEND_BREAK, this::sendBreak);
        addBuiltinWidget(widgets, SET_MARK_COMMAND, this::setMarkCommand);
        addBuiltinWidget(widgets, TRANSPOSE_CHARS, this::transposeChars);
        addBuiltinWidget(widgets, TRANSPOSE_WORDS, this::transposeWords);
        addBuiltinWidget(widgets, UNDEFINED_KEY, this::undefinedKey);
        addBuiltinWidget(widgets, UNIVERSAL_ARGUMENT, this::universalArgument);
        addBuiltinWidget(widgets, UNDO, this::undo);
        addBuiltinWidget(widgets, UP_CASE_WORD, this::upCaseWord);
        addBuiltinWidget(widgets, UP_HISTORY, this::upHistory);
        addBuiltinWidget(widgets, UP_LINE, this::upLine);
        addBuiltinWidget(widgets, UP_LINE_OR_HISTORY, this::upLineOrHistory);
        addBuiltinWidget(widgets, UP_LINE_OR_SEARCH, this::upLineOrSearch);
        addBuiltinWidget(widgets, VI_ADD_EOL, this::viAddEol);
        addBuiltinWidget(widgets, VI_ADD_NEXT, this::viAddNext);
        addBuiltinWidget(widgets, VI_BACKWARD_CHAR, this::viBackwardChar);
        addBuiltinWidget(widgets, VI_BACKWARD_DELETE_CHAR, this::viBackwardDeleteChar);
        addBuiltinWidget(widgets, VI_BACKWARD_BLANK_WORD, this::viBackwardBlankWord);
        addBuiltinWidget(widgets, VI_BACKWARD_BLANK_WORD_END, this::viBackwardBlankWordEnd);
        addBuiltinWidget(widgets, VI_BACKWARD_KILL_WORD, this::viBackwardKillWord);
        addBuiltinWidget(widgets, VI_BACKWARD_WORD, this::viBackwardWord);
        addBuiltinWidget(widgets, VI_BACKWARD_WORD_END, this::viBackwardWordEnd);
        addBuiltinWidget(widgets, VI_BEGINNING_OF_LINE, this::viBeginningOfLine);
        addBuiltinWidget(widgets, VI_CMD_MODE, this::viCmdMode);
        addBuiltinWidget(widgets, VI_DIGIT_OR_BEGINNING_OF_LINE, this::viDigitOrBeginningOfLine);
        addBuiltinWidget(widgets, VI_DOWN_LINE_OR_HISTORY, this::viDownLineOrHistory);
        addBuiltinWidget(widgets, VI_CHANGE, this::viChange);
        addBuiltinWidget(widgets, VI_CHANGE_EOL, this::viChangeEol);
        addBuiltinWidget(widgets, VI_CHANGE_WHOLE_LINE, this::viChangeWholeLine);
        addBuiltinWidget(widgets, VI_DELETE_CHAR, this::viDeleteChar);
        addBuiltinWidget(widgets, VI_DELETE, this::viDelete);
        addBuiltinWidget(widgets, VI_END_OF_LINE, this::viEndOfLine);
        addBuiltinWidget(widgets, VI_KILL_EOL, this::viKillEol);
        addBuiltinWidget(widgets, VI_FIRST_NON_BLANK, this::viFirstNonBlank);
        addBuiltinWidget(widgets, VI_FIND_NEXT_CHAR, this::viFindNextChar);
        addBuiltinWidget(widgets, VI_FIND_NEXT_CHAR_SKIP, this::viFindNextCharSkip);
        addBuiltinWidget(widgets, VI_FIND_PREV_CHAR, this::viFindPrevChar);
        addBuiltinWidget(widgets, VI_FIND_PREV_CHAR_SKIP, this::viFindPrevCharSkip);
        addBuiltinWidget(widgets, VI_FORWARD_BLANK_WORD, this::viForwardBlankWord);
        addBuiltinWidget(widgets, VI_FORWARD_BLANK_WORD_END, this::viForwardBlankWordEnd);
        addBuiltinWidget(widgets, VI_FORWARD_CHAR, this::viForwardChar);
        addBuiltinWidget(widgets, VI_FORWARD_WORD, this::viForwardWord);
        addBuiltinWidget(widgets, VI_FORWARD_WORD, this::viForwardWord);
        addBuiltinWidget(widgets, VI_FORWARD_WORD_END, this::viForwardWordEnd);
        addBuiltinWidget(widgets, VI_HISTORY_SEARCH_BACKWARD, this::viHistorySearchBackward);
        addBuiltinWidget(widgets, VI_HISTORY_SEARCH_FORWARD, this::viHistorySearchForward);
        addBuiltinWidget(widgets, VI_INSERT, this::viInsert);
        addBuiltinWidget(widgets, VI_INSERT_BOL, this::viInsertBol);
        addBuiltinWidget(widgets, VI_INSERT_COMMENT, this::viInsertComment);
        addBuiltinWidget(widgets, VI_JOIN, this::viJoin);
        addBuiltinWidget(widgets, VI_KILL_LINE, this::viKillWholeLine);
        addBuiltinWidget(widgets, VI_MATCH_BRACKET, this::viMatchBracket);
        addBuiltinWidget(widgets, VI_OPEN_LINE_ABOVE, this::viOpenLineAbove);
        addBuiltinWidget(widgets, VI_OPEN_LINE_BELOW, this::viOpenLineBelow);
        addBuiltinWidget(widgets, VI_PUT_AFTER, this::viPutAfter);
        addBuiltinWidget(widgets, VI_PUT_BEFORE, this::viPutBefore);
        addBuiltinWidget(widgets, VI_REPEAT_FIND, this::viRepeatFind);
        addBuiltinWidget(widgets, VI_REPEAT_SEARCH, this::viRepeatSearch);
        addBuiltinWidget(widgets, VI_REPLACE_CHARS, this::viReplaceChars);
        addBuiltinWidget(widgets, VI_REV_REPEAT_FIND, this::viRevRepeatFind);
        addBuiltinWidget(widgets, VI_REV_REPEAT_SEARCH, this::viRevRepeatSearch);
        addBuiltinWidget(widgets, VI_SWAP_CASE, this::viSwapCase);
        addBuiltinWidget(widgets, VI_UP_LINE_OR_HISTORY, this::viUpLineOrHistory);
        addBuiltinWidget(widgets, VI_YANK, this::viYankTo);
        addBuiltinWidget(widgets, VI_YANK_WHOLE_LINE, this::viYankWholeLine);
        addBuiltinWidget(widgets, VISUAL_LINE_MODE, this::visualLineMode);
        addBuiltinWidget(widgets, VISUAL_MODE, this::visualMode);
        addBuiltinWidget(widgets, WHAT_CURSOR_POSITION, this::whatCursorPosition);
        addBuiltinWidget(widgets, YANK, this::yank);
        addBuiltinWidget(widgets, YANK_POP, this::yankPop);
        addBuiltinWidget(widgets, MOUSE, this::mouse);
        addBuiltinWidget(widgets, BEGIN_PASTE, this::beginPaste);
        addBuiltinWidget(widgets, FOCUS_IN, this::focusIn);
        addBuiltinWidget(widgets, FOCUS_OUT, this::focusOut);
        return widgets;
    }

    private void addBuiltinWidget(Map<String, Widget> widgets, String name, Widget widget) {
        widgets.put(name, namedWidget("." + name, widget));
    }

    private Widget namedWidget(String name, Widget widget) {
        return new Widget() {
            @Override
            public String toString() {
                return name;
            }
            @Override
            public boolean apply() {
                return widget.apply();
            }
        };
    }

    public boolean redisplay() {
        redisplay(true);
        return true;
    }

    protected void redisplay(boolean flush) {
        try {
            lock.lock();

            if (skipRedisplay) {
                skipRedisplay = false;
                return;
            }

            Status status = Status.getStatus(terminal, false);
            if (status != null) {
                status.redraw();
            }

            if (size.getRows() > 0 && size.getRows() < MIN_ROWS) {
                AttributedStringBuilder sb = new AttributedStringBuilder().tabs(TAB_WIDTH);

                sb.append(prompt);
                concat(getHighlightedBuffer(buf.toString()).columnSplitLength(Integer.MAX_VALUE), sb);
                AttributedString full = sb.toAttributedString();

                sb.setLength(0);
                sb.append(prompt);
                String line = buf.upToCursor();
                if (maskingCallback != null) {
                    line = maskingCallback.display(line);
                }

                concat(new AttributedString(line).columnSplitLength(Integer.MAX_VALUE), sb);
                AttributedString toCursor = sb.toAttributedString();

                int w = WCWidth.wcwidth('\u2026');
                int width = size.getColumns();
                int cursor = toCursor.columnLength();
                int inc = width / 2 + 1;
                while (cursor <= smallTerminalOffset + w) {
                    smallTerminalOffset -= inc;
                }
                while (cursor >= smallTerminalOffset + width - w) {
                    smallTerminalOffset += inc;
                }
                if (smallTerminalOffset > 0) {
                    sb.setLength(0);
                    sb.append("\u2026");
                    sb.append(full.columnSubSequence(smallTerminalOffset + w, Integer.MAX_VALUE));
                    full = sb.toAttributedString();
                }
                int length = full.columnLength();
                if (length >= smallTerminalOffset + width) {
                    sb.setLength(0);
                    sb.append(full.columnSubSequence(0, width - w));
                    sb.append("\u2026");
                    full = sb.toAttributedString();
                }

                display.update(Collections.singletonList(full), cursor - smallTerminalOffset, flush);
                return;
            }

            List<AttributedString> secondaryPrompts = new ArrayList<>();
            AttributedString full = getDisplayedBufferWithPrompts(secondaryPrompts);

            List<AttributedString> newLines;
            if (size.getColumns() <= 0) {
                newLines = new ArrayList<>();
                newLines.add(full);
            } else {
                newLines = full.columnSplitLength(size.getColumns(), true, display.delayLineWrap());
            }

            List<AttributedString> rightPromptLines;
            if (rightPrompt.length() == 0 || size.getColumns() <= 0) {
                rightPromptLines = new ArrayList<>();
            } else {
                rightPromptLines = rightPrompt.columnSplitLength(size.getColumns());
            }
            while (newLines.size() < rightPromptLines.size()) {
                newLines.add(new AttributedString(""));
            }
            for (int i = 0; i < rightPromptLines.size(); i++) {
                AttributedString line = rightPromptLines.get(i);
                newLines.set(i, addRightPrompt(line, newLines.get(i)));
            }

            int cursorPos = -1;
            int cursorNewLinesId = -1;
            int cursorColPos = -1;
            if (size.getColumns() > 0) {
                AttributedStringBuilder sb = new AttributedStringBuilder().tabs(TAB_WIDTH);
                sb.append(prompt);
                String buffer = buf.upToCursor();
                if (maskingCallback != null) {
                    buffer = maskingCallback.display(buffer);
                }
                sb.append(insertSecondaryPrompts(new AttributedString(buffer), secondaryPrompts, false));
                List<AttributedString> promptLines = sb.columnSplitLength(size.getColumns(), false, display.delayLineWrap());
                if (!promptLines.isEmpty()) {
                    cursorNewLinesId = promptLines.size() - 1;
                    cursorColPos = promptLines.get(promptLines.size() - 1).columnLength();
                    cursorPos = size.cursorPos(cursorNewLinesId, cursorColPos);
                }
            }

            List<AttributedString> newLinesToDisplay = new ArrayList<>();
            int displaySize = size.getRows() - (status != null ? status.size() : 0);
            if (newLines.size() > displaySize && !isTerminalDumb()) {
                StringBuilder sb = new StringBuilder(">....");
                // blanks are needed when displaying command completion candidate list
                for (int i = sb.toString().length(); i < size.getColumns(); i++) {
                    sb.append(" ");
                }
                AttributedString partialCommandInfo = new AttributedString(sb.toString());
                int lineId = newLines.size() - displaySize + 1;
                int endId = displaySize;
                int startId = 1;
                if (lineId  > cursorNewLinesId) {
                    lineId = cursorNewLinesId;
                    endId = displaySize - 1;
                    startId = 0;
                } else {
                    newLinesToDisplay.add(partialCommandInfo);
                }
                int cursorRowPos = 0;
                for (int i = startId; i < endId; i++) {
                    if (cursorNewLinesId == lineId) {
                        cursorRowPos = i;
                    }
                    newLinesToDisplay.add(newLines.get(lineId++));
                }
                if (startId == 0) {
                    newLinesToDisplay.add(partialCommandInfo);
                }
                cursorPos = size.cursorPos(cursorRowPos, cursorColPos);
            } else {
                newLinesToDisplay = newLines;
            }
            display.update(newLinesToDisplay, cursorPos, flush);
        } finally {
            lock.unlock();
        }
    }

    private void concat(List<AttributedString> lines, AttributedStringBuilder sb) {
        if (lines.size() > 1) {
            for (int i = 0; i < lines.size() - 1; i++) {
                sb.append(lines.get(i));
                sb.style(sb.style().inverse());
                sb.append("\\n");
                sb.style(sb.style().inverseOff());
            }
        }
        sb.append(lines.get(lines.size() - 1));
    }

    private String matchPreviousCommand(String buffer) {
        if (buffer.length() == 0) {
            return "";
        }
        History history = getHistory();
        StringBuilder sb = new StringBuilder();
        char prev = '0';
        for (char c: buffer.toCharArray()) {
            if ((c == '(' || c == ')' || c == '[' || c == ']' || c == '{' || c == '}' || c == '^') && prev != '\\' ) {
                sb.append('\\');
            }
            sb.append(c);
            prev = c;
        }
        Pattern pattern = Pattern.compile(sb.toString() + ".*", Pattern.DOTALL);
        Iterator<History.Entry> iter = history.reverseIterator(history.last());
        String suggestion = "";
        int tot = 0;
        while (iter.hasNext()) {
            History.Entry entry = iter.next();
            Matcher matcher = pattern.matcher(entry.line());
            if (matcher.matches()) {
                suggestion = entry.line().substring(buffer.length());
                break;
            } else if (tot > 200) {
                break;
            }
            tot++;
        }
        return suggestion;
    }

    /**
     * Compute the full string to be displayed with the left, right and secondary prompts
     * @param secondaryPrompts a list to store the secondary prompts
     * @return the displayed string including the buffer, left prompts and the help below
     */
    public AttributedString getDisplayedBufferWithPrompts(List<AttributedString> secondaryPrompts) {
        AttributedString attBuf = getHighlightedBuffer(buf.toString());

        AttributedString tNewBuf = insertSecondaryPrompts(attBuf, secondaryPrompts);
        AttributedStringBuilder full = new AttributedStringBuilder().tabs(TAB_WIDTH);
        full.append(prompt);
        full.append(tNewBuf);
        if (doAutosuggestion) {
            String lastBinding = getLastBinding() != null ? getLastBinding() : "";
            if (autosuggestion == SuggestionType.HISTORY) {
                AttributedStringBuilder sb = new AttributedStringBuilder();
                tailTip = matchPreviousCommand(buf.toString());
                sb.styled(AttributedStyle::faint, tailTip);
                full.append(sb.toAttributedString());
            } else if (autosuggestion == SuggestionType.COMPLETER) {
                if (buf.length() > 0 && buf.length() == buf.cursor()
                    && (!lastBinding.equals("\t") || buf.prevChar() == ' ' || buf.prevChar() == '=')) {
                    clearChoices();
                    listChoices(true);
                } else if (!lastBinding.equals("\t")) {
                    clearChoices();
                }
            } else if (autosuggestion == SuggestionType.TAIL_TIP) {
                if (buf.length() == buf.cursor()) {
                    if (!lastBinding.equals("\t") || buf.prevChar() == ' ') {
                        clearChoices();
                    }
                    AttributedStringBuilder sb = new AttributedStringBuilder();
                    if (buf.prevChar() != ' ') {
                        if (!tailTip.startsWith("[")) {
                            int idx = tailTip.indexOf(' ');
                            int idb = buf.toString().lastIndexOf(' ');
                            int idd = buf.toString().lastIndexOf('-');
                            if (idx > 0 && ((idb == -1 && idb == idd) || (idb >= 0 && idb > idd))) {
                                tailTip = tailTip.substring(idx);
                            } else if (idb >= 0 && idb < idd) {
                                sb.append(" ");
                            }
                        } else {
                            sb.append(" ");
                        }
                    }
                    sb.styled(AttributedStyle::faint, tailTip);
                    full.append(sb.toAttributedString());
                }
            }
        }
        if (post != null) {
            full.append("\n");
            full.append(post.get());
        }
        doAutosuggestion = true;
        return full.toAttributedString();
    }

    private AttributedString getHighlightedBuffer(String buffer) {
        if (maskingCallback != null) {
            buffer = maskingCallback.display(buffer);
        }
        if (highlighter != null && !isSet(Option.DISABLE_HIGHLIGHTER)
                && buffer.length() < getInt(FEATURES_MAX_BUFFER_SIZE, DEFAULT_FEATURES_MAX_BUFFER_SIZE)) {
            return highlighter.highlight(this, buffer);
        }
        return new AttributedString(buffer);
    }

    private AttributedString expandPromptPattern(String pattern, int padToWidth,
                                                 String message, int line) {
        ArrayList<AttributedString> parts = new ArrayList<>();
        boolean isHidden = false;
        int padPartIndex = -1;
        StringBuilder padPartString = null;
        StringBuilder sb = new StringBuilder();
        // Add "%{" to avoid special case for end of string.
        pattern = pattern + "%{";
        int plen = pattern.length();
        int padChar = -1;
        int padPos = -1;
        int cols = 0;
        for (int i = 0; i < plen; ) {
            char ch = pattern.charAt(i++);
            if (ch == '%' && i < plen) {
                int count = 0;
                boolean countSeen = false;
                decode: while (true) {
                    ch = pattern.charAt(i++);
                    switch (ch) {
                        case '{':
                        case '}':
                            String str = sb.toString();
                            AttributedString astr;
                            if (!isHidden) {
                                astr = AttributedString.fromAnsi(str);
                                cols += astr.columnLength();
                            } else {
                                astr = new AttributedString(str, AttributedStyle.HIDDEN);
                            }
                            if (padPartIndex == parts.size()) {
                                padPartString = sb;
                                if (i < plen) {
                                    sb = new StringBuilder();
                                }
                            } else {
                                sb.setLength(0);
                            }
                            parts.add(astr);
                            isHidden = ch == '{';
                            break decode;
                        case '%':
                            sb.append(ch);
                            break decode;
                        case 'N':
                            sb.append(getInt(LINE_OFFSET, 0) + line);
                            break decode;
                        case 'M':
                            if (message != null)
                                sb.append(message);
                            break decode;
                        case 'P':
                            if (countSeen && count >= 0)
                                padToWidth = count;
                            if (i < plen) {
                                padChar = pattern.charAt(i++);
                                // FIXME check surrogate
                            }
                            padPos = sb.length();
                            padPartIndex = parts.size();
                            break decode;
                        case '-':
                        case '0':
                        case '1':
                        case '2':
                        case '3':
                        case '4':
                        case '5':
                        case '6':
                        case '7':
                        case '8':
                        case '9':
                            boolean neg = false;
                            if (ch == '-') {
                                neg = true;
                                ch = pattern.charAt(i++);
                            }
                            countSeen = true;
                            count = 0;
                            while (ch >= '0' && ch <= '9') {
                                count = (count < 0 ? 0 : 10 * count) + (ch - '0');
                                ch = pattern.charAt(i++);
                            }
                            if (neg) {
                                count = -count;
                            }
                            i--;
                            break;
                        default:
                            break decode;
                    }
                }
            } else
                sb.append(ch);
        }
        if (padToWidth > cols && padToWidth > 0) {
            int padCharCols = WCWidth.wcwidth(padChar);
            int padCount = (padToWidth - cols) / padCharCols;
            sb = padPartString;
            while (--padCount >= 0)
                sb.insert(padPos, (char) padChar); // FIXME if wide
            parts.set(padPartIndex, AttributedString.fromAnsi(sb.toString()));
        }
        return AttributedString.join(null, parts);
    }

    private AttributedString insertSecondaryPrompts(AttributedString str, List<AttributedString> prompts) {
        return insertSecondaryPrompts(str, prompts, true);
    }

    private AttributedString insertSecondaryPrompts(AttributedString strAtt, List<AttributedString> prompts, boolean computePrompts) {
        Objects.requireNonNull(prompts);
        List<AttributedString> lines = strAtt.columnSplitLength(Integer.MAX_VALUE);
        AttributedStringBuilder sb = new AttributedStringBuilder();
        String secondaryPromptPattern = getString(SECONDARY_PROMPT_PATTERN, DEFAULT_SECONDARY_PROMPT_PATTERN);
        boolean needsMessage = secondaryPromptPattern.contains("%M")
                && strAtt.length() < getInt(FEATURES_MAX_BUFFER_SIZE, DEFAULT_FEATURES_MAX_BUFFER_SIZE);
        AttributedStringBuilder buf = new AttributedStringBuilder();
        int width = 0;
        List<String> missings = new ArrayList<>();
        if (computePrompts && secondaryPromptPattern.contains("%P")) {
            width = prompt.columnLength();
            for (int line = 0; line < lines.size() - 1; line++) {
                AttributedString prompt;
                buf.append(lines.get(line)).append("\n");
                String missing = "";
                if (needsMessage) {
                    try {
                        parser.parse(buf.toString(), buf.length(), ParseContext.SECONDARY_PROMPT);
                    } catch (EOFError e) {
                        missing = e.getMissing();
                    } catch (SyntaxError e) {
                        // Ignore
                    }
                }
                missings.add(missing);
                prompt = expandPromptPattern(secondaryPromptPattern, 0, missing, line + 1);
                width = Math.max(width, prompt.columnLength());
            }
            buf.setLength(0);
        }
        int line = 0;
        while (line < lines.size() - 1) {
            sb.append(lines.get(line)).append("\n");
            buf.append(lines.get(line)).append("\n");
            AttributedString prompt;
            if (computePrompts) {
                String missing = "";
                if (needsMessage) {
                    if (missings.isEmpty()) {
                        try {
                            parser.parse(buf.toString(), buf.length(), ParseContext.SECONDARY_PROMPT);
                        } catch (EOFError e) {
                            missing = e.getMissing();
                        } catch (SyntaxError e) {
                            // Ignore
                        }
                    } else {
                        missing = missings.get(line);
                    }
                }
                prompt = expandPromptPattern(secondaryPromptPattern, width, missing, line + 1);
            } else {
                prompt = prompts.get(line);
            }
            prompts.add(prompt);
            sb.append(prompt);
            line++;
        }
        sb.append(lines.get(line));
        buf.append(lines.get(line));
        return sb.toAttributedString();
    }

    private AttributedString addRightPrompt(AttributedString prompt, AttributedString line) {
        int width = prompt.columnLength();
        boolean endsWithNl = line.length() > 0
            && line.charAt(line.length() - 1) == '\n';
        // columnLength counts -1 for the final newline; adjust for that
        int nb = size.getColumns() - width
            - (line.columnLength() + (endsWithNl ? 1 : 0));
        if (nb >= 3) {
            AttributedStringBuilder sb = new AttributedStringBuilder(size.getColumns());
            sb.append(line, 0, endsWithNl ? line.length() - 1 : line.length());
            for (int j = 0; j < nb; j++) {
                sb.append(' ');
            }
            sb.append(prompt);
            if (endsWithNl) {
                sb.append('\n');
            }
            line = sb.toAttributedString();
        }
        return line;
    }

    //
    // Completion
    //

    protected boolean insertTab() {
        return isSet(Option.INSERT_TAB)
                    && getLastBinding().equals("\t")
                    && buf.toString().matches("(^|[\\s\\S]*\n)[\r\n\t ]*");
    }

    protected boolean expandHistory() {
        String str = buf.toString();
        String exp = expander.expandHistory(history, str);
        if (!exp.equals(str)) {
            buf.clear();
            buf.write(exp);
            return true;
        } else {
            return false;
        }
    }

    protected enum CompletionType {
        Expand,
        ExpandComplete,
        Complete,
        List,
    }

    protected boolean expandWord() {
        if (insertTab()) {
            return selfInsert();
        } else {
            return doComplete(CompletionType.Expand, isSet(Option.MENU_COMPLETE), false);
        }
    }

    protected boolean expandOrComplete() {
        if (insertTab()) {
            return selfInsert();
        } else {
            return doComplete(CompletionType.ExpandComplete, isSet(Option.MENU_COMPLETE), false);
        }
    }

    protected boolean expandOrCompletePrefix() {
        if (insertTab()) {
            return selfInsert();
        } else {
            return doComplete(CompletionType.ExpandComplete, isSet(Option.MENU_COMPLETE), true);
        }
    }

    protected boolean completeWord() {
        if (insertTab()) {
            return selfInsert();
        } else {
            return doComplete(CompletionType.Complete, isSet(Option.MENU_COMPLETE), false);
        }
    }

    protected boolean menuComplete() {
        if (insertTab()) {
            return selfInsert();
        } else {
            return doComplete(CompletionType.Complete, true, false);
        }
    }

    protected boolean menuExpandOrComplete() {
        if (insertTab()) {
            return selfInsert();
        } else {
            return doComplete(CompletionType.ExpandComplete, true, false);
        }
    }

    protected boolean completePrefix() {
        if (insertTab()) {
            return selfInsert();
        } else {
            return doComplete(CompletionType.Complete, isSet(Option.MENU_COMPLETE), true);
        }
    }

    protected boolean listChoices() {
        return listChoices(false);
    }

    private boolean listChoices(boolean forSuggestion) {
        return doComplete(CompletionType.List, isSet(Option.MENU_COMPLETE), false, forSuggestion);
    }

    protected boolean deleteCharOrList() {
        if (buf.cursor() != buf.length() || buf.length() == 0) {
            return deleteChar();
        } else {
            return doComplete(CompletionType.List, isSet(Option.MENU_COMPLETE), false);
        }
    }

    protected boolean doComplete(CompletionType lst, boolean useMenu, boolean prefix) {
        return doComplete(lst, useMenu, prefix, false);
    }

    protected boolean doComplete(CompletionType lst, boolean useMenu, boolean prefix, boolean forSuggestion) {
        // If completion is disabled, just bail out
        if (getBoolean(DISABLE_COMPLETION, false)) {
            return true;
        }
        // Try to expand history first
        // If there is actually an expansion, bail out now
        if (!isSet(Option.DISABLE_EVENT_EXPANSION)) {
            try {
                if (expandHistory()) {
                    return true;
                }
            } catch (Exception e) {
                Log.info("Error while expanding history", e);
                return false;
            }
        }

        // Parse the command line
        CompletingParsedLine line;
        try {
            line = wrap(parser.parse(buf.toString(), buf.cursor(), ParseContext.COMPLETE));
        } catch (Exception e) {
            Log.info("Error while parsing line", e);
            return false;
        }

        // Find completion candidates
        List<Candidate> candidates = new ArrayList<>();
        try {
            if (completer != null) {
                completer.complete(this, line, candidates);
            }
        } catch (Exception e) {
            Log.info("Error while finding completion candidates", e);
            return false;
        }

        if (lst == CompletionType.ExpandComplete || lst == CompletionType.Expand) {
            String w = expander.expandVar(line.word());
            if (!line.word().equals(w)) {
                if (prefix) {
                    buf.backspace(line.wordCursor());
                } else {
                    buf.move(line.word().length() - line.wordCursor());
                    buf.backspace(line.word().length());
                }
                buf.write(w);
                return true;
            }
            if (lst == CompletionType.Expand) {
                return false;
            } else {
                lst = CompletionType.Complete;
            }
        }

        boolean caseInsensitive = isSet(Option.CASE_INSENSITIVE);
        int errors = getInt(ERRORS, DEFAULT_ERRORS);

        // Build a list of sorted candidates
        Map<String, List<Candidate>> sortedCandidates = new HashMap<>();
        for (Candidate cand : candidates) {
            sortedCandidates
                    .computeIfAbsent(AttributedString.fromAnsi(cand.value()).toString(), s -> new ArrayList<>())
                    .add(cand);
        }

        // Find matchers
        // TODO: glob completion
        List<Function<Map<String, List<Candidate>>,
                      Map<String, List<Candidate>>>> matchers;
        Predicate<String> exact;
        if (prefix) {
            String wd = line.word();
            String wdi = caseInsensitive ? wd.toLowerCase() : wd;
            String wp = wdi.substring(0, line.wordCursor());
            matchers = Arrays.asList(
                    simpleMatcher(s -> (caseInsensitive ? s.toLowerCase() : s).startsWith(wp)),
                    simpleMatcher(s -> (caseInsensitive ? s.toLowerCase() : s).contains(wp)),
                    typoMatcher(wp, errors, caseInsensitive)
            );
            exact = s -> caseInsensitive ? s.equalsIgnoreCase(wp) : s.equals(wp);
        } else if (isSet(Option.COMPLETE_IN_WORD)) {
            String wd = line.word();
            String wdi = caseInsensitive ? wd.toLowerCase() : wd;
            String wp = wdi.substring(0, line.wordCursor());
            String ws = wdi.substring(line.wordCursor());
            Pattern p1 = Pattern.compile(Pattern.quote(wp) + ".*" + Pattern.quote(ws) + ".*");
            Pattern p2 = Pattern.compile(".*" + Pattern.quote(wp) + ".*" + Pattern.quote(ws) + ".*");
            matchers = Arrays.asList(
                    simpleMatcher(s -> p1.matcher(caseInsensitive ? s.toLowerCase() : s).matches()),
                    simpleMatcher(s -> p2.matcher(caseInsensitive ? s.toLowerCase() : s).matches()),
                    typoMatcher(wdi, errors, caseInsensitive)
            );
            exact = s -> caseInsensitive ? s.equalsIgnoreCase(wd) : s.equals(wd);
        } else {
            String wd = line.word();
            String wdi = caseInsensitive ? wd.toLowerCase() : wd;
            if (isSet(Option.EMPTY_WORD_OPTIONS) || wd.length() > 0) {
                matchers = Arrays.asList(
                        simpleMatcher(s -> (caseInsensitive ? s.toLowerCase() : s).startsWith(wdi)),
                        simpleMatcher(s -> (caseInsensitive ? s.toLowerCase() : s).contains(wdi)),
                        typoMatcher(wdi, errors, caseInsensitive)
                );
            } else {
                matchers = Arrays.asList(
                        simpleMatcher(s -> !s.startsWith("-"))
                );
            }
            exact = s -> caseInsensitive ? s.equalsIgnoreCase(wd) : s.equals(wd);
        }
        // Find matching candidates
        Map<String, List<Candidate>> matching = Collections.emptyMap();
        for (Function<Map<String, List<Candidate>>,
                      Map<String, List<Candidate>>> matcher : matchers) {
            matching = matcher.apply(sortedCandidates);
            if (!matching.isEmpty()) {
                break;
            }
        }

        // If we have no matches, bail out
        if (matching.isEmpty()) {
            return false;
        }
        size.copy(terminal.getSize());
        try {
            // If we only need to display the list, do it now
            if (lst == CompletionType.List) {
                List<Candidate> possible = matching.entrySet().stream()
                        .flatMap(e -> e.getValue().stream())
                        .collect(Collectors.toList());
                doList(possible, line.word(), false, line::escape, forSuggestion);
                return !possible.isEmpty();
            }

            // Check if there's a single possible match
            Candidate completion = null;
            // If there's a single possible completion
            if (matching.size() == 1) {
                completion = matching.values().stream().flatMap(Collection::stream)
                        .findFirst().orElse(null);
            }
            // Or if RECOGNIZE_EXACT is set, try to find an exact match
            else if (isSet(Option.RECOGNIZE_EXACT)) {
                completion = matching.values().stream().flatMap(Collection::stream)
                        .filter(Candidate::complete)
                        .filter(c -> exact.test(c.value()))
                        .findFirst().orElse(null);
            }
            // Complete and exit
            if (completion != null && !completion.value().isEmpty()) {
                if (prefix) {
                    buf.backspace(line.rawWordCursor());
                } else {
                    buf.move(line.rawWordLength() - line.rawWordCursor());
                    buf.backspace(line.rawWordLength());
                }
                buf.write(line.escape(completion.value(), completion.complete()));
                if (completion.complete()) {
                    if (buf.currChar() != ' ') {
                        buf.write(" ");
                    } else {
                        buf.move(1);
                    }
                }
                if (completion.suffix() != null) {
                    redisplay();
                    Binding op = readBinding(getKeys());
                    if (op != null) {
                        String chars = getString(REMOVE_SUFFIX_CHARS, DEFAULT_REMOVE_SUFFIX_CHARS);
                        String ref = op instanceof Reference ? ((Reference) op).name() : null;
                        if (SELF_INSERT.equals(ref) && chars.indexOf(getLastBinding().charAt(0)) >= 0
                                || ACCEPT_LINE.equals(ref)) {
                            buf.backspace(completion.suffix().length());
                            if (getLastBinding().charAt(0) != ' ') {
                                buf.write(' ');
                            }
                        }
                        pushBackBinding(true);
                    }
                }
                return true;
            }

            List<Candidate> possible = matching.entrySet().stream()
                    .flatMap(e -> e.getValue().stream())
                    .collect(Collectors.toList());

            if (useMenu) {
                buf.move(line.word().length() - line.wordCursor());
                buf.backspace(line.word().length());
                doMenu(possible, line.word(), line::escape);
                return true;
            }

            // Find current word and move to end
            String current;
            if (prefix) {
                current = line.word().substring(0, line.wordCursor());
            } else {
                current = line.word();
                buf.move(line.rawWordLength() - line.rawWordCursor());
            }
            // Now, we need to find the unambiguous completion
            // TODO: need to find common suffix
            String commonPrefix = null;
            for (String key : matching.keySet()) {
                commonPrefix = commonPrefix == null ? key : getCommonStart(commonPrefix, key, caseInsensitive);
            }
            boolean hasUnambiguous = commonPrefix.startsWith(current) && !commonPrefix.equals(current);

            if (hasUnambiguous) {
                buf.backspace(line.rawWordLength());
                buf.write(line.escape(commonPrefix, false));
                callWidget(REDISPLAY);
                current = commonPrefix;
                if ((!isSet(Option.AUTO_LIST) && isSet(Option.AUTO_MENU))
                        || (isSet(Option.AUTO_LIST) && isSet(Option.LIST_AMBIGUOUS))) {
                    if (!nextBindingIsComplete()) {
                        return true;
                    }
                }
            }
            if (isSet(Option.AUTO_LIST)) {
                if (!doList(possible, current, true, line::escape)) {
                    return true;
                }
            }
            if (isSet(Option.AUTO_MENU)) {
                buf.backspace(current.length());
                doMenu(possible, line.word(), line::escape);
            }
            return true;
        } finally {
            size.copy(terminal.getBufferSize());
        }
    }

    private CompletingParsedLine wrap(ParsedLine line) {
        if (line instanceof CompletingParsedLine) {
            return (CompletingParsedLine) line;
        } else {
            return new CompletingParsedLine() {
                public String word() {
                    return line.word();
                }
                public int wordCursor() {
                    return line.wordCursor();
                }
                public int wordIndex() {
                    return line.wordIndex();
                }
                public List<String> words() {
                    return line.words();
                }
                public String line() {
                    return line.line();
                }
                public int cursor() {
                    return line.cursor();
                }
                public CharSequence escape(CharSequence candidate, boolean complete) {
                    return candidate;
                }
                public int rawWordCursor() {
                    return wordCursor();
                }
                public int rawWordLength() {
                    return word().length();
                }
            };
        }
    }

    protected Comparator<Candidate> getCandidateComparator(boolean caseInsensitive, String word) {
        String wdi = caseInsensitive ? word.toLowerCase() : word;
        ToIntFunction<String> wordDistance = w -> distance(wdi, caseInsensitive ? w.toLowerCase() : w);
        return Comparator
                .comparing(Candidate::value, Comparator.comparingInt(wordDistance))
                .thenComparing(Comparator.naturalOrder());
    }

    protected String getOthersGroupName() {
        return getString(OTHERS_GROUP_NAME, DEFAULT_OTHERS_GROUP_NAME);
    }

    protected String getOriginalGroupName() {
        return getString(ORIGINAL_GROUP_NAME, DEFAULT_ORIGINAL_GROUP_NAME);
    }


    protected Comparator<String> getGroupComparator() {
        return Comparator.<String>comparingInt(s -> getOthersGroupName().equals(s) ? 1 : getOriginalGroupName().equals(s) ? -1 : 0)
                .thenComparing(String::toLowerCase, Comparator.naturalOrder());
    }

    private void mergeCandidates(List<Candidate> possible) {
        // Merge candidates if the have the same key
        Map<String, List<Candidate>> keyedCandidates = new HashMap<>();
        for (Candidate candidate : possible) {
            if (candidate.key() != null) {
                List<Candidate> cands = keyedCandidates.computeIfAbsent(candidate.key(), s -> new ArrayList<>());
                cands.add(candidate);
            }
        }
        if (!keyedCandidates.isEmpty()) {
            for (List<Candidate> candidates : keyedCandidates.values()) {
                if (candidates.size() >= 1) {
                    possible.removeAll(candidates);
                    // Candidates with the same key are supposed to have
                    // the same description
                    candidates.sort(Comparator.comparing(Candidate::value));
                    Candidate first = candidates.get(0);
                    String disp = candidates.stream()
                            .map(Candidate::displ)
                            .collect(Collectors.joining(" "));
                    possible.add(new Candidate(first.value(), disp, first.group(),
                            first.descr(), first.suffix(), null, first.complete()));
                }
            }
        }
    }

    private Function<Map<String, List<Candidate>>,
                     Map<String, List<Candidate>>> simpleMatcher(Predicate<String> pred) {
        return m -> m.entrySet().stream()
                .filter(e -> pred.test(e.getKey()))
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
    }

    private Function<Map<String, List<Candidate>>,
                     Map<String, List<Candidate>>> typoMatcher(String word, int errors, boolean caseInsensitive) {
        return m -> {
            Map<String, List<Candidate>> map = m.entrySet().stream()
                    .filter(e -> distance(word, caseInsensitive ? e.getKey() : e.getKey().toLowerCase()) < errors)
                    .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
            if (map.size() > 1) {
                map.computeIfAbsent(word, w -> new ArrayList<>())
                        .add(new Candidate(word, word, getOriginalGroupName(), null, null, null, false));
            }
            return map;
        };
    }

    private int distance(String word, String cand) {
        if (word.length() < cand.length()) {
            int d1 = Levenshtein.distance(word, cand.substring(0, Math.min(cand.length(), word.length())));
            int d2 = Levenshtein.distance(word, cand);
            return Math.min(d1, d2);
        } else {
            return Levenshtein.distance(word, cand);
        }
    }

    protected boolean nextBindingIsComplete() {
        redisplay();
        KeyMap<Binding> keyMap = keyMaps.get(MENU);
        Binding operation = readBinding(getKeys(), keyMap);
        if (operation instanceof Reference && MENU_COMPLETE.equals(((Reference) operation).name())) {
            return true;
        } else {
            pushBackBinding();
            return false;
        }
    }

    private class MenuSupport implements Supplier<AttributedString> {
        final List<Candidate> possible;
        final BiFunction<CharSequence, Boolean, CharSequence> escaper;
        int selection;
        int topLine;
        String word;
        AttributedString computed;
        int lines;
        int columns;
        String completed;

        public MenuSupport(List<Candidate> original, String completed, BiFunction<CharSequence, Boolean, CharSequence> escaper) {
            this.possible = new ArrayList<>();
            this.escaper = escaper;
            this.selection = -1;
            this.topLine = 0;
            this.word = "";
            this.completed = completed;
            computePost(original, null, possible, completed);
            next();
        }

        public Candidate completion() {
            return possible.get(selection);
        }

        public void next() {
            selection = (selection + 1) % possible.size();
            update();
        }

        public void previous() {
            selection = (selection + possible.size() - 1) % possible.size();
            update();
        }

        /**
         * Move 'step' options along the major axis of the menu.<p>
         * ie. if the menu is listing rows first, change row (up/down);
         * otherwise move column (left/right)
         *
         * @param step number of options to move by
         */
        private void major(int step) {
            int axis = isSet(Option.LIST_ROWS_FIRST) ? columns : lines;
            int sel = selection + step * axis;
            if (sel < 0) {
                int pos = (sel + axis) % axis; // needs +axis as (-1)%x == -1
                int remainders = possible.size() % axis;
                sel = possible.size() - remainders + pos;
                if (sel >= possible.size()) {
                    sel -= axis;
                }
            } else if (sel >= possible.size()) {
                sel = sel % axis;
            }
            selection = sel;
            update();
        }

        /**
         * Move 'step' options along the minor axis of the menu.<p>
         * ie. if the menu is listing rows first, move along the row (left/right);
         * otherwise move along the column (up/down)
         *
         * @param step number of options to move by
         */
        private void minor(int step) {
            int axis = isSet(Option.LIST_ROWS_FIRST) ? columns : lines;
            int row = selection % axis;
            int options = possible.size();
            if (selection - row + axis > options) {
                // selection is the last row/column
                // so there are fewer options than other rows
                axis = options%axis;
            }
            selection = selection - row + ((axis + row + step) % axis);
            update();
        }

        public void up() {
            if (isSet(Option.LIST_ROWS_FIRST)) {
                major(-1);
            } else {
                minor(-1);
            }
        }

        public void down() {
            if (isSet(Option.LIST_ROWS_FIRST)) {
                major(1);
            } else {
                minor(1);
            }
        }

        public void left() {
            if (isSet(Option.LIST_ROWS_FIRST)) {
                minor(-1);
            } else {
                major(-1);
            }
        }

        public void right() {
            if (isSet(Option.LIST_ROWS_FIRST)) {
                minor(1);
            } else {
                major(1);
            }
        }

        private void update() {
            buf.backspace(word.length());
            word = escaper.apply(completion().value(), true).toString();
            buf.write(word);

            // Compute displayed prompt
            PostResult pr = computePost(possible, completion(), null, completed);
            AttributedString text = insertSecondaryPrompts(AttributedStringBuilder.append(prompt, buf.toString()), new ArrayList<>());
            int promptLines = text.columnSplitLength(size.getColumns(), false, display.delayLineWrap()).size();
            Status status = Status.getStatus(terminal, false);
            int displaySize = size.getRows() - (status != null ? status.size() : 0) - promptLines;
            if (pr.lines > displaySize) {
                int displayed = displaySize - 1;
                if (pr.selectedLine >= 0) {
                    if (pr.selectedLine < topLine) {
                        topLine = pr.selectedLine;
                    } else if (pr.selectedLine >= topLine + displayed) {
                        topLine = pr.selectedLine - displayed + 1;
                    }
                }
                AttributedString post = pr.post;
                if (post.length() > 0 && post.charAt(post.length() - 1) != '\n') {
                    post = new AttributedStringBuilder(post.length() + 1)
                            .append(post).append("\n").toAttributedString();
                }
                List<AttributedString> lines = post.columnSplitLength(size.getColumns(), true, display.delayLineWrap());
                List<AttributedString> sub = new ArrayList<>(lines.subList(topLine, topLine + displayed));
                sub.add(new AttributedStringBuilder()
                        .style(AttributedStyle.DEFAULT.foreground(AttributedStyle.CYAN))
                        .append("rows ")
                        .append(Integer.toString(topLine + 1))
                        .append(" to ")
                        .append(Integer.toString(topLine + displayed))
                        .append(" of ")
                        .append(Integer.toString(lines.size()))
                        .append("\n")
                        .style(AttributedStyle.DEFAULT).toAttributedString());
                computed = AttributedString.join(AttributedString.EMPTY, sub);
            } else {
                computed = pr.post;
            }
            lines = pr.lines;
            columns = (possible.size() + lines - 1) / lines;
        }

        @Override
        public AttributedString get() {
            return computed;
        }

    }

    protected boolean doMenu(List<Candidate> original, String completed, BiFunction<CharSequence, Boolean, CharSequence> escaper) {
        // Reorder candidates according to display order
        final List<Candidate> possible = new ArrayList<>();
        boolean caseInsensitive = isSet(Option.CASE_INSENSITIVE);
        original.sort(getCandidateComparator(caseInsensitive, completed));
        mergeCandidates(original);
        computePost(original, null, possible, completed);

        // Build menu support
        MenuSupport menuSupport = new MenuSupport(original, completed, escaper);
        post = menuSupport;
        callWidget(REDISPLAY);

        // Loop
        KeyMap<Binding> keyMap = keyMaps.get(MENU);
        Binding operation;
        while ((operation = readBinding(getKeys(), keyMap)) != null) {
            String ref = (operation instanceof Reference) ? ((Reference) operation).name() : "";
            switch (ref) {
                case MENU_COMPLETE:
                    menuSupport.next();
                    break;
                case REVERSE_MENU_COMPLETE:
                    menuSupport.previous();
                    break;
                case UP_LINE_OR_HISTORY:
                case UP_LINE_OR_SEARCH:
                    menuSupport.up();
                    break;
                case DOWN_LINE_OR_HISTORY:
                case DOWN_LINE_OR_SEARCH:
                    menuSupport.down();
                    break;
                case FORWARD_CHAR:
                    menuSupport.right();
                    break;
                case BACKWARD_CHAR:
                    menuSupport.left();
                    break;
                case CLEAR_SCREEN:
                    clearScreen();
                    break;
                default: {
                    Candidate completion = menuSupport.completion();
                    if (completion.suffix() != null) {
                        String chars = getString(REMOVE_SUFFIX_CHARS, DEFAULT_REMOVE_SUFFIX_CHARS);
                        if (SELF_INSERT.equals(ref)
                                && chars.indexOf(getLastBinding().charAt(0)) >= 0
                                || BACKWARD_DELETE_CHAR.equals(ref)) {
                            buf.backspace(completion.suffix().length());
                        }
                    }
                    if (completion.complete()
                            && getLastBinding().charAt(0) != ' '
                            && (SELF_INSERT.equals(ref) || getLastBinding().charAt(0) != ' ')) {
                        buf.write(' ');
                    }
                    if (!ACCEPT_LINE.equals(ref)
                            && !(SELF_INSERT.equals(ref)
                                && completion.suffix() != null
                                && completion.suffix().startsWith(getLastBinding()))) {
                        pushBackBinding(true);
                    }
                    post = null;
                    return true;
                }
            }
            doAutosuggestion = false;
            callWidget(REDISPLAY);
        }
        return false;
    }

    protected boolean clearChoices() {
        return doList(new ArrayList<Candidate>(), "", false, null, false);
    }

    protected boolean doList(List<Candidate> possible
                           , String completed, boolean runLoop, BiFunction<CharSequence, Boolean, CharSequence> escaper) {
        return doList(possible, completed, runLoop, escaper, false);
    }

    protected boolean doList(List<Candidate> possible
                           , String completed
                           , boolean runLoop, BiFunction<CharSequence, Boolean, CharSequence> escaper, boolean forSuggestion) {
        // If we list only and if there's a big
        // number of items, we should ask the user
        // for confirmation, display the list
        // and redraw the line at the bottom
        mergeCandidates(possible);
        AttributedString text = insertSecondaryPrompts(AttributedStringBuilder.append(prompt, buf.toString()), new ArrayList<>());
        int promptLines = text.columnSplitLength(size.getColumns(), false, display.delayLineWrap()).size();
        PostResult postResult = computePost(possible, null, null, completed);
        int lines = postResult.lines;
        int listMax = getInt(LIST_MAX, DEFAULT_LIST_MAX);
        if (listMax > 0 && possible.size() >= listMax
                || lines >= size.getRows() - promptLines) {
            if (!forSuggestion) {
                // prompt
                post = () -> new AttributedString(getAppName() + ": do you wish to see all " + possible.size()
                        + " possibilities (" + lines + " lines)?");
                redisplay(true);
                int c = readCharacter();
                if (c != 'y' && c != 'Y' && c != '\t') {
                    post = null;
                    return false;
                }
            } else {
                return false;
            }
        }

        boolean caseInsensitive = isSet(Option.CASE_INSENSITIVE);
        StringBuilder sb = new StringBuilder();
        while (true) {
            String current = completed + sb.toString();
            List<Candidate> cands;
            if (sb.length() > 0) {
                cands = possible.stream()
                        .filter(c -> caseInsensitive
                                    ? c.value().toLowerCase().startsWith(current.toLowerCase())
                                    : c.value().startsWith(current))
                        .sorted(getCandidateComparator(caseInsensitive, current))
                        .collect(Collectors.toList());
            } else {
                cands = possible.stream()
                        .sorted(getCandidateComparator(caseInsensitive, current))
                        .collect(Collectors.toList());
            }
            post = () -> {
                AttributedString t = insertSecondaryPrompts(AttributedStringBuilder.append(prompt, buf.toString()), new ArrayList<>());
                int pl = t.columnSplitLength(size.getColumns(), false, display.delayLineWrap()).size();
                PostResult pr = computePost(cands, null, null, current);
                if (pr.lines >= size.getRows() - pl) {
                    post = null;
                    int oldCursor = buf.cursor();
                    buf.cursor(buf.length());
                    redisplay(false);
                    buf.cursor(oldCursor);
                    println();
                    List<AttributedString> ls = postResult.post.columnSplitLength(size.getColumns(), false, display.delayLineWrap());
                    Display d = new Display(terminal, false);
                    d.resize(size.getRows(), size.getColumns());
                    d.update(ls, -1);
                    redrawLine();
                    return new AttributedString("");
                }
                return pr.post;
            };
            if (!runLoop) {
                return false;
            }
            redisplay();
            // TODO: use a different keyMap ?
            Binding b = doReadBinding(getKeys(), null);
            if (b instanceof Reference) {
                String name = ((Reference) b).name();
                if (BACKWARD_DELETE_CHAR.equals(name) || VI_BACKWARD_DELETE_CHAR.equals(name)) {
                    if (sb.length() == 0) {
                        pushBackBinding();
                        post = null;
                        return false;
                    } else {
                        sb.setLength(sb.length() - 1);
                        buf.backspace();
                    }
                } else if (SELF_INSERT.equals(name)) {
                    sb.append(getLastBinding());
                    callWidget(name);
                    if (cands.isEmpty()) {
                        post = null;
                        return false;
                    }
                } else if ("\t".equals(getLastBinding())) {
                    if (cands.size() == 1 || sb.length() > 0) {
                        post = null;
                        pushBackBinding();
                    } else if (isSet(Option.AUTO_MENU)) {
                        buf.backspace(escaper.apply(current, false).length());
                        doMenu(cands, current, escaper);
                    }
                    return false;
                } else {
                    pushBackBinding();
                    post = null;
                    return false;
                }
            } else if (b == null) {
                post = null;
                return false;
            }
        }
    }

    protected static class PostResult {
        final AttributedString post;
        final int lines;
        final int selectedLine;

        public PostResult(AttributedString post, int lines, int selectedLine) {
            this.post = post;
            this.lines = lines;
            this.selectedLine = selectedLine;
        }
    }

    protected PostResult computePost(List<Candidate> possible, Candidate selection, List<Candidate> ordered, String completed) {
        return computePost(possible, selection, ordered, completed, display::wcwidth, size.getColumns(), isSet(Option.AUTO_GROUP), isSet(Option.GROUP), isSet(Option.LIST_ROWS_FIRST));
    }

    protected PostResult computePost(List<Candidate> possible, Candidate selection, List<Candidate> ordered, String completed, Function<String, Integer> wcwidth, int width, boolean autoGroup, boolean groupName, boolean rowsFirst) {
        List<Object> strings = new ArrayList<>();
        if (groupName) {
            Comparator<String> groupComparator = getGroupComparator();
            Map<String, Map<String, Candidate>> sorted;
            sorted = groupComparator != null
                        ? new TreeMap<>(groupComparator)
                        : new LinkedHashMap<>();
            for (Candidate cand : possible) {
                String group = cand.group();
                sorted.computeIfAbsent(group != null ? group : "", s -> new LinkedHashMap<>())
                        .put(cand.value(), cand);
            }
            for (Map.Entry<String, Map<String, Candidate>> entry : sorted.entrySet()) {
                String group = entry.getKey();
                if (group.isEmpty() && sorted.size() > 1) {
                    group = getOthersGroupName();
                }
                if (!group.isEmpty() && autoGroup) {
                    strings.add(group);
                }
                strings.add(new ArrayList<>(entry.getValue().values()));
                if (ordered != null) {
                    ordered.addAll(entry.getValue().values());
                }
            }
        } else {
            Set<String> groups = new LinkedHashSet<>();
            TreeMap<String, Candidate> sorted = new TreeMap<>();
            for (Candidate cand : possible) {
                String group = cand.group();
                if (group != null) {
                    groups.add(group);
                }
                sorted.put(cand.value(), cand);
            }
            if (autoGroup) {
                strings.addAll(groups);
            }
            strings.add(new ArrayList<>(sorted.values()));
            if (ordered != null) {
                ordered.addAll(sorted.values());
            }
        }
        return toColumns(strings, selection, completed, wcwidth, width, rowsFirst);
    }

    private static final String DESC_PREFIX = "(";
    private static final String DESC_SUFFIX = ")";
    private static final int MARGIN_BETWEEN_DISPLAY_AND_DESC = 1;
    private static final int MARGIN_BETWEEN_COLUMNS = 3;

    @SuppressWarnings("unchecked")
    protected PostResult toColumns(List<Object> items, Candidate selection, String completed, Function<String, Integer> wcwidth, int width, boolean rowsFirst) {
        int[] out = new int[2];
        // TODO: support Option.LIST_PACKED
        // Compute column width
        int maxWidth = 0;
        for (Object item : items) {
            if (item instanceof String) {
                int len = wcwidth.apply((String) item);
                maxWidth = Math.max(maxWidth, len);
            }
            else if (item instanceof List) {
                for (Candidate cand : (List<Candidate>) item) {
                    int len = wcwidth.apply(cand.displ());
                    if (cand.descr() != null) {
                        len += MARGIN_BETWEEN_DISPLAY_AND_DESC;
                        len += DESC_PREFIX.length();
                        len += wcwidth.apply(cand.descr());
                        len += DESC_SUFFIX.length();
                    }
                    maxWidth = Math.max(maxWidth, len);
                }
            }
        }
        // Build columns
        AttributedStringBuilder sb = new AttributedStringBuilder();
        for (Object list : items) {
            toColumns(list, width, maxWidth, sb, selection, completed, rowsFirst, out);
        }
        if (sb.length() > 0 && sb.charAt(sb.length() - 1) == '\n') {
            sb.setLength(sb.length() - 1);
        }
        return new PostResult(sb.toAttributedString(), out[0], out[1]);
    }

    @SuppressWarnings("unchecked")
    protected void toColumns(Object items, int width, int maxWidth, AttributedStringBuilder sb, Candidate selection, String completed, boolean rowsFirst, int[] out) {
        if (maxWidth <= 0 || width <= 0) {
            return;
        }
        // This is a group
        if (items instanceof String) {
            sb.style(getCompletionStyleGroup())
                    .append((String) items)
                    .style(AttributedStyle.DEFAULT)
                    .append("\n");
            out[0]++;
        }
        // This is a Candidate list
        else if (items instanceof List) {
            List<Candidate> candidates = (List<Candidate>) items;
            maxWidth = Math.min(width, maxWidth);
            int c = width / maxWidth;
            while (c > 1 && c * maxWidth + (c - 1) * MARGIN_BETWEEN_COLUMNS >= width) {
                c--;
            }
            int lines = (candidates.size() + c - 1) / c;
            // Try to minimize the number of columns for the given number of rows
            // Prevents eg 9 candiates being split 6/3 instead of 5/4.
            final int columns = (candidates.size() + lines - 1) / lines;
            IntBinaryOperator index;
            if (rowsFirst) {
                index = (i, j) -> i * columns + j;
            } else {
                index = (i, j) -> j * lines + i;
            }
            for (int i = 0; i < lines; i++) {
                for (int j = 0; j < columns; j++) {
                    int idx = index.applyAsInt(i, j);
                    if (idx < candidates.size()) {
                        Candidate cand = candidates.get(idx);
                        boolean hasRightItem = j < columns - 1 && index.applyAsInt(i, j + 1) < candidates.size();
                        AttributedString left = AttributedString.fromAnsi(cand.displ());
                        AttributedString right = AttributedString.fromAnsi(cand.descr());
                        int lw = left.columnLength();
                        int rw = 0;
                        if (right != null) {
                            int rem = maxWidth - (lw + MARGIN_BETWEEN_DISPLAY_AND_DESC
                                    + DESC_PREFIX.length() + DESC_SUFFIX.length());
                            rw = right.columnLength();
                            if (rw > rem) {
                                right = AttributedStringBuilder.append(
                                            right.columnSubSequence(0, rem - WCWidth.wcwidth('\u2026')),
                                            "\u2026");
                                rw = right.columnLength();
                            }
                            right = AttributedStringBuilder.append(DESC_PREFIX, right, DESC_SUFFIX);
                            rw += DESC_PREFIX.length() + DESC_SUFFIX.length();
                        }
                        if (cand == selection) {
                            out[1] = i;
                            sb.style(getCompletionStyleSelection());
                            if (left.toString().regionMatches(
                                    isSet(Option.CASE_INSENSITIVE), 0, completed, 0, completed.length())) {
                                sb.append(left.toString(), 0, completed.length());
                                sb.append(left.toString(), completed.length(), left.length());
                            } else {
                                sb.append(left.toString());
                            }
                            for (int k = 0; k < maxWidth - lw - rw; k++) {
                                sb.append(' ');
                            }
                            if (right != null) {
                                sb.append(right);
                            }
                            sb.style(AttributedStyle.DEFAULT);
                        } else {
                            if (left.toString().regionMatches(
                                    isSet(Option.CASE_INSENSITIVE), 0, completed, 0, completed.length())) {
                                sb.style(getCompletionStyleStarting());
                                sb.append(left, 0, completed.length());
                                sb.style(AttributedStyle.DEFAULT);
                                sb.append(left, completed.length(), left.length());
                            } else {
                                sb.append(left);
                            }
                            if (right != null || hasRightItem) {
                                for (int k = 0; k < maxWidth - lw - rw; k++) {
                                    sb.append(' ');
                                }
                            }
                            if (right != null) {
                                sb.style(getCompletionStyleDescription());
                                sb.append(right);
                                sb.style(AttributedStyle.DEFAULT);
                            }
                        }
                        if (hasRightItem) {
                            for (int k = 0; k < MARGIN_BETWEEN_COLUMNS; k++) {
                                sb.append(' ');
                            }
                        }
                    }
                }
                sb.append('\n');
            }
            out[0] += lines;
        }
    }

    private AttributedStyle getCompletionStyleStarting() {
        return getCompletionStyle(COMPLETION_STYLE_STARTING, DEFAULT_COMPLETION_STYLE_STARTING);
    }

    protected AttributedStyle getCompletionStyleDescription() {
        return getCompletionStyle(COMPLETION_STYLE_DESCRIPTION, DEFAULT_COMPLETION_STYLE_DESCRIPTION);
    }

    protected AttributedStyle getCompletionStyleGroup() {
        return getCompletionStyle(COMPLETION_STYLE_GROUP, DEFAULT_COMPLETION_STYLE_GROUP);
    }

    protected AttributedStyle getCompletionStyleSelection() {
        return getCompletionStyle(COMPLETION_STYLE_SELECTION, DEFAULT_COMPLETION_STYLE_SELECTION);
    }

    protected AttributedStyle getCompletionStyle(String name, String value) {
        return buildStyle(getString(name, value));
    }

    protected AttributedStyle buildStyle(String str) {
        return AttributedString.fromAnsi("\u001b[" + str + "m ").styleAt(0);
    }

    private String getCommonStart(String str1, String str2, boolean caseInsensitive) {
        int[] s1 = str1.codePoints().toArray();
        int[] s2 = str2.codePoints().toArray();
        int len = 0;
        while (len < Math.min(s1.length, s2.length)) {
            int ch1 = s1[len];
            int ch2 = s2[len];
            if (ch1 != ch2 && caseInsensitive) {
                ch1 = Character.toUpperCase(ch1);
                ch2 = Character.toUpperCase(ch2);
                if (ch1 != ch2) {
                    ch1 = Character.toLowerCase(ch1);
                    ch2 = Character.toLowerCase(ch2);
                }
            }
            if (ch1 != ch2) {
                break;
            }
            len++;
        }
        return new String(s1, 0, len);
    }

    /**
     * Used in "vi" mode for argumented history move, to move a specific
     * number of history entries forward or back.
     *
     * @param next If true, move forward
     * @param count The number of entries to move
     * @return true if the move was successful
     */
    protected boolean moveHistory(final boolean next, int count) {
        boolean ok = true;
        for (int i = 0; i < count && (ok = moveHistory(next)); i++) {
            /* empty */
        }
        return ok;
    }

    /**
     * Move up or down the history tree.
     * @param next <code>true</code> to go to the next, <code>false</code> for the previous.
     * @return <code>true</code> if successful, <code>false</code> otherwise
     */
    protected boolean moveHistory(final boolean next) {
        if (!buf.toString().equals(history.current())) {
            modifiedHistory.put(history.index(), buf.toString());
        }
        if (next && !history.next()) {
            return false;
        }
        else if (!next && !history.previous()) {
            return false;
        }

        setBuffer(modifiedHistory.containsKey(history.index())
                    ? modifiedHistory.get(history.index())
                    : history.current());

        return true;
    }

    //
    // Printing
    //

    /**
     * Raw output printing.
     * @param str the string to print to the terminal
     */
    void print(String str) {
        terminal.writer().write(str);
    }

    void println(String s) {
        print(s);
        println();
    }

    /**
     * Output a platform-dependant newline.
     */
    void println() {
        terminal.puts(Capability.carriage_return);
        print("\n");
        redrawLine();
    }


    //
    // Actions
    //

    protected boolean killBuffer() {
        killRing.add(buf.toString());
        buf.clear();
        return true;
    }

    protected boolean killWholeLine() {
        if (buf.length() == 0) {
            return false;
        }
        int start;
        int end;
        if (count < 0) {
            end = buf.cursor();
            while (buf.atChar(end) != 0 && buf.atChar(end) != '\n') {
                end++;
            }
            start = end;
            for (int count = -this.count; count > 0; --count) {
                while (start > 0 && buf.atChar(start - 1) != '\n') {
                    start--;
                }
                start--;
            }
        } else {
            start = buf.cursor();
            while (start > 0 && buf.atChar(start - 1) != '\n') {
                start--;
            }
            end = start;
            while (count-- > 0) {
                while (end < buf.length() && buf.atChar(end) != '\n') {
                    end++;
                }
                if (end < buf.length()) {
                    end++;
                }
            }
        }
        String killed = buf.substring(start, end);
        buf.cursor(start);
        buf.delete(end - start);
        killRing.add(killed);
        return true;
    }

    /**
     * Kill the buffer ahead of the current cursor position.
     *
     * @return true if successful
     */
    public boolean killLine() {
        if (count < 0) {
            return callNeg(this::backwardKillLine);
        }
        if (buf.cursor() == buf.length()) {
            return false;
        }
        int cp = buf.cursor();
        int len = cp;
        while (count-- > 0) {
            if (buf.atChar(len) == '\n') {
                len++;
            } else {
                while (buf.atChar(len) != 0 && buf.atChar(len) != '\n') {
                    len++;
                }
            }
        }
        int num = len - cp;
        String killed = buf.substring(cp, cp + num);
        buf.delete(num);
        killRing.add(killed);
        return true;
    }

    public boolean backwardKillLine() {
        if (count < 0) {
            return callNeg(this::killLine);
        }
        if (buf.cursor() == 0) {
            return false;
        }
        int cp = buf.cursor();
        int beg = cp;
        while (count-- > 0) {
            if (beg == 0) {
                break;
            }
            if (buf.atChar(beg - 1) == '\n') {
                beg--;
            } else {
                while (beg > 0 && buf.atChar(beg - 1) != 0 && buf.atChar(beg - 1) != '\n') {
                    beg--;
                }
            }
        }
        int num = cp - beg;
        String killed = buf.substring(cp - beg, cp);
        buf.cursor(beg);
        buf.delete(num);
        killRing.add(killed);
        return true;
    }

    public boolean killRegion() {
        return doCopyKillRegion(true);
    }

    public boolean copyRegionAsKill() {
        return doCopyKillRegion(false);
    }

    private boolean doCopyKillRegion(boolean kill) {
        if (regionMark > buf.length()) {
            regionMark = buf.length();
        }
        if (regionActive == RegionType.LINE) {
            int start = regionMark;
            int end = buf.cursor();
            if (start < end) {
                while (start > 0 && buf.atChar(start - 1) != '\n') {
                    start--;
                }
                while (end < buf.length() - 1 && buf.atChar(end + 1) != '\n') {
                    end++;
                }
                if (isInViCmdMode()) {
                    end++;
                }
                killRing.add(buf.substring(start, end));
                if (kill) {
                    buf.backspace(end - start);
                }
            } else {
                while (end > 0 && buf.atChar(end - 1) != '\n') {
                    end--;
                }
                while (start < buf.length() && buf.atChar(start) != '\n') {
                    start++;
                }
                if (isInViCmdMode()) {
                    start++;
                }
                killRing.addBackwards(buf.substring(end, start));
                if (kill) {
                    buf.cursor(end);
                    buf.delete(start - end);
                }
            }
        } else if (regionMark > buf.cursor()) {
            if (isInViCmdMode()) {
                regionMark++;
            }
            killRing.add(buf.substring(buf.cursor(), regionMark));
            if (kill) {
                buf.delete(regionMark - buf.cursor());
            }
        } else {
            if (isInViCmdMode()) {
                buf.move(1);
            }
            killRing.add(buf.substring(regionMark, buf.cursor()));
            if (kill) {
                buf.backspace(buf.cursor() - regionMark);
            }
        }
        if (kill) {
            regionActive = RegionType.NONE;
        }
        return true;
    }

    public boolean yank() {
        String yanked = killRing.yank();
        if (yanked == null) {
            return false;
        } else {
            putString(yanked);
            return true;
        }
    }

    public boolean yankPop() {
        if (!killRing.lastYank()) {
            return false;
        }
        String current = killRing.yank();
        if (current == null) {
            // This shouldn't happen.
            return false;
        }
        buf.backspace(current.length());
        String yanked = killRing.yankPop();
        if (yanked == null) {
            // This shouldn't happen.
            return false;
        }

        putString(yanked);
        return true;
    }

    public boolean mouse() {
        MouseEvent event = readMouseEvent();
        if (event.getType() == MouseEvent.Type.Released
                && event.getButton() == MouseEvent.Button.Button1) {
            StringBuilder tsb = new StringBuilder();
            Cursor cursor = terminal.getCursorPosition(c -> tsb.append((char) c));
            bindingReader.runMacro(tsb.toString());

            List<AttributedString> secondaryPrompts = new ArrayList<>();
            getDisplayedBufferWithPrompts(secondaryPrompts);

            AttributedStringBuilder sb = new AttributedStringBuilder().tabs(TAB_WIDTH);
            sb.append(prompt);
            sb.append(insertSecondaryPrompts(new AttributedString(buf.upToCursor()), secondaryPrompts, false));
            List<AttributedString> promptLines = sb.columnSplitLength(size.getColumns(), false, display.delayLineWrap());

            int currentLine = promptLines.size() - 1;
            int wantedLine = Math.max(0, Math.min(currentLine + event.getY() - cursor.getY(), secondaryPrompts.size()));
            int pl0 = currentLine == 0 ? prompt.columnLength() : secondaryPrompts.get(currentLine - 1).columnLength();
            int pl1 = wantedLine == 0 ? prompt.columnLength() : secondaryPrompts.get(wantedLine - 1).columnLength();
            int adjust = pl1 - pl0;
            buf.moveXY(event.getX() - cursor.getX() - adjust, event.getY() - cursor.getY());
        }
        return true;
    }

    public boolean beginPaste() {
        String str = doReadStringUntil(BRACKETED_PASTE_END);
        regionActive = RegionType.PASTE;
        regionMark = getBuffer().cursor();
        getBuffer().write(str.replace('\r', '\n'));
        return true;
    }

    public boolean focusIn() {
        return false;
    }

    public boolean focusOut() {
        return false;
    }

    /**
     * Clean the used display
     * @return <code>true</code>
     */
    public boolean clear() {
        display.update(Collections.emptyList(), 0);
        return true;
    }

    /**
     * Clear the screen by issuing the ANSI "clear screen" code.
     * @return <code>true</code>
     */
    public boolean clearScreen() {
        if (terminal.puts(Capability.clear_screen)) {
            // ConEMU extended fonts support
            if (AbstractWindowsTerminal.TYPE_WINDOWS_CONEMU.equals(terminal.getType())
                    && !Boolean.getBoolean("org.jline.terminal.conemu.disable-activate")) {
                terminal.writer().write("\u001b[9999E");
            }
            Status status = Status.getStatus(terminal, false);
            if (status != null) {
                status.reset();
            }
            redrawLine();
        } else {
            println();
        }
        return true;
    }

    /**
     * Issue an audible keyboard bell.
     * @return <code>true</code>
     */
    public boolean beep() {
        BellType bell_preference = BellType.AUDIBLE;
        switch (getString(BELL_STYLE, DEFAULT_BELL_STYLE).toLowerCase()) {
            case "none":
            case "off":
                bell_preference = BellType.NONE;
                break;
            case "audible":
                bell_preference = BellType.AUDIBLE;
                break;
            case "visible":
                bell_preference = BellType.VISIBLE;
                break;
            case "on":
                bell_preference = getBoolean(PREFER_VISIBLE_BELL, false)
                        ? BellType.VISIBLE : BellType.AUDIBLE;
                break;
        }
        if (bell_preference == BellType.VISIBLE) {
            if (terminal.puts(Capability.flash_screen)
                    || terminal.puts(Capability.bell)) {
                flush();
            }
        } else if (bell_preference == BellType.AUDIBLE) {
            if (terminal.puts(Capability.bell)) {
                flush();
            }
        }
        return true;
    }

    //
    // Helpers
    //

    /**
     * Checks to see if the specified character is a delimiter. We consider a
     * character a delimiter if it is anything but a letter or digit.
     *
     * @param c     The character to test
     * @return      True if it is a delimiter
     */
    protected boolean isDelimiter(int c) {
        return !Character.isLetterOrDigit(c);
    }

    /**
     * Checks to see if a character is a whitespace character. Currently
     * this delegates to {@link Character#isWhitespace(char)}, however
     * eventually it should be hooked up so that the definition of whitespace
     * can be configured, as readline does.
     *
     * @param c The character to check
     * @return true if the character is a whitespace
     */
    protected boolean isWhitespace(int c) {
        return Character.isWhitespace(c);
    }

    protected boolean isViAlphaNum(int c) {
        return c == '_' || Character.isLetterOrDigit(c);
    }

    protected boolean isAlpha(int c) {
        return Character.isLetter(c);
    }

    protected boolean isWord(int c) {
        String wordchars = getString(WORDCHARS, DEFAULT_WORDCHARS);
        return Character.isLetterOrDigit(c)
                || (c < 128 && wordchars.indexOf((char) c) >= 0);
    }

    String getString(String name, String def) {
        return ReaderUtils.getString(this, name, def);
    }

    boolean getBoolean(String name, boolean def) {
        return ReaderUtils.getBoolean(this, name, def);
    }

    int getInt(String name, int def) {
        return ReaderUtils.getInt(this, name, def);
    }

    long getLong(String name, long def) {
        return ReaderUtils.getLong(this, name, def);
    }

    @Override
    public Map<String, KeyMap<Binding>> defaultKeyMaps() {
        Map<String, KeyMap<Binding>> keyMaps = new HashMap<>();
        keyMaps.put(EMACS, emacs());
        keyMaps.put(VICMD, viCmd());
        keyMaps.put(VIINS, viInsertion());
        keyMaps.put(MENU, menu());
        keyMaps.put(VIOPP, viOpp());
        keyMaps.put(VISUAL, visual());
        keyMaps.put(SAFE, safe());
        if (getBoolean(BIND_TTY_SPECIAL_CHARS, true)) {
            Attributes attr = terminal.getAttributes();
            bindConsoleChars(keyMaps.get(EMACS), attr);
            bindConsoleChars(keyMaps.get(VIINS), attr);
        }
        // Put default
        for (KeyMap<Binding> keyMap : keyMaps.values()) {
            keyMap.setUnicode(new Reference(SELF_INSERT));
            keyMap.setAmbiguousTimeout(getLong(AMBIGUOUS_BINDING, DEFAULT_AMBIGUOUS_BINDING));
        }
        // By default, link main to emacs
        keyMaps.put(MAIN, keyMaps.get(EMACS));
        return keyMaps;
    }

    public KeyMap<Binding> emacs() {
        KeyMap<Binding> emacs = new KeyMap<>();
        bindKeys(emacs);
        bind(emacs, SET_MARK_COMMAND,                       ctrl('@'));
        bind(emacs, BEGINNING_OF_LINE,                      ctrl('A'));
        bind(emacs, BACKWARD_CHAR,                          ctrl('B'));
        bind(emacs, DELETE_CHAR_OR_LIST,                    ctrl('D'));
        bind(emacs, END_OF_LINE,                            ctrl('E'));
        bind(emacs, FORWARD_CHAR,                           ctrl('F'));
        bind(emacs, SEND_BREAK,                             ctrl('G'));
        bind(emacs, BACKWARD_DELETE_CHAR,                   ctrl('H'));
        bind(emacs, EXPAND_OR_COMPLETE,                     ctrl('I'));
        bind(emacs, ACCEPT_LINE,                            ctrl('J'));
        bind(emacs, KILL_LINE,                              ctrl('K'));
        bind(emacs, CLEAR_SCREEN,                           ctrl('L'));
        bind(emacs, ACCEPT_LINE,                            ctrl('M'));
        bind(emacs, DOWN_LINE_OR_HISTORY,                   ctrl('N'));
        bind(emacs, ACCEPT_LINE_AND_DOWN_HISTORY,           ctrl('O'));
        bind(emacs, UP_LINE_OR_HISTORY,                     ctrl('P'));
        bind(emacs, HISTORY_INCREMENTAL_SEARCH_BACKWARD,    ctrl('R'));
        bind(emacs, HISTORY_INCREMENTAL_SEARCH_FORWARD,     ctrl('S'));
        bind(emacs, TRANSPOSE_CHARS,                        ctrl('T'));
        bind(emacs, KILL_WHOLE_LINE,                        ctrl('U'));
        bind(emacs, QUOTED_INSERT,                          ctrl('V'));
        bind(emacs, BACKWARD_KILL_WORD,                     ctrl('W'));
        bind(emacs, YANK,                                   ctrl('Y'));
        bind(emacs, CHARACTER_SEARCH,                       ctrl(']'));
        bind(emacs, UNDO,                                   ctrl('_'));
        bind(emacs, SELF_INSERT,                            range(" -~"));
        bind(emacs, INSERT_CLOSE_PAREN,                     ")");
        bind(emacs, INSERT_CLOSE_SQUARE,                    "]");
        bind(emacs, INSERT_CLOSE_CURLY,                     "}");
        bind(emacs, BACKWARD_DELETE_CHAR,                   del());
        bind(emacs, VI_MATCH_BRACKET,                       translate("^X^B"));
        bind(emacs, SEND_BREAK,                             translate("^X^G"));
        bind(emacs, EDIT_AND_EXECUTE_COMMAND,               translate("^X^E"));
        bind(emacs, VI_FIND_NEXT_CHAR,                      translate("^X^F"));
        bind(emacs, VI_JOIN,                                translate("^X^J"));
        bind(emacs, KILL_BUFFER,                            translate("^X^K"));
        bind(emacs, INFER_NEXT_HISTORY,                     translate("^X^N"));
        bind(emacs, OVERWRITE_MODE,                         translate("^X^O"));
        bind(emacs, REDO,                                   translate("^X^R"));
        bind(emacs, UNDO,                                   translate("^X^U"));
        bind(emacs, VI_CMD_MODE,                            translate("^X^V"));
        bind(emacs, EXCHANGE_POINT_AND_MARK,                translate("^X^X"));
        bind(emacs, DO_LOWERCASE_VERSION,                   translate("^XA-^XZ"));
        bind(emacs, WHAT_CURSOR_POSITION,                   translate("^X="));
        bind(emacs, KILL_LINE,                              translate("^X^?"));
        bind(emacs, SEND_BREAK,                             alt(ctrl('G')));
        bind(emacs, BACKWARD_KILL_WORD,                     alt(ctrl('H')));
        bind(emacs, SELF_INSERT_UNMETA,                     alt(ctrl('M')));
        bind(emacs, COMPLETE_WORD,                          alt(esc()));
        bind(emacs, CHARACTER_SEARCH_BACKWARD,              alt(ctrl(']')));
        bind(emacs, COPY_PREV_WORD,                         alt(ctrl('_')));
        bind(emacs, SET_MARK_COMMAND,                       alt(' '));
        bind(emacs, NEG_ARGUMENT,                           alt('-'));
        bind(emacs, DIGIT_ARGUMENT,                         range("\\E0-\\E9"));
        bind(emacs, BEGINNING_OF_HISTORY,                   alt('<'));
        bind(emacs, LIST_CHOICES,                           alt('='));
        bind(emacs, END_OF_HISTORY,                         alt('>'));
        bind(emacs, LIST_CHOICES,                           alt('?'));
        bind(emacs, DO_LOWERCASE_VERSION,                   range("^[A-^[Z"));
        bind(emacs, ACCEPT_AND_HOLD,                        alt('a'));
        bind(emacs, BACKWARD_WORD,                          alt('b'));
        bind(emacs, CAPITALIZE_WORD,                        alt('c'));
        bind(emacs, KILL_WORD,                              alt('d'));
        bind(emacs, KILL_WORD,                              translate("^[[3;5~")); // ctrl-delete
        bind(emacs, FORWARD_WORD,                           alt('f'));
        bind(emacs, DOWN_CASE_WORD,                         alt('l'));
        bind(emacs, HISTORY_SEARCH_FORWARD,                 alt('n'));
        bind(emacs, HISTORY_SEARCH_BACKWARD,                alt('p'));
        bind(emacs, TRANSPOSE_WORDS,                        alt('t'));
        bind(emacs, UP_CASE_WORD,                           alt('u'));
        bind(emacs, YANK_POP,                               alt('y'));
        bind(emacs, BACKWARD_KILL_WORD,                     alt(del()));
        bindArrowKeys(emacs);
        bind(emacs, FORWARD_WORD,                           translate("^[[1;5C")); // ctrl-left
        bind(emacs, BACKWARD_WORD,                          translate("^[[1;5D")); // ctrl-right
        bind(emacs, FORWARD_WORD,                           alt(key(Capability.key_right)));
        bind(emacs, BACKWARD_WORD,                          alt(key(Capability.key_left)));
        bind(emacs, FORWARD_WORD,                           alt(translate("^[[C")));
        bind(emacs, BACKWARD_WORD,                          alt(translate("^[[D")));
        return emacs;
    }

    public KeyMap<Binding> viInsertion() {
        KeyMap<Binding> viins = new KeyMap<>();
        bindKeys(viins);
        bind(viins, SELF_INSERT,                            range("^@-^_"));
        bind(viins, LIST_CHOICES,                           ctrl('D'));
        bind(viins, SEND_BREAK,                             ctrl('G'));
        bind(viins, BACKWARD_DELETE_CHAR,                   ctrl('H'));
        bind(viins, EXPAND_OR_COMPLETE,                     ctrl('I'));
        bind(viins, ACCEPT_LINE,                            ctrl('J'));
        bind(viins, CLEAR_SCREEN,                           ctrl('L'));
        bind(viins, ACCEPT_LINE,                            ctrl('M'));
        bind(viins, MENU_COMPLETE,                          ctrl('N'));
        bind(viins, REVERSE_MENU_COMPLETE,                  ctrl('P'));
        bind(viins, HISTORY_INCREMENTAL_SEARCH_BACKWARD,    ctrl('R'));
        bind(viins, HISTORY_INCREMENTAL_SEARCH_FORWARD,     ctrl('S'));
        bind(viins, TRANSPOSE_CHARS,                        ctrl('T'));
        bind(viins, KILL_WHOLE_LINE,                        ctrl('U'));
        bind(viins, QUOTED_INSERT,                          ctrl('V'));
        bind(viins, BACKWARD_KILL_WORD,                     ctrl('W'));
        bind(viins, YANK,                                   ctrl('Y'));
        bind(viins, VI_CMD_MODE,                            ctrl('['));
        bind(viins, UNDO,                                   ctrl('_'));
        bind(viins, HISTORY_INCREMENTAL_SEARCH_BACKWARD,    ctrl('X') + "r");
        bind(viins, HISTORY_INCREMENTAL_SEARCH_FORWARD,     ctrl('X') + "s");
        bind(viins, SELF_INSERT,                            range(" -~"));
        bind(viins, INSERT_CLOSE_PAREN,                     ")");
        bind(viins, INSERT_CLOSE_SQUARE,                    "]");
        bind(viins, INSERT_CLOSE_CURLY,                     "}");
        bind(viins, BACKWARD_DELETE_CHAR,                   del());
        bindArrowKeys(viins);
        return viins;
    }

    public KeyMap<Binding> viCmd() {
        KeyMap<Binding> vicmd = new KeyMap<>();
        bind(vicmd, LIST_CHOICES,                           ctrl('D'));
        bind(vicmd, EMACS_EDITING_MODE,                     ctrl('E'));
        bind(vicmd, SEND_BREAK,                             ctrl('G'));
        bind(vicmd, VI_BACKWARD_CHAR,                       ctrl('H'));
        bind(vicmd, ACCEPT_LINE,                            ctrl('J'));
        bind(vicmd, KILL_LINE,                              ctrl('K'));
        bind(vicmd, CLEAR_SCREEN,                           ctrl('L'));
        bind(vicmd, ACCEPT_LINE,                            ctrl('M'));
        bind(vicmd, VI_DOWN_LINE_OR_HISTORY,                ctrl('N'));
        bind(vicmd, VI_UP_LINE_OR_HISTORY,                  ctrl('P'));
        bind(vicmd, QUOTED_INSERT,                          ctrl('Q'));
        bind(vicmd, HISTORY_INCREMENTAL_SEARCH_BACKWARD,    ctrl('R'));
        bind(vicmd, HISTORY_INCREMENTAL_SEARCH_FORWARD,     ctrl('S'));
        bind(vicmd, TRANSPOSE_CHARS,                        ctrl('T'));
        bind(vicmd, KILL_WHOLE_LINE,                        ctrl('U'));
        bind(vicmd, QUOTED_INSERT,                          ctrl('V'));
        bind(vicmd, BACKWARD_KILL_WORD,                     ctrl('W'));
        bind(vicmd, YANK,                                   ctrl('Y'));
        bind(vicmd, HISTORY_INCREMENTAL_SEARCH_BACKWARD,    ctrl('X') + "r");
        bind(vicmd, HISTORY_INCREMENTAL_SEARCH_FORWARD,     ctrl('X') + "s");
        bind(vicmd, SEND_BREAK,                             alt(ctrl('G')));
        bind(vicmd, BACKWARD_KILL_WORD,                     alt(ctrl('H')));
        bind(vicmd, SELF_INSERT_UNMETA,                     alt(ctrl('M')));
        bind(vicmd, COMPLETE_WORD,                          alt(esc()));
        bind(vicmd, CHARACTER_SEARCH_BACKWARD,              alt(ctrl(']')));
        bind(vicmd, SET_MARK_COMMAND,                       alt(' '));
//        bind(vicmd, INSERT_COMMENT,                         alt('#'));
//        bind(vicmd, INSERT_COMPLETIONS,                     alt('*'));
        bind(vicmd, DIGIT_ARGUMENT,                         alt('-'));
        bind(vicmd, BEGINNING_OF_HISTORY,                   alt('<'));
        bind(vicmd, LIST_CHOICES,                           alt('='));
        bind(vicmd, END_OF_HISTORY,                         alt('>'));
        bind(vicmd, LIST_CHOICES,                           alt('?'));
        bind(vicmd, DO_LOWERCASE_VERSION,                   range("^[A-^[Z"));
        bind(vicmd, BACKWARD_WORD,                          alt('b'));
        bind(vicmd, CAPITALIZE_WORD,                        alt('c'));
        bind(vicmd, KILL_WORD,                              alt('d'));
        bind(vicmd, FORWARD_WORD,                           alt('f'));
        bind(vicmd, DOWN_CASE_WORD,                         alt('l'));
        bind(vicmd, HISTORY_SEARCH_FORWARD,                 alt('n'));
        bind(vicmd, HISTORY_SEARCH_BACKWARD,                alt('p'));
        bind(vicmd, TRANSPOSE_WORDS,                        alt('t'));
        bind(vicmd, UP_CASE_WORD,                           alt('u'));
        bind(vicmd, YANK_POP,                               alt('y'));
        bind(vicmd, BACKWARD_KILL_WORD,                     alt(del()));

        bind(vicmd, FORWARD_CHAR,                           " ");
        bind(vicmd, VI_INSERT_COMMENT,                      "#");
        bind(vicmd, END_OF_LINE,                            "$");
        bind(vicmd, VI_MATCH_BRACKET,                       "%");
        bind(vicmd, VI_DOWN_LINE_OR_HISTORY,                "+");
        bind(vicmd, VI_REV_REPEAT_FIND,                     ",");
        bind(vicmd, VI_UP_LINE_OR_HISTORY,                  "-");
        bind(vicmd, VI_REPEAT_CHANGE,                       ".");
        bind(vicmd, VI_HISTORY_SEARCH_BACKWARD,             "/");
        bind(vicmd, VI_DIGIT_OR_BEGINNING_OF_LINE,          "0");
        bind(vicmd, DIGIT_ARGUMENT,                         range("1-9"));
        bind(vicmd, VI_REPEAT_FIND,                         ";");
        bind(vicmd, LIST_CHOICES,                           "=");
        bind(vicmd, VI_HISTORY_SEARCH_FORWARD,              "?");
        bind(vicmd, VI_ADD_EOL,                             "A");
        bind(vicmd, VI_BACKWARD_BLANK_WORD,                 "B");
        bind(vicmd, VI_CHANGE_EOL,                          "C");
        bind(vicmd, VI_KILL_EOL,                            "D");
        bind(vicmd, VI_FORWARD_BLANK_WORD_END,              "E");
        bind(vicmd, VI_FIND_PREV_CHAR,                      "F");
        bind(vicmd, VI_FETCH_HISTORY,                       "G");
        bind(vicmd, VI_INSERT_BOL,                          "I");
        bind(vicmd, VI_JOIN,                                "J");
        bind(vicmd, VI_REV_REPEAT_SEARCH,                   "N");
        bind(vicmd, VI_OPEN_LINE_ABOVE,                     "O");
        bind(vicmd, VI_PUT_BEFORE,                          "P");
        bind(vicmd, VI_REPLACE,                             "R");
        bind(vicmd, VI_KILL_LINE,                           "S");
        bind(vicmd, VI_FIND_PREV_CHAR_SKIP,                 "T");
        bind(vicmd, REDO,                                   "U");
        bind(vicmd, VISUAL_LINE_MODE,                       "V");
        bind(vicmd, VI_FORWARD_BLANK_WORD,                  "W");
        bind(vicmd, VI_BACKWARD_DELETE_CHAR,                "X");
        bind(vicmd, VI_YANK_WHOLE_LINE,                     "Y");
        bind(vicmd, VI_FIRST_NON_BLANK,                     "^");
        bind(vicmd, VI_ADD_NEXT,                            "a");
        bind(vicmd, VI_BACKWARD_WORD,                       "b");
        bind(vicmd, VI_CHANGE,                              "c");
        bind(vicmd, VI_DELETE,                              "d");
        bind(vicmd, VI_FORWARD_WORD_END,                    "e");
        bind(vicmd, VI_FIND_NEXT_CHAR,                      "f");
        bind(vicmd, WHAT_CURSOR_POSITION,                   "ga");
        bind(vicmd, VI_BACKWARD_BLANK_WORD_END,             "gE");
        bind(vicmd, VI_BACKWARD_WORD_END,                   "ge");
        bind(vicmd, VI_BACKWARD_CHAR,                       "h");
        bind(vicmd, VI_INSERT,                              "i");
        bind(vicmd, DOWN_LINE_OR_HISTORY,                   "j");
        bind(vicmd, UP_LINE_OR_HISTORY,                     "k");
        bind(vicmd, VI_FORWARD_CHAR,                        "l");
        bind(vicmd, VI_REPEAT_SEARCH,                       "n");
        bind(vicmd, VI_OPEN_LINE_BELOW,                     "o");
        bind(vicmd, VI_PUT_AFTER,                           "p");
        bind(vicmd, VI_REPLACE_CHARS,                       "r");
        bind(vicmd, VI_SUBSTITUTE,                          "s");
        bind(vicmd, VI_FIND_NEXT_CHAR_SKIP,                 "t");
        bind(vicmd, UNDO,                                   "u");
        bind(vicmd, VISUAL_MODE,                            "v");
        bind(vicmd, VI_FORWARD_WORD,                        "w");
        bind(vicmd, VI_DELETE_CHAR,                         "x");
        bind(vicmd, VI_YANK,                                "y");
        bind(vicmd, VI_GOTO_COLUMN,                         "|");
        bind(vicmd, VI_SWAP_CASE,                           "~");
        bind(vicmd, VI_BACKWARD_CHAR,                       del());

        bindArrowKeys(vicmd);
        return vicmd;
    }

    public KeyMap<Binding> menu() {
        KeyMap<Binding> menu = new KeyMap<>();
        bind(menu, MENU_COMPLETE,                     "\t");
        bind(menu, REVERSE_MENU_COMPLETE,             key(Capability.back_tab));
        bind(menu, ACCEPT_LINE,                       "\r", "\n");
        bindArrowKeys(menu);
        return menu;
    }

    public KeyMap<Binding> safe() {
        KeyMap<Binding> safe = new KeyMap<>();
        bind(safe, SELF_INSERT,                 range("^@-^?"));
        bind(safe, ACCEPT_LINE,                 "\r", "\n");
        bind(safe, SEND_BREAK,                  ctrl('G'));
        return safe;
    }

    public KeyMap<Binding> visual() {
        KeyMap<Binding> visual = new KeyMap<>();
        bind(visual, UP_LINE,                   key(Capability.key_up),     "k");
        bind(visual, DOWN_LINE,                 key(Capability.key_down),   "j");
        bind(visual, this::deactivateRegion,    esc());
        bind(visual, EXCHANGE_POINT_AND_MARK,   "o");
        bind(visual, PUT_REPLACE_SELECTION,     "p");
        bind(visual, VI_DELETE,                 "x");
        bind(visual, VI_OPER_SWAP_CASE,         "~");
        return visual;
    }

    public KeyMap<Binding> viOpp() {
        KeyMap<Binding> viOpp = new KeyMap<>();
        bind(viOpp, UP_LINE,                    key(Capability.key_up),     "k");
        bind(viOpp, DOWN_LINE,                  key(Capability.key_down),   "j");
        bind(viOpp, VI_CMD_MODE,                esc());
        return viOpp;
    }

    private void bind(KeyMap<Binding> map, String widget, Iterable<? extends CharSequence> keySeqs) {
        map.bind(new Reference(widget), keySeqs);
    }

    private void bind(KeyMap<Binding> map, String widget, CharSequence... keySeqs) {
        map.bind(new Reference(widget), keySeqs);
    }

    private void bind(KeyMap<Binding> map, Widget widget, CharSequence... keySeqs) {
        map.bind(widget, keySeqs);
    }

    private String key(Capability capability) {
        return KeyMap.key(terminal, capability);
    }

    private void bindKeys(KeyMap<Binding> emacs) {
        Widget beep = namedWidget("beep", this::beep);
        Stream.of(Capability.values())
                .filter(c -> c.name().startsWith("key_"))
                .map(this::key)
                .forEach(k -> bind(emacs, beep, k));
    }

    private void bindArrowKeys(KeyMap<Binding> map) {
        bind(map, UP_LINE_OR_SEARCH,    key(Capability.key_up));
        bind(map, DOWN_LINE_OR_SEARCH,  key(Capability.key_down));
        bind(map, BACKWARD_CHAR,        key(Capability.key_left));
        bind(map, FORWARD_CHAR,         key(Capability.key_right));
        bind(map, BEGINNING_OF_LINE,    key(Capability.key_home));
        bind(map, END_OF_LINE,          key(Capability.key_end));
        bind(map, DELETE_CHAR,          key(Capability.key_dc));
        bind(map, KILL_WHOLE_LINE,      key(Capability.key_dl));
        bind(map, OVERWRITE_MODE,       key(Capability.key_ic));
        bind(map, MOUSE,                key(Capability.key_mouse));
        bind(map, BEGIN_PASTE,          BRACKETED_PASTE_BEGIN);
        bind(map, FOCUS_IN,             FOCUS_IN_SEQ);
        bind(map, FOCUS_OUT,            FOCUS_OUT_SEQ);
    }

    /**
     * Bind special chars defined by the terminal instead of
     * the default bindings
     */
    private void bindConsoleChars(KeyMap<Binding> keyMap, Attributes attr) {
        if (attr != null) {
            rebind(keyMap, BACKWARD_DELETE_CHAR,
                    del(), (char) attr.getControlChar(ControlChar.VERASE));
            rebind(keyMap, BACKWARD_KILL_WORD,
                    ctrl('W'),  (char) attr.getControlChar(ControlChar.VWERASE));
            rebind(keyMap, KILL_WHOLE_LINE,
                    ctrl('U'), (char) attr.getControlChar(ControlChar.VKILL));
            rebind(keyMap, QUOTED_INSERT,
                    ctrl('V'), (char) attr.getControlChar(ControlChar.VLNEXT));
        }
    }

    private void rebind(KeyMap<Binding> keyMap, String operation, String prevBinding, char newBinding) {
        if (newBinding > 0 && newBinding < 128) {
            Reference ref = new Reference(operation);
            bind(keyMap, SELF_INSERT, prevBinding);
            keyMap.bind(ref, Character.toString(newBinding));
        }
    }

}
