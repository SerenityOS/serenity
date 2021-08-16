/*
 * Copyright (c) 2002-2016, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package jdk.internal.org.jline.terminal.impl;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileDescriptor;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jdk.internal.org.jline.terminal.Attributes;
import jdk.internal.org.jline.terminal.Attributes.ControlChar;
import jdk.internal.org.jline.terminal.Attributes.ControlFlag;
import jdk.internal.org.jline.terminal.Attributes.InputFlag;
import jdk.internal.org.jline.terminal.Attributes.LocalFlag;
import jdk.internal.org.jline.terminal.Attributes.OutputFlag;
import jdk.internal.org.jline.terminal.Size;
import jdk.internal.org.jline.terminal.spi.Pty;
import jdk.internal.org.jline.utils.OSUtils;

import static jdk.internal.org.jline.utils.ExecHelper.exec;

public class ExecPty extends AbstractPty implements Pty {

    private final String name;
    private final boolean system;

    public static Pty current() throws IOException {
        try {
            String result = exec(true, OSUtils.TTY_COMMAND);
            return new ExecPty(result.trim(), true);
        } catch (IOException e) {
            throw new IOException("Not a tty", e);
        }
    }

    protected ExecPty(String name, boolean system) {
        this.name = name;
        this.system = system;
    }

    @Override
    public void close() throws IOException {
    }

    public String getName() {
        return name;
    }

    @Override
    public InputStream getMasterInput() {
        throw new UnsupportedOperationException();
    }

    @Override
    public OutputStream getMasterOutput() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected InputStream doGetSlaveInput() throws IOException {
        return system
                ? new FileInputStream(FileDescriptor.in)
                : new FileInputStream(getName());
    }

    @Override
    public OutputStream getSlaveOutput() throws IOException {
        return system
                ? new FileOutputStream(FileDescriptor.out)
                : new FileOutputStream(getName());
    }

    @Override
    public Attributes getAttr() throws IOException {
        String cfg = doGetConfig();
        return doGetAttr(cfg);
    }

    @Override
    protected void doSetAttr(Attributes attr) throws IOException {
        List<String> commands = getFlagsToSet(attr, getAttr());
        if (!commands.isEmpty()) {
            commands.add(0, OSUtils.STTY_COMMAND);
            if (!system) {
                commands.add(1, OSUtils.STTY_F_OPTION);
                commands.add(2, getName());
            }
            try {
                exec(system, commands.toArray(new String[commands.size()]));
            } catch (IOException e) {
                // Handle partial failures with GNU stty, see #97
                if (e.toString().contains("unable to perform all requested operations")) {
                    commands = getFlagsToSet(attr, getAttr());
                    if (!commands.isEmpty()) {
                        throw new IOException("Could not set the following flags: " + String.join(", ", commands), e);
                    }
                } else {
                    throw e;
                }
            }
        }
    }

    protected List<String> getFlagsToSet(Attributes attr, Attributes current) {
        List<String> commands = new ArrayList<>();
        for (InputFlag flag : InputFlag.values()) {
            if (attr.getInputFlag(flag) != current.getInputFlag(flag)) {
                commands.add((attr.getInputFlag(flag) ? flag.name() : "-" + flag.name()).toLowerCase());
            }
        }
        for (OutputFlag flag : OutputFlag.values()) {
            if (attr.getOutputFlag(flag) != current.getOutputFlag(flag)) {
                commands.add((attr.getOutputFlag(flag) ? flag.name() : "-" + flag.name()).toLowerCase());
            }
        }
        for (ControlFlag flag : ControlFlag.values()) {
            if (attr.getControlFlag(flag) != current.getControlFlag(flag)) {
                commands.add((attr.getControlFlag(flag) ? flag.name() : "-" + flag.name()).toLowerCase());
            }
        }
        for (LocalFlag flag : LocalFlag.values()) {
            if (attr.getLocalFlag(flag) != current.getLocalFlag(flag)) {
                commands.add((attr.getLocalFlag(flag) ? flag.name() : "-" + flag.name()).toLowerCase());
            }
        }
        String undef = System.getProperty("os.name").toLowerCase().startsWith("hp") ? "^-" : "undef";
        for (ControlChar cchar : ControlChar.values()) {
            int v = attr.getControlChar(cchar);
            if (v >= 0 && v != current.getControlChar(cchar)) {
                String str = "";
                commands.add(cchar.name().toLowerCase().substring(1));
                if (cchar == ControlChar.VMIN || cchar == ControlChar.VTIME) {
                    commands.add(Integer.toString(v));
                }
                else if (v == 0) {
                    commands.add(undef);
                }
                else {
                    if (v >= 128) {
                        v -= 128;
                        str += "M-";
                    }
                    if (v < 32 || v == 127) {
                        v ^= 0x40;
                        str += "^";
                    }
                    str += (char) v;
                    commands.add(str);
                }
            }
        }
        return commands;
    }

    @Override
    public Size getSize() throws IOException {
        String cfg = doGetConfig();
        return doGetSize(cfg);
    }

    protected String doGetConfig() throws IOException {
        return system
                ? exec(true,  OSUtils.STTY_COMMAND, "-a")
                : exec(false, OSUtils.STTY_COMMAND, OSUtils.STTY_F_OPTION, getName(), "-a");
    }

    static Attributes doGetAttr(String cfg) throws IOException {
        Attributes attributes = new Attributes();
        for (InputFlag flag : InputFlag.values()) {
            Boolean value = doGetFlag(cfg, flag);
            if (value != null) {
                attributes.setInputFlag(flag, value);
            }
        }
        for (OutputFlag flag : OutputFlag.values()) {
            Boolean value = doGetFlag(cfg, flag);
            if (value != null) {
                attributes.setOutputFlag(flag, value);
            }
        }
        for (ControlFlag flag : ControlFlag.values()) {
            Boolean value = doGetFlag(cfg, flag);
            if (value != null) {
                attributes.setControlFlag(flag, value);
            }
        }
        for (LocalFlag flag : LocalFlag.values()) {
            Boolean value = doGetFlag(cfg, flag);
            if (value != null) {
                attributes.setLocalFlag(flag, value);
            }
        }
        for (ControlChar cchar : ControlChar.values()) {
            String name = cchar.name().toLowerCase().substring(1);
            if ("reprint".endsWith(name)) {
                name = "(?:reprint|rprnt)";
            }
            Matcher matcher = Pattern.compile("[\\s;]" + name + "\\s*=\\s*(.+?)[\\s;]").matcher(cfg);
            if (matcher.find()) {
                attributes.setControlChar(cchar, parseControlChar(matcher.group(1).toUpperCase()));
            }
        }
        return attributes;
    }

    private static Boolean doGetFlag(String cfg, Enum<?> flag) {
        Matcher matcher = Pattern.compile("(?:^|[\\s;])(\\-?" + flag.name().toLowerCase() + ")(?:[\\s;]|$)").matcher(cfg);
        return matcher.find() ? !matcher.group(1).startsWith("-") : null;
    }

    static int parseControlChar(String str) {
        // undef
        if ("<UNDEF>".equals(str)) {
            return -1;
        }
        // del
        if ("DEL".equalsIgnoreCase(str)) {
            return 127;
        }
        // octal
        if (str.charAt(0) == '0') {
            return Integer.parseInt(str, 8);
        }
        // decimal
        if (str.charAt(0) >= '1' && str.charAt(0) <= '9') {
            return Integer.parseInt(str, 10);
        }
        // control char
        if (str.charAt(0) == '^') {
            if (str.charAt(1) == '?') {
                return 127;
            } else {
                return str.charAt(1) - 64;
            }
        } else if (str.charAt(0) == 'M' && str.charAt(1) == '-') {
            if (str.charAt(2) == '^') {
                if (str.charAt(3) == '?') {
                    return 127 + 128;
                } else {
                    return str.charAt(3) - 64 + 128;
                }
            } else {
                return str.charAt(2) + 128;
            }
        } else {
            return str.charAt(0);
        }
    }

    static Size doGetSize(String cfg) throws IOException {
        return new Size(doGetInt("columns", cfg), doGetInt("rows", cfg));
    }

    static int doGetInt(String name, String cfg) throws IOException {
        String[] patterns = new String[] {
                "\\b([0-9]+)\\s+" + name + "\\b",
                "\\b" + name + "\\s+([0-9]+)\\b",
                "\\b" + name + "\\s*=\\s*([0-9]+)\\b"
        };
        for (String pattern : patterns) {
            Matcher matcher = Pattern.compile(pattern).matcher(cfg);
            if (matcher.find()) {
                return Integer.parseInt(matcher.group(1));
            }
        }
        throw new IOException("Unable to parse " + name);
    }

    @Override
    public void setSize(Size size) throws IOException {
        if (system) {
            exec(true,
                 OSUtils.STTY_COMMAND,
                 "columns", Integer.toString(size.getColumns()),
                 "rows", Integer.toString(size.getRows()));
        } else {
            exec(false,
                 OSUtils.STTY_COMMAND,
                 OSUtils.STTY_F_OPTION, getName(),
                 "columns", Integer.toString(size.getColumns()),
                 "rows", Integer.toString(size.getRows()));
        }
    }

    @Override
    public String toString() {
        return "ExecPty[" + getName() + (system ? ", system]" : "]");
    }

}
