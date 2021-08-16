/*
 * Copyright (c) 2002, 2019, Oracle and/or its affiliates. All rights reserved.
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

package javadoc.tester;

import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A class to check the links in a set of HTML files.
 */
public class LinkChecker extends HtmlChecker {

    private final Map<Path, IDTable> allFiles;
    private final Map<URI, IDTable> allURIs;

    private int links;
    private int duplicateIds;
    private int missingIds;

    private IDTable currTable;
    private boolean html5;

    LinkChecker(PrintStream out, Function<Path,String> fileReader) {
        super(out, fileReader);
        allFiles = new HashMap<>();
        allURIs = new HashMap<>();
    }

    @Override
    public void report() {
        List<Path> missingFiles = getMissingFiles();
        if (!missingFiles.isEmpty()) {
            report("Missing files: (" + missingFiles.size() + ")");
            missingFiles.stream()
                    .sorted()
                    .forEach(this::reportMissingFile);
            errors += missingFiles.size();
        }

        if (!allURIs.isEmpty()) {
            report(false, "External URLs:");
            allURIs.keySet().stream()
                    .sorted(new URIComparator())
                    .forEach(uri -> report(false, "  %s", uri.toString()));
        }

        int anchors = 0;
        for (IDTable t : allFiles.values()) {
            anchors += t.map.values().stream()
                    .filter(e -> !e.getReferences().isEmpty())
                    .count();
        }
        for (IDTable t : allURIs.values()) {
            anchors += t.map.values().stream()
                    .filter(e -> !e.references.isEmpty())
                    .count();
        }

        report(false, "Checked " + files + " files.");
        report(false, "Found " + links + " references to " + anchors + " anchors "
                + "in " + allFiles.size() + " files and " + allURIs.size() + " other URIs.");
        report(!missingFiles.isEmpty(),   "%6d missing files", missingFiles.size());
        report(duplicateIds > 0, "%6d duplicate ids", duplicateIds);
        report(missingIds > 0,   "%6d missing ids", missingIds);

        Map<String, Integer> schemeCounts = new TreeMap<>();
        Map<String, Integer> hostCounts = new TreeMap<>(new HostComparator());
        for (URI uri : allURIs.keySet()) {
            String scheme = uri.getScheme();
            if (scheme != null) {
                schemeCounts.put(scheme, schemeCounts.computeIfAbsent(scheme, s -> 0) + 1);
            }
            String host = uri.getHost();
            if (host != null) {
                hostCounts.put(host, hostCounts.computeIfAbsent(host, h -> 0) + 1);
            }
        }

        if (schemeCounts.size() > 0) {
            report(false, "Schemes");
            schemeCounts.forEach((s, n) -> report(!isSchemeOK(s), "%6d %s", n, s));
        }

        if (hostCounts.size() > 0) {
            report(false, "Hosts");
            hostCounts.forEach((h, n) -> report(false, "%6d %s", n, h));
        }
    }

    private void report(String message, Object... args) {
        out.println(String.format(message, args));
    }

    private void report(boolean highlight, String message, Object... args) {
        out.print(highlight ? "* " : "  ");
        out.println(String.format(message, args));
    }

    private void reportMissingFile(Path file) {
        report("%s", relativePath(file));
        IDTable table = allFiles.get(file);
        Set<Path> refs = new TreeSet<>();
        for (ID id : table.map.values()) {
            if (id.references != null) {
                for (Position p : id.references) {
                    refs.add(p.path);
                }
            }
        }
        int n = 0;
        int MAX_REFS = 10;
        for (Path ref : refs) {
            report("    in " + relativePath(ref));
            if (++n == MAX_REFS) {
                report("    ... and %d more", refs.size() - n);
                break;
            }
        }
    }

    @Override
    public void startFile(Path path) {
        currTable = allFiles.computeIfAbsent(currFile, p -> new IDTable(p));
        html5 = false;
    }

    @Override
    public void endFile() {
        currTable.check();
    }

    @Override
    public void docType(String doctype) {
        html5 = doctype.matches("(?i)<\\?doctype\\s+html>");
    }

    @Override @SuppressWarnings("fallthrough")
    public void startElement(String name, Map<String, String> attrs, boolean selfClosing) {
        int line = getLineNumber();
        switch (name) {
            case "a":
                String nameAttr = html5 ? null : attrs.get("name");
                if (nameAttr != null) {
                    foundAnchor(line, nameAttr);
                }
                // fallthrough
            case "link":
                String href = attrs.get("href");
                if (href != null) {
                    foundReference(line, href);
                }
                break;
        }

        String idAttr = attrs.get("id");
        if (idAttr != null) {
            foundAnchor(line, idAttr);
        }
    }

    @Override
    public void endElement(String name) { }

    private void foundAnchor(int line, String name) {
        currTable.addID(line, name);
    }

    private void foundReference(int line, String ref) {
        links++;
        try {
            URI uri = new URI(ref);
            if (uri.isAbsolute()) {
                foundReference(line, uri);
            } else {
                Path p;
                String uriPath = uri.getPath();
                if (uriPath == null || uriPath.isEmpty()) {
                    p = currFile;
                } else {
                    p = currFile.getParent().resolve(uriPath).normalize();
                }
                foundReference(line, p, uri.getFragment());
            }
        } catch (URISyntaxException e) {
            error(currFile, line, "invalid URI: " + e);
        }
    }

    private void foundReference(int line, Path p, String fragment) {
        IDTable t = allFiles.computeIfAbsent(p, key -> new IDTable(key));
        t.addReference(fragment, currFile, line);
    }

    private void foundReference(int line, URI uri) {
        if (!isSchemeOK(uri.getScheme())) {
            error(currFile, line, "bad scheme in URI");
        }

        String fragment = uri.getFragment();
        try {
            URI noFrag = new URI(uri.toString().replaceAll("#\\Q" + fragment + "\\E$", ""));
            IDTable t = allURIs.computeIfAbsent(noFrag, key -> new IDTable(key.toString()));
            t.addReference(fragment, currFile, line);
        } catch (URISyntaxException e) {
            throw new Error(e);
        }
    }

    private boolean isSchemeOK(String uriScheme) {
        if (uriScheme == null) {
            return true;
        }

        switch (uriScheme) {
            case "file":
            case "ftp":
            case "http":
            case "https":
            case "javascript":
            case "mailto":
                return true;

            default:
                return false;
        }
    }

    private List<Path> getMissingFiles() {
        return allFiles.entrySet().stream()
                .filter(e -> !Files.exists(e.getKey()))
                .map(e -> e.getKey())
                .collect(Collectors.toList());
    }

    /**
     * A position in a file, as identified by a file name and line number.
     */
    static class Position implements Comparable<Position> {
        Path path;
        int line;

        Position(Path path, int line) {
            this.path = path;
            this.line = line;
        }

        @Override
        public int compareTo(Position o) {
            int v = path.compareTo(o.path);
            return v != 0 ? v : Integer.compare(line, o.line);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            } else if (obj == null || getClass() != obj.getClass()) {
                return false;
            } else {
                final Position other = (Position) obj;
                return Objects.equals(this.path, other.path)
                        && this.line == other.line;
            }
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(path) * 37 + line;
        }
    }

    /**
     * Infor for an ID within an HTML file, and a set of positions that reference it.
     */
    static class ID {
        boolean declared;
        Set<Position> references;

        Set<Position> getReferences() {
            return (references) == null ? Collections.emptySet() : references;
        }
    }

    /**
     * A table for the set of IDs in an HTML file.
     */
    class IDTable {
        private String name;
        private boolean checked;
        private final Map<String, ID> map = new HashMap<>();

        IDTable(Path p) {
            this(relativePath(p).toString());
        }

        IDTable(String name) {
            this.name = name;
        }

        void addID(int line, String name) {
            if (checked) {
                throw new IllegalStateException("Adding ID after file has been read");
            }
            Objects.requireNonNull(name);
            ID id = map.computeIfAbsent(name, x -> new ID());
            if (id.declared) {
                error(currFile, line, "name already declared: " + name);
                duplicateIds++;
            } else {
                id.declared = true;
            }
        }

        void addReference(String name, Path from, int line) {
            if (checked) {
                if (name != null) {
                    ID id = map.get(name);
                    if (id == null || !id.declared) {
                        error(from, line, "id not found: " + this.name + "#" + name);
                    }
                }
            } else {
                ID id = map.computeIfAbsent(name, x -> new ID());
                if (id.references == null) {
                    id.references = new TreeSet<>();
                }
                id.references.add(new Position(from, line));
            }
        }

        void check() {
            map.forEach((name, id) -> {
                if (name != null && !id.declared) {
                    //log.error(currFile, 0, "id not declared: " + name);
                    for (Position ref : id.references) {
                        error(ref.path, ref.line, "id not found: " + this.name + "#" + name);
                    }
                    missingIds++;
                }
            });
            checked = true;
        }
    }

    static class URIComparator implements Comparator<URI> {
        final HostComparator hostComparator = new HostComparator();

        @Override
        public int compare(URI o1, URI o2) {
            if (o1.isOpaque() || o2.isOpaque()) {
                return o1.compareTo(o2);
            }
            String h1 = o1.getHost();
            String h2 = o2.getHost();
            String s1 = o1.getScheme();
            String s2 = o2.getScheme();
            if (h1 == null || h1.isEmpty() || s1 == null || s1.isEmpty()
                    || h2 == null || h2.isEmpty() || s2 == null || s2.isEmpty()) {
                return o1.compareTo(o2);
            }
            int v = hostComparator.compare(h1, h2);
            if (v != 0) {
                return v;
            }
            v = s1.compareTo(s2);
            if (v != 0) {
                return v;
            }
            return o1.compareTo(o2);
        }
    }

    static class HostComparator implements Comparator<String> {
        @Override
        public int compare(String h1, String h2) {
            List<String> l1 = new ArrayList<>(Arrays.asList(h1.split("\\.")));
            Collections.reverse(l1);
            String r1 = String.join(".", l1);
            List<String> l2 = new ArrayList<>(Arrays.asList(h2.split("\\.")));
            Collections.reverse(l2);
            String r2 = String.join(".", l2);
            return r1.compareTo(r2);
        }
    }

}
