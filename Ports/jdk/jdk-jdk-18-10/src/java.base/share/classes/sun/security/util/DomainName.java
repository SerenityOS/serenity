/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static java.nio.charset.StandardCharsets.UTF_8;

import sun.security.ssl.SSLLogger;

/**
 * Allows public suffixes and registered domains to be determined from a
 * string that represents a target domain name. A database of known
 * registered suffixes is used to perform the determination.
 *
 * A public suffix is defined as the rightmost part of a domain name
 * that is not owned by an individual registrant. Examples of
 * public suffixes are:
 *      com
 *      edu
 *      co.uk
 *      k12.ak.us
 *      com.tw
 *      \u7db2\u8def.tw
 *
 * Public suffixes effectively denote registration authorities.
 *
 * A registered domain is a public suffix preceded by one domain label
 * and a ".". Examples are:
 *      oracle.com
 *      mit.edu
 *
 * The internal database is derived from the information maintained at
 * http://publicsuffix.org. The information is fixed for a particular
 * JDK installation, but may be updated in future releases or updates.
 *
 * Because of the large number of top-level domains (TLDs) and public
 * suffix rules, we only load the rules on demand -- from a Zip file
 * containing an entry for each TLD.
 *
 * As each entry is loaded, its data is stored permanently in a cache.
 *
 * The containment hierarchy for the data is shown below:
 *
 * Rules --> contains all the rules for a particular TLD
 *    RuleSet --> contains all the rules that match 1 label
 *    RuleSet --> contains all the rules that match 2 labels
 *    RuleSet --> contains all the rules that match 3 labels
 *      :
 *    RuleSet --> contains all the rules that match N labels
 *      HashSet of rules, where each rule is an exception rule, a "normal"
 *      rule, a wildcard rule (rules that contain a wildcard prefix only),
 *      or a LinkedList of "other" rules
 *
 * The general matching algorithm tries to find a longest match. So, the
 * search begins at the RuleSet with the most labels, and works backwards.
 *
 * Exceptions take priority over all other rules, and if a Rule contains
 * any exceptions, then even if we find a "normal" match, we search all
 * other RuleSets for exceptions. It is assumed that all other rules don't
 * intersect/overlap. If this happens, a match will be returned, but not
 * necessarily the expected one. For a further explanation of the rules,
 * see http://publicsuffix.org/list/.
 *
 * The "other" rules are for the (possible future) case where wildcards
 * are located in a rule any place other than the beginning.
 */

class DomainName {
    /**
     * For efficiency, the set of rules for each TLD is kept
     * in text files and only loaded if needed.
     */
    private static final Map<String, Rules> cache = new ConcurrentHashMap<>();

    private DomainName() {}

    /**
     * Returns the registered domain of the specified domain.
     *
     * @param domain the domain name
     * @return the registered domain, or null if not known or not registerable
     * @throws NullPointerException if domain is null
     */
    public static RegisteredDomain registeredDomain(String domain) {
        Match match = getMatch(domain);
        return (match != null) ? match.registeredDomain() : null;
    }

    private static Match getMatch(String domain) {
        if (domain == null) {
            throw new NullPointerException();
        }
        Rules rules = Rules.getRules(domain);
        return rules == null ? null : rules.match(domain);
    }

    /**
     * A Rules object contains a list of rules for a particular TLD.
     *
     * Rules are stored in a linked list of RuleSet objects. The list is
     * indexed according to the number of labels in the name (minus one)
     * such that all rules with the same number of labels are stored
     * in the same RuleSet.
     *
     * Doing this means we can find the longest match first, and also we
     * can stop comparing as soon as we find a match.
     */
    private static class Rules {

        private final LinkedList<RuleSet> ruleSets = new LinkedList<>();
        private final boolean hasExceptions;

        private Rules(InputStream is) throws IOException {
            InputStreamReader isr = new InputStreamReader(is, UTF_8);
            BufferedReader reader = new BufferedReader(isr);
            boolean hasExceptions = false;

            String line;
            int type = reader.read();
            while (type != -1 && (line = reader.readLine()) != null) {
                int numLabels = RuleSet.numLabels(line);
                if (numLabels != 0) {
                    RuleSet ruleset = getRuleSet(numLabels - 1);
                    ruleset.addRule(type, line);
                    hasExceptions |= ruleset.hasExceptions;
                }
                type = reader.read();
            }
            this.hasExceptions = hasExceptions;
        }

        static Rules getRules(String domain) {
            String tld = getTopLevelDomain(domain);
            if (tld.isEmpty()) {
                return null;
            }
            return cache.computeIfAbsent(tld, k -> createRules(tld));
        }

        private static String getTopLevelDomain(String domain) {
            int n = domain.lastIndexOf('.');
            if (n == -1) {
                return domain;
            }
            return domain.substring(n + 1);
        }

        private static Rules createRules(String tld) {
            try (InputStream pubSuffixStream = getPubSuffixStream()) {
                if (pubSuffixStream == null) {
                    return null;
                }
                return getRules(tld, new ZipInputStream(pubSuffixStream));
            } catch (IOException e) {
                if (SSLLogger.isOn && SSLLogger.isOn("ssl")) {
                    SSLLogger.fine(
                        "cannot parse public suffix data for " + tld +
                         ": " + e.getMessage());
                }
                return null;
            }
        }

        private static InputStream getPubSuffixStream() {
            @SuppressWarnings("removal")
            InputStream is = AccessController.doPrivileged(
                new PrivilegedAction<>() {
                    @Override
                    public InputStream run() {
                        File f = new File(System.getProperty("java.home"),
                            "lib/security/public_suffix_list.dat");
                        try {
                            return new FileInputStream(f);
                        } catch (FileNotFoundException e) {
                            return null;
                        }
                    }
                }
            );
            if (is == null) {
                if (SSLLogger.isOn && SSLLogger.isOn("ssl") &&
                        SSLLogger.isOn("trustmanager")) {
                    SSLLogger.fine(
                        "lib/security/public_suffix_list.dat not found");
                }
            }
            return is;
        }

        private static Rules getRules(String tld,
                                      ZipInputStream zis) throws IOException {
            boolean found = false;
            ZipEntry ze = zis.getNextEntry();
            while (ze != null && !found) {
                if (ze.getName().equals(tld)) {
                    found = true;
                } else {
                    ze = zis.getNextEntry();
                }
            }
            if (!found) {
                if (SSLLogger.isOn && SSLLogger.isOn("ssl")) {
                    SSLLogger.fine("Domain " + tld + " not found");
                }
                return null;
            }
            return new Rules(zis);
        }

        /**
         * Return the requested RuleSet. If it hasn't been created yet,
         * create it and any RuleSets leading up to it.
         */
        private RuleSet getRuleSet(int index) {
            if (index < ruleSets.size()) {
                return ruleSets.get(index);
            }
            RuleSet r = null;
            for (int i = ruleSets.size(); i <= index; i++) {
                r = new RuleSet(i + 1);
                ruleSets.add(r);
            }
            return r;
        }

        /**
         * Find a match for the target string.
         */
        Match match(String domain) {
            // Start at the end of the rules list, looking for longest match.
            // After we find a normal match, we only look for exceptions.
            Match possibleMatch = null;

            Iterator<RuleSet> it = ruleSets.descendingIterator();
            while (it.hasNext()) {
                RuleSet ruleSet = it.next();
                Match match = ruleSet.match(domain);
                if (match != null) {
                    if (match.type() == Rule.Type.EXCEPTION || !hasExceptions) {
                        return match;
                    }
                    if (possibleMatch == null) {
                        possibleMatch = match;
                    }
                }
            }
            return possibleMatch;
        }

        /**
         * Represents a set of rules with the same number of labels
         * and for a particular TLD.
         *
         * Examples:
         *      numLabels = 2
         *      names: co.uk, ac.uk
         *      wildcards *.de (only "de" stored in HashSet)
         *      exceptions: !foo.de (stored as "foo.de")
         */
        private static class RuleSet {
            // the number of labels in this ruleset
            private final int numLabels;
            private final Set<Rule> rules = new HashSet<>();
            boolean hasExceptions = false;
            private static final RegisteredDomain.Type[] AUTHS =
                RegisteredDomain.Type.values();

            RuleSet(int n) {
                numLabels = n;
            }

            void addRule(int auth, String rule) {
                if (rule.startsWith("!")) {
                    rules.add(new Rule(rule.substring(1), Rule.Type.EXCEPTION,
                                       AUTHS[auth]));
                    hasExceptions = true;
                } else if (rule.startsWith("*.") &&
                           rule.lastIndexOf('*') == 0) {
                    rules.add(new Rule(rule.substring(2), Rule.Type.WILDCARD,
                                       AUTHS[auth]));
                } else if (rule.indexOf('*') == -1) {
                    // a "normal" label
                    rules.add(new Rule(rule, Rule.Type.NORMAL, AUTHS[auth]));
                } else {
                    // There is a wildcard in a non-leading label. This case
                    // doesn't currently exist, but we need to handle it anyway.
                    rules.add(new OtherRule(rule, AUTHS[auth], split(rule)));
                }
            }

            Match match(String domain) {
                Match match = null;
                for (Rule rule : rules) {
                    switch (rule.type) {
                        case NORMAL:
                            if (match == null) {
                                match = matchNormal(domain, rule);
                            }
                            break;
                        case WILDCARD:
                            if (match == null) {
                                match = matchWildcard(domain, rule);
                            }
                            break;
                        case OTHER:
                            if (match == null) {
                                match = matchOther(domain, rule);
                            }
                            break;
                        case EXCEPTION:
                            Match excMatch = matchException(domain, rule);
                            if (excMatch != null) {
                                return excMatch;
                            }
                            break;
                    }
                }
                return match;
            }

            private static LinkedList<String> split(String rule) {
                String[] labels = rule.split("\\.");
                return new LinkedList<>(Arrays.asList(labels));
            }

            private static int numLabels(String rule) {
                if (rule.isEmpty()) {
                    return 0;
                }
                int len = rule.length();
                int count = 0;
                int index = 0;
                while (index < len) {
                    int pos;
                    if ((pos = rule.indexOf('.', index)) == -1) {
                        return count + 1;
                    }
                    index = pos + 1;
                    count++;
                }
                return count;
            }

            /**
             * Check for a match with an explicit name rule or a wildcard rule
             * (i.e., a non-exception rule).
             */
            private Match matchNormal(String domain, Rule rule) {
                int index = labels(domain, numLabels);
                if (index == -1) {
                    return null;
                }

                // Check for explicit names.
                String substring = domain.substring(index);
                if (rule.domain.equals(substring)) {
                    return new CommonMatch(domain, rule, index);
                }

                return null;
            }

            private Match matchWildcard(String domain, Rule rule) {
                // Now check for wildcards. In this case, there is one fewer
                // label than numLabels.
                int index = labels(domain, numLabels - 1);
                if (index > 0) {
                    String substring = domain.substring(index);

                    if (rule.domain.equals(substring)) {
                        return new CommonMatch(domain, rule,
                                               labels(domain, numLabels));
                    }
                }

                return null;
            }

            /**
             * Check for a match with an exception rule.
             */
            private Match matchException(String domain, Rule rule) {
                int index = labels(domain, numLabels);
                if (index == -1) {
                    return null;
                }
                String substring = domain.substring(index);

                if (rule.domain.equals(substring)) {
                    return new CommonMatch(domain, rule,
                                           labels(domain, numLabels - 1));
                }

                return null;
            }

            /**
             * A left-to-right comparison of labels.
             * The simplest approach to doing match() would be to
             * use a descending iterator giving a right-to-left comparison.
             * But, it's more efficient to do left-to-right compares
             * because the left most labels are the ones most likely to be
             * different. We just have to figure out which label to start at.
             */
            private Match matchOther(String domain, Rule rule) {
                OtherRule otherRule = (OtherRule)rule;
                LinkedList<String> target = split(domain);

                int diff = target.size() - numLabels;
                if (diff < 0) {
                    return null;
                }

                boolean found = true;
                for (int i = 0; i < numLabels; i++) {
                    String ruleLabel = otherRule.labels.get(i);
                    String targetLabel = target.get(i + diff);

                    if (ruleLabel.charAt(0) != '*' &&
                        !ruleLabel.equalsIgnoreCase(targetLabel)) {
                        found = false;
                        break;
                    }
                }
                if (found) {
                    return new OtherMatch(rule, numLabels, target);
                }
                return null;
            }

            /**
             * Returns a substring (index) with the n right-most labels from s.
             * Returns -1 if s does not have at least n labels, 0, if the
             * substring is s.
             */
            private static int labels(String s, int n) {
                if (n < 1) {
                    return -1;
                }
                int index = s.length();
                for (int i = 0; i < n; i++) {
                    int next = s.lastIndexOf('.', index);
                    if (next == -1) {
                        if (i == n - 1) {
                            return 0;
                        } else {
                            return -1;
                        }
                    }
                    index = next - 1;
                }
                return index + 2;
            }
        }
    }

    private static class Rule {
        enum Type { EXCEPTION, NORMAL, OTHER, WILDCARD }

        String domain;
        Type type;
        RegisteredDomain.Type auth;
        Rule(String domain, Type type, RegisteredDomain.Type auth) {
            this.domain = domain;
            this.type = type;
            this.auth = auth;
        }
    }

    private static class OtherRule extends Rule {
        List<String> labels;
        OtherRule(String domain, RegisteredDomain.Type auth,
                  List<String> labels) {
            super(domain, Type.OTHER, auth);
            this.labels = labels;
        }
    }

    /**
     * Represents a string's match with a rule in the public suffix list.
     */
    private interface Match {
        RegisteredDomain registeredDomain();
        Rule.Type type();
    }

    private static class RegisteredDomainImpl implements RegisteredDomain {
        private final String name;
        private final Type type;
        private final String publicSuffix;
        RegisteredDomainImpl(String name, Type type, String publicSuffix) {
            this.name = name;
            this.type = type;
            this.publicSuffix = publicSuffix;
        }
        @Override
        public String name() {
            return name;
        }
        @Override
        public Type type() {
            return type;
        }
        @Override
        public String publicSuffix() {
            return publicSuffix;
        }
    }

    /**
     * Represents a match against a standard rule in the public suffix list.
     * A standard rule is an explicit name, a wildcard rule with a wildcard
     * only in the leading label, or an exception rule.
     */
    private static class CommonMatch implements Match {
        private String domain;
        private int publicSuffix; // index to
        private int registeredDomain; // index to
        private final Rule rule;

        CommonMatch(String domain, Rule rule, int publicSuffix) {
            this.domain = domain;
            this.publicSuffix = publicSuffix;
            this.rule = rule;
            // now locate the previous label
            registeredDomain = domain.lastIndexOf('.', publicSuffix - 2);
            if (registeredDomain == -1) {
                registeredDomain = 0;
            } else {
                registeredDomain++;
            }
        }

        @Override
        public RegisteredDomain registeredDomain() {
            if (publicSuffix == 0) {
                return null;
            }
            return new RegisteredDomainImpl(domain.substring(registeredDomain),
                                            rule.auth,
                                            domain.substring(publicSuffix));
        }

        @Override
        public Rule.Type type() {
            return rule.type;
        }
    }

    /**
     * Represents a non-match with {@code NO_MATCH} or a match against
     * a non-standard rule in the public suffix list. A non-standard rule
     * is a wildcard rule that includes wildcards in a label other than
     * the leading label. The public suffix list doesn't currently have
     * such rules.
     */
    private static class OtherMatch implements Match {
        private final Rule rule;
        private final int numLabels;
        private final LinkedList<String> target;

        OtherMatch(Rule rule, int numLabels, LinkedList<String> target) {
            this.rule = rule;
            this.numLabels = numLabels;
            this.target = target;
        }

        @Override
        public RegisteredDomain registeredDomain() {
            int nlabels = numLabels + 1;
            if (nlabels > target.size()) {
                // special case when registered domain is same as pub suff
                return null;
            }
            return new RegisteredDomainImpl(getSuffixes(nlabels),
                                            rule.auth, getSuffixes(numLabels));
        }

        @Override
        public Rule.Type type() {
            return rule.type;
        }

        private String getSuffixes(int n) {
            Iterator<String> targetIter = target.descendingIterator();
            StringBuilder sb = new StringBuilder();
            while (n > 0 && targetIter.hasNext()) {
                String s = targetIter.next();
                sb.insert(0, s);
                if (n > 1) {
                    sb.insert(0, '.');
                }
                n--;
            }
            return sb.toString();
        }
    }
}
