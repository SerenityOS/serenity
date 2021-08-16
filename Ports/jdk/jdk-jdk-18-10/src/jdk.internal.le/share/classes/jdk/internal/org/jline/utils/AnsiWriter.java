/*
 * Copyright (C) 2009-2018 the original author(s).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jdk.internal.org.jline.utils;

import java.io.FilterWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * A ANSI writer extracts ANSI escape codes written to
 * a {@link Writer} and calls corresponding <code>process*</code> methods.
 *
 * For more information about ANSI escape codes, see:
 * http://en.wikipedia.org/wiki/ANSI_escape_code
 *
 * This class just filters out the escape codes so that they are not
 * sent out to the underlying {@link Writer}: <code>process*</code> methods
 * are empty. Subclasses should actually perform the ANSI escape behaviors
 * by implementing active code in <code>process*</code> methods.
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 * @author Joris Kuipers
 * @since 1.0
 */
public class AnsiWriter extends FilterWriter {

    private static final char[] RESET_CODE = "\033[0m".toCharArray();

    public AnsiWriter(Writer out) {
        super(out);
    }

    private final static int MAX_ESCAPE_SEQUENCE_LENGTH = 100;
    private final char[] buffer = new char[MAX_ESCAPE_SEQUENCE_LENGTH];
    private int pos = 0;
    private int startOfValue;
    private final ArrayList<Object> options = new ArrayList<>();

    private static final int LOOKING_FOR_FIRST_ESC_CHAR = 0;
    private static final int LOOKING_FOR_SECOND_ESC_CHAR = 1;
    private static final int LOOKING_FOR_NEXT_ARG = 2;
    private static final int LOOKING_FOR_STR_ARG_END = 3;
    private static final int LOOKING_FOR_INT_ARG_END = 4;
    private static final int LOOKING_FOR_OSC_COMMAND = 5;
    private static final int LOOKING_FOR_OSC_COMMAND_END = 6;
    private static final int LOOKING_FOR_OSC_PARAM = 7;
    private static final int LOOKING_FOR_ST = 8;
    private static final int LOOKING_FOR_CHARSET = 9;

    int state = LOOKING_FOR_FIRST_ESC_CHAR;

    private static final int FIRST_ESC_CHAR = 27;
    private static final int SECOND_ESC_CHAR = '[';
    private static final int SECOND_OSC_CHAR = ']';
    private static final int BEL = 7;
    private static final int SECOND_ST_CHAR = '\\';
    private static final int SECOND_CHARSET0_CHAR = '(';
    private static final int SECOND_CHARSET1_CHAR = ')';

    @Override
    public synchronized void write(int data) throws IOException {
        switch (state) {
            case LOOKING_FOR_FIRST_ESC_CHAR:
                if (data == FIRST_ESC_CHAR) {
                    buffer[pos++] = (char) data;
                    state = LOOKING_FOR_SECOND_ESC_CHAR;
                } else {
                    out.write(data);
                }
                break;

            case LOOKING_FOR_SECOND_ESC_CHAR:
                buffer[pos++] = (char) data;
                if (data == SECOND_ESC_CHAR) {
                    state = LOOKING_FOR_NEXT_ARG;
                } else if (data == SECOND_OSC_CHAR) {
                    state = LOOKING_FOR_OSC_COMMAND;
                } else if (data == SECOND_CHARSET0_CHAR) {
                    options.add((int) '0');
                    state = LOOKING_FOR_CHARSET;
                } else if (data == SECOND_CHARSET1_CHAR) {
                    options.add((int) '1');
                    state = LOOKING_FOR_CHARSET;
                } else {
                    reset(false);
                }
                break;

            case LOOKING_FOR_NEXT_ARG:
                buffer[pos++] = (char) data;
                if ('"' == data) {
                    startOfValue = pos - 1;
                    state = LOOKING_FOR_STR_ARG_END;
                } else if ('0' <= data && data <= '9') {
                    startOfValue = pos - 1;
                    state = LOOKING_FOR_INT_ARG_END;
                } else if (';' == data) {
                    options.add(null);
                } else if ('?' == data) {
                    options.add('?');
                } else if ('=' == data) {
                    options.add('=');
                } else {
                    boolean skip = true;
                    try {
                        skip = processEscapeCommand(options, data);
                    } finally {
                        reset(skip);
                    }
                }
                break;
            default:
                break;

            case LOOKING_FOR_INT_ARG_END:
                buffer[pos++] = (char) data;
                if (!('0' <= data && data <= '9')) {
                    String strValue = new String(buffer, startOfValue, (pos - 1) - startOfValue);
                    Integer value = Integer.valueOf(strValue);
                    options.add(value);
                    if (data == ';') {
                        state = LOOKING_FOR_NEXT_ARG;
                    } else {
                        boolean skip = true;
                        try {
                            skip = processEscapeCommand(options, data);
                        } finally {
                            reset(skip);
                        }
                    }
                }
                break;

            case LOOKING_FOR_STR_ARG_END:
                buffer[pos++] = (char) data;
                if ('"' != data) {
                    String value = new String(buffer, startOfValue, (pos - 1) - startOfValue);
                    options.add(value);
                    if (data == ';') {
                        state = LOOKING_FOR_NEXT_ARG;
                    } else {
                        reset(processEscapeCommand(options, data));
                    }
                }
                break;

            case LOOKING_FOR_OSC_COMMAND:
                buffer[pos++] = (char) data;
                if ('0' <= data && data <= '9') {
                    startOfValue = pos - 1;
                    state = LOOKING_FOR_OSC_COMMAND_END;
                } else {
                    reset(false);
                }
                break;

            case LOOKING_FOR_OSC_COMMAND_END:
                buffer[pos++] = (char) data;
                if (';' == data) {
                    String strValue = new String(buffer, startOfValue, (pos - 1) - startOfValue);
                    Integer value = Integer.valueOf(strValue);
                    options.add(value);
                    startOfValue = pos;
                    state = LOOKING_FOR_OSC_PARAM;
                } else if ('0' <= data && data <= '9') {
                    // already pushed digit to buffer, just keep looking
                } else {
                    // oops, did not expect this
                    reset(false);
                }
                break;

            case LOOKING_FOR_OSC_PARAM:
                buffer[pos++] = (char) data;
                if (BEL == data) {
                    String value = new String(buffer, startOfValue, (pos - 1) - startOfValue);
                    options.add(value);
                    boolean skip = true;
                    try {
                        skip = processOperatingSystemCommand(options);
                    } finally {
                        reset(skip);
                    }
                } else if (FIRST_ESC_CHAR == data) {
                    state = LOOKING_FOR_ST;
                } else {
                    // just keep looking while adding text
                }
                break;

            case LOOKING_FOR_ST:
                buffer[pos++] = (char) data;
                if (SECOND_ST_CHAR == data) {
                    String value = new String(buffer, startOfValue, (pos - 2) - startOfValue);
                    options.add(value);
                    boolean skip = true;
                    try {
                        skip = processOperatingSystemCommand(options);
                    } finally {
                        reset(skip);
                    }
                } else {
                    state = LOOKING_FOR_OSC_PARAM;
                }
                break;

            case LOOKING_FOR_CHARSET:
                options.add((char) data);
                reset(processCharsetSelect(options));
                break;
        }

        // Is it just too long?
        if (pos >= buffer.length) {
            reset(false);
        }
    }

    /**
     * Resets all state to continue with regular parsing
     * @param skipBuffer if current buffer should be skipped or written to out
     * @throws IOException if an error occurs
     */
    private void reset(boolean skipBuffer) throws IOException {
        if (!skipBuffer) {
            out.write(buffer, 0, pos);
        }
        pos = 0;
        startOfValue = 0;
        options.clear();
        state = LOOKING_FOR_FIRST_ESC_CHAR;
    }

    /**
     * Helper for processEscapeCommand() to iterate over integer options
     * @param  optionsIterator  the underlying iterator
     * @throws IOException      if no more non-null values left
     */
    private int getNextOptionInt(Iterator<Object> optionsIterator) throws IOException {
        for (;;) {
            if (!optionsIterator.hasNext())
                throw new IllegalArgumentException();
            Object arg = optionsIterator.next();
            if (arg != null)
                return (Integer) arg;
        }
    }

    /**
     * Process escape command
     * @param options the list of options
     * @param command the command
     * @throws IOException if an error occurs
     * @return true if the escape command was processed.
     */
    private boolean processEscapeCommand(ArrayList<Object> options, int command) throws IOException {
        try {
            switch (command) {
                case 'A':
                    processCursorUp(optionInt(options, 0, 1));
                    return true;
                case 'B':
                    processCursorDown(optionInt(options, 0, 1));
                    return true;
                case 'C':
                    processCursorRight(optionInt(options, 0, 1));
                    return true;
                case 'D':
                    processCursorLeft(optionInt(options, 0, 1));
                    return true;
                case 'E':
                    processCursorDownLine(optionInt(options, 0, 1));
                    return true;
                case 'F':
                    processCursorUpLine(optionInt(options, 0, 1));
                    return true;
                case 'G':
                    processCursorToColumn(optionInt(options, 0));
                    return true;
                case 'H':
                case 'f':
                    processCursorTo(optionInt(options, 0, 1), optionInt(options, 1, 1));
                    return true;
                case 'J':
                    processEraseScreen(optionInt(options, 0, 0));
                    return true;
                case 'K':
                    processEraseLine(optionInt(options, 0, 0));
                    return true;
                case 'L':
                    processInsertLine(optionInt(options, 0, 1));
                    return true;
                case 'M':
                    processDeleteLine(optionInt(options, 0, 1));
                    return true;
                case 'S':
                    processScrollUp(optionInt(options, 0, 1));
                    return true;
                case 'T':
                    processScrollDown(optionInt(options, 0, 1));
                    return true;
                case 'm':
                    // Validate all options are ints...
                    for (Object next : options) {
                        if (next != null && next.getClass() != Integer.class) {
                            throw new IllegalArgumentException();
                        }
                    }

                    int count = 0;
                    Iterator<Object> optionsIterator = options.iterator();
                    while (optionsIterator.hasNext()) {
                        Object next = optionsIterator.next();
                        if (next != null) {
                            count++;
                            int value = (Integer) next;
                            if (30 <= value && value <= 37) {
                                processSetForegroundColor(value - 30);
                            } else if (40 <= value && value <= 47) {
                                processSetBackgroundColor(value - 40);
                            } else if (90 <= value && value <= 97) {
                                processSetForegroundColor(value - 90, true);
                            } else if (100 <= value && value <= 107) {
                                processSetBackgroundColor(value - 100, true);
                            } else if (value == 38 || value == 48) {
                                // extended color like `esc[38;5;<index>m` or `esc[38;2;<r>;<g>;<b>m`
                                int arg2or5 = getNextOptionInt(optionsIterator);
                                if (arg2or5 == 2) {
                                    // 24 bit color style like `esc[38;2;<r>;<g>;<b>m`
                                    int r = getNextOptionInt(optionsIterator);
                                    int g = getNextOptionInt(optionsIterator);
                                    int b = getNextOptionInt(optionsIterator);
                                    if (r >= 0 && r <= 255 && g >= 0 && g <= 255 && b >= 0 && b <= 255) {
                                        if (value == 38)
                                            processSetForegroundColorExt(r, g, b);
                                        else
                                            processSetBackgroundColorExt(r, g, b);
                                    } else {
                                        throw new IllegalArgumentException();
                                    }
                                }
                                else if (arg2or5 == 5) {
                                    // 256 color style like `esc[38;5;<index>m`
                                    int paletteIndex = getNextOptionInt(optionsIterator);
                                    if (paletteIndex >= 0 && paletteIndex <= 255) {
                                        if (value == 38)
                                            processSetForegroundColorExt(paletteIndex);
                                        else
                                            processSetBackgroundColorExt(paletteIndex);
                                    } else {
                                        throw new IllegalArgumentException();
                                    }
                                }
                                else {
                                    throw new IllegalArgumentException();
                                }
                            } else {
                                switch (value) {
                                    case 39:
                                        processDefaultTextColor();
                                        break;
                                    case 49:
                                        processDefaultBackgroundColor();
                                        break;
                                    case 0:
                                        processAttributeRest();
                                        break;
                                    default:
                                        processSetAttribute(value);
                                }
                            }
                        }
                    }
                    if (count == 0) {
                        processAttributeRest();
                    }
                    return true;
                case 's':
                    processSaveCursorPosition();
                    return true;
                case 'u':
                    processRestoreCursorPosition();
                    return true;

                default:
                    if ('a' <= command && 'z' <= command) {
                        processUnknownExtension(options, command);
                        return true;
                    }
                    if ('A' <= command && 'Z' <= command) {
                        processUnknownExtension(options, command);
                        return true;
                    }
                    return false;
            }
        } catch (IllegalArgumentException ignore) {
        }
        return false;
    }

    /**
     * Process operating system command.
     * @param options the options list
     * @return true if the operating system command was processed.
     */
    private boolean processOperatingSystemCommand(ArrayList<Object> options) throws IOException {
        int command = optionInt(options, 0);
        String label = (String) options.get(1);
        // for command > 2 label could be composed (i.e. contain ';'), but we'll leave
        // it to processUnknownOperatingSystemCommand implementations to handle that
        try {
            switch (command) {
                case 0:
                    processChangeIconNameAndWindowTitle(label);
                    return true;
                case 1:
                    processChangeIconName(label);
                    return true;
                case 2:
                    processChangeWindowTitle(label);
                    return true;

                default:
                    // not exactly unknown, but not supported through dedicated process methods:
                    processUnknownOperatingSystemCommand(command, label);
                    return true;
            }
        } catch (IllegalArgumentException ignore) {
        }
        return false;
    }

    /**
     * Process <code>CSI u</code> ANSI code, corresponding to <code>RCP \u2013 Restore Cursor Position</code>
     * @throws IOException if an error occurs
     */
    protected void processRestoreCursorPosition() throws IOException {
    }

    /**
     * Process <code>CSI s</code> ANSI code, corresponding to <code>SCP \u2013 Save Cursor Position</code>
     * @throws IOException if an error occurs
     */
    protected void processSaveCursorPosition() throws IOException {
    }

    /**
     * Process <code>CSI s</code> ANSI code, corresponding to <code>IL \u2013 Insert Line</code>
     * @param optionInt the option
     * @throws IOException if an error occurs
     */
    protected void processInsertLine(int optionInt) throws IOException {
    }

    /**
     * Process <code>CSI s</code> ANSI code, corresponding to <code>DL \u2013 Delete Line</code>
     * @param optionInt the option
     * @throws IOException if an error occurs
     */
    protected void processDeleteLine(int optionInt) throws IOException {
    }

    /**
     * Process <code>CSI n T</code> ANSI code, corresponding to <code>SD \u2013 Scroll Down</code>
     * @param optionInt the option
     * @throws IOException if an error occurs
     */
    protected void processScrollDown(int optionInt) throws IOException {
    }

    /**
     * Process <code>CSI n U</code> ANSI code, corresponding to <code>SU \u2013 Scroll Up</code>
     * @param optionInt the option
     * @throws IOException if an error occurs
     */
    protected void processScrollUp(int optionInt) throws IOException {
    }

    protected static final int ERASE_SCREEN_TO_END = 0;
    protected static final int ERASE_SCREEN_TO_BEGINING = 1;
    protected static final int ERASE_SCREEN = 2;

    /**
     * Process <code>CSI n J</code> ANSI code, corresponding to <code>ED \u2013 Erase in Display</code>
     * @param eraseOption the erase option
     * @throws IOException if an error occurs
     */
    protected void processEraseScreen(int eraseOption) throws IOException {
    }

    protected static final int ERASE_LINE_TO_END = 0;
    protected static final int ERASE_LINE_TO_BEGINING = 1;
    protected static final int ERASE_LINE = 2;

    /**
     * Process <code>CSI n K</code> ANSI code, corresponding to <code>ED \u2013 Erase in Line</code>
     * @param eraseOption the erase option
     * @throws IOException if an error occurs
     */
    protected void processEraseLine(int eraseOption) throws IOException {
    }

    protected static final int ATTRIBUTE_INTENSITY_BOLD = 1; //         Intensity: Bold
    protected static final int ATTRIBUTE_INTENSITY_FAINT = 2; //        Intensity; Faint        not widely supported
    protected static final int ATTRIBUTE_ITALIC = 3; //         Italic; on      not widely supported. Sometimes treated as inverse.
    protected static final int ATTRIBUTE_UNDERLINE = 4; //      Underline; Single
    protected static final int ATTRIBUTE_BLINK_SLOW = 5; //     Blink; Slow     less than 150 per minute
    protected static final int ATTRIBUTE_BLINK_FAST = 6; //     Blink; Rapid    MS-DOS ANSI.SYS; 150 per minute or more
    protected static final int ATTRIBUTE_NEGATIVE_ON = 7; //    Image; Negative         inverse or reverse; swap foreground and background
    protected static final int ATTRIBUTE_CONCEAL_ON = 8; //     Conceal on
    protected static final int ATTRIBUTE_UNDERLINE_DOUBLE = 21; //      Underline; Double       not widely supported
    protected static final int ATTRIBUTE_INTENSITY_NORMAL = 22; //      Intensity; Normal       not bold and not faint
    protected static final int ATTRIBUTE_UNDERLINE_OFF = 24; //         Underline; None
    protected static final int ATTRIBUTE_BLINK_OFF = 25; //     Blink; off
    @Deprecated
    protected static final int ATTRIBUTE_NEGATIVE_Off = 27; //  Image; Positive
    protected static final int ATTRIBUTE_NEGATIVE_OFF = 27; //  Image; Positive
    protected static final int ATTRIBUTE_CONCEAL_OFF = 28; //   Reveal  conceal off

    /**
     * process <code>SGR</code> other than <code>0</code> (reset), <code>30-39</code> (foreground),
     * <code>40-49</code> (background), <code>90-97</code> (foreground high intensity) or
     * <code>100-107</code> (background high intensity)
     * @param attribute the attribute to set
     * @throws IOException if an error occurs
     * @see #processAttributeRest()
     * @see #processSetForegroundColor(int)
     * @see #processSetForegroundColor(int, boolean)
     * @see #processSetForegroundColorExt(int)
     * @see #processSetForegroundColorExt(int, int, int)
     * @see #processDefaultTextColor()
     * @see #processDefaultBackgroundColor()
     */
    protected void processSetAttribute(int attribute) throws IOException {
    }

    protected static final int BLACK = 0;
    protected static final int RED = 1;
    protected static final int GREEN = 2;
    protected static final int YELLOW = 3;
    protected static final int BLUE = 4;
    protected static final int MAGENTA = 5;
    protected static final int CYAN = 6;
    protected static final int WHITE = 7;

    /**
     * process <code>SGR 30-37</code> corresponding to <code>Set text color (foreground)</code>.
     * @param color the text color
     * @throws IOException if an error occurs
     */
    protected void processSetForegroundColor(int color) throws IOException {
        processSetForegroundColor(color, false);
    }

    /**
     * process <code>SGR 30-37</code> or <code>SGR 90-97</code> corresponding to
     * <code>Set text color (foreground)</code> either in normal mode or high intensity.
     * @param color the text color
     * @param bright is high intensity?
     * @throws IOException if an error occurs
     */
    protected void processSetForegroundColor(int color, boolean bright) throws IOException {
        processSetForegroundColorExt(bright ? color + 8 : color);
    }

    /**
     * process <code>SGR 38</code> corresponding to <code>extended set text color (foreground)</code>
     * with a palette of 255 colors.
     * @param paletteIndex the text color in the palette
     * @throws IOException if an error occurs
     */
    protected void processSetForegroundColorExt(int paletteIndex) throws IOException {
    }

    /**
     * process <code>SGR 38</code> corresponding to <code>extended set text color (foreground)</code>
     * with a 24 bits RGB definition of the color.
     * @param r red
     * @param g green
     * @param b blue
     * @throws IOException if an error occurs
     */
    protected void processSetForegroundColorExt(int r, int g, int b) throws IOException {
        processSetForegroundColorExt(Colors.roundRgbColor(r, g, b, 16));
    }

    /**
     * process <code>SGR 40-47</code> corresponding to <code>Set background color</code>.
     * @param color the background color
     * @throws IOException if an error occurs
     */
    protected void processSetBackgroundColor(int color) throws IOException {
        processSetBackgroundColor(color, false);
    }

    /**
     * process <code>SGR 40-47</code> or <code>SGR 100-107</code> corresponding to
     * <code>Set background color</code> either in normal mode or high intensity.
     * @param color the background color
     * @param bright is high intensity?
     * @throws IOException if an error occurs
     */
    protected void processSetBackgroundColor(int color, boolean bright) throws IOException {
        processSetBackgroundColorExt(bright ? color + 8 : color);
    }

    /**
     * process <code>SGR 48</code> corresponding to <code>extended set background color</code>
     * with a palette of 255 colors.
     * @param paletteIndex the background color in the palette
     * @throws IOException if an error occurs
     */
    protected void processSetBackgroundColorExt(int paletteIndex) throws IOException {
    }

    /**
     * process <code>SGR 48</code> corresponding to <code>extended set background color</code>
     * with a 24 bits RGB definition of the color.
     * @param r red
     * @param g green
     * @param b blue
     * @throws IOException if an error occurs
     */
    protected void processSetBackgroundColorExt(int r, int g, int b) throws IOException {
        processSetBackgroundColorExt(Colors.roundRgbColor(r, g, b, 16));
    }

    /**
     * process <code>SGR 39</code> corresponding to <code>Default text color (foreground)</code>
     * @throws IOException if an error occurs
     */
    protected void processDefaultTextColor() throws IOException {
    }

    /**
     * process <code>SGR 49</code> corresponding to <code>Default background color</code>
     * @throws IOException if an error occurs
     */
    protected void processDefaultBackgroundColor() throws IOException {
    }

    /**
     * process <code>SGR 0</code> corresponding to <code>Reset / Normal</code>
     * @throws IOException if an error occurs
     */
    protected void processAttributeRest() throws IOException {
    }

    /**
     * process <code>CSI n ; m H</code> corresponding to <code>CUP \u2013 Cursor Position</code> or
     * <code>CSI n ; m f</code> corresponding to <code>HVP \u2013 Horizontal and Vertical Position</code>
     * @param row the row
     * @param col the column
     * @throws IOException if an error occurs
     */
    protected void processCursorTo(int row, int col) throws IOException {
    }

    /**
     * process <code>CSI n G</code> corresponding to <code>CHA \u2013 Cursor Horizontal Absolute</code>
     * @param x the column
     * @throws IOException if an error occurs
     */
    protected void processCursorToColumn(int x) throws IOException {
    }

    /**
     * process <code>CSI n F</code> corresponding to <code>CPL \u2013 Cursor Previous Line</code>
     * @param count line count
     * @throws IOException if an error occurs
     */
    protected void processCursorUpLine(int count) throws IOException {
    }

    /**
     * process <code>CSI n E</code> corresponding to <code>CNL \u2013 Cursor Next Line</code>
     * @param count line count
     * @throws IOException if an error occurs
     */
    protected void processCursorDownLine(int count) throws IOException {
        // Poor mans impl..
        for (int i = 0; i < count; i++) {
            out.write('\n');
        }
    }

    /**
     * process <code>CSI n D</code> corresponding to <code>CUB \u2013 Cursor Back</code>
     * @param count the count
     * @throws IOException if an error occurs
     */
    protected void processCursorLeft(int count) throws IOException {
    }

    /**
     * process <code>CSI n C</code> corresponding to <code>CUF \u2013 Cursor Forward</code>
     * @param count the count
     * @throws IOException if an error occurs
     */
    protected void processCursorRight(int count) throws IOException {
        // Poor mans impl..
        for (int i = 0; i < count; i++) {
            out.write(' ');
        }
    }

    /**
     * process <code>CSI n B</code> corresponding to <code>CUD \u2013 Cursor Down</code>
     * @param count the count
     * @throws IOException if an error occurs
     */
    protected void processCursorDown(int count) throws IOException {
    }

    /**
     * process <code>CSI n A</code> corresponding to <code>CUU \u2013 Cursor Up</code>
     * @param count the count
     * @throws IOException if an error occurs
     */
    protected void processCursorUp(int count) throws IOException {
    }

    protected void processUnknownExtension(ArrayList<Object> options, int command) {
    }

    /**
     * process <code>OSC 0;text BEL</code> corresponding to <code>Change Window and Icon label</code>
     * @param label the label
     */
    protected void processChangeIconNameAndWindowTitle(String label) {
        processChangeIconName(label);
        processChangeWindowTitle(label);
    }

    /**
     * process <code>OSC 1;text BEL</code> corresponding to <code>Change Icon label</code>
     * @param name the icon name
     */
    protected void processChangeIconName(String name) {
    }

    /**
     * process <code>OSC 2;text BEL</code> corresponding to <code>Change Window title</code>
     * @param title the title
     */
    protected void processChangeWindowTitle(String title) {
    }

    /**
     * Process unknown <code>OSC</code> command.
     * @param command the command
     * @param param the param
     */
    protected void processUnknownOperatingSystemCommand(int command, String param) {
    }

    /**
     * Process character set sequence.
     * @param options
     * @return true if the charcter set select command was processed.
     */
    private boolean processCharsetSelect(ArrayList<Object> options) throws IOException {
        int set = optionInt(options, 0);
        char seq = (Character) options.get(1);
        processCharsetSelect(set, seq);
        return true;
    }

    protected void processCharsetSelect(int set, char seq) {
    }

    private int optionInt(ArrayList<Object> options, int index) {
        if (options.size() <= index)
            throw new IllegalArgumentException();
        Object value = options.get(index);
        if (value == null)
            throw new IllegalArgumentException();
        if (!value.getClass().equals(Integer.class))
            throw new IllegalArgumentException();
        return (Integer) value;
    }

    private int optionInt(ArrayList<Object> options, int index, int defaultValue) {
        if (options.size() > index) {
            Object value = options.get(index);
            if (value == null) {
                return defaultValue;
            }
            return (Integer) value;
        }
        return defaultValue;
    }

    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        // TODO: Optimize this
        for (int i = 0; i < len; i++) {
            write(cbuf[off + i]);
        }
    }

    @Override
    public void write(String str, int off, int len) throws IOException {
        // TODO: Optimize this
        for (int i = 0; i < len; i++) {
            write(str.charAt(off + i));
        }
    }

    @Override
    public void close() throws IOException {
        write(RESET_CODE);
        flush();
        super.close();
    }

}
