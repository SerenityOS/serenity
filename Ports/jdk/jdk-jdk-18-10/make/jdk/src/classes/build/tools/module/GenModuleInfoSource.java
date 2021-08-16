/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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
package build.tools.module;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import static java.util.stream.Collectors.*;

/**
 * A build tool to extend the module-info.java in the source tree for
 * platform-specific exports, opens, uses, and provides and write to
 * the specified output file.
 *
 * GenModuleInfoSource will be invoked for each module that has
 * module-info.java.extra in the source directory.
 *
 * The extra exports, opens, uses, provides can be specified
 * in module-info.java.extra.
 * Injecting platform-specific requires is not supported.
 *
 * @see build.tools.module.ModuleInfoExtraTest for basic testing
 */
public class GenModuleInfoSource {
    private final static String USAGE =
            "Usage: GenModuleInfoSource \n" +
                    " [-d]\n" +
                    " -o <output file>\n" +
                    "  --source-file <module-info-java>\n" +
                    "  --modules <module-name>[,<module-name>...]\n" +
                    "  <module-info.java.extra> ...\n";

    static boolean debug = false;
    static boolean verbose = false;
    public static void main(String... args) throws Exception {
        Path outfile = null;
        Path moduleInfoJava = null;
        Set<String> modules = Collections.emptySet();
        List<Path> extras = new ArrayList<>();
        // validate input arguments
        for (int i = 0; i < args.length; i++){
            String option = args[i];
            String arg = i+1 < args.length ? args[i+1] : null;
            switch (option) {
                case "-d":
                    debug = true;
                    break;
                case "-o":
                    outfile = Paths.get(arg);
                    i++;
                    break;
                case "--source-file":
                    moduleInfoJava = Paths.get(arg);
                    if (Files.notExists(moduleInfoJava)) {
                        throw new IllegalArgumentException(moduleInfoJava + " not exist");
                    }
                    i++;
                    break;
                case "--modules":
                    modules = Arrays.stream(arg.split(","))
                                    .collect(toSet());
                    i++;
                    break;
                case "-v":
                    verbose = true;
                    break;
                default:
                    Path file = Paths.get(option);
                    if (Files.notExists(file)) {
                        throw new IllegalArgumentException(file + " not exist");
                    }
                    extras.add(file);
            }
        }

        if (moduleInfoJava == null || outfile == null ||
                modules.isEmpty() || extras.isEmpty()) {
            System.err.println(USAGE);
            System.exit(-1);
        }

        GenModuleInfoSource genModuleInfo =
            new GenModuleInfoSource(moduleInfoJava, extras, modules);

        // generate new module-info.java
        genModuleInfo.generate(outfile);
    }

    final Path sourceFile;
    final List<Path> extraFiles;
    final ModuleInfo extras;
    final Set<String> modules;
    final ModuleInfo moduleInfo;
    GenModuleInfoSource(Path sourceFile, List<Path> extraFiles, Set<String> modules)
        throws IOException
    {
        this.sourceFile = sourceFile;
        this.extraFiles = extraFiles;
        this.modules = modules;
        this.moduleInfo = new ModuleInfo();
        this.moduleInfo.parse(sourceFile);

        // parse module-info.java.extra
        this.extras = new ModuleInfo();
        for (Path file : extraFiles) {
            extras.parseExtra(file);
        }

        // merge with module-info.java.extra
        moduleInfo.augmentModuleInfo(extras, modules);
    }

    void generate(Path output) throws IOException {
        List<String> lines = Files.readAllLines(sourceFile);
        try (BufferedWriter bw = Files.newBufferedWriter(output);
             PrintWriter writer = new PrintWriter(bw)) {
            // write the copyright header and lines up to module declaration
            for (String l : lines) {
                writer.println(l);
                if (l.trim().startsWith("module ")) {
                    if (debug) {
                        // print URI rather than file path to avoid escape
                        writer.format("    // source file: %s%n", sourceFile.toUri());
                        for (Path file : extraFiles) {
                            writer.format("    //              %s%n", file.toUri());
                        }
                    }
                    break;
                }
            }

            // requires
            for (String l : lines) {
                if (l.trim().startsWith("requires"))
                    writer.println(l);
            }

            // write exports, opens, uses, and provides
            moduleInfo.print(writer);

            // close
            writer.println("}");
        }
    }


    class ModuleInfo {
        final Map<String, Statement> exports = new HashMap<>();
        final Map<String, Statement> opens = new HashMap<>();
        final Map<String, Statement> uses = new HashMap<>();
        final Map<String, Statement> provides = new HashMap<>();

        Statement getStatement(String directive, String name) {
            Objects.requireNonNull(name);
            switch (directive) {
                case "exports":
                    if (moduleInfo.exports.containsKey(name) &&
                        moduleInfo.exports.get(name).isUnqualified()) {
                        throw new IllegalArgumentException(sourceFile +
                            " already has " + directive + " " + name);
                    }
                    return exports.computeIfAbsent(name,
                        _n -> new Statement("exports", "to", name));

                case "opens":
                    if (moduleInfo.opens.containsKey(name) &&
                        moduleInfo.opens.get(name).isUnqualified()) {
                        throw new IllegalArgumentException(sourceFile +
                            " already has " + directive + " " + name);
                    }

                    if (moduleInfo.opens.containsKey(name)) {
                        throw new IllegalArgumentException(sourceFile +
                            " already has " + directive + " " + name);
                    }
                    return opens.computeIfAbsent(name,
                        _n -> new Statement("opens", "to", name));

                case "uses":
                    return uses.computeIfAbsent(name,
                        _n -> new Statement("uses", "", name));

                case "provides":
                    return provides.computeIfAbsent(name,
                        _n -> new Statement("provides", "with", name, true));

                default:
                    throw new IllegalArgumentException(directive);
            }

        }

        /*
         * Augment this ModuleInfo with module-info.java.extra
         */
        void augmentModuleInfo(ModuleInfo extraFiles, Set<String> modules) {
            // API package exported in the original module-info.java
            extraFiles.exports.entrySet()
                .stream()
                .filter(e -> exports.containsKey(e.getKey()) &&
                    e.getValue().filter(modules))
                .forEach(e -> mergeExportsOrOpens(exports.get(e.getKey()),
                    e.getValue(),
                    modules));

            // add exports that are not defined in the original module-info.java
            extraFiles.exports.entrySet()
                .stream()
                .filter(e -> !exports.containsKey(e.getKey()) &&
                    e.getValue().filter(modules))
                .forEach(e -> addTargets(getStatement("exports", e.getKey()),
                    e.getValue(),
                    modules));

            // API package opened in the original module-info.java
            extraFiles.opens.entrySet()
                .stream()
                .filter(e -> opens.containsKey(e.getKey()) &&
                    e.getValue().filter(modules))
                .forEach(e -> mergeExportsOrOpens(opens.get(e.getKey()),
                    e.getValue(),
                    modules));

            // add opens that are not defined in the original module-info.java
            extraFiles.opens.entrySet()
                .stream()
                .filter(e -> !opens.containsKey(e.getKey()) &&
                    e.getValue().filter(modules))
                .forEach(e -> addTargets(getStatement("opens", e.getKey()),
                    e.getValue(),
                    modules));

            // provides
            extraFiles.provides.keySet()
                .stream()
                .filter(service -> provides.containsKey(service))
                .forEach(service -> mergeProvides(service,
                    extraFiles.provides.get(service)));
            extraFiles.provides.keySet()
                .stream()
                .filter(service -> !provides.containsKey(service))
                .forEach(service -> provides.put(service,
                    extraFiles.provides.get(service)));

            // uses
            extraFiles.uses.keySet()
                .stream()
                .filter(service -> !uses.containsKey(service))
                .forEach(service -> uses.put(service, extraFiles.uses.get(service)));
        }

        // add qualified exports or opens to known modules only
        private void addTargets(Statement statement,
                                Statement extra,
                                Set<String> modules)
        {
            extra.targets.stream()
                .filter(mn -> modules.contains(mn))
                .forEach(mn -> statement.addTarget(mn));
        }

        private void mergeExportsOrOpens(Statement statement,
                                         Statement extra,
                                         Set<String> modules)
        {
            String pn = statement.name;
            if (statement.isUnqualified() && extra.isQualified()) {
                throw new RuntimeException("can't add qualified exports to " +
                    "unqualified exports " + pn);
            }

            Set<String> mods = extra.targets.stream()
                .filter(mn -> statement.targets.contains(mn))
                .collect(toSet());
            if (mods.size() > 0) {
                throw new RuntimeException("qualified exports " + pn + " to " +
                    mods.toString() + " already declared in " + sourceFile);
            }

            // add qualified exports or opens to known modules only
            addTargets(statement, extra, modules);
        }

        private void mergeProvides(String service, Statement extra) {
            Statement statement = provides.get(service);

            Set<String> mods = extra.targets.stream()
                .filter(mn -> statement.targets.contains(mn))
                .collect(toSet());

            if (mods.size() > 0) {
                throw new RuntimeException("qualified exports " + service + " to " +
                    mods.toString() + " already declared in " + sourceFile);
            }

            extra.targets.stream()
                .forEach(mn -> statement.addTarget(mn));
        }


        void print(PrintWriter writer) {
            // print unqualified exports
            exports.entrySet().stream()
                .filter(e -> e.getValue().targets.isEmpty())
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> writer.println(e.getValue()));

            // print qualified exports
            exports.entrySet().stream()
                .filter(e -> !e.getValue().targets.isEmpty())
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> writer.println(e.getValue()));

            // print unqualified opens
            opens.entrySet().stream()
                .filter(e -> e.getValue().targets.isEmpty())
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> writer.println(e.getValue()));

            // print qualified opens
            opens.entrySet().stream()
                .filter(e -> !e.getValue().targets.isEmpty())
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> writer.println(e.getValue()));

            // uses and provides
            writer.println();
            uses.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> writer.println(e.getValue()));
            provides.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> writer.println(e.getValue()));
        }


        private void parse(Path file) throws IOException {
            Parser parser = new Parser(file);
            parser.run();
            if (verbose) {
                parser.dump();
            }
            process(parser, false);
        }

        private void parseExtra(Path file) throws IOException {
            Parser parser = new Parser(file);
            parser.run();
            if (verbose) {
                parser.dump();
            }
            process(parser, true);
        }


        private void process(Parser parser, boolean extraFile) throws IOException {
            // no duplicate statement local in each file
            Map<String, Statement> exports = new HashMap<>();
            Map<String, Statement> opens = new HashMap<>();
            Map<String, Statement> uses = new HashMap<>();
            Map<String, Statement> provides = new HashMap<>();

            String token = null;
            boolean hasCurlyBracket = false;
            while ((token = parser.nextToken()) != null) {
                if (token.equals("module")) {
                    String modulename = nextIdentifier(parser);
                    if (extraFile) {
                        throw parser.newError("cannot declare module in " + parser.sourceFile);
                    }
                    skipTokenOrThrow(parser, "{", "missing {");
                    hasCurlyBracket = true;
                } else if (token.equals("requires")) {
                    token = nextIdentifier(parser);
                    if (token.equals("transitive")) {
                        token = nextIdentifier(parser);
                    }
                    if (extraFile) {
                        throw parser.newError("cannot declare requires in " + parser.sourceFile);
                    }
                    skipTokenOrThrow(parser, ";", "missing semicolon");
                } else if (isExportsOpensProvidesUses(token)) {
                    // new statement
                    String keyword = token;
                    String name = nextIdentifier(parser);
                    Statement statement = getStatement(keyword, name);
                    switch (keyword) {
                        case "exports":
                            if (exports.containsKey(name)) {
                                throw parser.newError("multiple " + keyword + " " + name);
                            }
                            exports.put(name, statement);
                            break;
                        case "opens":
                            if (opens.containsKey(name)) {
                                throw parser.newError("multiple " + keyword + " " + name);
                            }
                            opens.put(name, statement);
                            break;
                        case "uses":
                            if (uses.containsKey(name)) {
                                throw parser.newError("multiple " + keyword + " " + name);
                            }
                            uses.put(name, statement);
                            break;
                        case "provides":
                            if (provides.containsKey(name)) {
                                throw parser.newError("multiple " + keyword + " " + name);
                            }
                            provides.put(name, statement);
                            break;
                    }
                    String lookAhead = lookAhead(parser);
                    if (lookAhead.equals(statement.qualifier)) {
                        parser.nextToken(); // skip qualifier
                        while ((lookAhead = parser.peekToken()) != null) {
                            // add target name
                            name = nextIdentifier(parser);
                            statement.addTarget(name);
                            lookAhead = lookAhead(parser);
                            if (lookAhead.equals(",") || lookAhead.equals(";")) {
                                parser.nextToken();
                            } else {
                                throw parser.newError("missing semicolon");
                            }
                            if (lookAhead.equals(";")) {
                                break;
                            }
                        }
                    } else {
                        skipTokenOrThrow(parser, ";", "missing semicolon");
                    }
                } else if (token.equals(";")) {
                    continue;
                } else if (hasCurlyBracket && token.equals("}")) {
                    hasCurlyBracket = false;
                    if (parser.peekToken() != null) {  // must be EOF
                        throw parser.newError("is malformed");
                    }
                } else {
                    throw parser.newError("missing keyword");
                }
            }
            if (hasCurlyBracket) {
                parser.newError("missing }");
            }
        }

        private boolean isExportsOpensProvidesUses(String word) {
            switch (word) {
                case "exports":
                case "opens":
                case "provides":
                case "uses":
                    return true;
                default:
                    return false;
            }
        }

        private String lookAhead(Parser parser) {
            String lookAhead = parser.peekToken();
            if (lookAhead == null) { // EOF
                throw parser.newError("reach end of file");
            }
            return lookAhead;
        }

        private String nextIdentifier(Parser parser) {
            String lookAhead = parser.peekToken();
            boolean maybeIdentifier = true;
            switch (lookAhead) {
                case "module":
                case "requires":
                case "exports":
                case "opens":
                case "provides":
                case "uses":
                case "to":
                case "with":
                case ",":
                case ";":
                case "{":
                case "}":
                    maybeIdentifier = false;
            }
            if (lookAhead == null || !maybeIdentifier) {
                throw parser.newError("<identifier> missing");
            }

            return parser.nextToken();
        }

        private String skipTokenOrThrow(Parser parser, String token, String msg) {
            // look ahead to report the proper line number
            String lookAhead = parser.peekToken();
            if (!token.equals(lookAhead)) {
                throw parser.newError(msg);
            }
            return parser.nextToken();
        }
    }

    static class Statement {
        final String directive;
        final String qualifier;
        final String name;
        final Set<String> targets = new LinkedHashSet<>();
        final boolean ordered;

        Statement(String directive, String qualifier, String name) {
            this(directive, qualifier, name, false);
        }

        Statement(String directive, String qualifier, String name, boolean ordered) {
            this.directive = directive;
            this.qualifier = qualifier;
            this.name = name;
            this.ordered = ordered;
        }

        Statement addTarget(String mn) {
            if (mn.isEmpty())
                throw new IllegalArgumentException("empty module name");
            targets.add(mn);
            return this;
        }

        boolean isQualified() {
            return targets.size() > 0;
        }

        boolean isUnqualified() {
            return targets.isEmpty();
        }

        /**
         * Returns true if this statement is unqualified or it has
         * at least one target in the given names.
         */
        boolean filter(Set<String> names) {
            if (isUnqualified()) {
                return true;
            } else {
                return targets.stream()
                    .filter(mn -> names.contains(mn))
                    .findAny().isPresent();
            }
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("    ");
            sb.append(directive).append(" ").append(name);
            if (targets.isEmpty()) {
                sb.append(";");
            } else if (targets.size() == 1) {
                sb.append(" ").append(qualifier)
                  .append(orderedTargets().collect(joining(",", " ", ";")));
            } else {
                sb.append(" ").append(qualifier)
                  .append(orderedTargets()
                      .map(target -> String.format("        %s", target))
                      .collect(joining(",\n", "\n", ";")));
            }
            return sb.toString();
        }

        public Stream<String> orderedTargets() {
            return ordered ? targets.stream()
                           : targets.stream().sorted();
        }
    }

    static void trace(String fmt, Object... params) {
        if (verbose) {
            System.out.format(fmt, params);
        }
    }

    static class Parser {
        private static final List<String> EMPTY = List.of();

        private final Path sourceFile;
        private boolean inCommentBlock = false;
        private List<List<String>> tokens = new ArrayList<>();
        private int lineNumber = 1;
        private int index = 0;

        Parser(Path file) {
            this.sourceFile = file;
        }

        void run() throws IOException {
            List<String> lines = Files.readAllLines(sourceFile);
            for (int lineNumber = 1; lineNumber <= lines.size(); lineNumber++) {
                String l = lines.get(lineNumber - 1).trim();
                tokenize(l);
            }
        }

        /*
         * Tokenize the given string.  Comments are skipped.
         */
        List<String> tokenize(String l) {
            while (!l.isEmpty()) {
                if (inCommentBlock) {
                    int comment = l.indexOf("*/");
                    if (comment == -1)
                        return emptyTokens();

                    // end comment block
                    inCommentBlock = false;
                    if ((comment + 2) >= l.length()) {
                        return emptyTokens();
                    }
                    l = l.substring(comment + 2, l.length()).trim();
                }

                // skip comment
                int comment = l.indexOf("//");
                if (comment >= 0) {
                    l = l.substring(0, comment).trim();
                    if (l.isEmpty()) return emptyTokens();
                }

                if (l.isEmpty()) {
                    return emptyTokens();
                }

                int beginComment = l.indexOf("/*");
                int endComment = l.indexOf("*/");
                if (beginComment == -1)
                    return tokens(l);

                String s1 = l.substring(0, beginComment).trim();
                if (endComment > 0) {
                    String s2 = l.substring(endComment + 2, l.length()).trim();
                    if (s1.isEmpty()) {
                        l = s2;
                    } else if (s2.isEmpty()) {
                        l = s1;
                    } else {
                        l = s1 + " " + s2;
                    }
                } else {
                    inCommentBlock = true;
                    return tokens(s1);
                }
            }
            return tokens(l);
        }

        private List<String> emptyTokens() {
            this.tokens.add(EMPTY);
            return EMPTY;
        }
        private List<String> tokens(String l) {
            List<String> tokens = new ArrayList<>();
            for (String s : l.split("\\s+")) {
                int pos=0;
                s = s.trim();
                if (s.isEmpty())
                     continue;

                int i = s.indexOf(',', pos);
                int j = s.indexOf(';', pos);
                while ((i >= 0 && i < s.length()) || (j >= 0 && j < s.length())) {
                    if (j == -1 || (i >= 0 && i < j)) {
                        String n = s.substring(pos, i).trim();
                        if (!n.isEmpty()) {
                            tokens.add(n);
                        }
                        tokens.add(s.substring(i, i + 1));
                        pos = i + 1;
                        i = s.indexOf(',', pos);
                    } else {
                        String n = s.substring(pos, j).trim();
                        if (!n.isEmpty()) {
                            tokens.add(n);
                        }
                        tokens.add(s.substring(j, j + 1));
                        pos = j + 1;
                        j = s.indexOf(';', pos);
                    }
                }

                String n = s.substring(pos).trim();
                if (!n.isEmpty()) {
                    tokens.add(n);
                }
            }
            this.tokens.add(tokens);
            return tokens;
        }

        /*
         * Returns next token.
         */
        String nextToken() {
            while (lineNumber <= tokens.size()) {
                List<String> l = tokens.get(lineNumber-1);
                if (index < l.size()) {
                    return l.get(index++);
                } else {
                    lineNumber++;
                    index = 0;
                }
            }
            return null;
        }

        /*
         * Peeks next token.
         */
        String peekToken() {
            int ln = lineNumber;
            int i = index;
            while (ln <= tokens.size()) {
                List<String> l = tokens.get(ln-1);
                if (i < l.size()) {
                    return l.get(i++);
                } else {
                    ln++;
                    i = 0;
                }
            }
            return null;
        }

        Error newError(String msg) {
            if (lineNumber <= tokens.size()) {
                throw new Error(sourceFile + ", line " +
                    lineNumber + ", " + msg + " \"" + lineAt(lineNumber) + "\"");
            } else {
                throw new Error(sourceFile + ", line " + lineNumber + ", " + msg);
            }
        }

        void dump() {
            for (int i = 1; i <= tokens.size(); i++) {
                System.out.format("%d: %s%n", i, lineAt(i));
            }
        }

        private String lineAt(int i) {
            return tokens.get(i-1).stream().collect(Collectors.joining(" "));
        }
    }
}

