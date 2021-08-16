/*
 * Copyright (c) 2002-2018, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package jdk.internal.org.jline.reader;

import java.util.List;

/**
 * A completer is the mechanism by which tab-completion candidates will be resolved.
 *
 * @author <a href="mailto:mwp1@cornell.edu">Marc Prud'hommeaux</a>
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 * @author <a href="mailto:gnodet@gmail.com">Guillaume Nodet</a>
 * @since 2.3
 */
public interface Completer
{
    /**
     * Populates <i>candidates</i> with a list of possible completions for the <i>command line</i>.
     *
     * The list of candidates will be sorted and filtered by the LineReader, so that
     * the list of candidates displayed to the user will usually be smaller than
     * the list given by the completer.  Thus it is not necessary for the completer
     * to do any matching based on the current buffer.  On the contrary, in order
     * for the typo matcher to work, all possible candidates for the word being
     * completed should be returned.
     *
     * @param reader        The line reader
     * @param line          The parsed command line
     * @param candidates    The {@link List} of candidates to populate
     */
    void complete(LineReader reader, ParsedLine line, List<Candidate> candidates);
}
