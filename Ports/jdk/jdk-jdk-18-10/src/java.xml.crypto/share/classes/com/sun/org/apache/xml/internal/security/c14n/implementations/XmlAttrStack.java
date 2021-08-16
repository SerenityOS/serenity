/*
 * reserved comment block
 * DO NOT REMOVE OR ALTER!
 */
/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.sun.org.apache.xml.internal.security.c14n.implementations;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Attr;

/**
 * An XmlAttrStack that is shared between the Canonical XML 1.0 and 1.1 implementations.
 */
class XmlAttrStack {

    private static final com.sun.org.slf4j.internal.Logger LOG =
        com.sun.org.slf4j.internal.LoggerFactory.getLogger(XmlAttrStack.class);

    private static class XmlsStackElement {
        int level;
        boolean rendered = false;
        final List<Attr> nodes = new ArrayList<>();
    }

    private int currentLevel = 0;
    private int lastlevel = 0;
    private XmlsStackElement cur;

    private final List<XmlsStackElement> levels = new ArrayList<>();
    private final boolean c14n11;

    public XmlAttrStack(boolean c14n11) {
        this.c14n11 = c14n11;
    }

    void push(int level) {
        currentLevel = level;
        if (currentLevel == -1) {
            return;
        }
        cur = null;
        while (lastlevel >= currentLevel) {
            levels.remove(levels.size() - 1);
            int newSize = levels.size();
            if (newSize == 0) {
                lastlevel = 0;
                return;
            }
            lastlevel = levels.get(newSize - 1).level;
        }
    }

    void addXmlnsAttr(Attr n) {
        if (cur == null) {
            cur = new XmlsStackElement();
            cur.level = currentLevel;
            levels.add(cur);
            lastlevel = currentLevel;
        }
        cur.nodes.add(n);
    }

    void getXmlnsAttr(Collection<Attr> col) {
        int size = levels.size() - 1;
        if (cur == null) {
            cur = new XmlsStackElement();
            cur.level = currentLevel;
            lastlevel = currentLevel;
            levels.add(cur);
        }
        boolean parentRendered = false;
        XmlsStackElement e = null;
        if (size == -1) {
            parentRendered = true;
        } else {
            e = levels.get(size);
            if (e.rendered && e.level + 1 == currentLevel) {
                parentRendered = true;
            }
        }
        if (parentRendered) {
            col.addAll(cur.nodes);
            cur.rendered = true;
            return;
        }

        Map<String, Attr> loa = new HashMap<>();
        if (c14n11) {
            List<Attr> baseAttrs = new ArrayList<>();
            boolean successiveOmitted = true;
            for (; size >= 0; size--) {
                e = levels.get(size);
                if (e.rendered) {
                    successiveOmitted = false;
                }
                Iterator<Attr> it = e.nodes.iterator();
                while (it.hasNext() && successiveOmitted) {
                    Attr n = it.next();
                    if ("base".equals(n.getLocalName()) && !e.rendered) {
                        baseAttrs.add(n);
                    } else if (!loa.containsKey(n.getName())) {
                        loa.put(n.getName(), n);
                    }
                }
            }
            if (!baseAttrs.isEmpty()) {
                Iterator<Attr> it = col.iterator();
                String base = null;
                Attr baseAttr = null;
                while (it.hasNext()) {
                    Attr n = it.next();
                    if ("base".equals(n.getLocalName())) {
                        base = n.getValue();
                        baseAttr = n;
                        break;
                    }
                }
                it = baseAttrs.iterator();
                while (it.hasNext()) {
                    Attr n = it.next();
                    if (base == null) {
                        base = n.getValue();
                        baseAttr = n;
                    } else {
                        try {
                            base = joinURI(n.getValue(), base);
                        } catch (URISyntaxException ue) {
                            LOG.debug(ue.getMessage(), ue);
                        }
                    }
                }
                if (base != null && base.length() != 0) {
                    baseAttr.setValue(base);
                    col.add(baseAttr);
                }
            }
        } else {
            for (; size >= 0; size--) {
                e = levels.get(size);
                Iterator<Attr> it = e.nodes.iterator();
                while (it.hasNext()) {
                    Attr n = it.next();
                    if (!loa.containsKey(n.getName())) {
                        loa.put(n.getName(), n);
                    }
                }
            }
        }

        cur.rendered = true;
        col.addAll(loa.values());
    }

    private static String joinURI(String baseURI, String relativeURI) throws URISyntaxException {
        String bscheme = null;
        String bauthority = null;
        String bpath = "";
        String bquery = null;

        // pre-parse the baseURI
        if (baseURI != null) {
            if (baseURI.endsWith("..")) {
                baseURI = baseURI + "/";
            }
            URI base = new URI(baseURI);
            bscheme = base.getScheme();
            bauthority = base.getAuthority();
            bpath = base.getPath();
            bquery = base.getQuery();
        }

        URI r = new URI(relativeURI);
        String rscheme = r.getScheme();
        String rauthority = r.getAuthority();
        String rpath = r.getPath();
        String rquery = r.getQuery();

        String tscheme, tauthority, tpath, tquery;
        if (rscheme != null && rscheme.equals(bscheme)) {
            rscheme = null;
        }
        if (rscheme != null) {
            tscheme = rscheme;
            tauthority = rauthority;
            tpath = removeDotSegments(rpath);
            tquery = rquery;
        } else {
            if (rauthority != null) {
                tauthority = rauthority;
                tpath = removeDotSegments(rpath);
                tquery = rquery;
            } else {
                if (rpath.length() == 0) {
                    tpath = bpath;
                    if (rquery != null) {
                        tquery = rquery;
                    } else {
                        tquery = bquery;
                    }
                } else {
                    if (rpath.charAt(0) == '/') {
                        tpath = removeDotSegments(rpath);
                    } else {
                        if (bauthority != null && bpath.length() == 0) {
                            tpath = "/" + rpath;
                        } else {
                            int last = bpath.lastIndexOf('/');
                            if (last == -1) {
                                tpath = rpath;
                            } else {
                                tpath = bpath.substring(0, last+1) + rpath;
                            }
                        }
                        tpath = removeDotSegments(tpath);
                    }
                    tquery = rquery;
                }
                tauthority = bauthority;
            }
            tscheme = bscheme;
        }
        return new URI(tscheme, tauthority, tpath, tquery, null).toString();
    }

    private static String removeDotSegments(String path) {
        LOG.debug("STEP OUTPUT BUFFER\t\tINPUT BUFFER");

        // 1. The input buffer is initialized with the now-appended path
        // components then replace occurrences of "//" in the input buffer
        // with "/" until no more occurrences of "//" are in the input buffer.
        String input = path;
        while (input.indexOf("//") > -1) {
            input = input.replaceAll("//", "/");
        }

        // Initialize the output buffer with the empty string.
        StringBuilder output = new StringBuilder();

        // If the input buffer starts with a root slash "/" then move this
        // character to the output buffer.
        if (input.charAt(0) == '/') {
            output.append('/');
            input = input.substring(1);
        }

        printStep("1 ", output.toString(), input);

        // While the input buffer is not empty, loop as follows
        while (input.length() != 0) {
            // 2A. If the input buffer begins with a prefix of "./",
            // then remove that prefix from the input buffer
            // else if the input buffer begins with a prefix of "../", then
            // if also the output does not contain the root slash "/" only,
            // then move this prefix to the end of the output buffer else
            // remove that prefix
            if (input.startsWith("./")) {
                input = input.substring(2);
                printStep("2A", output.toString(), input);
            } else if (input.startsWith("../")) {
                input = input.substring(3);
                if (!"/".equals(output.toString())) {
                    output.append("../");
                }
                printStep("2A", output.toString(), input);
                // 2B. if the input buffer begins with a prefix of "/./" or "/.",
                // where "." is a complete path segment, then replace that prefix
                // with "/" in the input buffer; otherwise,
            } else if (input.startsWith("/./")) {
                input = input.substring(2);
                printStep("2B", output.toString(), input);
            } else if ("/.".equals(input)) {
                // FIXME: what is complete path segment?
                input = input.replaceFirst("/.", "/");
                printStep("2B", output.toString(), input);
                // 2C. if the input buffer begins with a prefix of "/../" or "/..",
                // where ".." is a complete path segment, then replace that prefix
                // with "/" in the input buffer and if also the output buffer is
                // empty, last segment in the output buffer equals "../" or "..",
                // where ".." is a complete path segment, then append ".." or "/.."
                // for the latter case respectively to the output buffer else
                // remove the last segment and its preceding "/" (if any) from the
                // output buffer and if hereby the first character in the output
                // buffer was removed and it was not the root slash then delete a
                // leading slash from the input buffer; otherwise,
            } else if (input.startsWith("/../")) {
                input = input.substring(3);
                if (output.length() == 0) {
                    output.append('/');
                } else if (output.toString().endsWith("../")) {
                    output.append("..");
                } else if (output.toString().endsWith("..")) {
                    output.append("/..");
                } else {
                    int index = output.lastIndexOf("/");
                    if (index == -1) {
                        output = new StringBuilder();
                        if (input.charAt(0) == '/') {
                            input = input.substring(1);
                        }
                    } else {
                        output = output.delete(index, output.length());
                    }
                }
                printStep("2C", output.toString(), input);
            } else if ("/..".equals(input)) {
                // FIXME: what is complete path segment?
                input = input.replaceFirst("/..", "/");
                if (output.length() == 0) {
                    output.append('/');
                } else if (output.toString().endsWith("../")) {
                    output.append("..");
                } else if (output.toString().endsWith("..")) {
                    output.append("/..");
                } else {
                    int index = output.lastIndexOf("/");
                    if (index == -1) {
                        output = new StringBuilder();
                        if (input.charAt(0) == '/') {
                            input = input.substring(1);
                        }
                    } else {
                        output = output.delete(index, output.length());
                    }
                }
                printStep("2C", output.toString(), input);
                // 2D. if the input buffer consists only of ".", then remove
                // that from the input buffer else if the input buffer consists
                // only of ".." and if the output buffer does not contain only
                // the root slash "/", then move the ".." to the output buffer
                // else delte it.; otherwise,
            } else if (".".equals(input)) {
                input = "";
                printStep("2D", output.toString(), input);
            } else if ("..".equals(input)) {
                if (!"/".equals(output.toString())) {
                    output.append("..");
                }
                input = "";
                printStep("2D", output.toString(), input);
                // 2E. move the first path segment (if any) in the input buffer
                // to the end of the output buffer, including the initial "/"
                // character (if any) and any subsequent characters up to, but not
                // including, the next "/" character or the end of the input buffer.
            } else {
                int end = -1;
                int begin = input.indexOf('/');
                if (begin == 0) {
                    end = input.indexOf('/', 1);
                } else {
                    end = begin;
                    begin = 0;
                }
                String segment;
                if (end == -1) {
                    segment = input.substring(begin);
                    input = "";
                } else {
                    segment = input.substring(begin, end);
                    input = input.substring(end);
                }
                output.append(segment);
                printStep("2E", output.toString(), input);
            }
        }

        // 3. Finally, if the only or last segment of the output buffer is
        // "..", where ".." is a complete path segment not followed by a slash
        // then append a slash "/". The output buffer is returned as the result
        // of remove_dot_segments
        if (output.toString().endsWith("..")) {
            output.append('/');
            printStep("3 ", output.toString(), input);
        }

        return output.toString();
    }

    private static void printStep(String step, String output, String input) {
        if (LOG.isDebugEnabled()) {
            LOG.debug(" " + step + ":   " + output);
            if (output.length() == 0) {
                LOG.debug("\t\t\t\t" + input);
            } else {
                LOG.debug("\t\t\t" + input);
            }
        }
    }
}
