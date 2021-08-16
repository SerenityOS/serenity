/*
 * Copyright (c) 2002-2016, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package jdk.internal.org.jline.reader.impl.completer;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import jdk.internal.org.jline.reader.Candidate;
import jdk.internal.org.jline.reader.Completer;
import jdk.internal.org.jline.reader.LineReader;
import jdk.internal.org.jline.reader.ParsedLine;

/**
 * Completer which contains multiple completers and aggregates them together.
 *
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 * @since 2.3
 */
public class AggregateCompleter
    implements Completer
{
    private final Collection<Completer> completers;

    /**
     * Construct an AggregateCompleter with the given completers.
     * The completers will be used in the order given.
     *
     * @param completers the completers
     */
    public AggregateCompleter(final Completer... completers) {
        this(Arrays.asList(completers));
    }

    /**
     * Construct an AggregateCompleter with the given completers.
     * The completers will be used in the order given.
     *
     * @param completers the completers
     */
    public AggregateCompleter(Collection<Completer> completers) {
        assert completers != null;
        this.completers = completers;
    }

    /**
     * Retrieve the collection of completers currently being aggregated.
     *
     * @return the aggregated completers
     */
    public Collection<Completer> getCompleters() {
        return completers;
    }

    /**
     * Perform a completion operation across all aggregated completers.
     *
     * The effect is similar to the following code:
     * <blockquote><pre>{@code completers.forEach(c -> c.complete(reader, line, candidates));}</pre></blockquote>
     *
     * @see Completer#complete(LineReader, ParsedLine, List)
     */
    public void complete(LineReader reader, final ParsedLine line, final List<Candidate> candidates) {
        Objects.requireNonNull(line);
        Objects.requireNonNull(candidates);
        completers.forEach(c -> c.complete(reader, line, candidates));
    }

    /**
     * @return a string representing the aggregated completers
     */
    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "completers=" + completers +
            '}';
    }

}
