/*
 * Copyright (c) 2002-2018, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package jdk.internal.org.jline.reader.impl.completer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import jdk.internal.org.jline.reader.Candidate;
import jdk.internal.org.jline.reader.Completer;
import jdk.internal.org.jline.reader.LineReader;
import jdk.internal.org.jline.reader.LineReader.Option;
import jdk.internal.org.jline.reader.ParsedLine;
import jdk.internal.org.jline.terminal.Terminal;
import jdk.internal.org.jline.utils.AttributedStringBuilder;
import jdk.internal.org.jline.utils.AttributedStyle;

/**
 * A file name completer takes the buffer and issues a list of
 * potential completions.
 * <p>
 * This completer tries to behave as similar as possible to
 * <i>bash</i>'s file name completion (using GNU readline)
 * with the following exceptions:
 * <ul>
 * <li>Candidates that are directories will end with "/"</li>
 * <li>Wildcard regular expressions are not evaluated or replaced</li>
 * <li>The "~" character can be used to represent the user's home,
 * but it cannot complete to other users' homes, since java does
 * not provide any way of determining that easily</li>
 * </ul>
 *
 * @author <a href="mailto:mwp1@cornell.edu">Marc Prud'hommeaux</a>
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 * @since 2.3
 * @deprecated use <code>org.jline.builtins.Completers$FileNameCompleter</code> instead
 */
@Deprecated
public class FileNameCompleter implements Completer
{

    public void complete(LineReader reader, ParsedLine commandLine, final List<Candidate> candidates) {
        assert commandLine != null;
        assert candidates != null;

        String buffer = commandLine.word().substring(0, commandLine.wordCursor());

        Path current;
        String curBuf;
        String sep = getUserDir().getFileSystem().getSeparator();
        int lastSep = buffer.lastIndexOf(sep);
        if (lastSep >= 0) {
            curBuf = buffer.substring(0, lastSep + 1);
            if (curBuf.startsWith("~")) {
                if (curBuf.startsWith("~" + sep)) {
                    current = getUserHome().resolve(curBuf.substring(2));
                } else {
                    current = getUserHome().getParent().resolve(curBuf.substring(1));
                }
            } else {
                current = getUserDir().resolve(curBuf);
            }
        } else {
            curBuf = "";
            current = getUserDir();
        }
        try {
            Files.newDirectoryStream(current, this::accept).forEach(p -> {
                String value = curBuf + p.getFileName().toString();
                if (Files.isDirectory(p)) {
                    candidates.add(new Candidate(
                            value + (reader.isSet(Option.AUTO_PARAM_SLASH) ? sep : ""),
                            getDisplay(reader.getTerminal(), p),
                            null, null,
                            reader.isSet(Option.AUTO_REMOVE_SLASH) ? sep : null,
                            null,
                            false));
                } else {
                    candidates.add(new Candidate(value, getDisplay(reader.getTerminal(), p),
                            null, null, null, null, true));
                }
            });
        } catch (IOException e) {
            // Ignore
        }
    }

    protected boolean accept(Path path) {
        try {
            return !Files.isHidden(path);
        } catch (IOException e) {
            return false;
        }
    }

    protected Path getUserDir() {
        return Paths.get(System.getProperty("user.dir"));
    }

    protected Path getUserHome() {
        return Paths.get(System.getProperty("user.home"));
    }

    protected String getDisplay(Terminal terminal, Path p) {
        // TODO: use $LS_COLORS for output
        String name = p.getFileName().toString();
        if (Files.isDirectory(p)) {
            AttributedStringBuilder sb = new AttributedStringBuilder();
            sb.styled(AttributedStyle.BOLD.foreground(AttributedStyle.RED), name);
            sb.append("/");
            name = sb.toAnsi(terminal);
        } else if (Files.isSymbolicLink(p)) {
            AttributedStringBuilder sb = new AttributedStringBuilder();
            sb.styled(AttributedStyle.BOLD.foreground(AttributedStyle.RED), name);
            sb.append("@");
            name = sb.toAnsi(terminal);
        }
        return name;
    }

}
