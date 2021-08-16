/*
 * Copyright (c) 2002-2019, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package jdk.internal.org.jline.terminal;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import jdk.internal.org.jline.terminal.impl.AbstractPosixTerminal;
import jdk.internal.org.jline.terminal.impl.AbstractTerminal;
import jdk.internal.org.jline.terminal.impl.DumbTerminal;
import jdk.internal.org.jline.terminal.impl.ExecPty;
import jdk.internal.org.jline.terminal.impl.ExternalTerminal;
import jdk.internal.org.jline.terminal.impl.PosixPtyTerminal;
import jdk.internal.org.jline.terminal.impl.PosixSysTerminal;
import jdk.internal.org.jline.terminal.spi.JansiSupport;
import jdk.internal.org.jline.terminal.spi.JnaSupport;
import jdk.internal.org.jline.terminal.spi.Pty;
import jdk.internal.org.jline.utils.Log;
import jdk.internal.org.jline.utils.OSUtils;

/**
 * Builder class to create terminals.
 */
public final class TerminalBuilder {

    //
    // System properties
    //

    public static final String PROP_ENCODING = "org.jline.terminal.encoding";
    public static final String PROP_CODEPAGE = "org.jline.terminal.codepage";
    public static final String PROP_TYPE = "org.jline.terminal.type";
    public static final String PROP_JNA = "org.jline.terminal.jna";
    public static final String PROP_JANSI = "org.jline.terminal.jansi";
    public static final String PROP_EXEC = "org.jline.terminal.exec";
    public static final String PROP_DUMB = "org.jline.terminal.dumb";
    public static final String PROP_DUMB_COLOR = "org.jline.terminal.dumb.color";

    //
    // Other system properties controlling various jline parts
    //

    public static final String PROP_NON_BLOCKING_READS = "org.jline.terminal.pty.nonBlockingReads";
    public static final String PROP_COLOR_DISTANCE = "org.jline.utils.colorDistance";
    public static final String PROP_DISABLE_ALTERNATE_CHARSET = "org.jline.utils.disableAlternateCharset";

    /**
     * Returns the default system terminal.
     * Terminals should be closed properly using the {@link Terminal#close()}
     * method in order to restore the original terminal state.
     *
     * <p>
     * This call is equivalent to:
     * <code>builder().build()</code>
     * </p>
     *
     * @return the default system terminal
     * @throws IOException if an error occurs
     */
    public static Terminal terminal() throws IOException {
        return builder().build();
    }

    /**
     * Creates a new terminal builder instance.
     *
     * @return a builder
     */
    public static TerminalBuilder builder() {
        return new TerminalBuilder();
    }

    private static final AtomicReference<Terminal> SYSTEM_TERMINAL = new AtomicReference<>();

    private String name;
    private InputStream in;
    private OutputStream out;
    private String type;
    private Charset encoding;
    private int codepage;
    private Boolean system;
    private Boolean jna;
    private Boolean jansi;
    private Boolean exec;
    private Boolean dumb;
    private Attributes attributes;
    private Size size;
    private boolean nativeSignals = false;
    private Terminal.SignalHandler signalHandler = Terminal.SignalHandler.SIG_DFL;
    private boolean paused = false;
    private Function<InputStream, InputStream> inputStreamWrapper = in -> in;

    private TerminalBuilder() {
    }

    public TerminalBuilder name(String name) {
        this.name = name;
        return this;
    }

    public TerminalBuilder streams(InputStream in, OutputStream out) {
        this.in = in;
        this.out = out;
        return this;
    }

    public TerminalBuilder system(boolean system) {
        this.system = system;
        return this;
    }

    public TerminalBuilder jna(boolean jna) {
        this.jna = jna;
        return this;
    }

    public TerminalBuilder jansi(boolean jansi) {
        this.jansi = jansi;
        return this;
    }

    public TerminalBuilder exec(boolean exec) {
        this.exec = exec;
        return this;
    }

    public TerminalBuilder dumb(boolean dumb) {
        this.dumb = dumb;
        return this;
    }

    public TerminalBuilder type(String type) {
        this.type = type;
        return this;
    }

    /**
     * Set the encoding to use for reading/writing from the console.
     * If {@code null} (the default value), JLine will automatically select
     * a {@link Charset}, usually the default system encoding. However,
     * on some platforms (e.g. Windows) it may use a different one depending
     * on the {@link Terminal} implementation.
     *
     * <p>Use {@link Terminal#encoding()} to get the {@link Charset} that
     * should be used for a {@link Terminal}.</p>
     *
     * @param encoding The encoding to use or null to automatically select one
     * @return The builder
     * @throws UnsupportedCharsetException If the given encoding is not supported
     * @see Terminal#encoding()
     */
    public TerminalBuilder encoding(String encoding) throws UnsupportedCharsetException {
        return encoding(encoding != null ? Charset.forName(encoding) : null);
    }

    /**
     * Set the {@link Charset} to use for reading/writing from the console.
     * If {@code null} (the default value), JLine will automatically select
     * a {@link Charset}, usually the default system encoding. However,
     * on some platforms (e.g. Windows) it may use a different one depending
     * on the {@link Terminal} implementation.
     *
     * <p>Use {@link Terminal#encoding()} to get the {@link Charset} that
     * should be used to read/write from a {@link Terminal}.</p>
     *
     * @param encoding The encoding to use or null to automatically select one
     * @return The builder
     * @see Terminal#encoding()
     */
    public TerminalBuilder encoding(Charset encoding) {
        this.encoding = encoding;
        return this;
    }

    /**
     * @param codepage the codepage
     * @return The builder
     * @deprecated JLine now writes Unicode output independently from the selected
     *   code page. Using this option will only make it emulate the selected code
     *   page for {@link Terminal#input()} and {@link Terminal#output()}.
     */
    @Deprecated
    public TerminalBuilder codepage(int codepage) {
        this.codepage = codepage;
        return this;
    }

    /**
     * Attributes to use when creating a non system terminal,
     * i.e. when the builder has been given the input and
     * outut streams using the {@link #streams(InputStream, OutputStream)} method
     * or when {@link #system(boolean)} has been explicitely called with
     * <code>false</code>.
     *
     * @param attributes the attributes to use
     * @return The builder
     * @see #size(Size)
     * @see #system(boolean)
     */
    public TerminalBuilder attributes(Attributes attributes) {
        this.attributes = attributes;
        return this;
    }

    /**
     * Initial size to use when creating a non system terminal,
     * i.e. when the builder has been given the input and
     * outut streams using the {@link #streams(InputStream, OutputStream)} method
     * or when {@link #system(boolean)} has been explicitely called with
     * <code>false</code>.
     *
     * @param size the initial size
     * @return The builder
     * @see #attributes(Attributes)
     * @see #system(boolean)
     */
    public TerminalBuilder size(Size size) {
        this.size = size;
        return this;
    }

    public TerminalBuilder nativeSignals(boolean nativeSignals) {
        this.nativeSignals = nativeSignals;
        return this;
    }

    public TerminalBuilder signalHandler(Terminal.SignalHandler signalHandler) {
        this.signalHandler = signalHandler;
        return this;
    }

    /**
     * Initial paused state of the terminal (defaults to false).
     * By default, the terminal is started, but in some cases,
     * one might want to make sure the input stream is not consumed
     * before needed, in which case the terminal needs to be created
     * in a paused state.
     * @param paused the initial paused state
     * @return The builder
     * @see Terminal#pause()
     */
    public TerminalBuilder paused(boolean paused) {
        this.paused = paused;
        return this;
    }

    public TerminalBuilder inputStreamWrapper(Function<InputStream, InputStream> wrapper) {
        this.inputStreamWrapper = wrapper;
        return this;
    }

    public Terminal build() throws IOException {
        Terminal terminal = doBuild();
        Log.debug(() -> "Using terminal " + terminal.getClass().getSimpleName());
        if (terminal instanceof AbstractPosixTerminal) {
            Log.debug(() -> "Using pty " + ((AbstractPosixTerminal) terminal).getPty().getClass().getSimpleName());
        }
        return terminal;
    }

    private Terminal doBuild() throws IOException {
        String name = this.name;
        if (name == null) {
            name = "JLine terminal";
        }
        Charset encoding = this.encoding;
        if (encoding == null) {
            String charsetName = System.getProperty(PROP_ENCODING);
            if (charsetName != null && Charset.isSupported(charsetName)) {
                encoding = Charset.forName(charsetName);
            }
        }
        int codepage = this.codepage;
        if (codepage <= 0) {
            String str = System.getProperty(PROP_CODEPAGE);
            if (str != null) {
                codepage = Integer.parseInt(str);
            }
        }
        String type = this.type;
        if (type == null) {
            type = System.getProperty(PROP_TYPE);
        }
        if (type == null) {
            type = System.getenv("TERM");
        }
        Boolean jna = this.jna;
        if (jna == null) {
            jna = getBoolean(PROP_JNA, true);
        }
        Boolean jansi = this.jansi;
        if (jansi == null) {
            jansi = getBoolean(PROP_JANSI, true);
        }
        Boolean exec = this.exec;
        if (exec == null) {
            exec = getBoolean(PROP_EXEC, true);
        }
        Boolean dumb = this.dumb;
        if (dumb == null) {
            dumb = getBoolean(PROP_DUMB, null);
        }
        if ((system != null && system) || (system == null && in == null && out == null)) {
            if (attributes != null || size != null) {
                Log.warn("Attributes and size fields are ignored when creating a system terminal");
            }
            IllegalStateException exception = new IllegalStateException("Unable to create a system terminal");
            Terminal terminal = null;
            if (OSUtils.IS_WINDOWS) {
                boolean cygwinTerm = "cygwin".equals(System.getenv("TERM"));
                boolean ansiPassThrough = OSUtils.IS_CONEMU;
                //
                // Cygwin support
                //
                if ((OSUtils.IS_CYGWIN || OSUtils.IS_MSYSTEM) && exec && !cygwinTerm) {
                    try {
                        Pty pty = ExecPty.current();
                        // Cygwin defaults to XTERM, but actually supports 256 colors,
                        // so if the value comes from the environment, change it to xterm-256color
                        if ("xterm".equals(type) && this.type == null && System.getProperty(PROP_TYPE) == null) {
                            type = "xterm-256color";
                        }
                        terminal = new PosixSysTerminal(name, type, pty, inputStreamWrapper.apply(pty.getSlaveInput()), pty.getSlaveOutput(), encoding, nativeSignals, signalHandler);
                    } catch (IOException e) {
                        // Ignore if not a tty
                        Log.debug("Error creating EXEC based terminal: ", e.getMessage(), e);
                        exception.addSuppressed(e);
                    }
                }
                if (jna) {
                    try {
                        terminal = load(JnaSupport.class).winSysTerminal(name, type, ansiPassThrough, encoding, codepage, nativeSignals, signalHandler, paused, inputStreamWrapper);
                    } catch (Throwable t) {
                        Log.debug("Error creating JNA based terminal: ", t.getMessage(), t);
                        exception.addSuppressed(t);
                    }
                }
                if (jansi) {
                    try {
                        terminal = load(JansiSupport.class).winSysTerminal(name, type, ansiPassThrough, encoding, codepage, nativeSignals, signalHandler, paused);
                    } catch (Throwable t) {
                        Log.debug("Error creating JANSI based terminal: ", t.getMessage(), t);
                        exception.addSuppressed(t);
                    }
                }
            } else {
                if (jna) {
                    try {
                        Pty pty = load(JnaSupport.class).current();
                        terminal = new PosixSysTerminal(name, type, pty, inputStreamWrapper.apply(pty.getSlaveInput()), pty.getSlaveOutput(), encoding, nativeSignals, signalHandler);
                    } catch (Throwable t) {
                        // ignore
                        Log.debug("Error creating JNA based terminal: ", t.getMessage(), t);
                        exception.addSuppressed(t);
                    }
                }
                if (jansi) {
                    try {
                        Pty pty = load(JansiSupport.class).current();
                        terminal = new PosixSysTerminal(name, type, pty, inputStreamWrapper.apply(pty.getSlaveInput()), pty.getSlaveOutput(), encoding, nativeSignals, signalHandler);
                    } catch (Throwable t) {
                        Log.debug("Error creating JANSI based terminal: ", t.getMessage(), t);
                        exception.addSuppressed(t);
                    }
                }
                if (exec) {
                    try {
                        Pty pty = ExecPty.current();
                        terminal = new PosixSysTerminal(name, type, pty, inputStreamWrapper.apply(pty.getSlaveInput()), pty.getSlaveOutput(), encoding, nativeSignals, signalHandler);
                    } catch (Throwable t) {
                        // Ignore if not a tty
                        Log.debug("Error creating EXEC based terminal: ", t.getMessage(), t);
                        exception.addSuppressed(t);
                    }
                }
            }
            if (terminal instanceof AbstractTerminal) {
                AbstractTerminal t = (AbstractTerminal) terminal;
                if (SYSTEM_TERMINAL.compareAndSet(null, t)) {
                    t.setOnClose(new Runnable() {
                        @Override
                        public void run() {
                            SYSTEM_TERMINAL.compareAndSet(t, null);
                        }
                    });
                } else {
                    exception.addSuppressed(new IllegalStateException("A system terminal is already running. " +
                            "Make sure to use the created system Terminal on the LineReaderBuilder if you're using one " +
                            "or that previously created system Terminals have been correctly closed."));
                    terminal.close();
                    terminal = null;
                }
            }
            if (terminal == null && (dumb == null || dumb)) {
                // forced colored dumb terminal
                boolean color = getBoolean(PROP_DUMB_COLOR, false);
                // detect emacs using the env variable
                if (!color) {
                    color = System.getenv("INSIDE_EMACS") != null;
                }
                // detect Intellij Idea
                if (!color) {
                    String command = getParentProcessCommand();
                    color = command != null && command.contains("idea");
                }
                if (!color && dumb == null) {
                    if (Log.isDebugEnabled()) {
                        Log.warn("Creating a dumb terminal", exception);
                    } else {
                        Log.warn("Unable to create a system terminal, creating a dumb terminal (enable debug logging for more information)");
                    }
                }
                terminal = new DumbTerminal(name, color ? Terminal.TYPE_DUMB_COLOR : Terminal.TYPE_DUMB,
                        new FileInputStream(FileDescriptor.in),
                        new FileOutputStream(FileDescriptor.out),
                        encoding, signalHandler);
            }
            if (terminal == null) {
                throw exception;
            }
            return terminal;
        } else {
            if (jna) {
                try {
                    Pty pty = load(JnaSupport.class).open(attributes, size);
                    return new PosixPtyTerminal(name, type, pty, in, out, encoding, signalHandler, paused);
                } catch (Throwable t) {
                    Log.debug("Error creating JNA based terminal: ", t.getMessage(), t);
                }
            }
            if (jansi) {
                try {
                    Pty pty = load(JansiSupport.class).open(attributes, size);
                    return new PosixPtyTerminal(name, type, pty, in, out, encoding, signalHandler, paused);
                } catch (Throwable t) {
                    Log.debug("Error creating JANSI based terminal: ", t.getMessage(), t);
                }
            }
            return new ExternalTerminal(name, type, in, out, encoding, signalHandler, paused, attributes, size);
        }
    }

    private static String getParentProcessCommand() {
        try {
            Class<?> phClass = Class.forName("java.lang.ProcessHandle");
            Object current = phClass.getMethod("current").invoke(null);
            Object parent = ((Optional<?>) phClass.getMethod("parent").invoke(current)).orElse(null);
            Method infoMethod = phClass.getMethod("info");
            Object info = infoMethod.invoke(parent);
            Object command = ((Optional<?>) infoMethod.getReturnType().getMethod("command").invoke(info)).orElse(null);
            return (String) command;
        } catch (Throwable t) {
            return null;
        }
    }

    private static Boolean getBoolean(String name, Boolean def) {
        try {
            String str = System.getProperty(name);
            if (str != null) {
                return Boolean.parseBoolean(str);
            }
        } catch (IllegalArgumentException | NullPointerException e) {
        }
        return def;
    }

    private <S> S load(Class<S> clazz) {
        return ServiceLoader.load(clazz, clazz.getClassLoader()).iterator().next();
    }
}
