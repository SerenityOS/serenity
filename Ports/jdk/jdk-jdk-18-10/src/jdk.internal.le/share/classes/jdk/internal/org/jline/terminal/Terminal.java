/*
 * Copyright (c) 2002-2018, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package jdk.internal.org.jline.terminal;

import java.io.Closeable;
import java.io.Flushable;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

import jdk.internal.org.jline.terminal.impl.NativeSignalHandler;
import jdk.internal.org.jline.utils.InfoCmp.Capability;
import jdk.internal.org.jline.utils.NonBlockingReader;

/**
 * A terminal representing a virtual terminal on the computer.
 *
 * Terminals should be closed by calling the {@link #close()} method
 * in order to restore their original state.
 */
public interface Terminal extends Closeable, Flushable {

    /**
     * Type used for dumb terminals.
     */
    String TYPE_DUMB = "dumb";
    String TYPE_DUMB_COLOR = "dumb-color";

    String getName();

    //
    // Signal support
    //

    enum Signal {
        INT,
        QUIT,
        TSTP,
        CONT,
        INFO,
        WINCH
    }

    interface SignalHandler {

        SignalHandler SIG_DFL = NativeSignalHandler.SIG_DFL;
        SignalHandler SIG_IGN = NativeSignalHandler.SIG_IGN;

        void handle(Signal signal);
    }

    SignalHandler handle(Signal signal, SignalHandler handler);

    void raise(Signal signal);

    //
    // Input / output
    //

    /**
     * Retrieve the <code>Reader</code> for this terminal.
     * This is the standard way to read input from this terminal.
     * The reader is non blocking.
     *
     * @return The non blocking reader
     */
    NonBlockingReader reader();

    /**
     * Retrieve the <code>Writer</code> for this terminal.
     * This is the standard way to write to this terminal.
     *
     * @return The writer
     */
    PrintWriter writer();

    /**
     * Returns the {@link Charset} that should be used to encode characters
     * for {@link #input()} and {@link #output()}.
     *
     * @return The terminal encoding
     */
    Charset encoding();

    /**
     * Retrieve the input stream for this terminal.
     * In some rare cases, there may be a need to access the
     * terminal input stream directly. In the usual cases,
     * use the {@link #reader()} instead.
     *
     * @return The input stream
     *
     * @see #reader()
     */
    InputStream input();

    /**
     * Retrieve the output stream for this terminal.
     * In some rare cases, there may be a need to access the
     * terminal output stream directly. In the usual cases,
     * use the {@link #writer()} instead.
     *
     * @return The output stream
     *
     * @see #writer()
     */
    OutputStream output();

    //
    // Input control
    //

    /**
     * Whether this terminal supports {@link #pause()} and {@link #resume()} calls.
     *
     * @return whether this terminal supports {@link #pause()} and {@link #resume()} calls.
     * @see #paused()
     * @see #pause()
     * @see #resume()
     */
    boolean canPauseResume();

    /**
     * Stop reading the input stream.
     *
     * @see #resume()
     * @see #paused()
     */
    void pause();

    /**
     * Stop reading the input stream and optionally wait for the underlying threads to finish.
     *
     * @param wait <code>true</code> to wait until the terminal is actually paused
     * @throws InterruptedException if the call has been interrupted
     */
    void pause(boolean wait) throws InterruptedException;

    /**
     * Resume reading the input stream.
     *
     * @see #pause()
     * @see #paused()
     */
    void resume();

    /**
     * Check whether the terminal is currently reading the input stream or not.
     * In order to process signal as quickly as possible, the terminal need to read
     * the input stream and buffer it internally so that it can detect specific
     * characters in the input stream (Ctrl+C, Ctrl+D, etc...) and raise the
     * appropriate signals.
     * However, there are some cases where this processing should be disabled, for
     * example when handing the terminal control to a subprocess.
     *
     * @return whether the terminal is currently reading the input stream or not
     *
     * @see #pause()
     * @see #resume()
     */
    boolean paused();

    //
    // Pty settings
    //

    Attributes enterRawMode();

    boolean echo();

    boolean echo(boolean echo);

    Attributes getAttributes();

    void setAttributes(Attributes attr);

    /**
     * Retrieve the size of the visible window
     * @return the visible terminal size
     * @see #getBufferSize()
     */
    Size getSize();

    void setSize(Size size);

    default int getWidth() {
        return getSize().getColumns();
    }

    default int getHeight() {
        return getSize().getRows();
    }

    /**
     * Retrieve the size of the window buffer.
     * Some terminals can be configured to have a buffer size
     * larger than the visible window size and provide scroll bars.
     * In such cases, this method should attempt to return the size
     * of the whole buffer.  The <code>getBufferSize()</code> method
     * can be used to avoid wrapping when using the terminal in a line
     * editing mode, while the {@link #getSize()} method should be
     * used when using full screen mode.
     * @return the terminal buffer size
     * @see #getSize()
     */
    default Size getBufferSize() {
        return getSize();
    }

    void flush();

    //
    // Infocmp capabilities
    //

    String getType();

    boolean puts(Capability capability, Object... params);

    boolean getBooleanCapability(Capability capability);

    Integer getNumericCapability(Capability capability);

    String getStringCapability(Capability capability);

    //
    // Cursor support
    //

    /**
     * Query the terminal to report the cursor position.
     *
     * As the response is read from the input stream, some
     * characters may be read before the cursor position is actually
     * read. Those characters can be given back using
     * <code>org.jline.keymap.BindingReader#runMacro(String)</code>
     *
     * @param discarded a consumer receiving discarded characters
     * @return <code>null</code> if cursor position reporting
     *                  is not supported or a valid cursor position
     */
    Cursor getCursorPosition(IntConsumer discarded);

    //
    // Mouse support
    //

    enum MouseTracking {
        /**
         * Disable mouse tracking
         */
        Off,
        /**
         * Track button press and release.
         */
        Normal,
        /**
         * Also report button-motion events.  Mouse movements are reported if the mouse pointer
         * has moved to a different character cell.
         */
        Button,
        /**
         * Report all motions events, even if no mouse button is down.
         */
        Any
    }

    /**
     * Returns <code>true</code> if the terminal has support for mouse.
     * @return whether mouse is supported by the terminal
     * @see #trackMouse(MouseTracking)
     */
    boolean hasMouseSupport();

    /**
     * Change the mouse tracking mouse.
     * To start mouse tracking, this method must be called with a valid mouse tracking mode.
     * Mouse events will be reported by writing the {@link Capability#key_mouse} to the input stream.
     * When this character sequence is detected, the {@link #readMouseEvent()} method can be
     * called to actually read the corresponding mouse event.
     *
     * @param tracking the mouse tracking mode
     * @return <code>true</code> if mouse tracking is supported
     */
    boolean trackMouse(MouseTracking tracking);

    /**
     * Read a MouseEvent from the terminal input stream.
     * Such an event must have been detected by scanning the terminal's {@link Capability#key_mouse}
     * in the stream immediately before reading the event.
     *
     * @return the decoded mouse event.
     * @see #trackMouse(MouseTracking)
     */
    MouseEvent readMouseEvent();

    /**
     * Read a MouseEvent from the given input stream.
     *
     * @param reader the input supplier
     * @return the decoded mouse event
     */
    MouseEvent readMouseEvent(IntSupplier reader);

    /**
     * Returns <code>true</code> if the terminal has support for focus tracking.
     * @return whether focus tracking is supported by the terminal
     * @see #trackFocus(boolean)
     */
    boolean hasFocusSupport();

    /**
     * Enable or disable focus tracking mode.
     * When focus tracking has been activated, each time the terminal grabs the focus,
     * the string "\33[I" will be sent to the input stream and each time the focus is lost,
     * the string "\33[O" will be sent to the input stream.
     *
     * @param tracking whether the focus tracking mode should be enabled or not
     * @return <code>true</code> if focus tracking is supported
     */
    boolean trackFocus(boolean tracking);
}
