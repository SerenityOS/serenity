/*
 * Copyright (c) 2002-2019, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package jdk.internal.org.jline.utils;

import java.util.Objects;
import java.util.Collections;
import java.util.ArrayList;
import java.util.List;
import jdk.internal.org.jline.terminal.Terminal;
import jdk.internal.org.jline.terminal.impl.AbstractTerminal;
import jdk.internal.org.jline.utils.InfoCmp.Capability;
import jdk.internal.org.jline.terminal.Size;

public class Status {

    protected final AbstractTerminal terminal;
    protected final boolean supported;
    protected List<AttributedString> oldLines = Collections.emptyList();
    protected List<AttributedString> linesToRestore = Collections.emptyList();
    protected int rows;
    protected int columns;
    protected boolean force;
    protected boolean suspended = false;
    protected AttributedString borderString;
    protected int border = 0;

    public static Status getStatus(Terminal terminal) {
        return getStatus(terminal, true);
    }

    public static Status getStatus(Terminal terminal, boolean create) {
        return terminal instanceof AbstractTerminal
                ? ((AbstractTerminal) terminal).getStatus(create)
                : null;
    }


    public Status(AbstractTerminal terminal) {
        this.terminal = Objects.requireNonNull(terminal, "terminal can not be null");
        this.supported = terminal.getStringCapability(Capability.change_scroll_region) != null
            && terminal.getStringCapability(Capability.save_cursor) != null
            && terminal.getStringCapability(Capability.restore_cursor) != null
            && terminal.getStringCapability(Capability.cursor_address) != null;
        if (supported) {
            char borderChar = '\u2700';
            AttributedStringBuilder bb = new AttributedStringBuilder();
            for (int i = 0; i < 200; i++) {
                bb.append(borderChar);
            }
            borderString = bb.toAttributedString();
            resize();
        }
    }

    public void setBorder(boolean border) {
        this.border = border ? 1 : 0;
    }

    public void resize() {
        Size size = terminal.getSize();
        this.rows = size.getRows();
        this.columns = size.getColumns();
        this.force = true;
    }

    public void reset() {
        this.force = true;
    }

    public void hardReset() {
        if (suspended) {
            return;
        }
        List<AttributedString> lines = new ArrayList<>(oldLines);
        int b = border;
        update(null);
        border = b;
        update(lines);
    }

    public void redraw() {
        if (suspended) {
            return;
        }
        update(oldLines);
    }

    public void clear() {
        privateClear(oldLines.size());
    }

    private void clearAll() {
        int b = border;
        border = 0;
        privateClear(oldLines.size() + b);
    }

    private void privateClear(int statusSize) {
        List<AttributedString> as = new ArrayList<>();
        for (int i = 0; i < statusSize; i++) {
            as.add(new AttributedString(""));
        }
        if (!as.isEmpty()) {
            update(as);
        }
    }

    public void update(List<AttributedString> lines) {
        if (!supported) {
            return;
        }
        if (lines == null) {
            lines = Collections.emptyList();
        }
        if (suspended) {
            linesToRestore = new ArrayList<>(lines);
            return;
        }
        if (lines.isEmpty()) {
            clearAll();
        }
        if (oldLines.equals(lines) && !force) {
            return;
        }
        int statusSize = lines.size() + (lines.size() == 0 ? 0 : border);
        int nb = statusSize - oldLines.size() - (oldLines.size() == 0 ? 0 : border);
        if (nb > 0) {
            for (int i = 0; i < nb; i++) {
                terminal.puts(Capability.cursor_down);
            }
            for (int i = 0; i < nb; i++) {
                terminal.puts(Capability.cursor_up);
            }
        }
        terminal.puts(Capability.save_cursor);
        terminal.puts(Capability.cursor_address, rows - statusSize, 0);
        if (!terminal.puts(Capability.clr_eos)) {
            for (int i = rows - statusSize; i < rows; i++) {
                terminal.puts(Capability.cursor_address, i, 0);
                terminal.puts(Capability.clr_eol);
            }
        }
        if (border == 1 && lines.size() > 0) {
            terminal.puts(Capability.cursor_address, rows - statusSize, 0);
            borderString.columnSubSequence(0, columns).print(terminal);
        }
        for (int i = 0; i < lines.size(); i++) {
            terminal.puts(Capability.cursor_address, rows - lines.size() + i, 0);
            if (lines.get(i).length() > columns) {
                AttributedStringBuilder asb = new AttributedStringBuilder();
                asb.append(lines.get(i).substring(0, columns - 3)).append("...", new AttributedStyle(AttributedStyle.INVERSE));
                asb.toAttributedString().columnSubSequence(0, columns).print(terminal);
            } else {
                lines.get(i).columnSubSequence(0, columns).print(terminal);
            }
        }
        terminal.puts(Capability.change_scroll_region, 0, rows - 1 - statusSize);
        terminal.puts(Capability.restore_cursor);
        terminal.flush();
        oldLines = new ArrayList<>(lines);
        force = false;
    }

    public void suspend() {
        if (suspended) {
            return;
        }
        linesToRestore = new ArrayList<>(oldLines);
        int b = border;
        update(null);
        border = b;
        suspended = true;
    }

    public void restore() {
        if (!suspended) {
            return;
        }
        suspended = false;
        update(linesToRestore);
        linesToRestore = Collections.emptyList();
    }

    public int size() {
        return oldLines.size() + border;
    }

}
