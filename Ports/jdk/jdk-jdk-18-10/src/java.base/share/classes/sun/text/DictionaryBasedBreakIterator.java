/*
 * Copyright (c) 1999, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/*
 *
 * (C) Copyright Taligent, Inc. 1996, 1997 - All Rights Reserved
 * (C) Copyright IBM Corp. 1996 - 2002 - All Rights Reserved
 *
 * The original version of this source code and documentation
 * is copyrighted and owned by Taligent, Inc., a wholly-owned
 * subsidiary of IBM. These materials are provided under terms
 * of a License Agreement between Taligent and Sun. This technology
 * is protected by multiple US and International patents.
 *
 * This notice and attribution to Taligent may not be removed.
 * Taligent is a registered trademark of Taligent, Inc.
 */

package sun.text;

import java.text.CharacterIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * A subclass of RuleBasedBreakIterator that adds the ability to use a dictionary
 * to further subdivide ranges of text beyond what is possible using just the
 * state-table-based algorithm.  This is necessary, for example, to handle
 * word and line breaking in Thai, which doesn't use spaces between words.  The
 * state-table-based algorithm used by RuleBasedBreakIterator is used to divide
 * up text as far as possible, and then contiguous ranges of letters are
 * repeatedly compared against a list of known words (i.e., the dictionary)
 * to divide them up into words.
 *
 * DictionaryBasedBreakIterator uses the same rule language as RuleBasedBreakIterator,
 * but adds one more special substitution name: &lt;dictionary&gt;.  This substitution
 * name is used to identify characters in words in the dictionary.  The idea is that
 * if the iterator passes over a chunk of text that includes two or more characters
 * in a row that are included in &lt;dictionary&gt;, it goes back through that range and
 * derives additional break positions (if possible) using the dictionary.
 *
 * DictionaryBasedBreakIterator is also constructed with the filename of a dictionary
 * file.  It follows a prescribed search path to locate the dictionary (right now,
 * it looks for it in /com/ibm/text/resources in each directory in the classpath,
 * and won't find it in JAR files, but this location is likely to change).  The
 * dictionary file is in a serialized binary format.  We have a very primitive (and
 * slow) BuildDictionaryFile utility for creating dictionary files, but aren't
 * currently making it public.  Contact us for help.
 */
public class DictionaryBasedBreakIterator extends RuleBasedBreakIterator {

    /**
     * a list of known words that is used to divide up contiguous ranges of letters,
     * stored in a compressed, indexed, format that offers fast access
     */
    private BreakDictionary dictionary;

    /**
     * a list of flags indicating which character categories are contained in
     * the dictionary file (this is used to determine which ranges of characters
     * to apply the dictionary to)
     */
    private boolean[] categoryFlags;

    /**
     * a temporary hiding place for the number of dictionary characters in the
     * last range passed over by next()
     */
    private int dictionaryCharCount;

    /**
     * when a range of characters is divided up using the dictionary, the break
     * positions that are discovered are stored here, preventing us from having
     * to use either the dictionary or the state table again until the iterator
     * leaves this range of text
     */
    private int[] cachedBreakPositions;

    /**
     * if cachedBreakPositions is not null, this indicates which item in the
     * cache the current iteration position refers to
     */
    private int positionInCache;

    /**
     * Constructs a DictionaryBasedBreakIterator.
     *
     * @param ruleFile       the name of the rule data file
     * @param ruleData       the rule data loaded from the rule data file
     * @param dictionaryFile the name of the dictionary file
     * @param dictionaryData the dictionary data loaded from the dictionary file
     * @throws MissingResourceException if rule data or dictionary initialization failed
     */
    public DictionaryBasedBreakIterator(String ruleFile, byte[] ruleData,
                                        String dictionaryFile, byte[] dictionaryData) {
        super(ruleFile, ruleData);
        byte[] tmp = super.getAdditionalData();
        if (tmp != null) {
            prepareCategoryFlags(tmp);
            super.setAdditionalData(null);
        }
        dictionary = new BreakDictionary(dictionaryFile, dictionaryData);
    }

    private void prepareCategoryFlags(byte[] data) {
        categoryFlags = new boolean[data.length];
        for (int i = 0; i < data.length; i++) {
            categoryFlags[i] = (data[i] == (byte)1) ? true : false;
        }
    }

    @Override
    public void setText(CharacterIterator newText) {
        super.setText(newText);
        cachedBreakPositions = null;
        dictionaryCharCount = 0;
        positionInCache = 0;
    }

    /**
     * Sets the current iteration position to the beginning of the text.
     * (i.e., the CharacterIterator's starting offset).
     * @return The offset of the beginning of the text.
     */
    @Override
    public int first() {
        cachedBreakPositions = null;
        dictionaryCharCount = 0;
        positionInCache = 0;
        return super.first();
    }

    /**
     * Sets the current iteration position to the end of the text.
     * (i.e., the CharacterIterator's ending offset).
     * @return The text's past-the-end offset.
     */
    @Override
    public int last() {
        cachedBreakPositions = null;
        dictionaryCharCount = 0;
        positionInCache = 0;
        return super.last();
    }

    /**
     * Advances the iterator one step backwards.
     * @return The position of the last boundary position before the
     * current iteration position
     */
    @Override
    public int previous() {
        CharacterIterator text = getText();

        // if we have cached break positions and we're still in the range
        // covered by them, just move one step backward in the cache
        if (cachedBreakPositions != null && positionInCache > 0) {
            --positionInCache;
            text.setIndex(cachedBreakPositions[positionInCache]);
            return cachedBreakPositions[positionInCache];
        }

        // otherwise, dump the cache and use the inherited previous() method to move
        // backward.  This may fill up the cache with new break positions, in which
        // case we have to mark our position in the cache
        else {
            cachedBreakPositions = null;
            int result = super.previous();
            if (cachedBreakPositions != null) {
                positionInCache = cachedBreakPositions.length - 2;
            }
            return result;
        }
    }

    /**
     * Sets the current iteration position to the last boundary position
     * before the specified position.
     * @param offset The position to begin searching from
     * @return The position of the last boundary before "offset"
     */
    @Override
    public int preceding(int offset) {
        CharacterIterator text = getText();
        checkOffset(offset, text);

        // if we have no cached break positions, or "offset" is outside the
        // range covered by the cache, we can just call the inherited routine
        // (which will eventually call other routines in this class that may
        // refresh the cache)
        if (cachedBreakPositions == null || offset <= cachedBreakPositions[0] ||
                offset > cachedBreakPositions[cachedBreakPositions.length - 1]) {
            cachedBreakPositions = null;
            return super.preceding(offset);
        }

        // on the other hand, if "offset" is within the range covered by the cache,
        // then all we have to do is search the cache for the last break position
        // before "offset"
        else {
            positionInCache = 0;
            while (positionInCache < cachedBreakPositions.length
                   && offset > cachedBreakPositions[positionInCache]) {
                ++positionInCache;
            }
            --positionInCache;
            text.setIndex(cachedBreakPositions[positionInCache]);
            return text.getIndex();
        }
    }

    /**
     * Sets the current iteration position to the first boundary position after
     * the specified position.
     * @param offset The position to begin searching forward from
     * @return The position of the first boundary after "offset"
     */
    @Override
    public int following(int offset) {
        CharacterIterator text = getText();
        checkOffset(offset, text);

        // if we have no cached break positions, or if "offset" is outside the
        // range covered by the cache, then dump the cache and call our
        // inherited following() method.  This will call other methods in this
        // class that may refresh the cache.
        if (cachedBreakPositions == null || offset < cachedBreakPositions[0] ||
                offset >= cachedBreakPositions[cachedBreakPositions.length - 1]) {
            cachedBreakPositions = null;
            return super.following(offset);
        }

        // on the other hand, if "offset" is within the range covered by the
        // cache, then just search the cache for the first break position
        // after "offset"
        else {
            positionInCache = 0;
            while (positionInCache < cachedBreakPositions.length
                   && offset >= cachedBreakPositions[positionInCache]) {
                ++positionInCache;
            }
            text.setIndex(cachedBreakPositions[positionInCache]);
            return text.getIndex();
        }
    }

    /**
     * This is the implementation function for next().
     */
    @Override
    protected int handleNext() {
        CharacterIterator text = getText();

        // if there are no cached break positions, or if we've just moved
        // off the end of the range covered by the cache, we have to dump
        // and possibly regenerate the cache
        if (cachedBreakPositions == null ||
            positionInCache == cachedBreakPositions.length - 1) {

            // start by using the inherited handleNext() to find a tentative return
            // value.   dictionaryCharCount tells us how many dictionary characters
            // we passed over on our way to the tentative return value
            int startPos = text.getIndex();
            dictionaryCharCount = 0;
            int result = super.handleNext();

            // if we passed over more than one dictionary character, then we use
            // divideUpDictionaryRange() to regenerate the cached break positions
            // for the new range
            if (dictionaryCharCount > 1 && result - startPos > 1) {
                divideUpDictionaryRange(startPos, result);
            }

            // otherwise, the value we got back from the inherited fuction
            // is our return value, and we can dump the cache
            else {
                cachedBreakPositions = null;
                return result;
            }
        }

        // if the cache of break positions has been regenerated (or existed all
        // along), then just advance to the next break position in the cache
        // and return it
        if (cachedBreakPositions != null) {
            ++positionInCache;
            text.setIndex(cachedBreakPositions[positionInCache]);
            return cachedBreakPositions[positionInCache];
        }
        return -9999;   // SHOULD NEVER GET HERE!
    }

    /**
     * Looks up a character category for a character.
     */
    @Override
    protected int lookupCategory(int c) {
        // this override of lookupCategory() exists only to keep track of whether we've
        // passed over any dictionary characters.  It calls the inherited lookupCategory()
        // to do the real work, and then checks whether its return value is one of the
        // categories represented in the dictionary.  If it is, bump the dictionary-
        // character count.
        int result = super.lookupCategory(c);
        if (result != RuleBasedBreakIterator.IGNORE && categoryFlags[result]) {
            ++dictionaryCharCount;
        }
        return result;
    }

    /**
     * This is the function that actually implements the dictionary-based
     * algorithm.  Given the endpoints of a range of text, it uses the
     * dictionary to determine the positions of any boundaries in this
     * range.  It stores all the boundary positions it discovers in
     * cachedBreakPositions so that we only have to do this work once
     * for each time we enter the range.
     */
    @SuppressWarnings("unchecked")
    private void divideUpDictionaryRange(int startPos, int endPos) {
        CharacterIterator text = getText();

        // the range we're dividing may begin or end with non-dictionary characters
        // (i.e., for line breaking, we may have leading or trailing punctuation
        // that needs to be kept with the word).  Seek from the beginning of the
        // range to the first dictionary character
        text.setIndex(startPos);
        int c = getCurrent();
        int category = lookupCategory(c);
        while (category == IGNORE || !categoryFlags[category]) {
            c = getNext();
            category = lookupCategory(c);
        }

        // initialize.  We maintain two stacks: currentBreakPositions contains
        // the list of break positions that will be returned if we successfully
        // finish traversing the whole range now.  possibleBreakPositions lists
        // all other possible word ends we've passed along the way.  (Whenever
        // we reach an error [a sequence of characters that can't begin any word
        // in the dictionary], we back up, possibly delete some breaks from
        // currentBreakPositions, move a break from possibleBreakPositions
        // to currentBreakPositions, and start over from there.  This process
        // continues in this way until we either successfully make it all the way
        // across the range, or exhaust all of our combinations of break
        // positions.)
        Stack<Integer> currentBreakPositions = new Stack<>();
        Stack<Integer> possibleBreakPositions = new Stack<>();
        List<Integer> wrongBreakPositions = new ArrayList<>();

        // the dictionary is implemented as a trie, which is treated as a state
        // machine.  -1 represents the end of a legal word.  Every word in the
        // dictionary is represented by a path from the root node to -1.  A path
        // that ends in state 0 is an illegal combination of characters.
        int state = 0;

        // these two variables are used for error handling.  We keep track of the
        // farthest we've gotten through the range being divided, and the combination
        // of breaks that got us that far.  If we use up all possible break
        // combinations, the text contains an error or a word that's not in the
        // dictionary.  In this case, we "bless" the break positions that got us the
        // farthest as real break positions, and then start over from scratch with
        // the character where the error occurred.
        int farthestEndPoint = text.getIndex();
        Stack<Integer> bestBreakPositions = null;

        // initialize (we always exit the loop with a break statement)
        c = getCurrent();
        while (true) {

            // if we can transition to state "-1" from our current state, we're
            // on the last character of a legal word.  Push that position onto
            // the possible-break-positions stack
            if (dictionary.getNextState(state, 0) == -1) {
                possibleBreakPositions.push(text.getIndex());
            }

            // look up the new state to transition to in the dictionary
            state = dictionary.getNextStateFromCharacter(state, c);

            // if the character we're sitting on causes us to transition to
            // the "end of word" state, then it was a non-dictionary character
            // and we've successfully traversed the whole range.  Drop out
            // of the loop.
            if (state == -1) {
                currentBreakPositions.push(text.getIndex());
                break;
            }

            // if the character we're sitting on causes us to transition to
            // the error state, or if we've gone off the end of the range
            // without transitioning to the "end of word" state, we've hit
            // an error...
            else if (state == 0 || text.getIndex() >= endPos) {

                // if this is the farthest we've gotten, take note of it in
                // case there's an error in the text
                if (text.getIndex() > farthestEndPoint) {
                    farthestEndPoint = text.getIndex();

                    @SuppressWarnings("unchecked")
                    Stack<Integer> currentBreakPositionsCopy = (Stack<Integer>) currentBreakPositions.clone();

                    bestBreakPositions = currentBreakPositionsCopy;
                }

                // wrongBreakPositions is a list of all break positions
                // we've tried starting that didn't allow us to traverse
                // all the way through the text.  Every time we pop a
                // break position off of currentBreakPositions, we put it
                // into wrongBreakPositions to avoid trying it again later.
                // If we make it to this spot, we're either going to back
                // up to a break in possibleBreakPositions and try starting
                // over from there, or we've exhausted all possible break
                // positions and are going to do the fallback procedure.
                // This loop prevents us from messing with anything in
                // possibleBreakPositions that didn't work as a starting
                // point the last time we tried it (this is to prevent a bunch of
                // repetitive checks from slowing down some extreme cases)
                while (!possibleBreakPositions.isEmpty()
                        && wrongBreakPositions.contains(possibleBreakPositions.peek())) {
                    possibleBreakPositions.pop();
                }

                // if we've used up all possible break-position combinations, there's
                // an error or an unknown word in the text.  In this case, we start
                // over, treating the farthest character we've reached as the beginning
                // of the range, and "blessing" the break positions that got us that
                // far as real break positions
                if (possibleBreakPositions.isEmpty()) {
                    if (bestBreakPositions != null) {
                        currentBreakPositions = bestBreakPositions;
                        if (farthestEndPoint < endPos) {
                            text.setIndex(farthestEndPoint + 1);
                        }
                        else {
                            break;
                        }
                    }
                    else {
                        if ((currentBreakPositions.size() == 0 ||
                             currentBreakPositions.peek().intValue() != text.getIndex())
                            && text.getIndex() != startPos) {
                            currentBreakPositions.push(text.getIndex());
                        }
                        getNext();
                        currentBreakPositions.push(text.getIndex());
                    }
                }

                // if we still have more break positions we can try, then promote the
                // last break in possibleBreakPositions into currentBreakPositions,
                // and get rid of all entries in currentBreakPositions that come after
                // it.  Then back up to that position and start over from there (i.e.,
                // treat that position as the beginning of a new word)
                else {
                    Integer temp = possibleBreakPositions.pop();
                    Integer temp2 = null;
                    while (!currentBreakPositions.isEmpty() && temp.intValue() <
                           currentBreakPositions.peek().intValue()) {
                        temp2 = currentBreakPositions.pop();
                        wrongBreakPositions.add(temp2);
                    }
                    currentBreakPositions.push(temp);
                    text.setIndex(currentBreakPositions.peek().intValue());
                }

                // re-sync "c" for the next go-round, and drop out of the loop if
                // we've made it off the end of the range
                c = getCurrent();
                if (text.getIndex() >= endPos) {
                    break;
                }
            }

            // if we didn't hit any exceptional conditions on this last iteration,
            // just advance to the next character and loop
            else {
                c = getNext();
            }
        }

        // dump the last break position in the list, and replace it with the actual
        // end of the range (which may be the same character, or may be further on
        // because the range actually ended with non-dictionary characters we want to
        // keep with the word)
        if (!currentBreakPositions.isEmpty()) {
            currentBreakPositions.pop();
        }
        currentBreakPositions.push(endPos);

        // create a regular array to hold the break positions and copy
        // the break positions from the stack to the array (in addition,
        // our starting position goes into this array as a break position).
        // This array becomes the cache of break positions used by next()
        // and previous(), so this is where we actually refresh the cache.
        cachedBreakPositions = new int[currentBreakPositions.size() + 1];
        cachedBreakPositions[0] = startPos;

        for (int i = 0; i < currentBreakPositions.size(); i++) {
            cachedBreakPositions[i + 1] = currentBreakPositions.elementAt(i).intValue();
        }
        positionInCache = 0;
    }
}
