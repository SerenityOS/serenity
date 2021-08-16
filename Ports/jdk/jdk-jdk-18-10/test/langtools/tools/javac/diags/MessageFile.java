/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class to facilitate manipulating compiler.properties.
 */
class MessageFile {
    static final Pattern emptyOrCommentPattern = Pattern.compile("( *#.*)?");
    static final Pattern typePattern = Pattern.compile("[-\\\\'A-Z\\.a-z ]+( \\([A-Za-z 0-9]+\\))?");
    static final Pattern infoPattern = Pattern.compile(String.format("# ([0-9]+: %s, )*[0-9]+: %s",
            typePattern.pattern(), typePattern.pattern()));

    /**
     * A line of text within the message file.
     * The lines form a doubly linked list for simple navigation.
     */
    class Line {
        String text;
        Line prev;
        Line next;

        Line(String text) {
            this.text = text;
        }

        boolean isEmptyOrComment() {
            return emptyOrCommentPattern.matcher(text).matches();
        }

        boolean isInfo() {
            return infoPattern.matcher(text).matches();
        }

        boolean hasContinuation() {
            return (next != null) && text.endsWith("\\");
        }

        Line insertAfter(String text) {
            Line l = new Line(text);
            insertAfter(l);
            return l;
        }

        void insertAfter(Line l) {
            assert l.prev == null && l.next == null;
            l.prev = this;
            l.next = next;
            if (next == null)
                lastLine = l;
            else
                next.prev = l;
            next = l;
        }

        Line insertBefore(String text) {
            Line l = new Line(text);
            insertBefore(l);
            return l;
        }

        void insertBefore(Line l) {
            assert l.prev == null && l.next == null;
            l.prev = prev;
            l.next = this;
            if (prev == null)
                firstLine = l;
            else
                prev.next = l;
            prev = l;
        }

        void remove() {
            if (prev == null)
                firstLine = next;
            else
                prev.next = next;
            if (next == null)
                lastLine = prev;
            else
                next.prev = prev;
            prev = null;
            next = null;
        }
    }

    /**
     * A message within the message file.
     * A message is a series of lines containing a "name=value" property,
     * optionally preceded by a comment describing the use of placeholders
     * such as {0}, {1}, etc within the property value.
     */
    static final class Message {
        final Line firstLine;
        private Info info;

        Message(Line l) {
            firstLine = l;
        }

        boolean needInfo() {
            Line l = firstLine;
            while (true) {
                if (l.text.matches(".*\\{[0-9]+\\}.*"))
                    return true;
                if (!l.hasContinuation())
                    return false;
                l = l.next;
            }
        }

        Set<Integer> getPlaceholders() {
            Pattern p = Pattern.compile("\\{([0-9]+)\\}");
            Set<Integer> results = new TreeSet<Integer>();
            Line l = firstLine;
            while (true) {
                Matcher m = p.matcher(l.text);
                while (m.find())
                    results.add(Integer.parseInt(m.group(1)));
                if (!l.hasContinuation())
                    return results;
                l = l.next;
            }
        }

        /**
         * Get the Info object for this message. It may be empty if there
         * if no comment preceding the property specification.
         */
        Info getInfo() {
            if (info == null) {
                Line l = firstLine.prev;
                if (l != null && l.isInfo())
                    info = new Info(l.text);
                else
                    info = new Info();
            }
            return info;
        }

        /**
         * Set the Info for this message.
         * If there was an info comment preceding the property specification,
         * it will be updated; otherwise, one will be inserted.
         */
        void setInfo(Info info) {
            this.info = info;
            Line l = firstLine.prev;
            if (l != null && l.isInfo())
                l.text = info.toComment();
            else
                firstLine.insertBefore(info.toComment());
        }

        /**
         * Get all the lines pertaining to this message.
         */
        List<Line> getLines(boolean includeAllPrecedingComments) {
            List<Line> lines = new ArrayList<Line>();
            Line l = firstLine;
            if (includeAllPrecedingComments) {
                // scan back to find end of prev message
                while (l.prev != null && l.prev.isEmptyOrComment())
                    l = l.prev;
                // skip leading blank lines
                while (l.text.isEmpty())
                    l = l.next;
            } else {
                if (l.prev != null && l.prev.isInfo())
                    l = l.prev;
            }

            // include any preceding lines
            for ( ; l != firstLine; l = l.next)
                lines.add(l);

            // include message lines
            for (l = firstLine; l != null && l.hasContinuation(); l = l.next)
                lines.add(l);
            lines.add(l);

            // include trailing blank line if present
            l = l.next;
            if (l != null && l.text.isEmpty())
                lines.add(l);

            return lines;
        }
    }

    /**
     * An object to represent the comment that may precede the property
     * specification in a Message.
     * The comment is modelled as a list of fields, where the fields correspond
     * to the placeholder values (e.g. {0}, {1}, etc) within the message value.
     */
    static final class Info {
        /**
         * An ordered set of descriptions for a placeholder value in a
         * message.
         */
        static class Field {
            boolean unused;
            Set<String> values;
            boolean listOfAny = false;
            boolean setOfAny = false;
            Field(String s) {
                s = s.substring(s.indexOf(": ") + 2);
                values = new LinkedHashSet<String>(Arrays.asList(s.split(" or ")));
                for (String v: values) {
                    if (v.startsWith("list of"))
                        listOfAny = true;
                    if (v.startsWith("set of"))
                        setOfAny = true;
                }
            }

            /**
             * Return true if this field logically contains all the values of
             * another field.
             */
            boolean contains(Field other) {
                if (unused != other.unused)
                    return false;

                for (String v: other.values) {
                    if (values.contains(v))
                        continue;
                    if (v.equals("null") || v.equals("string"))
                        continue;
                    if (v.equals("list") && listOfAny)
                        continue;
                    if (v.equals("set") && setOfAny)
                        continue;
                    return false;
                }
                return true;
            }

            /**
             * Merge the values of another field into this field.
             */
            void merge(Field other) {
                unused |= other.unused;
                values.addAll(other.values);

                // cleanup unnecessary entries

                if (values.contains("null") && values.size() > 1) {
                    // "null" is superceded by anything else
                    values.remove("null");
                }

                if (values.contains("string") && values.size() > 1) {
                    // "string" is superceded by anything else
                    values.remove("string");
                }

                if (values.contains("list")) {
                    // list is superceded by "list of ..."
                    for (String s: values) {
                        if (s.startsWith("list of ")) {
                            values.remove("list");
                            break;
                        }
                    }
                }

                if (values.contains("set")) {
                    // set is superceded by "set of ..."
                    for (String s: values) {
                        if (s.startsWith("set of ")) {
                            values.remove("set");
                            break;
                        }
                    }
                }

                if (other.values.contains("unused")) {
                    values.clear();
                    values.add("unused");
                }
            }

            void markUnused() {
                values = new LinkedHashSet<String>();
                values.add("unused");
                listOfAny = false;
                setOfAny = false;
            }

            @Override
            public String toString() {
                return values.toString();
            }
        }

        /** The fields of the Info object. */
        List<Field> fields = new ArrayList<Field>();

        Info() { }

        Info(String text) throws IllegalArgumentException {
            if (!text.startsWith("# "))
                throw new IllegalArgumentException();
            String[] segs = text.substring(2).split(", ");
            fields = new ArrayList<Field>();
            for (String seg: segs) {
                fields.add(new Field(seg));
            }
        }

        Info(Set<String> infos) throws IllegalArgumentException {
            for (String s: infos)
                merge(new Info(s));
        }

        boolean isEmpty() {
            return fields.isEmpty();
        }

        boolean contains(Info other) {
            if (other.isEmpty())
                return true;

            if (fields.size() != other.fields.size())
                return false;

            Iterator<Field> oIter = other.fields.iterator();
            for (Field values: fields) {
                if (!values.contains(oIter.next()))
                    return false;
            }

            return true;
        }

        void merge(Info other) {
            if (fields.isEmpty()) {
                fields.addAll(other.fields);
                return;
            }

            if (other.fields.size() != fields.size())
                throw new IllegalArgumentException();

            Iterator<Field> oIter = other.fields.iterator();
            for (Field d: fields) {
                d.merge(oIter.next());
            }
        }

        void markUnused(Set<Integer> used) {
            for (int i = 0; i < fields.size(); i++) {
                if (!used.contains(i))
                    fields.get(i).markUnused();
            }
        }

        @Override
        public String toString() {
            return fields.toString();
        }

        String toComment() {
            StringBuilder sb = new StringBuilder();
            sb.append("# ");
            String sep = "";
            int i = 0;
            for (Field f: fields) {
                sb.append(sep);
                sb.append(i++);
                sb.append(": ");
                sep = "";
                for (String s: f.values) {
                    sb.append(sep);
                    sb.append(s);
                    sep = " or ";
                }
                sep = ", ";
            }
            return sb.toString();
        }
    }

    Line firstLine;
    Line lastLine;
    Map<String, Message> messages = new TreeMap<String, Message>();

    MessageFile(File file) throws IOException {
        Reader in = new FileReader(file);
        try {
            read(in);
        } finally {
            in.close();
        }
    }

    MessageFile(Reader in) throws IOException {
        read(in);
    }

    final void read(Reader in) throws IOException {
        BufferedReader br = (in instanceof BufferedReader)
                ? (BufferedReader) in
                : new BufferedReader(in);
        String line;
        while ((line = br.readLine()) != null) {
            Line l;
            if (firstLine == null)
                l = firstLine = lastLine = new Line(line);
            else
                l = lastLine.insertAfter(line);
            if (line.startsWith("compiler.")) {
                int eq = line.indexOf("=");
                if (eq > 0)
                    messages.put(line.substring(0, eq), new Message(l));
            }
        }
    }

    void write(File file) throws IOException {
        Writer out = new FileWriter(file);
        try {
            write(out);
        } finally {
            out.close();
        }
    }

    void write(Writer out) throws IOException {
        BufferedWriter bw = (out instanceof BufferedWriter)
                ? (BufferedWriter) out
                : new BufferedWriter(out);
        for (Line l = firstLine; l != null; l = l.next) {
            bw.write(l.text);
            bw.write("\n"); // always use Unix line endings
        }
        bw.flush();
    }
}
