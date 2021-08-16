/*
 * Copyright (c) 2002-2019, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package jdk.internal.org.jline.terminal.impl.jna.win;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.function.Function;
import java.util.function.IntConsumer;

//import com.sun.jna.LastErrorException;
//import com.sun.jna.Pointer;
//import com.sun.jna.ptr.IntByReference;

import jdk.internal.org.jline.terminal.Cursor;
import jdk.internal.org.jline.terminal.Size;
import jdk.internal.org.jline.terminal.Terminal;
import jdk.internal.org.jline.terminal.impl.AbstractWindowsTerminal;
import jdk.internal.org.jline.utils.InfoCmp;
import jdk.internal.org.jline.utils.OSUtils;

public class JnaWinSysTerminal extends AbstractWindowsTerminal {

    private static final Pointer consoleIn = Kernel32.INSTANCE.GetStdHandle(Kernel32.STD_INPUT_HANDLE);
    private static final Pointer consoleOut = Kernel32.INSTANCE.GetStdHandle(Kernel32.STD_OUTPUT_HANDLE);

    public static JnaWinSysTerminal createTerminal(String name, String type, boolean ansiPassThrough, Charset encoding, int codepage, boolean nativeSignals, SignalHandler signalHandler, boolean paused, Function<InputStream, InputStream> inputStreamWrapper) throws IOException {
        Writer writer;
        if (ansiPassThrough) {
            if (type == null) {
                type = OSUtils.IS_CONEMU ? TYPE_WINDOWS_CONEMU : TYPE_WINDOWS;
            }
            writer = new JnaWinConsoleWriter(consoleOut);
        } else {
            IntByReference mode = new IntByReference();
            Kernel32.INSTANCE.GetConsoleMode(consoleOut, mode);
            try {
                Kernel32.INSTANCE.SetConsoleMode(consoleOut, mode.getValue() | AbstractWindowsTerminal.ENABLE_VIRTUAL_TERMINAL_PROCESSING);
                if (type == null) {
                    type = TYPE_WINDOWS_VTP;
                }
                writer = new JnaWinConsoleWriter(consoleOut);
            } catch (LastErrorException e) {
                if (OSUtils.IS_CONEMU) {
                    if (type == null) {
                        type = TYPE_WINDOWS_CONEMU;
                    }
                    writer = new JnaWinConsoleWriter(consoleOut);
                } else {
                    if (type == null) {
                        type = TYPE_WINDOWS;
                    }
                    writer = new WindowsAnsiWriter(new BufferedWriter(new JnaWinConsoleWriter(consoleOut)), consoleOut);
                }
            }
        }
        JnaWinSysTerminal terminal = new JnaWinSysTerminal(writer, name, type, encoding, codepage, nativeSignals, signalHandler, inputStreamWrapper);
        // Start input pump thread
        if (!paused) {
            terminal.resume();
        }
        return terminal;
    }

    JnaWinSysTerminal(Writer writer, String name, String type, Charset encoding, int codepage, boolean nativeSignals, SignalHandler signalHandler, Function<InputStream, InputStream> inputStreamWrapper) throws IOException {
        super(writer, name, type, encoding, codepage, nativeSignals, signalHandler, inputStreamWrapper);
        strings.put(InfoCmp.Capability.key_mouse, "\\E[M");
    }

    @Override
    protected int getConsoleOutputCP() {
        return Kernel32.INSTANCE.GetConsoleOutputCP();
    }

    @Override
    protected int getConsoleMode() {
        IntByReference mode = new IntByReference();
        Kernel32.INSTANCE.GetConsoleMode(consoleIn, mode);
        return mode.getValue();
    }

    @Override
    protected void setConsoleMode(int mode) {
        Kernel32.INSTANCE.SetConsoleMode(consoleIn, mode);
    }

    public Size getSize() {
        Kernel32.CONSOLE_SCREEN_BUFFER_INFO info = new Kernel32.CONSOLE_SCREEN_BUFFER_INFO();
        Kernel32.INSTANCE.GetConsoleScreenBufferInfo(consoleOut, info);
        return new Size(info.windowWidth(), info.windowHeight());
    }

    public Size getBufferSize() {
        Kernel32.CONSOLE_SCREEN_BUFFER_INFO info = new Kernel32.CONSOLE_SCREEN_BUFFER_INFO();
        Kernel32.INSTANCE.GetConsoleScreenBufferInfo(consoleOut, info);
        return new Size(info.dwSize.X, info.dwSize.Y);
    }

    protected boolean processConsoleInput() throws IOException {
        Kernel32.INPUT_RECORD event = readConsoleInput(100);
        if (event == null) {
            return false;
        }

        switch (event.EventType) {
            case Kernel32.INPUT_RECORD.KEY_EVENT:
                processKeyEvent(event.Event.KeyEvent);
                return true;
            case Kernel32.INPUT_RECORD.WINDOW_BUFFER_SIZE_EVENT:
                raise(Signal.WINCH);
                return false;
            case Kernel32.INPUT_RECORD.MOUSE_EVENT:
                processMouseEvent(event.Event.MouseEvent);
                return true;
            case Kernel32.INPUT_RECORD.FOCUS_EVENT:
                processFocusEvent(event.Event.FocusEvent.bSetFocus);
                return true;
            default:
                // Skip event
                return false;
        }
    }

    private void processKeyEvent(Kernel32.KEY_EVENT_RECORD keyEvent) throws IOException {
        processKeyEvent(keyEvent.bKeyDown, keyEvent.wVirtualKeyCode, keyEvent.uChar.UnicodeChar, keyEvent.dwControlKeyState);
    }

    private char[] focus = new char[] { '\033', '[', ' ' };

    private void processFocusEvent(boolean hasFocus) throws IOException {
        if (focusTracking) {
            focus[2] = hasFocus ? 'I' : 'O';
            slaveInputPipe.write(focus);
        }
    }

    private char[] mouse = new char[] { '\033', '[', 'M', ' ', ' ', ' ' };

    private void processMouseEvent(Kernel32.MOUSE_EVENT_RECORD mouseEvent) throws IOException {
        int dwEventFlags = mouseEvent.dwEventFlags;
        int dwButtonState = mouseEvent.dwButtonState;
        if (tracking == MouseTracking.Off
                || tracking == MouseTracking.Normal && dwEventFlags == Kernel32.MOUSE_MOVED
                || tracking == MouseTracking.Button && dwEventFlags == Kernel32.MOUSE_MOVED && dwButtonState == 0) {
            return;
        }
        int cb = 0;
        dwEventFlags &= ~ Kernel32.DOUBLE_CLICK; // Treat double-clicks as normal
        if (dwEventFlags == Kernel32.MOUSE_WHEELED) {
            cb |= 64;
            if ((dwButtonState >> 16) < 0) {
                cb |= 1;
            }
        } else if (dwEventFlags == Kernel32.MOUSE_HWHEELED) {
            return;
        } else if ((dwButtonState & Kernel32.FROM_LEFT_1ST_BUTTON_PRESSED) != 0) {
            cb |= 0x00;
        } else if ((dwButtonState & Kernel32.RIGHTMOST_BUTTON_PRESSED) != 0) {
            cb |= 0x01;
        } else if ((dwButtonState & Kernel32.FROM_LEFT_2ND_BUTTON_PRESSED) != 0) {
            cb |= 0x02;
        } else {
            cb |= 0x03;
        }
        int cx = mouseEvent.dwMousePosition.X;
        int cy = mouseEvent.dwMousePosition.Y;
        mouse[3] = (char) (' ' + cb);
        mouse[4] = (char) (' ' + cx + 1);
        mouse[5] = (char) (' ' + cy + 1);
        slaveInputPipe.write(mouse);
    }

    private final Kernel32.INPUT_RECORD[] inputEvents = new Kernel32.INPUT_RECORD[1];
    private final IntByReference eventsRead = new IntByReference();

    private Kernel32.INPUT_RECORD readConsoleInput(int dwMilliseconds) throws IOException {
        if (Kernel32.INSTANCE.WaitForSingleObject(consoleIn, dwMilliseconds) != 0) {
            return null;
        }
        Kernel32.INSTANCE.ReadConsoleInput(consoleIn, inputEvents, 1, eventsRead);
        if (eventsRead.getValue() == 1) {
            return inputEvents[0];
        } else {
            return null;
        }
    }

    @Override
    public Cursor getCursorPosition(IntConsumer discarded) {
        Kernel32.CONSOLE_SCREEN_BUFFER_INFO info = new Kernel32.CONSOLE_SCREEN_BUFFER_INFO();
        Kernel32.INSTANCE.GetConsoleScreenBufferInfo(consoleOut, info);
        return new Cursor(info.dwCursorPosition.X, info.dwCursorPosition.Y);
    }

}
