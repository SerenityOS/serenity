/*
 * Copyright (c) 1998, 2014, Oracle and/or its affiliates. All rights reserved.
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

package javax.swing.text.html.parser;

/**
 * A content model state. This is basically a list of pointers to
 * the BNF expression representing the model (the ContentModel).
 * Each element in a DTD has a content model which describes the
 * elements that may occur inside, and the order in which they can
 * occur.
 * <p>
 * Each time a token is reduced a new state is created.
 * <p>
 * See Annex H on page 556 of the SGML handbook for more information.
 *
 * @see Parser
 * @see DTD
 * @see Element
 * @see ContentModel
 * @author Arthur van Hoff
 */
class ContentModelState {
    ContentModel model;
    long value;
    ContentModelState next;

    /**
     * Create a content model state for a content model.
     */
    public ContentModelState(ContentModel model) {
        this(model, null, 0);
    }

    /**
     * Create a content model state for a content model given the
     * remaining state that needs to be reduce.
     */
    ContentModelState(Object content, ContentModelState next) {
        this(content, next, 0);
    }

    /**
     * Create a content model state for a content model given the
     * remaining state that needs to be reduce.
     */
    ContentModelState(Object content, ContentModelState next, long value) {
        this.model = (ContentModel)content;
        this.next = next;
        this.value = value;
    }

    /**
     * Return the content model that is relevant to the current state.
     */
    public ContentModel getModel() {
        ContentModel m = model;
        for (int i = 0; i < value; i++) {
            if (m.next != null) {
                m = m.next;
            } else {
                return null;
            }
        }
        return m;
    }

    /**
     * Check if the state can be terminated. That is there are no more
     * tokens required in the input stream.
     * @return true if the model can terminate without further input
     */
    @SuppressWarnings("fallthrough")
    public boolean terminate() {
        switch (model.type) {
          case '+':
            if ((value == 0) && !(model).empty()) {
                return false;
            }
            // Fall through
          case '*':
          case '?':
            return (next == null) || next.terminate();

          case '|':
            for (ContentModel m = (ContentModel)model.content ; m != null ; m = m.next) {
                if (m.empty()) {
                    return (next == null) || next.terminate();
                }
            }
            return false;

          case '&': {
            ContentModel m = (ContentModel)model.content;

            for (int i = 0 ; m != null ; i++, m = m.next) {
                if ((value & (1L << i)) == 0) {
                    if (!m.empty()) {
                        return false;
                    }
                }
            }
            return (next == null) || next.terminate();
          }

          case ',': {
            ContentModel m = (ContentModel)model.content;
            for (int i = 0 ; i < value ; i++, m = m.next);

            for (; (m != null) && m.empty() ; m = m.next);
            if (m != null) {
                return false;
            }
            return (next == null) || next.terminate();
          }

        default:
          return false;
        }
    }

    /**
     * Check if the state can be terminated. That is there are no more
     * tokens required in the input stream.
     * @return the only possible element that can occur next
     */
    public Element first() {
        switch (model.type) {
          case '*':
          case '?':
          case '|':
          case '&':
            return null;

          case '+':
            return model.first();

          case ',': {
              ContentModel m = (ContentModel)model.content;
              for (int i = 0 ; i < value ; i++, m = m.next);
              return m.first();
          }

          default:
            return model.first();
        }
    }

    /**
     * Advance this state to a new state. An exception is thrown if the
     * token is illegal at this point in the content model.
     * @return next state after reducing a token
     */
    public ContentModelState advance(Object token) {
        switch (model.type) {
          case '+':
            if (model.first(token)) {
                return new ContentModelState(model.content,
                        new ContentModelState(model, next, value + 1)).advance(token);
            }
            if (value != 0) {
                if (next != null) {
                    return next.advance(token);
                } else {
                    return null;
                }
            }
            break;

          case '*':
            if (model.first(token)) {
                return new ContentModelState(model.content, this).advance(token);
            }
            if (next != null) {
                return next.advance(token);
            } else {
                return null;
            }

          case '?':
            if (model.first(token)) {
                return new ContentModelState(model.content, next).advance(token);
            }
            if (next != null) {
                return next.advance(token);
            } else {
                return null;
            }

          case '|':
            for (ContentModel m = (ContentModel)model.content ; m != null ; m = m.next) {
                if (m.first(token)) {
                    return new ContentModelState(m, next).advance(token);
                }
            }
            break;

          case ',': {
            ContentModel m = (ContentModel)model.content;
            for (int i = 0 ; i < value ; i++, m = m.next);

            if (m.first(token) || m.empty()) {
                if (m.next == null) {
                    return new ContentModelState(m, next).advance(token);
                } else {
                    return new ContentModelState(m,
                            new ContentModelState(model, next, value + 1)).advance(token);
                }
            }
            break;
          }

          case '&': {
            ContentModel m = (ContentModel)model.content;
            boolean complete = true;

            for (int i = 0 ; m != null ; i++, m = m.next) {
                if ((value & (1L << i)) == 0) {
                    if (m.first(token)) {
                        return new ContentModelState(m,
                                new ContentModelState(model, next, value | (1L << i))).advance(token);
                    }
                    if (!m.empty()) {
                        complete = false;
                    }
                }
            }
            if (complete) {
                if (next != null) {
                    return next.advance(token);
                } else {
                    return null;
                }
            }
            break;
          }

          default:
            if (model.content == token) {
                if (next == null && (token instanceof Element) &&
                    ((Element)token).content != null) {
                    return new ContentModelState(((Element)token).content);
                }
                return next;
            }
            // PENDING: Currently we don't correctly deal with optional start
            // tags. This can most notably be seen with the 4.01 spec where
            // TBODY's start and end tags are optional.
            // Uncommenting this and the PENDING in ContentModel will
            // correctly skip the omit tags, but the delegate is not notified.
            // Some additional API needs to be added to track skipped tags,
            // and this can then be added back.
/*
            if ((model.content instanceof Element)) {
                Element e = (Element)model.content;

                if (e.omitStart() && e.content != null) {
                    return new ContentModelState(e.content, next).advance(
                                           token);
                }
            }
*/
        }

        // We used to throw this exception at this point.  However, it
        // was determined that throwing this exception was more expensive
        // than returning null, and we could not justify to ourselves why
        // it was necessary to throw an exception, rather than simply
        // returning null.  I'm leaving it in a commented out state so
        // that it can be easily restored if the situation ever arises.
        //
        // throw new IllegalArgumentException("invalid token: " + token);
        return null;
    }
}
