/*
 * Copyright (c) 2002-2016, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package jdk.internal.org.jline.terminal.impl;

import jdk.internal.org.jline.terminal.Cursor;
import jdk.internal.org.jline.terminal.Terminal;
import jdk.internal.org.jline.utils.Curses;
import jdk.internal.org.jline.utils.InfoCmp;

import java.io.IOError;
import java.io.IOException;
import java.util.function.IntConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CursorSupport {

    public static Cursor getCursorPosition(Terminal terminal, IntConsumer discarded) {
        try {
            String u6 = terminal.getStringCapability(InfoCmp.Capability.user6);
            String u7 = terminal.getStringCapability(InfoCmp.Capability.user7);
            if (u6 == null || u7 == null) {
                return null;
            }
            // Prepare parser
            boolean inc1 = false;
            StringBuilder patb = new StringBuilder();
            int index = 0;
            while (index < u6.length()) {
                char ch;
                switch (ch = u6.charAt(index++)) {
                    case '\\':
                        switch (u6.charAt(index++)) {
                            case 'e':
                            case 'E':
                                patb.append("\\x1b");
                                break;
                            default:
                                throw new IllegalArgumentException();
                        }
                        break;
                    case '%':
                        ch = u6.charAt(index++);
                        switch (ch) {
                            case '%':
                                patb.append('%');
                                break;
                            case 'i':
                                inc1 = true;
                                break;
                            case 'd':
                                patb.append("([0-9]+)");
                                break;
                            default:
                                throw new IllegalArgumentException();
                        }
                        break;
                    default:
                        switch (ch) {
                            case '[':
                                patb.append('\\');
                                break;
                        }
                        patb.append(ch);
                        break;
                }
            }
            Pattern pattern = Pattern.compile(patb.toString());
            // Output cursor position request
            Curses.tputs(terminal.writer(), u7);
            terminal.flush();
            StringBuilder sb = new StringBuilder();
            int start = 0;
            while (true) {
                int c = terminal.reader().read();
                if (c < 0) {
                    return null;
                }
                sb.append((char) c);
                Matcher matcher = pattern.matcher(sb.substring(start));
                if (matcher.matches()) {
                    int y = Integer.parseInt(matcher.group(1));
                    int x = Integer.parseInt(matcher.group(2));
                    if (inc1) {
                        x--;
                        y--;
                    }
                    if (discarded != null) {
                        for (int i = 0; i < start; i++) {
                            discarded.accept(sb.charAt(i));
                        }
                    }
                    return new Cursor(x, y);
                } else if (!matcher.hitEnd()) {
                    start++;
                }
            }
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

}
