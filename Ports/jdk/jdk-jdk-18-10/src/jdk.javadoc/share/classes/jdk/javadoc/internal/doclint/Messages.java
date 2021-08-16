/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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

package jdk.javadoc.internal.doclint;

import java.io.PrintWriter;
import java.text.MessageFormat;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.tools.Diagnostic;

import com.sun.source.doctree.DocTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.util.StringUtils;
import jdk.javadoc.internal.doclint.Env.AccessKind;

/**
 * Message reporting for DocLint.
 *
 * Options are used to filter out messages based on group and access level.
 * Support can be enabled for accumulating statistics of different kinds of
 * messages.
 *
 * <p><b>This is NOT part of any supported API.
 * If you write code that depends on this, you do so at your own
 * risk.  This code and its internal interfaces are subject to change
 * or deletion without notice.</b></p>
 */
public class Messages {
    /**
     * Groups used to categorize messages, so that messages in each group
     * can be enabled or disabled via options.
     */
    public enum Group {
        ACCESSIBILITY,
        HTML,
        MISSING,
        SYNTAX,
        REFERENCE;

        String optName() { return StringUtils.toLowerCase(name()); }
        String notOptName() { return "-" + optName(); }

        static boolean accepts(String opt) {
            for (Group g: values())
                if (opt.equals(g.optName())) return true;
            return false;
        }
    }

    private final Options options;
    private final Stats stats;

    ResourceBundle bundle;
    Env env;

    Messages(Env env) {
        this.env = env;
        String name = getClass().getPackage().getName() + ".resources.doclint";
        bundle = ResourceBundle.getBundle(name, Locale.ENGLISH);

        stats = new Stats(bundle);
        options = new Options(stats);
    }

    void error(Group group, DocTree tree, String code, Object... args) {
        report(group, Diagnostic.Kind.ERROR, tree, code, args);
    }

    void warning(Group group, DocTree tree, String code, Object... args) {
        report(group, Diagnostic.Kind.WARNING, tree, code, args);
    }

    void setOptions(String opts) {
        options.setOptions(opts);
    }

    void setStatsEnabled(boolean b) {
        stats.setEnabled(b);
    }

    void reportStats(PrintWriter out) {
        stats.report(out);
    }

    protected void report(Group group, Diagnostic.Kind dkind, DocTree tree, String code, Object... args) {
        if (options.isEnabled(group, env.currAccess)) {
            String msg = (code == null) ? (String) args[0] : localize(code, args);
            env.trees.printMessage(dkind, msg, tree,
                    env.currDocComment, env.currPath.getCompilationUnit());

            stats.record(group, dkind, code);
        }
    }

    protected void report(Group group, Diagnostic.Kind dkind, Tree tree, String code, Object... args) {
        if (options.isEnabled(group, env.currAccess)) {
            String msg = localize(code, args);
            env.trees.printMessage(dkind, msg, tree, env.currPath.getCompilationUnit());

            stats.record(group, dkind, code);
        }
    }

    String localize(String code, Object... args) {
        String msg = bundle.getString(code);
        if (msg == null) {
            StringBuilder sb = new StringBuilder();
            sb.append("message file broken: code=").append(code);
            if (args.length > 0) {
                sb.append(" arguments={0}");
                for (int i = 1; i < args.length; i++) {
                    sb.append(", {").append(i).append("}");
                }
            }
            msg = sb.toString();
        }
        return MessageFormat.format(msg, args);
    }

    // <editor-fold defaultstate="collapsed" desc="Options">

    /**
     * Handler for (sub)options specific to message handling.
     */
    static class Options {
        Map<String, Env.AccessKind> map = new HashMap<>();
        private final Stats stats;

        static boolean isValidOptions(String opts) {
            for (String opt: opts.split(",")) {
                if (!isValidOption(StringUtils.toLowerCase(opt.trim())))
                    return false;
            }
            return true;
        }

        private static boolean isValidOption(String opt) {
            if (opt.equals("none") || opt.equals(Stats.OPT))
                return true;

            int begin = opt.startsWith("-") ? 1 : 0;
            int sep = opt.indexOf("/");
            String grp = opt.substring(begin, (sep != -1) ? sep : opt.length());
            return ((begin == 0 && grp.equals("all")) || Group.accepts(grp))
                    && ((sep == -1) || AccessKind.accepts(opt.substring(sep + 1)));
        }

        Options(Stats stats) {
            this.stats = stats;
        }

        /** Determine if a message group is enabled for a particular access level. */
        boolean isEnabled(Group g, Env.AccessKind access) {
            if (map.isEmpty())
                map.put("all", Env.AccessKind.PROTECTED);

            Env.AccessKind ak = map.get(g.optName());
            if (ak != null && access.compareTo(ak) >= 0)
                return true;

            ak = map.get(ALL);
            if (ak != null && access.compareTo(ak) >= 0) {
                ak = map.get(g.notOptName());
                if (ak == null || access.compareTo(ak) > 0) // note >, not >=
                    return true;
            }

            return false;
        }

        void setOptions(String opts) {
            if (opts == null)
                setOption(ALL, Env.AccessKind.PRIVATE);
            else {
                for (String opt: opts.split(","))
                    setOption(StringUtils.toLowerCase(opt.trim()));
            }
        }

        private void setOption(String arg) throws IllegalArgumentException {
            if (arg.equals(Stats.OPT)) {
                stats.setEnabled(true);
                return;
            }

            int sep = arg.indexOf("/");
            if (sep > 0) {
                Env.AccessKind ak = Env.AccessKind.valueOf(StringUtils.toUpperCase(arg.substring(sep + 1)));
                setOption(arg.substring(0, sep), ak);
            } else {
                setOption(arg, null);
            }
        }

        private void setOption(String opt, Env.AccessKind ak) {
            map.put(opt, (ak != null) ? ak
                    : opt.startsWith("-") ? Env.AccessKind.PUBLIC : Env.AccessKind.PRIVATE);
        }

        private static final String ALL = "all";
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Statistics">

    /**
     * Optionally record statistics of different kinds of message.
     */
    static class Stats {
        public static final String OPT = "stats";
        public static final String NO_CODE = "";
        final ResourceBundle bundle;

        // tables only initialized if enabled
        int[] groupCounts;
        int[] dkindCounts;
        Map<String, Integer> codeCounts;

        Stats(ResourceBundle bundle) {
            this.bundle = bundle;
        }

        void setEnabled(boolean b) {
            if (b) {
                groupCounts = new int[Messages.Group.values().length];
                dkindCounts = new int[Diagnostic.Kind.values().length];
                codeCounts = new HashMap<>();
            } else {
                groupCounts = null;
                dkindCounts = null;
                codeCounts = null;
            }
        }

        void record(Messages.Group g, Diagnostic.Kind dkind, String code) {
            if (codeCounts == null) {
                return;
            }
            groupCounts[g.ordinal()]++;
            dkindCounts[dkind.ordinal()]++;
            if (code == null) {
                code = NO_CODE;
            }
            Integer i = codeCounts.get(code);
            codeCounts.put(code, (i == null) ? 1 : i + 1);
        }

        void report(PrintWriter out) {
            if (codeCounts == null) {
                return;
            }
            out.println("By group...");
            Table groupTable = new Table();
            for (Messages.Group g : Messages.Group.values()) {
                groupTable.put(g.optName(), groupCounts[g.ordinal()]);
            }
            groupTable.print(out);
            out.println();
            out.println("By diagnostic kind...");
            Table dkindTable = new Table();
            for (Diagnostic.Kind k : Diagnostic.Kind.values()) {
                dkindTable.put(StringUtils.toLowerCase(k.toString()), dkindCounts[k.ordinal()]);
            }
            dkindTable.print(out);
            out.println();
            out.println("By message kind...");
            Table codeTable = new Table();
            for (Map.Entry<String, Integer> e : codeCounts.entrySet()) {
                String code = e.getKey();
                String msg;
                try {
                    msg = code.equals(NO_CODE) ? "OTHER" : bundle.getString(code);
                } catch (MissingResourceException ex) {
                    msg = code;
                }
                codeTable.put(msg, e.getValue());
            }
            codeTable.print(out);
        }

        /**
         * A table of (int, String) sorted by decreasing int.
         */
        private static class Table {

            private static final Comparator<Integer> DECREASING = (o1, o2) -> o2.compareTo(o1);
            private final TreeMap<Integer, Set<String>> map = new TreeMap<>(DECREASING);

            void put(String label, int n) {
                if (n == 0) {
                    return;
                }
                Set<String> labels = map.get(n);
                if (labels == null) {
                    map.put(n, labels = new TreeSet<>());
                }
                labels.add(label);
            }

            void print(PrintWriter out) {
                for (Map.Entry<Integer, Set<String>> e : map.entrySet()) {
                    int count = e.getKey();
                    Set<String> labels = e.getValue();
                    for (String label : labels) {
                        out.println(String.format("%6d: %s", count, label));
                    }
                }
            }
        }
    }
    // </editor-fold>
}
