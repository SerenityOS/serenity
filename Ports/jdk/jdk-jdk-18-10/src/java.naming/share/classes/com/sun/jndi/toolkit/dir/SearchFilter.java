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
package com.sun.jndi.toolkit.dir;

import javax.naming.*;
import javax.naming.directory.*;
import java.util.Enumeration;
import java.util.HexFormat;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.Locale;

/**
  * A class for parsing LDAP search filters (defined in RFC 1960, 2254)
  *
  * @author Jon Ruiz
  * @author Rosanna Lee
  */
public class SearchFilter implements AttrFilter {

    interface StringFilter extends AttrFilter {
        public void parse() throws InvalidSearchFilterException;
    }

    // %%% "filter" and "pos" are not declared "private" due to bug 4064984.
    String                      filter;
    int                         pos;
    private StringFilter        rootFilter;

    protected static final boolean debug = false;

    protected static final char         BEGIN_FILTER_TOKEN = '(';
    protected static final char         END_FILTER_TOKEN = ')';
    protected static final char         AND_TOKEN = '&';
    protected static final char         OR_TOKEN = '|';
    protected static final char         NOT_TOKEN = '!';
    protected static final char         EQUAL_TOKEN = '=';
    protected static final char         APPROX_TOKEN = '~';
    protected static final char         LESS_TOKEN = '<';
    protected static final char         GREATER_TOKEN = '>';
    protected static final char         EXTEND_TOKEN = ':';
    protected static final char         WILDCARD_TOKEN = '*';

    public SearchFilter(String filter) throws InvalidSearchFilterException {
        this.filter = filter;
        pos = 0;
        normalizeFilter();
        rootFilter = this.createNextFilter();
    }

    // Returns true if targetAttrs passes the filter
    public boolean check(Attributes targetAttrs) throws NamingException {
        if (targetAttrs == null)
            return false;

        return rootFilter.check(targetAttrs);
    }

    /*
     * Utility routines used by member classes
     */

    // does some pre-processing on the string to make it look exactly lik
    // what the parser expects. This only needs to be called once.
    protected void normalizeFilter() {
        skipWhiteSpace(); // get rid of any leading whitespaces

        // Sometimes, search filters don't have "(" and ")" - add them
        if(getCurrentChar() != BEGIN_FILTER_TOKEN) {
            filter = BEGIN_FILTER_TOKEN + filter + END_FILTER_TOKEN;
        }
        // this would be a good place to strip whitespace if desired

        if(debug) {System.out.println("SearchFilter: normalized filter:" +
                                      filter);}
    }

    private void skipWhiteSpace() {
        while (Character.isWhitespace(getCurrentChar())) {
            consumeChar();
        }
    }

    protected StringFilter createNextFilter()
        throws InvalidSearchFilterException {
        StringFilter filter;

        skipWhiteSpace();

        try {
            // make sure every filter starts with "("
            if(getCurrentChar() != BEGIN_FILTER_TOKEN) {
                throw new InvalidSearchFilterException("expected \"" +
                                                       BEGIN_FILTER_TOKEN +
                                                       "\" at position " +
                                                       pos);
            }

            // skip past the "("
            this.consumeChar();

            skipWhiteSpace();

            // use the next character to determine the type of filter
            switch(getCurrentChar()) {
            case AND_TOKEN:
                if (debug) {System.out.println("SearchFilter: creating AND");}
                filter = new CompoundFilter(true);
                filter.parse();
                break;
            case OR_TOKEN:
                if (debug) {System.out.println("SearchFilter: creating OR");}
                filter = new CompoundFilter(false);
                filter.parse();
                break;
            case NOT_TOKEN:
                if (debug) {System.out.println("SearchFilter: creating OR");}
                filter = new NotFilter();
                filter.parse();
                break;
            default:
                if (debug) {System.out.println("SearchFilter: creating SIMPLE");}
                filter = new AtomicFilter();
                filter.parse();
                break;
            }

            skipWhiteSpace();

            // make sure every filter ends with ")"
            if(getCurrentChar() != END_FILTER_TOKEN) {
                throw new InvalidSearchFilterException("expected \"" +
                                                       END_FILTER_TOKEN +
                                                       "\" at position " +
                                                       pos);
            }

            // skip past the ")"
            this.consumeChar();
        } catch (InvalidSearchFilterException e) {
            if (debug) {System.out.println("rethrowing e");}
            throw e; // just rethrow these

        // catch all - any uncaught exception while parsing will end up here
        } catch  (Exception e) {
            if(debug) {System.out.println(e.getMessage());e.printStackTrace();}
            throw new InvalidSearchFilterException("Unable to parse " +
                    "character " + pos + " in \""+
                    this.filter + "\"");
        }

        return filter;
    }

    protected char getCurrentChar() {
        return filter.charAt(pos);
    }

    protected char relCharAt(int i) {
        return filter.charAt(pos + i);
    }

    protected void consumeChar() {
        pos++;
    }

    protected void consumeChars(int i) {
        pos += i;
    }

    protected int relIndexOf(int ch) {
        return filter.indexOf(ch, pos) - pos;
    }

    protected String relSubstring(int beginIndex, int endIndex){
        if(debug){System.out.println("relSubString: " + beginIndex +
                                     " " + endIndex);}
        return filter.substring(beginIndex+pos, endIndex+pos);
    }


   /**
     * A class for dealing with compound filters ("and" & "or" filters).
     */
    final class CompoundFilter implements StringFilter {
        private Vector<StringFilter>  subFilters;
        private boolean polarity;

        CompoundFilter(boolean polarity) {
            subFilters = new Vector<>();
            this.polarity = polarity;
        }

        public void parse() throws InvalidSearchFilterException {
            SearchFilter.this.consumeChar(); // consume the "&"
            while(SearchFilter.this.getCurrentChar() != END_FILTER_TOKEN) {
                if (debug) {System.out.println("CompoundFilter: adding");}
                StringFilter filter = SearchFilter.this.createNextFilter();
                subFilters.addElement(filter);
                skipWhiteSpace();
            }
        }

        public boolean check(Attributes targetAttrs) throws NamingException {
            for(int i = 0; i<subFilters.size(); i++) {
                StringFilter filter = subFilters.elementAt(i);
                if(filter.check(targetAttrs) != this.polarity) {
                    return !polarity;
                }
            }
            return polarity;
        }
    } /* CompoundFilter */

   /**
     * A class for dealing with NOT filters
     */
    final class NotFilter implements StringFilter {
        private StringFilter    filter;

        public void parse() throws InvalidSearchFilterException {
            SearchFilter.this.consumeChar(); // consume the "!"
            filter = SearchFilter.this.createNextFilter();
        }

        public boolean check(Attributes targetAttrs) throws NamingException {
            return !filter.check(targetAttrs);
        }
    } /* notFilter */

    // note: declared here since member classes can't have static variables
    static final int EQUAL_MATCH = 1;
    static final int APPROX_MATCH = 2;
    static final int GREATER_MATCH = 3;
    static final int LESS_MATCH = 4;

    /**
     * A class for dealing with atomic filters
     */
    final class AtomicFilter implements StringFilter {
        private String attrID;
        private String value;
        private int    matchType;

        public void parse() throws InvalidSearchFilterException {

            skipWhiteSpace();

            try {
                // find the end
                int endPos = SearchFilter.this.relIndexOf(END_FILTER_TOKEN);

                //determine the match type
                int i = SearchFilter.this.relIndexOf(EQUAL_TOKEN);
                if(debug) {System.out.println("AtomicFilter: = at " + i);}
                int qualifier = SearchFilter.this.relCharAt(i-1);
                switch(qualifier) {
                case APPROX_TOKEN:
                    if (debug) {System.out.println("Atomic: APPROX found");}
                    matchType = APPROX_MATCH;
                    attrID = SearchFilter.this.relSubstring(0, i-1);
                    value = SearchFilter.this.relSubstring(i+1, endPos);
                    break;

                case GREATER_TOKEN:
                    if (debug) {System.out.println("Atomic: GREATER found");}
                    matchType = GREATER_MATCH;
                    attrID = SearchFilter.this.relSubstring(0, i-1);
                    value = SearchFilter.this.relSubstring(i+1, endPos);
                    break;

                case LESS_TOKEN:
                    if (debug) {System.out.println("Atomic: LESS found");}
                    matchType = LESS_MATCH;
                    attrID = SearchFilter.this.relSubstring(0, i-1);
                    value = SearchFilter.this.relSubstring(i+1, endPos);
                    break;

                case EXTEND_TOKEN:
                    if(debug) {System.out.println("Atomic: EXTEND found");}
                    throw new OperationNotSupportedException("Extensible match not supported");

                default:
                    if (debug) {System.out.println("Atomic: EQUAL found");}
                    matchType = EQUAL_MATCH;
                    attrID = SearchFilter.this.relSubstring(0,i);
                    value = SearchFilter.this.relSubstring(i+1, endPos);
                    break;
                }

                attrID = attrID.trim();
                value = value.trim();

                //update our position
                SearchFilter.this.consumeChars(endPos);

            } catch (Exception e) {
                if (debug) {System.out.println(e.getMessage());
                            e.printStackTrace();}
                InvalidSearchFilterException sfe =
                    new InvalidSearchFilterException("Unable to parse " +
                    "character " + SearchFilter.this.pos + " in \""+
                    SearchFilter.this.filter + "\"");
                sfe.setRootCause(e);
                throw(sfe);
            }

            if(debug) {System.out.println("AtomicFilter: " + attrID + "=" +
                                          value);}
        }

        public boolean check(Attributes targetAttrs) {
            Enumeration<?> candidates;

            try {
                Attribute attr = targetAttrs.get(attrID);
                if(attr == null) {
                    return false;
                }
                candidates = attr.getAll();
            } catch (NamingException ne) {
                if (debug) {System.out.println("AtomicFilter: should never " +
                                               "here");}
                return false;
            }

            while(candidates.hasMoreElements()) {
                String val = candidates.nextElement().toString();
                if (debug) {System.out.println("Atomic: comparing: " + val);}
                switch(matchType) {
                case APPROX_MATCH:
                case EQUAL_MATCH:
                    if(substringMatch(this.value, val)) {
                    if (debug) {System.out.println("Atomic: EQUAL match");}
                        return true;
                    }
                    break;
                case GREATER_MATCH:
                    if (debug) {System.out.println("Atomic: GREATER match");}
                    if(val.compareTo(this.value) >= 0) {
                        return true;
                    }
                    break;
                case LESS_MATCH:
                    if (debug) {System.out.println("Atomic: LESS match");}
                    if(val.compareTo(this.value) <= 0) {
                        return true;
                    }
                    break;
                default:
                    if (debug) {System.out.println("AtomicFilter: unknown " +
                                                   "matchType");}
                }
            }
            return false;
        }

        // used for substring comparisons (where proto has "*" wildcards
        private boolean substringMatch(String proto, String value) {
            // simple case 1: "*" means attribute presence is being tested
            if(proto.equals(Character.toString(WILDCARD_TOKEN))) {
                if(debug) {System.out.println("simple presence assertion");}
                return true;
            }

            // simple case 2: if there are no wildcards, call String.equals()
            if(proto.indexOf(WILDCARD_TOKEN) == -1) {
                return proto.equalsIgnoreCase(value);
            }

            if(debug) {System.out.println("doing substring comparison");}
            // do the work: make sure all the substrings are present
            int currentPos = 0;
            StringTokenizer subStrs = new StringTokenizer(proto, "*", false);

            // do we need to begin with the first token?
            if(proto.charAt(0) != WILDCARD_TOKEN &&
                    !value.toLowerCase(Locale.ENGLISH).startsWith(
                        subStrs.nextToken().toLowerCase(Locale.ENGLISH))) {
                if(debug) {
                    System.out.println("faild initial test");
                }
                return false;
            }

            while(subStrs.hasMoreTokens()) {
                String currentStr = subStrs.nextToken();
                if (debug) {System.out.println("looking for \"" +
                                               currentStr +"\"");}
                currentPos = value.toLowerCase(Locale.ENGLISH).indexOf(
                       currentStr.toLowerCase(Locale.ENGLISH), currentPos);

                if(currentPos == -1) {
                    return false;
                }
                currentPos += currentStr.length();
            }

            // do we need to end with the last token?
            if(proto.charAt(proto.length() - 1) != WILDCARD_TOKEN &&
               currentPos != value.length() ) {
                if(debug) {System.out.println("faild final test");}
                return false;
            }

            return true;
        }

    } /* AtomicFilter */

    // ----- static methods for producing string filters given attribute set
    // ----- or object array


    /**
      * Creates an LDAP filter as a conjunction of the attributes supplied.
      */
    public static String format(Attributes attrs) throws NamingException {
        if (attrs == null || attrs.size() == 0) {
            return "objectClass=*";
        }

        String answer;
        answer = "(& ";
        Attribute attr;
        for (NamingEnumeration<? extends Attribute> e = attrs.getAll();
             e.hasMore(); ) {
            attr = e.next();
            if (attr.size() == 0 || (attr.size() == 1 && attr.get() == null)) {
                // only checking presence of attribute
                answer += "(" + attr.getID() + "=" + "*)";
            } else {
                for (NamingEnumeration<?> ve = attr.getAll();
                     ve.hasMore(); ) {
                    String val = getEncodedStringRep(ve.next());
                    if (val != null) {
                        answer += "(" + attr.getID() + "=" + val + ")";
                    }
                }
            }
        }

        answer += ")";
        //System.out.println("filter: " + answer);
        return answer;
    }

    /**
      * Returns the string representation of an object (such as an attr value).
      * If obj is a byte array, encode each item as \xx, where xx is hex encoding
      * of the byte value.
      * Else, if obj is not a String, use its string representation (toString()).
      * Special characters in obj (or its string representation) are then
      * encoded appropriately according to RFC 2254.
      *         *       \2a
      *         (       \28
      *         )       \29
      *         \       \5c
      *         NUL     \00
      */
    private static String getEncodedStringRep(Object obj) throws NamingException {
        String str;
        if (obj == null)
            return null;

        if (obj instanceof byte[]) {
            // binary data must be encoded as \hh where hh is a hex char
            HexFormat hex = HexFormat.of().withUpperCase().withPrefix("\\");
            byte[] bytes = (byte[])obj;
            return hex.formatHex(bytes);
        }
        if (!(obj instanceof String)) {
            str = obj.toString();
        } else {
            str = (String)obj;
        }
        int len = str.length();
        StringBuilder sb = new StringBuilder(len);
        char ch;
        for (int i = 0; i < len; i++) {
            switch (ch=str.charAt(i)) {
            case '*':
                sb.append("\\2a");
                break;
            case '(':
                sb.append("\\28");
                break;
            case ')':
                sb.append("\\29");
                break;
            case '\\':
                sb.append("\\5c");
                break;
            case 0:
                sb.append("\\00");
                break;
            default:
                sb.append(ch);
            }
        }
        return sb.toString();
    }


    /**
      * Finds the first occurrence of {@code ch} in {@code val} starting
      * from position {@code start}. It doesn't count if {@code ch}
      * has been escaped by a backslash (\)
      */
    public static int findUnescaped(char ch, String val, int start) {
        int len = val.length();

        while (start < len) {
            int where = val.indexOf(ch, start);
            // if at start of string, or not there at all, or if not escaped
            if (where == start || where == -1 || val.charAt(where-1) != '\\')
                return where;

            // start search after escaped star
            start = where + 1;
        }
        return -1;
    }

    /**
     * Formats the expression {@code expr} using arguments from the array
     * {@code args}.
     *
     * <code>{i}</code> specifies the <code>i</code>'th element from
     * the array <code>args</code> is to be substituted for the
     * string "<code>{i}</code>".
     *
     * To escape '{' or '}' (or any other character), use '\'.
     *
     * Uses getEncodedStringRep() to do encoding.
     */

    public static String format(String expr, Object[] args)
        throws NamingException {

         int param;
         int where = 0, start = 0;
         StringBuilder answer = new StringBuilder(expr.length());

         while ((where = findUnescaped('{', expr, start)) >= 0) {
             int pstart = where + 1; // skip '{'
             int pend = expr.indexOf('}', pstart);

             if (pend < 0) {
                 throw new InvalidSearchFilterException("unbalanced {: " + expr);
             }

             // at this point, pend should be pointing at '}'
             try {
                 param = Integer.parseInt(expr.substring(pstart, pend));
             } catch (NumberFormatException e) {
                 throw new InvalidSearchFilterException(
                     "integer expected inside {}: " + expr);
             }

             if (param >= args.length) {
                 throw new InvalidSearchFilterException(
                     "number exceeds argument list: " + param);
             }

             answer.append(expr.substring(start, where)).append(getEncodedStringRep(args[param]));
             start = pend + 1; // skip '}'
         }

         if (start < expr.length())
             answer.append(expr.substring(start));

        return answer.toString();
    }

    /*
     * returns an Attributes instance containing only attributeIDs given in
     * "attributeIDs" whose values come from the given DSContext.
     */
    public static Attributes selectAttributes(Attributes originals,
        String[] attrIDs) throws NamingException {

        if (attrIDs == null)
            return originals;

        Attributes result = new BasicAttributes();

        for(int i=0; i<attrIDs.length; i++) {
            Attribute attr = originals.get(attrIDs[i]);
            if(attr != null) {
                result.put(attr);
            }
        }

        return result;
    }

/*  For testing filter
    public static void main(String[] args) {

        Attributes attrs = new BasicAttributes(LdapClient.caseIgnore);
        attrs.put("cn", "Rosanna Lee");
        attrs.put("sn", "Lee");
        attrs.put("fn", "Rosanna");
        attrs.put("id", "10414");
        attrs.put("machine", "jurassic");


        try {
            System.out.println(format(attrs));

            String  expr = "(&(Age = {0})(Account Balance <= {1}))";
            Object[] fargs = new Object[2];
            // fill in the parameters
            fargs[0] = new Integer(65);
            fargs[1] = new Float(5000);

            System.out.println(format(expr, fargs));


            System.out.println(format("bin={0}",
                new Object[] {new byte[] {0, 1, 2, 3, 4, 5}}));

            System.out.println(format("bin=\\{anything}", null));

        } catch (NamingException e) {
            e.printStackTrace();
        }
    }
*/

}
