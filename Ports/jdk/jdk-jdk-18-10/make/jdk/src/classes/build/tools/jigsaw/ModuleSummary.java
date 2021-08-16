/*
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates. All rights reserved.
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

package build.tools.jigsaw;

import java.io.IOException;
import java.io.PrintStream;
import java.lang.module.Configuration;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.lang.module.ResolvedModule;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import static java.lang.module.ModuleDescriptor.*;
import static build.tools.jigsaw.ModuleSummary.HtmlDocument.Selector.*;
import static build.tools.jigsaw.ModuleSummary.HtmlDocument.Division.*;

public class ModuleSummary {
    private static final String USAGE = "Usage: ModuleSummary --module-path <dir> -o <outfile> [--root mn]*";

    public static void main(String[] args) throws Exception {
        int i=0;
        Path modpath = null;
        Path outfile = null;
        Set<String> roots = new HashSet<>();
        while (i < args.length && args[i].startsWith("-")) {
            String arg = args[i++];
            switch (arg) {
                case "--module-path":
                    modpath = Paths.get(args[i++]);
                    break;
                case "-o":
                    outfile = Paths.get(args[i++]);
                    break;
                case "--root":
                    roots.add(args[i++]);
                default:
                    System.err.println(USAGE);
                    System.exit(-1);
            }
        }
        if (outfile == null || modpath == null) {
            System.err.println(USAGE);
            System.exit(1);
        }
        Path dir = outfile.getParent() != null ? outfile.getParent() : Paths.get(".");
        Files.createDirectories(dir);

        Map<String, ModuleSummary> modules = new HashMap<>();
        Set<ModuleReference> mrefs = ModuleFinder.ofSystem().findAll();
        for (ModuleReference mref : mrefs) {
            String mn = mref.descriptor().name();
            Path jmod = modpath.resolve(mn + ".jmod");
            modules.put(mn, new ModuleSummary(mref, jmod));
        }

        if (roots.isEmpty()) {
            roots.addAll(modules.keySet());
        }
        genReport(outfile, modules, roots, "JDK Module Summary");
    }

    static void genReport(Path outfile, Map<String, ModuleSummary> modules, Set<String> roots, String title)
        throws IOException
    {
        Configuration cf = resolve(roots);
        try (PrintStream out = new PrintStream(Files.newOutputStream(outfile))) {
            HtmlDocument doc = new HtmlDocument(title, modules);
            Set<ModuleDescriptor> descriptors = cf.modules().stream()
                    .map(ResolvedModule::reference)
                    .map(ModuleReference::descriptor)
                    .collect(Collectors.toSet());
            doc.writeTo(out, descriptors);
        }
    }

    private final String name;
    private final ModuleDescriptor descriptor;
    private final JmodInfo jmodInfo;
    ModuleSummary(ModuleReference mref, Path jmod) throws IOException {
        this.name = mref.descriptor().name();
        this.descriptor = mref.descriptor();
        this.jmodInfo = new JmodInfo(jmod);
    }

    String name() {
        return name;
    }

    long uncompressedSize() {
        return jmodInfo.size;
    }

    long jmodFileSize() {
        return jmodInfo.filesize; // estimated compressed size
    }

    ModuleDescriptor descriptor() {
        return descriptor;
    }

    int numClasses() {
        return jmodInfo.classCount;
    }

    long classBytes() {
        return jmodInfo.classBytes;
    }

    int numResources() {
        return jmodInfo.resourceCount;
    }

    long resourceBytes() {
        return jmodInfo.resourceBytes;
    }

    int numConfigs() {
        return jmodInfo.configCount;
    }
    long configBytes() {
        return jmodInfo.configBytes;
    }
    int numCommands() {
        return jmodInfo.nativeCmds.size();
    }

    long commandBytes() {
        return jmodInfo.nativeCmds.values().stream()
                .mapToLong(l -> l.longValue()).sum() - jmodInfo.debugInfoCmdBytes;
    }
    int numCommandsDebug() {
        return jmodInfo.debugInfoCmdCount;
    }
    long commandDebugBytes() {
        return jmodInfo.debugInfoCmdBytes;
    }
    int numNativeLibraries() {
        return jmodInfo.nativeLibs.size();
    }

    long nativeLibrariesBytes() {
        return jmodInfo.nativeLibs.values().stream()
                .mapToLong(l -> l.longValue()).sum() - jmodInfo.debugInfoLibBytes;
    }
    int numNativeLibrariesDebug() {
        return jmodInfo.debugInfoLibCount;
    }

    long nativeLibrariesDebugBytes() {
        return jmodInfo.debugInfoLibBytes;
    }

    Map<String,Long> commands() {
        return jmodInfo.nativeCmds;
    }

    Map<String,Long> nativeLibs() {
        return jmodInfo.nativeLibs;
    }

    Map<String,Long> configFiles() {
        return jmodInfo.configFiles;
    }


    static class JmodInfo {
        final long size;
        final long filesize;
        final int  classCount;
        final long classBytes;
        final int  resourceCount;
        final long resourceBytes;
        final int  configCount;
        final long configBytes;
        final int  debugInfoLibCount;
        final long debugInfoLibBytes;
        final int  debugInfoCmdCount;
        final long debugInfoCmdBytes;
        final Map<String,Long> configFiles = new HashMap<>();
        final Map<String,Long> nativeCmds = new HashMap<>();
        final Map<String,Long> nativeLibs = new HashMap<>();

        JmodInfo(Path jmod) throws IOException {
            long total = 0;
            long cBytes = 0, rBytes = 0, cfBytes = 0, dizLibBytes = 0, dizCmdBytes = 0;
            int  cCount = 0, rCount = 0, cfCount = 0, dizLibCount = 0, dizCmdCount = 0;
            try (ZipFile zf = new ZipFile(jmod.toFile())) {
                for (Enumeration<? extends ZipEntry> e = zf.entries(); e.hasMoreElements(); ) {
                    ZipEntry ze = e.nextElement();
                    String fn = ze.getName();
                    int pos = fn.indexOf('/');
                    String dir = fn.substring(0, pos);
                    String filename = fn.substring(fn.lastIndexOf('/') + 1);
                    // name shown in the column
                    String name = filename;

                    long len = ze.getSize();
                    total += len;
                    switch (dir) {
                        case NATIVE_LIBS:
                            nativeLibs.put(name, len);
                            if (filename.endsWith(".diz")) {
                                dizLibCount++;
                                dizLibBytes += len;
                            }
                            break;
                        case NATIVE_CMDS:
                            nativeCmds.put(name, len);
                            if (filename.endsWith(".diz")) {
                                dizCmdCount++;
                                dizCmdBytes += len;
                            }
                            break;
                        case CLASSES:
                            if (filename.endsWith(".class")) {
                                cCount++;
                                cBytes += len;
                            } else {
                                rCount++;
                                rBytes += len;
                            }
                            break;
                        case CONFIG:
                            configFiles.put(name, len);
                            cfCount++;
                            cfBytes += len;
                            break;
                        default:
                            break;
                    }
                }
                this.filesize = jmod.toFile().length();
                this.classCount = cCount;
                this.classBytes = cBytes;
                this.resourceCount = rCount;
                this.resourceBytes = rBytes;
                this.configCount = cfCount;
                this.configBytes = cfBytes;
                this.size = total;
                this.debugInfoLibCount = dizLibCount;
                this.debugInfoLibBytes = dizLibBytes;
                this.debugInfoCmdCount = dizCmdCount;
                this.debugInfoCmdBytes = dizCmdBytes;
            }
        }

        static final String NATIVE_LIBS = "native";
        static final String NATIVE_CMDS = "bin";
        static final String CLASSES     = "classes";
        static final String CONFIG      = "conf";

        static final String MODULE_ID = "module/id";
        static final String MODULE_MAIN_CLASS = "module/main-class";
    }

    static Configuration resolve(Set<String> roots) {
        return Configuration.empty()
            .resolve(ModuleFinder.ofSystem(),
                     ModuleFinder.of(),
                     roots);
    }

    static class HtmlDocument {
        final String title;
        final Map<String, ModuleSummary> modules;
        boolean requiresTransitiveNote = false;
        boolean aggregatorNote = false;
        boolean totalBytesNote = false;
        HtmlDocument(String title, Map<String, ModuleSummary> modules) {
            this.title = title;
            this.modules = modules;
        }

        void writeTo(PrintStream out, Set<ModuleDescriptor> selectedModules) {
            out.format("<html><head>%n");
            out.format("<title>%s</title>%n", title);
            // stylesheet
            Arrays.stream(HtmlDocument.STYLES).forEach(out::println);
            out.format("</head>%n");

            // body begins
            out.format("<body>%n");

            // title and date
            out.println(DOCTITLE.toString(title));
            out.println(VERSION.toString(String.format("%tc", new Date())));

            // total modules and sizes
            long totalBytes = selectedModules.stream()
                    .map(ModuleDescriptor::name)
                    .map(modules::get)
                    .mapToLong(ModuleSummary::uncompressedSize)
                    .sum();
            String[] sections = new String[] {
                    String.format("%s: %d", "Total modules", selectedModules.size()),
                    String.format("%s: %,d bytes (%s %s)", "Total size",
                                  totalBytes,
                                  System.getProperty("os.name"),
                                  System.getProperty("os.arch"))
            };
            out.println(SECTION.toString(sections));

            // write table and header
            out.println(String.format("<table class=\"%s\">", MODULES));
            out.println(header("Module", "Requires", "Exports",
                    "Services", "Commands/Native Libraries/Configs"));

            // write contents - one row per module
            selectedModules.stream()
                    .sorted(Comparator.comparing(ModuleDescriptor::name))
                    .map(m -> modules.get(m.name()))
                    .map(ModuleTableRow::new)
                    .forEach(table -> table.writeTo(out));

            out.format("</table>");  // end table
            out.format("</body>");
            out.println("</html>");
        }

        String header(String... columns) {
            StringBuilder sb = new StringBuilder();
            sb.append("<tr>");
            Arrays.stream(columns)
                    .forEach(cn -> sb.append("  <th>").append(cn).append("</th>").append("\n"));
            sb.append("</tr>");
            return sb.toString();
        }

        static enum Selector {
            MODULES("modules"),
            MODULE("module"),
            MODULE_DEF("code name def"),
            AGGREGATOR("code name def agg"),
            REQUIRES("code"),
            REQUIRES_PUBLIC("code reexp"),
            BR("br"),
            CODE("code"),
            NUMBER("number"),;
            final String name;
            Selector(String name) {
                this.name = name;
            }
            @Override
            public String toString() {
                return name;
            }
        }

        static enum Division {
            DOCTITLE("doctitle"),
            VERSION("versions"),
            SECTION("section");
            final String name;

            Division(String name) {
                this.name = name;
            }

            public String toString(String... lines) {
                String value = Arrays.stream(lines).collect(Collectors.joining("<br>\n"));
                return "<div class=\"" + name + "\">" + value + "</div>";
            }
        }

        class ModuleTableRow {
            private final ModuleSummary ms;
            private final Set<ModuleDescriptor> deps;
            private final int maxRows;
            private final boolean aggregator;
            ModuleTableRow(ModuleSummary ms) {
                this.ms = ms;
                Configuration cf = resolve(Set.of(ms.name()));
                this.deps = cf.modules().stream()
                        .map(ResolvedModule::reference)
                        .map(ModuleReference::descriptor)
                        .collect(Collectors.toSet());
                int count = (ms.numClasses() > 0 ? 1 : 0) +
                            (ms.numResources() > 0 ? 1 : 0) +
                            (ms.numConfigs() > 0 ? 1 : 0) +
                            (ms.numNativeLibraries() > 0 ? 1 : 0) +
                            (ms.numNativeLibrariesDebug() > 0 ? 1 : 0) +
                            (ms.numCommands() > 0 ? 1 : 0) +
                            (ms.numCommandsDebug() > 0 ? 1 : 0);
                this.aggregator = ms.numClasses() == 1 && count == 1; // only module-info.class

                // 5 fixed rows (name + 2 transitive count/size + 2 blank rows)
                this.maxRows = 5 + count + (aggregator && !aggregatorNote ? 2 : 0);
            }

            public void writeTo(PrintStream out) {
                out.println(String.format("<tr id=\"%s\" class=\"%s\">", ms.name(), MODULE));
                out.println(moduleColumn());
                out.println(requiresColumn());
                out.println(exportsColumn());
                out.println(servicesColumn());
                out.println(otherSectionColumn());
                out.println("</td>");
                out.println("</tr>");
            }

            public String moduleColumn() {
                // module name
                StringBuilder sb = new StringBuilder("  ");
                sb.append("<td>");
                sb.append(String.format("<table class=\"%s\">", MODULE)).append("\n");
                sb.append(moduleName(ms.name()));
                sb.append(blankRow());
                // metadata
                sb.append(toTableRow("class", "classes", ms.numClasses(), ms.classBytes()));
                sb.append(toTableRow("resource", "resources", ms.numResources(), ms.resourceBytes()));
                sb.append(toTableRow("config", "configs", ms.numConfigs(), ms.configBytes()));
                sb.append(toTableRow("native library", "native libraries",
                                     ms.numNativeLibraries(), ms.nativeLibrariesBytes()));
                sb.append(toTableRow("native library debug", "native libraries debug",
                                     ms.numNativeLibrariesDebug(), ms.nativeLibrariesDebugBytes()));
                sb.append(toTableRow("command", "commands", ms.numCommands(), ms.commandBytes()));
                sb.append(toTableRow("command debug", "commands debug",
                                     ms.numCommandsDebug(), ms.commandDebugBytes()));
                sb.append(blankRow());

                // transitive dependencies
                long reqBytes = deps.stream()
                                    .filter(d -> !d.name().equals(ms.name()))
                                    .mapToLong(d -> modules.get(d.name()).uncompressedSize())
                                    .sum();
                long reqJmodFileSize = deps.stream()
                                            .mapToLong(d -> modules.get(d.name()).jmodFileSize())
                                            .sum();
                // size
                if (totalBytesNote) {
                    sb.append(toTableRow("Total bytes", ms.uncompressedSize()));
                    sb.append(toTableRow("Total bytes of dependencies", reqBytes));
                } else {
                    // print footnote
                    sb.append(toTableRow("Total bytes<sup>1</sup>", ms.uncompressedSize()));
                    sb.append(toTableRow("Total bytes of dependencies<sup>2</sup>", reqBytes));
                }
                String files = deps.size() == 1 ? "file" : "files";
                sb.append(toTableRow(String.format("Total jmod bytes (%d %s)", deps.size(), files), reqJmodFileSize));

                if (aggregator && !aggregatorNote) {
                    aggregatorNote = true;
                    sb.append(blankRow());
                    sb.append(toTableRow("<i>* aggregator is a module with module-info.class only</i>", BR));
                }
                if (!totalBytesNote) {
                    totalBytesNote = true;
                    sb.append(blankRow());
                    sb.append(toTableRow("<i><sup>1</sup>sum of all files including debug files</i>", BR));
                    sb.append(toTableRow("<i><sup>2</sup>sum of direct and indirect dependencies</i>", BR));
                }
                sb.append("</table>").append("</td>");
                return sb.toString();
            }

            private String moduleName(String mn) {
                if (aggregator) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(String.format("<tr><td colspan=\"2\"><span class=\"%s\">", AGGREGATOR))
                      .append(mn)
                      .append("</span>").append("&nbsp;&nbsp;");
                    if (!aggregatorNote) {
                        sb.append("(aggregator<sup>*</sup>)");
                    } else {
                        sb.append("(aggregator)");
                    }
                    sb.append("</td></tr>");
                    return sb.toString();
                } else {
                    return toTableRow(mn, MODULE_DEF);
                }
            }

            public String requiresColumn() {
                StringBuilder sb = new StringBuilder();
                sb.append(String.format("<td>"));
                boolean footnote = requiresTransitiveNote;
                ms.descriptor().requires().stream()
                        .sorted(Comparator.comparing(Requires::name))
                        .forEach(r -> {
                            boolean requiresTransitive = r.modifiers().contains(Requires.Modifier.TRANSITIVE);
                            Selector sel = requiresTransitive ? REQUIRES_PUBLIC : REQUIRES;
                            String req = String.format("<a class=\"%s\" href=\"#%s\">%s</a>",
                                                       sel, r.name(), r.name());
                            if (!requiresTransitiveNote && requiresTransitive) {
                                requiresTransitiveNote = true;
                                req += "<sup>*</sup>";
                            }
                            sb.append(req).append("\n").append("<br>");
                        });

                if (!ms.name().equals("java.base")) {
                    int directDeps = ms.descriptor().requires().size();
                    int indirectDeps = deps.size()-directDeps-1;
                    for (int i=directDeps; i< (maxRows-1); i++) {
                        sb.append("<br>");
                    }
                    sb.append("<br>");
                    sb.append("<i>+").append(indirectDeps).append(" transitive dependencies</i>");
                }
                if (footnote != requiresTransitiveNote) {
                    sb.append("<br><br>").append("<i>* bold denotes requires transitive</i>");
                }
                sb.append("</td>");
                return sb.toString();
            }

            public String exportsColumn() {
                StringBuilder sb = new StringBuilder();
                sb.append(String.format("  <td class=\"%s\">", CODE));
                ms.descriptor().exports().stream()
                        .sorted(Comparator.comparing(Exports::source))
                        .filter(e -> !e.isQualified())
                        .forEach(e -> sb.append(e.source()).append("<br>").append("\n"));
                sb.append("</td>");
                return sb.toString();
            }

            private String providesEntry(Provides p) {
                StringBuilder sb = new StringBuilder();
                sb.append(String.format("provides %s<br>\n", p.service()));
                List<String> pvs = new ArrayList<>(p.providers());
                pvs.sort(Comparator.naturalOrder());
                for (int i = 0; i < pvs.size(); i++) {      // My kingdom for Stream::zip ...
                    String fmt = ((i == 0)
                                  ? "&nbsp;&nbsp;&nbsp;&nbsp;with %s"
                                  : ",<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; %s");
                    sb.append(String.format(fmt, pvs.get(i)));
                }
                sb.append("\n");
                return sb.toString();
            }

            public String servicesColumn() {
                StringBuilder sb = new StringBuilder();
                sb.append(String.format("  <td class=\"%s\">", CODE));
                ms.descriptor().uses().stream()
                        .sorted()
                        .forEach(s -> sb.append("uses ").append(s).append("<br>").append("\n"));
                ms.descriptor().provides().stream()
                        .sorted(Comparator.comparing(Provides::service))
                        .map(this::providesEntry)
                        .forEach(p -> sb.append(p).append("<br>").append("\n"));
                sb.append("</td>");
                return sb.toString();
            }

            public String otherSectionColumn() {
                StringBuilder sb = new StringBuilder();
                sb.append("<td>");
                sb.append(String.format("<table class=\"%s\">", MODULE)).append("\n");
                // commands
                if (ms.numCommands() > 0) {
                    sb.append(toTableRow("bin/", CODE));
                    ms.commands().entrySet().stream()
                            .sorted(Map.Entry.comparingByKey())
                            .forEach(e -> sb.append(toTableRow(e.getKey(), e.getValue(), CODE)));
                    sb.append(blankRow());
                }

                // native libraries
                if (ms.numNativeLibraries() > 0) {
                    sb.append(toTableRow("lib/", CODE));
                    ms.nativeLibs().entrySet().stream()
                            .sorted(Map.Entry.comparingByKey())
                            .forEach(e -> sb.append(toTableRow(e.getKey(), e.getValue(), CODE)));
                    sb.append(blankRow());
                }

                // config files
                if (ms.numConfigs() > 0) {
                    sb.append(toTableRow("conf/", CODE));
                    ms.configFiles().entrySet().stream()
                            .sorted(Map.Entry.comparingByKey())
                            .forEach(e -> sb.append(toTableRow(e.getKey(), e.getValue(), CODE)));
                }
                // totals
                sb.append("</table>").append("</td>");
                return sb.toString();
            }

            private String blankRow() {
                return toTableRow("&nbsp;", BR);
            }

            private String toTableRow(String col, Selector selector) {
                TableDataBuilder builder = new TableDataBuilder();
                builder.colspan(selector, 2, col);
                return builder.build();
            }

            private String toTableRow(String col1, long col2) {
                return toTableRow(col1, col2, BR);
            }

            private String toTableRow(String col1, long col2, Selector selector) {
                TableDataBuilder builder = new TableDataBuilder();
                builder.data(selector, col1);
                builder.data(col2);
                return builder.build();

            }

            private String toTableRow(String singular, String plural, int count, long bytes) {
                if (count == 0) {
                    return "";
                }
                TableDataBuilder builder = new TableDataBuilder();
                if (count == 1) {
                    builder.data(count + " " + singular);
                } else {
                    builder.data(count + " " + plural);
                }
                builder.data(bytes);
                return builder.build();
            }

            class TableDataBuilder {
                private final StringBuilder sb;
                TableDataBuilder() {
                    this.sb = new StringBuilder("<tr>");
                }
                TableDataBuilder data(String s) {
                    data(BR, s);
                    return this;
                }
                TableDataBuilder data(long num) {
                    data(NUMBER, String.format("%,d", num));
                    return this;
                }
                TableDataBuilder colspan(Selector selector, int columns, String data) {
                    sb.append("<td colspan=\"").append(columns).append("\">");
                    sb.append("<span class=\"").append(selector).append("\">");
                    sb.append(data).append("</span></td>");
                    return this;
                }

                TableDataBuilder data(Selector selector, String data) {
                    sb.append("<td class=\"").append(selector).append("\">");
                    sb.append(data).append("</td>");
                    return this;
                }
                String build() {
                    sb.append("</tr>");
                    return sb.toString();
                }
            }
        }

        private static final String[] STYLES = new String[]{
                "<link rel=\"stylesheet\" type=\"text/css\" href=\"/.fonts/dejavu.css\"/>",
                "<style type=\"text/css\">",
                "        HTML, BODY, DIV, SPAN, APPLET, OBJECT, IFRAME, H1, H2, H3, H4, H5, H6, P,",
                "        BLOCKQUOTE, PRE, A, ABBR, ACRONYM, ADDRESS, BIG, CITE, CODE, DEL, DFN, EM,",
                "        IMG, INS, KBD, Q, S, SAMP, SMALL, STRIKE, STRONG, SUB, SUP, TT, VAR, B, U,",
                "        I, CENTER, DL, DT, DD, OL, UL, LI, FIELDSET, FORM, LABEL, LEGEND, TABLE,",
                "        CAPTION, TBODY, TFOOT, THEAD, TR, TH, TD, ARTICLE, ASIDE, CANVAS, DETAILS,",
                "        EMBED, FIGURE, FIGCAPTION, FOOTER, HEADER, HGROUP, MENU, NAV, OUTPUT, RUBY,",
                "        SECTION, SUMMARY, TIME, MARK, AUDIO, VIDEO {",
                "          margin: 0; padding: 0; border: 0; font-size: 100%; font: inherit;",
                "          vertical-align: baseline; }",
                "        ARTICLE, ASIDE, DETAILS, FIGCAPTION, FIGURE, ",
                "        FOOTER, HEADER, HGROUP, MENU, NAV, SECTION { display: block; }",
                "        BLOCKQUOTE, Q { quotes: none; }",
                "        BLOCKQUOTE:before, BLOCKQUOTE:after, Q:before, Q:after {",
                "                content: ''; content: none; }",
                "        TABLE { border-collapse: collapse; border-spacing: 0; }",
                "        A { text-decoration: none; }",
                "        A:link { color: #437291; }",
                "        A:visited { color: #666666; }",
                "        A.anchor:link, A.anchor:visited { color: black; }",
                "        A[href]:hover { color: #e76f00; }",
                "        A IMG { border-width: 0px; }",
                "        HTML { font-size: 20px; } /* baseline grid */",
                "        HTML > BODY { font-size: 14px; }",
                "        BODY {",
                "          background: white;",
                "          margin: 40px;",
                "          margin-bottom: 150%;",
                "          line-height: 20px;",
                "          -webkit-text-size-adjust: 100%; /* iOS */",
                "          color: #222;",
                "        }",
                "        BODY { font-family: \"DejaVu Serif\", \"Lucida Bright\", \"Bookman Old Style\",",
                "                            Georgia, serif; }",
                "        CODE, TT, .jref, DIV.spec .open, TABLE.profiles {",
                "          font-family: \"DejaVu Sans\", \"Lucida Sans\", Helvetica, sans-serif; }",
                "        PRE, .code { font-family: \"DejaVu Sans Mono\", \"Bitstream Vera Sans Mono\",",
                "                            Monaco, \"Courier New\", monospace; }",
                "        H1, H2, H3, H4 { color: green; font-weight: bold; }",
                "        I { font-style: italic; }",
                "        TH { font-weight: bold; }",
                "        P { text-indent: 40px; }",
                "        P:first-child, UL + P, OL + P, BLOCKQUOTE + P, TABLE + P, P.subsection,",
                "          P.break, DIV.profiles-table + P { text-indent: 0; }",
                "        P.break { margin-top: 10px; }",
                "        P.subsection { margin-top: 20px; }",
                "        P.subsection SPAN.title { font-weight: bold; padding-right: 20px; }",
                "        UL, OL { margin: 10px 0; padding-left: 40px; }",
                "        LI { margin-bottom: 10px; }",
                "        UL.compact LI { margin-bottom: 0; }",
                "        PRE { padding: 0; margin: 10px 0 10px 20px; background: #eee; width: 45em; }",
                "        BLOCKQUOTE { margin: 10px 0; margin-left: 20px; }",
                "        LI BLOCKQUOTE { margin-left: 0; }",
                "        UL LI { list-style-type: square; }",
                "        .todo { color: darkred; text-align: right; }",
                "        .error { color: red; font-weight: bold; }",
                "        .warn { color: #ee0000; font-weight: bold; }",
                "        DIV.doctitle { margin-top: -13px;",
                "          font-size: 22px; line-height: 40px; font-weight: bold; }",
                "        DIV.twarn { color: #cc0000; font-weight: bold; margin-bottom: 9px; }",
                "        DIV.subtitle { margin-top: 2px; font-size: 18px; font-weight: bold; }",
                "        DIV.authors { margin-top: 10px; margin-bottom: 10px; font-size: 16px; }",
                "        DIV.author A { font-style: italic; }",
                "        DIV.version { margin-top: 10px; font-size: 12px; }",
                "        DIV.version, DIV.legal-notice { font-size: 12px; line-height: 15px; }",
                "        SPAN.hash { font-size: 9px; }",
                "        DIV.version SPAN.modified { color: green; font-weight: bold; }",
                "        DIV.head { margin-bottom: 20px; }",
                "        DIV.section > DIV.title, DIV.section DIV.number SPAN {",
                "          font-size: 15px; font-weight: bold; }",
                "        TABLE { border-collapse: collapse; border: none; }",
                "        TD.number { text-align: right; }",
                "        TD, TH { text-align: left; white-space: nowrap; }",
                "        TD.name, SPAN.name { font-weight: bold; }",
                "        ",
                "        TABLE.module { width: 100%; }",
                "        TABLE.module TD:first-child { padding-right: 10px; }",
                "        TR.module > TD { padding: 10px 0; border-top: 1px solid black; }",
                "        TR > TH { padding-bottom: 10px; }",
                "        TR.br TD { padding-top: 20px; }",
                "        TABLE.modules { margin-top: 20px; }",
                "        TABLE.modules > TBODY > TR > TD:nth-child(even) { background: #eee; }",
                "        TABLE.modules > TBODY > TR > TD, TABLE.modules > TBODY > TR > TH {",
                "          padding-left: 10px; padding-right: 10px; }",
                "        .reexp, .def { font-weight: bold; }",
                "        .agg { font-style: italic; }",
                "        SUP { height: 0; line-height: 1; position: relative;",
                "              vertical-align: baseline; bottom: 1ex; font-size: 11px; }",
                "</style>",
        };
    }
}
