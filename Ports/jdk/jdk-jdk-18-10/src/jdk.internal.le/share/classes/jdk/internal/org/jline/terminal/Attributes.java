/*
 * Copyright (c) 2002-2016, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package jdk.internal.org.jline.terminal;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Attributes {

    /**
     * Control characters
     */
    public enum ControlChar {
        VEOF,
        VEOL,
        VEOL2,
        VERASE,
        VWERASE,
        VKILL,
        VREPRINT,
        VINTR,
        VQUIT,
        VSUSP,
        VDSUSP,
        VSTART,
        VSTOP,
        VLNEXT,
        VDISCARD,
        VMIN,
        VTIME,
        VSTATUS
    }

    /**
     * Input flags - software input processing
     */
    public enum InputFlag {
        IGNBRK,       /* ignore BREAK condition */
        BRKINT,       /* map BREAK to SIGINTR */
        IGNPAR,       /* ignore (discard) parity errors */
        PARMRK,       /* mark parity and framing errors */
        INPCK,        /* enable checking of parity errors */
        ISTRIP,       /* strip 8th bit off chars */
        INLCR,        /* map NL into CR */
        IGNCR,        /* ignore CR */
        ICRNL,        /* map CR to NL (ala CRMOD) */
        IXON,         /* enable output flow control */
        IXOFF,        /* enable input flow control */
        IXANY,        /* any char will restart after stop */
        IMAXBEL,      /* ring bell on input queue full */
        IUTF8         /* maintain state for UTF-8 VERASE */
    }

    /*
     * Output flags - software output processing
     */
    public enum OutputFlag {
        OPOST,       /* enable following output processing */
        ONLCR,       /* map NL to CR-NL (ala CRMOD) */
        OXTABS,      /* expand tabs to spaces */
        ONOEOT,      /* discard EOT's (^D) on output) */
        OCRNL,       /* map CR to NL on output */
        ONOCR,       /* no CR output at column 0 */
        ONLRET,      /* NL performs CR function */
        OFILL,       /* use fill characters for delay */
        NLDLY,       /* \n delay */
        TABDLY,      /* horizontal tab delay */
        CRDLY,       /* \r delay */
        FFDLY,       /* form feed delay */
        BSDLY,       /* \b delay */
        VTDLY,       /* vertical tab delay */
        OFDEL        /* fill is DEL, else NUL */
    }

    /*
     * Control flags - hardware control of terminal
     */
    public enum ControlFlag {
        CIGNORE,          /* ignore control flags */
        CS5,              /* 5 bits    (pseudo) */
        CS6,              /* 6 bits */
        CS7,              /* 7 bits */
        CS8,              /* 8 bits */
        CSTOPB,           /* send 2 stop bits */
        CREAD,            /* enable receiver */
        PARENB,           /* parity enable */
        PARODD,           /* odd parity, else even */
        HUPCL,            /* hang up on last close */
        CLOCAL,           /* ignore modem status lines */
        CCTS_OFLOW,       /* CTS flow control of output */
        CRTS_IFLOW,       /* RTS flow control of input */
        CDTR_IFLOW,       /* DTR flow control of input */
        CDSR_OFLOW,       /* DSR flow control of output */
        CCAR_OFLOW        /* DCD flow control of output */
    }

    /*
     * "Local" flags - dumping ground for other state
     *
     * Warning: some flags in this structure begin with
     * the letter "I" and look like they belong in the
     * input flag.
     */
    public enum LocalFlag {
        ECHOKE,           /* visual erase for line kill */
        ECHOE,            /* visually erase chars */
        ECHOK,            /* echo NL after line kill */
        ECHO,             /* enable echoing */
        ECHONL,           /* echo NL even if ECHO is off */
        ECHOPRT,          /* visual erase mode for hardcopy */
        ECHOCTL,          /* echo control chars as ^(Char) */
        ISIG,             /* enable signals INTR, QUIT, [D]SUSP */
        ICANON,           /* canonicalize input lines */
        ALTWERASE,        /* use alternate WERASE algorithm */
        IEXTEN,           /* enable DISCARD and LNEXT */
        EXTPROC,          /* external processing */
        TOSTOP,           /* stop background jobs from output */
        FLUSHO,           /* output being flushed (state) */
        NOKERNINFO,       /* no kernel output from VSTATUS */
        PENDIN,           /* XXX retype pending input (state) */
        NOFLSH            /* don't flush after interrupt */
    }

    final EnumSet<InputFlag> iflag = EnumSet.noneOf(InputFlag.class);
    final EnumSet<OutputFlag> oflag = EnumSet.noneOf(OutputFlag.class);
    final EnumSet<ControlFlag> cflag = EnumSet.noneOf(ControlFlag.class);
    final EnumSet<LocalFlag> lflag = EnumSet.noneOf(LocalFlag.class);
    final EnumMap<ControlChar, Integer> cchars = new EnumMap<>(ControlChar.class);

    public Attributes() {
    }

    public Attributes(Attributes attr) {
        copy(attr);
    }

    //
    // Input flags
    //

    public EnumSet<InputFlag> getInputFlags() {
        return iflag;
    }

    public void setInputFlags(EnumSet<InputFlag> flags) {
        iflag.clear();
        iflag.addAll(flags);
    }

    public boolean getInputFlag(InputFlag flag) {
        return iflag.contains(flag);
    }

    public void setInputFlags(EnumSet<InputFlag> flags, boolean value) {
        if (value) {
            iflag.addAll(flags);
        } else {
            iflag.removeAll(flags);
        }
    }

    public void setInputFlag(InputFlag flag, boolean value) {
        if (value) {
            iflag.add(flag);
        } else {
            iflag.remove(flag);
        }
    }

    //
    // Output flags
    //

    public EnumSet<OutputFlag> getOutputFlags() {
        return oflag;
    }

    public void setOutputFlags(EnumSet<OutputFlag> flags) {
        oflag.clear();
        oflag.addAll(flags);
    }

    public boolean getOutputFlag(OutputFlag flag) {
        return oflag.contains(flag);
    }

    public void setOutputFlags(EnumSet<OutputFlag> flags, boolean value) {
        if (value) {
            oflag.addAll(flags);
        } else {
            oflag.removeAll(flags);
        }
    }

    public void setOutputFlag(OutputFlag flag, boolean value) {
        if (value) {
            oflag.add(flag);
        } else {
            oflag.remove(flag);
        }
    }

    //
    // Control flags
    //

    public EnumSet<ControlFlag> getControlFlags() {
        return cflag;
    }

    public void setControlFlags(EnumSet<ControlFlag> flags) {
        cflag.clear();
        cflag.addAll(flags);
    }

    public boolean getControlFlag(ControlFlag flag) {
        return cflag.contains(flag);
    }

    public void setControlFlags(EnumSet<ControlFlag> flags, boolean value) {
        if (value) {
            cflag.addAll(flags);
        } else {
            cflag.removeAll(flags);
        }
    }

    public void setControlFlag(ControlFlag flag, boolean value) {
        if (value) {
            cflag.add(flag);
        } else {
            cflag.remove(flag);
        }
    }

    //
    // Local flags
    //

    public EnumSet<LocalFlag> getLocalFlags() {
        return lflag;
    }

    public void setLocalFlags(EnumSet<LocalFlag> flags) {
        lflag.clear();
        lflag.addAll(flags);
    }

    public boolean getLocalFlag(LocalFlag flag) {
        return lflag.contains(flag);
    }

    public void setLocalFlags(EnumSet<LocalFlag> flags, boolean value) {
        if (value) {
            lflag.addAll(flags);
        } else {
            lflag.removeAll(flags);
        }
    }

    public void setLocalFlag(LocalFlag flag, boolean value) {
        if (value) {
            lflag.add(flag);
        } else {
            lflag.remove(flag);
        }
    }

    //
    // Control chars
    //

    public EnumMap<ControlChar, Integer> getControlChars() {
        return cchars;
    }

    public void setControlChars(EnumMap<ControlChar, Integer> chars) {
        cchars.clear();
        cchars.putAll(chars);
    }

    public int getControlChar(ControlChar c) {
        Integer v = cchars.get(c);
        return v != null ? v : -1;
    }

    public void setControlChar(ControlChar c, int value) {
        cchars.put(c, value);
    }

    //
    // Miscellaneous methods
    //

    public void copy(Attributes attributes) {
        setControlFlags(attributes.getControlFlags());
        setInputFlags(attributes.getInputFlags());
        setLocalFlags(attributes.getLocalFlags());
        setOutputFlags(attributes.getOutputFlags());
        setControlChars(attributes.getControlChars());
    }

    @Override
    public String toString() {
        return "Attributes[" +
                "lflags: " + append(lflag) + ", " +
                "iflags: " + append(iflag) + ", " +
                "oflags: " + append(oflag) + ", " +
                "cflags: " + append(cflag) + ", " +
                "cchars: " + append(EnumSet.allOf(ControlChar.class), this::display) +
                "]";
    }

    private String display(ControlChar c) {
        String value;
        int ch = getControlChar(c);
        if (c == ControlChar.VMIN || c == ControlChar.VTIME) {
            value = Integer.toString(ch);
        } else if (ch < 0) {
            value = "<undef>";
        } else if (ch < 32) {
            value = "^" + (char) (ch + 'A' - 1);
        } else if (ch == 127) {
            value = "^?";
        } else if (ch >= 128) {
            value = String.format("\\u%04x", ch);
        } else {
            value = String.valueOf((char) ch);
        }
        return c.name().toLowerCase().substring(1) + "=" + value;
    }

    private <T extends Enum<T>> String append(EnumSet<T> set) {
        return append(set, e -> e.name().toLowerCase());
    }

    private <T extends Enum<T>> String append(EnumSet<T> set, Function<T, String> toString) {
        return set.stream().map(toString).collect(Collectors.joining(" "));
    }

}
