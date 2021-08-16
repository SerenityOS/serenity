/*
 * Copyright (c) 2002-2019, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package jdk.internal.org.jline.reader.impl.completer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

import jdk.internal.org.jline.reader.Candidate;
import jdk.internal.org.jline.reader.Completer;
import jdk.internal.org.jline.reader.LineReader;
import jdk.internal.org.jline.reader.ParsedLine;
import jdk.internal.org.jline.utils.AttributedString;

/**
 * Completer for a set of strings.
 *
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 * @since 2.3
 */
public class StringsCompleter implements Completer
{
    protected Collection<Candidate> candidates = new ArrayList<>();
    protected Supplier<Collection<String>> stringsSupplier;

    public StringsCompleter() {
    }

    public StringsCompleter(Supplier<Collection<String>> stringsSupplier) {
        assert stringsSupplier != null;
        candidates = null;
        this.stringsSupplier = stringsSupplier;
    }

    public StringsCompleter(String... strings) {
        this(Arrays.asList(strings));
    }

    public StringsCompleter(Iterable<String> strings) {
        assert strings != null;
        for (String string : strings) {
            candidates.add(new Candidate(AttributedString.stripAnsi(string), string, null, null, null, null, true));
        }
    }

    public StringsCompleter(Candidate ... candidates) {
        this(Arrays.asList(candidates));
    }

    public StringsCompleter(Collection<Candidate> candidates) {
        assert candidates != null;
        this.candidates.addAll(candidates);
    }

    public void complete(LineReader reader, final ParsedLine commandLine, final List<Candidate> candidates) {
        assert commandLine != null;
        assert candidates != null;
        if (this.candidates != null) {
            candidates.addAll(this.candidates);
        } else {
            for (String string : stringsSupplier.get()) {
                candidates.add(new Candidate(AttributedString.stripAnsi(string), string, null, null, null, null, true));
            }
        }
    }

}
