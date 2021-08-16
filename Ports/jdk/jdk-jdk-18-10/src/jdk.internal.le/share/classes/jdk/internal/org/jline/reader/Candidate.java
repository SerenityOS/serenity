/*
 * Copyright (c) 2002-2019, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package jdk.internal.org.jline.reader;

import java.util.Objects;

/**
 * A completion candidate.
 *
 * @author <a href="mailto:gnodet@gmail.com">Guillaume Nodet</a>
 */
public class Candidate implements Comparable<Candidate> {

    private final String value;
    private final String displ;
    private final String group;
    private final String descr;
    private final String suffix;
    private final String key;
    private final boolean complete;

    /**
     * Simple constructor with only a single String as an argument.
     *
     * @param value the candidate
     */
    public Candidate(String value) {
        this(value, value, null, null, null, null, true);
    }

    /**
     * Constructs a new Candidate.
     *
     * @param value the value
     * @param displ the display string
     * @param group the group
     * @param descr the description
     * @param suffix the suffix
     * @param key the key
     * @param complete the complete flag
     */
    public Candidate(String value, String displ, String group, String descr, String suffix, String key, boolean complete) {
        this.value = Objects.requireNonNull(value);
        this.displ = Objects.requireNonNull(displ);
        this.group = group;
        this.descr = descr;
        this.suffix = suffix;
        this.key = key;
        this.complete = complete;
    }

    /**
     * The value that will be used for the actual completion.
     * This string should not contain ANSI sequences.
     * @return the value
     */
    public String value() {
        return value;
    }

    /**
     * The string that will be displayed to the user.
     * This string may contain ANSI sequences.
     * @return the display string
     */
    public String displ() {
        return displ;
    }

    /**
     * The group name for this candidate.
     * Candidates can be grouped together and this string is used
     * as a key for the group and displayed to the user.
     * @return the group
     *
     * @see LineReader.Option#GROUP
     * @see LineReader.Option#AUTO_GROUP
     */
    public String group() {
        return group;
    }

    /**
     * Description of this candidate, usually a small help message
     * to understand the meaning of this candidate.
     * This string may contain ANSI sequences.
     * @return the description
     */
    public String descr() {
        return descr;
    }

    /**
     * The suffix is added when this candidate is displayed.
     * However, if the next character entered does not match,
     * the suffix will be automatically removed.
     * This string should not contain ANSI sequences.
     * @return the suffix
     *
     * @see LineReader.Option#AUTO_REMOVE_SLASH
     * @see LineReader#REMOVE_SUFFIX_CHARS
     */
    public String suffix() {
        return suffix;
    }

    /**
     * Candidates which have the same key will be merged together.
     * For example, if a command has multiple aliases, they can be merged
     * if they are using the same key.
     * @return the key
     */
    public String key() {
        return key;
    }

    /**
     * Boolean indicating whether this candidate is complete or
     * if the completer may further expand the candidate value
     * after this candidate has been selected.
     * This can be the case when completing folders for example.
     * If the candidate is complete and is selected, a space
     * separator will be added.
     * @return the completion flag
     */
    public boolean complete() {
        return complete;
    }

    @Override
    public int compareTo(Candidate o) {
        return value.compareTo(o.value);
    }
}
