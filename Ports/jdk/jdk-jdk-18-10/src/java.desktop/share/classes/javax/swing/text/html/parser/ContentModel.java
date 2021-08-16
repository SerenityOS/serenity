/*
 * Copyright (c) 1998, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Vector;
import java.util.Enumeration;
import java.io.*;


/**
 * A representation of a content model. A content model is
 * basically a restricted BNF expression. It is restricted in
 * the sense that it must be deterministic. This means that you
 * don't have to represent it as a finite state automaton.<p>
 * See Annex H on page 556 of the SGML handbook for more information.
 *
 * @author   Arthur van Hoff
 *
 */
@SuppressWarnings("serial") // Same-version serialization only
public final class ContentModel implements Serializable {
    /**
     * Type. Either '*', '?', '+', ',', '|', '&amp;'.
     */
    public int type;

    /**
     * The content. Either an Element or a ContentModel.
     */
    public Object content;

    /**
     * The next content model (in a ',', '|' or '&amp;' expression).
     */
    public ContentModel next;

    /**
     * Creates {@code ContentModel}
     */
    public ContentModel() {
    }

    /**
     * Create a content model for an element.
     *
     * @param content  the element
     */
    public ContentModel(Element content) {
        this(0, content, null);
    }

    /**
     * Create a content model of a particular type.
     *
     * @param type     the type
     * @param content  the content
     */
    public ContentModel(int type, ContentModel content) {
        this(type, content, null);
    }

    /**
     * Create a content model of a particular type.
     *
     * @param type     the type
     * @param content  the content
     * @param next     the next content model
     */
    public ContentModel(int type, Object content, ContentModel next) {
        this.type = type;
        this.content = content;
        this.next = next;
    }

    /**
     * Return true if the content model could
     * match an empty input stream.
     *
     * @return {@code true} if the content model could
     *         match an empty input stream
     */
    public boolean empty() {
        switch (type) {
          case '*':
          case '?':
            return true;

          case '+':
          case '|':
            for (ContentModel m = (ContentModel)content ; m != null ; m = m.next) {
                if (m.empty()) {
                    return true;
                }
            }
            return false;

          case ',':
          case '&':
            for (ContentModel m = (ContentModel)content ; m != null ; m = m.next) {
                if (!m.empty()) {
                    return false;
                }
            }
            return true;

          default:
            return false;
        }
    }

    /**
     * Update elemVec with the list of elements that are
     * part of the this contentModel.
     *
     * @param elemVec  the list of elements
     */
     public void getElements(Vector<Element> elemVec) {
         switch (type) {
         case '*':
         case '?':
         case '+':
             ((ContentModel)content).getElements(elemVec);
             break;
         case ',':
         case '|':
         case '&':
             for (ContentModel m=(ContentModel)content; m != null; m=m.next){
                 m.getElements(elemVec);
             }
             break;
         default:
             elemVec.addElement((Element)content);
         }
     }

     private boolean[] valSet;
     private boolean[] val;
     // A cache used by first().  This cache was found to speed parsing
     // by about 10% (based on measurements of the 4-12 code base after
     // buffering was fixed).

    /**
     * Return true if the token could potentially be the
     * first token in the input stream.
     *
     * @param token  the token
     *
     * @return {@code true} if the token could potentially be the first token
     *         in the input stream
     */
    public boolean first(Object token) {
        switch (type) {
          case '*':
          case '?':
          case '+':
            return ((ContentModel)content).first(token);

          case ',':
            for (ContentModel m = (ContentModel)content ; m != null ; m = m.next) {
                if (m.first(token)) {
                    return true;
                }
                if (!m.empty()) {
                    return false;
                }
            }
            return false;

          case '|':
          case '&': {
            Element e = (Element) token;
            if (valSet == null || valSet.length <= Element.getMaxIndex()) {
                valSet = new boolean[Element.getMaxIndex() + 1];
                val = new boolean[valSet.length];
            }
            if (valSet[e.index]) {
                return val[e.index];
            }
            for (ContentModel m = (ContentModel)content ; m != null ; m = m.next) {
                if (m.first(token)) {
                    val[e.index] = true;
                    break;
                }
            }
            valSet[e.index] = true;
            return val[e.index];
          }

          default:
            return (content == token);
            // PENDING: refer to comment in ContentModelState
/*
              if (content == token) {
                  return true;
              }
              Element e = (Element)content;
              if (e.omitStart() && e.content != null) {
                  return e.content.first(token);
              }
              return false;
*/
        }
    }

    /**
     * Return the element that must be next.
     *
     * @return the element that must be next
     */
    public Element first() {
        switch (type) {
          case '&':
          case '|':
          case '*':
          case '?':
            return null;

          case '+':
          case ',':
            return ((ContentModel)content).first();

          default:
            return (Element)content;
        }
    }

    /**
     * Convert to a string.
     *
     * @return the string representation of this {@code ContentModel}
     */
    public String toString() {
        switch (type) {
          case '*':
            return content + "*";
          case '?':
            return content + "?";
          case '+':
            return content + "+";

          case ',':
          case '|':
          case '&':
            char[] data = {' ', (char)type, ' '};
            String str = "";
            for (ContentModel m = (ContentModel)content ; m != null ; m = m.next) {
                str = str + m;
                if (m.next != null) {
                    str += new String(data);
                }
            }
            return "(" + str + ")";

          default:
            return content.toString();
        }
    }
}
