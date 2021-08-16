/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 8072692
 * @summary Check the matching implemented by SecurityManager.checkPackageAccess
 * @run main/othervm -Djava.security.manager=allow CheckPackageMatching
 */

import java.security.Security;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

/*
 * The purpose of this test is not to verify the content of the package
 * access list - but to ensure that the matching implemented by the
 * SecurityManager is correct. This is why we have our own pattern matching
 * algorithm here.
 */
public class CheckPackageMatching {

    /**
     * The restricted packages listed in the package.access property of the
     * java.security file.
     */
    private static final String[] packages = actual().toArray(new String[0]);

    /**
     * Returns the list of restricted packages in the package.access property.
     */
    private static List<String> actual() {
        String prop = Security.getProperty("package.access");
        List<String> packages = new ArrayList<>();
        if (prop != null && !prop.equals("")) {
            StringTokenizer tok = new StringTokenizer(prop, ",");
            while (tok.hasMoreElements()) {
                String s = tok.nextToken().trim();
                packages.add(s);
            }
        }
        return packages;
    }

    /**
     * PackageMatcher implements a state machine that matches package
     * names against packages parsed from the package access list.
     */
    private abstract static class PackageMatcher {
        // For each state, chars[state] contains the chars that matches.
        private final char[][] chars;
        // For each state, states[state][i] contains the next state to go
        // to when chars[state][i] matches the current character.
        private final int[][] states;

        // Some markers. We're making the assumption that 0
        // cannot be a valid character for a package name.
        //
        // We use 0 for marking that we expect an end of string in
        // char[state][i].
        private static final char END_OF_STRING = 0;
        // This special state value indicates that we expect the string to end
        // there.
        private static final int END_STATE = -1;
        // This special state value indicates that we can accept any character
        // from now on.
        private static final int WILDCARD_STATE = Integer.MIN_VALUE;

        // Create the data for a new state machine to match package names from
        // the array of package names passed as argument.
        // Each package name in the array is expected to end with '.'
        // For each package in packages we're going to compile state data
        // that will match the regexp:
        // ^packages[i].substring(0, packages[i].length()-1).replace(".","\\.")$|^packages[i].replace(".","\\.").*
        //
        // Let's say the package array is:
        //
        // String[] packages = { "sun.", "com.sun.jmx.", "com.sun.proxy.",
        //                       "apple." };
        //
        // then the state machine will need data that looks like:
        //
        // char[][] chars = {
        //    { 'a', 'c', 's' }, { 'p' }, { 'p' }, { 'l' }, { 'e' }, { 0, '.' },
        //    { 'o' }, { 'm' }, { '.' }, { 's' }, { 'u' }, { 'n' }, { '.' },
        //    { 'j', 'p'},
        //    { 'm' }, { 'x' }, { 0, '.' },
        //    { 'r' }, { 'o' }, { 'x' }, { 'y' }, { 0, '.' },
        //    { 'u' }, { 'n' }, { 0, '.' }
        // }
        // int[][] states = {
        //    { 1, 6, 22 }, { 2 }, { 3 }, { 4 }, { 5 },
        //    { END_STATE, WILDCARD_STATE },
        //    { 7 }, { 8 }, { 9 }, { 10 }, { 11 }, { 12 }, { 13 }, { 14, 17 },
        //    { 15 }, { 16 }, { END_STATE, WILDCARD_STATE },
        //    { 18 }, { 19 }, { 20 }, { 21 }, { END_STATE, WILDCARD_STATE },
        //    { 23 }, { 24 }, { END_STATE, WILDCARD_STATE }
        // }
        //
        // The machine will start by loading the chars and states for state 0
        // chars[0] => { 'a', 'c', 's' } states[0] => { 1, 6, 22 }
        // then it examines the char at index 0 in the candidate name.
        // if the char matches one of the characters in chars[0], then it goes
        // to the corresponding state in states[0]. For instance - if the first
        // char in the candidate name is 's', which corresponds to chars[0][2] -
        // then it will proceed with the next char in the candidate name and go
        // to state 22 (as indicated by states[0][2]) - where it will load the
        // chars and states for states 22: chars[22] = { 'u' },
        // states[22] = { 23 } etc... until the candidate char at the current
        // index matches no char in chars[states] => the candidate name doesn't
        // match - or until it finds a success termination condition: the
        // candidate chars are exhausted and states[state][0] is END_STATE, or
        // the candidate chars are not exhausted - and
        // states[state][chars[state]] is WILDCARD_STATE indicating a '.*' like
        // regexp.
        //
        // [Note that the chars in chars[i] are sorted]
        //
        // The compile(...) method is reponsible for building the state machine
        // data and is called only once in the constructor.
        //
        // The matches(String candidate) method will tell whether the candidate
        // matches by implementing the algorithm described above.
        //
        PackageMatcher(String[] packages) {
            final boolean[] selected = new boolean[packages.length];
            Arrays.fill(selected, true);
            final ArrayList<char[]> charList = new ArrayList<>();
            final ArrayList<int[]> stateList = new ArrayList<>();
            compile(0, 0, packages, selected, charList, stateList);
            chars = charList.toArray(new char[0][0]);
            states = stateList.toArray(new int[0][0]);
        }

        /**
         * Compiles the state machine data (recursive).
         *
         * @param step  The index of the character which we're looking at in
         *              this step.
         * @param state The current state (starts at 0).
         * @param pkgs  The list of packages from which the automaton is built.
         * @param selected  Indicates which packages we're looking at in this
                            step.
         * @param charList  The list from which we will build
                            {@code char[][] chars;}
         * @param stateList The list from which we will build
                            {@code int[][]  states;}
         * @return the next available state.
         */
        private int compile(int step, int state, String[] pkgs,
                            boolean[] selected, ArrayList<char[]> charList,
                            ArrayList<int[]> stateList) {
            final char[] next = new char[pkgs.length];
            final int[] nexti = new int[pkgs.length];
            int j = 0;
            char min = Character.MAX_VALUE; char max = 0;
            for (int i = 0; i < pkgs.length; i++) {
                if (!selected[i]) continue;
                final String p = pkgs[i];
                final int len = p.length();
                if (step > len) {
                    selected[i] = false;
                    continue;
                }
                if (len - 1 == step) {
                    boolean unknown = true;
                    for (int k = 0; k < j ; k++) {
                        if (next[k] == END_OF_STRING) {
                            unknown = false;
                            break;
                        }
                    }
                    if (unknown) {
                        next[j] = END_OF_STRING;
                        j++;
                    }
                    nexti[i] = END_STATE;
                }
                final char c = p.charAt(step);
                nexti[i] = len - 1 == step ? END_STATE : c;
                boolean unknown = j == 0 || c < min || c > max;
                if (!unknown) {
                    if (c != min || c != max) {
                        unknown = true;
                        for (int k = 0; k < j ; k++) {
                            if (next[k] == c) {
                                unknown = false;
                                break;
                            }
                        }
                    }
                }
                if (unknown) {
                    min = min > c ? c : min;
                    max = max < c ? c : max;
                    next[j] = c;
                    j++;
                }
            }
            final char[] nc = new char[j];
            final int[]  nst = new int[j];
            System.arraycopy(next, 0, nc, 0, nc.length);
            Arrays.sort(nc);
            final boolean ns[] = new boolean[pkgs.length];

            charList.ensureCapacity(state + 1);
            stateList.ensureCapacity(state + 1);
            charList.add(state, nc);
            stateList.add(state, nst);
            state = state + 1;
            for (int k = 0; k < nc.length; k++) {
                int selectedCount = 0;
                boolean endStateFound = false;
                boolean wildcardFound = false;
                for (int l = 0; l < nexti.length; l++) {
                    if (!(ns[l] = selected[l])) {
                        continue;
                    }
                    ns[l] = nexti[l] == nc[k] || nexti[l] == END_STATE
                            && nc[k] == '.';
                    endStateFound = endStateFound || nc[k] == END_OF_STRING
                                    && nexti[l] == END_STATE;
                    wildcardFound = wildcardFound || nc[k] == '.'
                                    && nexti[l] == END_STATE;
                    if (ns[l]) {
                        selectedCount++;
                    }
                }
                nst[k] = (endStateFound ? END_STATE
                         : wildcardFound ? WILDCARD_STATE : state);
                if (selectedCount == 0 || wildcardFound) {
                    continue;
                }
                state = compile(step + 1, state, pkgs, ns, charList, stateList);
            }
            return state;
        }

        /**
         * Matches 'pkg' against the list of package names compiled in the
         * state machine data.
         *
         * @param pkg The package name to match. Must not end with '.'.
         * @return true if the package name matches, false otherwise.
         */
        public boolean matches(String pkg) {
            int state = 0;
            int i;
            final int len = pkg.length();
            next: for (i = 0; i <= len; i++) {
                if (state == WILDCARD_STATE) {
                    return true; // all characters will match.
                }
                if (state == END_STATE) {
                    return i == len;
                }
                final char[] ch = chars[state];
                final int[] st = states[state];
                if (i == len) {
                    // matches only if we have exhausted the string.
                    return st[0] == END_STATE;
                }
                if (st[0] == END_STATE && st.length == 1) {
                    // matches only if we have exhausted the string.
                    return i == len;
                }
                final char c = pkg.charAt(i); // look at next char...
                for (int j = st[0] == END_STATE ? 1 : 0; j < ch.length; j++) {
                    final char n = ch[j];
                    if (c == n) {      // found a match
                        state = st[j]; // get the next state.
                        continue next; // go to next state
                    } else if (c < n) {
                        break; // chars are sorted. we won't find it. no match.
                    }
                }
                break; // no match
            }
            return false;
        }
    }

    private static final class TestPackageMatcher extends PackageMatcher {
        private final List<String> list;

        TestPackageMatcher(String[] packages) {
            super(packages);
            this.list = Collections.unmodifiableList(Arrays.asList(packages));
        }

        @Override
        public boolean matches(String pkg) {
            final boolean match1 = super.matches(pkg);
            boolean match2 = false;
            String p2 = pkg + ".";
            for (String p : list) {
                if (pkg.startsWith(p) || p2.equals(p)) {
                    match2 = true;
                    break;
                }
            }
            if (match1 != match2) {
                System.err.println("Test Bug: PackageMatcher.matches(\"" +
                                   pkg + "\") returned " + match1);
                System.err.println("Package Access List is: " + list);
                throw new Error("Test Bug: PackageMatcher.matches(\"" +
                                pkg + "\") returned " + match1);
            }
            return match1;
        }
    }

    private static void smokeTest() {
        // these checks should pass.
        System.getSecurityManager().checkPackageAccess("com.sun.blah");
        System.getSecurityManager().checkPackageAccess("com.sun.jm");
        System.getSecurityManager().checkPackageAccess("com.sun.jmxa");
        System.getSecurityManager().checkPackageAccess("jmx");
        List<String> actual = Arrays.asList(packages);
        if (!actual.contains("sun.misc.")) {
            throw new Error("package.access does not contain 'sun.misc.'");
        }
    }

    // This is a sanity test for our own test code.
    private static void testTheTest(String[] pkgs, char[][] chars,
                                    int[][] states) {

        PackageMatcher m = new TestPackageMatcher(pkgs);
        String unexpected = "";
        if (!Arrays.deepEquals(chars, m.chars)) {
            System.err.println("Char arrays differ");
            if (chars.length != m.chars.length) {
                System.err.println("Char array lengths differ: expected="
                        + chars.length + " actual=" + m.chars.length);
            }
            System.err.println(Arrays.deepToString(m.chars).replace((char)0,
                                                   '0'));
            unexpected = "chars[]";
        }
        if (!Arrays.deepEquals(states, m.states)) {
            System.err.println("State arrays differ");
            if (states.length != m.states.length) {
                System.err.println("Char array lengths differ: expected="
                        + states.length + " actual=" + m.states.length);
            }
            System.err.println(Arrays.deepToString(m.states));
            if (unexpected.length() > 0) {
                unexpected = unexpected + " and ";
            }
            unexpected = unexpected + "states[]";
        }

        if (unexpected.length() > 0) {
            throw new Error("Unexpected "+unexpected+" in PackageMatcher");
        }

        testMatches(m, pkgs);
    }

    // This is a sanity test for our own test code.
    private static void testTheTest() {
        final String[] packages2 = { "sun.", "com.sun.jmx.",
                                     "com.sun.proxy.", "apple." };

        final int END_STATE = PackageMatcher.END_STATE;
        final int WILDCARD_STATE = PackageMatcher.WILDCARD_STATE;

        final char[][] chars2 = {
            { 'a', 'c', 's' }, { 'p' }, { 'p' }, { 'l' }, { 'e' }, { 0, '.' },
            { 'o' }, { 'm' }, { '.' }, { 's' }, { 'u' }, { 'n' }, { '.' },
            { 'j', 'p'},
            { 'm' }, { 'x' }, { 0, '.' },
            { 'r' }, { 'o' }, { 'x' }, { 'y' }, { 0, '.' },
            { 'u' }, { 'n' }, { 0, '.' }
         };

         final int[][] states2 = {
            { 1, 6, 22 }, { 2 }, { 3 }, { 4 }, { 5 },
            { END_STATE, WILDCARD_STATE },
            { 7 }, { 8 }, { 9 }, { 10 }, { 11 }, { 12 }, { 13 }, { 14, 17 },
            { 15 }, { 16 }, { END_STATE, WILDCARD_STATE },
            { 18 }, { 19 }, { 20 }, { 21 }, { END_STATE, WILDCARD_STATE },
            { 23 }, { 24 }, { END_STATE, WILDCARD_STATE }
         };

         testTheTest(packages2, chars2, states2);

         final String[] packages3 = { "sun.", "com.sun.pro.",
                                      "com.sun.proxy.", "apple." };

         final char[][] chars3 = {
            { 'a', 'c', 's' }, { 'p' }, { 'p' }, { 'l' }, { 'e' }, { 0, '.' },
            { 'o' }, { 'm' }, { '.' }, { 's' }, { 'u' }, { 'n' }, { '.' },
            { 'p' }, { 'r' }, { 'o' }, { 0, '.', 'x' },
            { 'y' }, { 0, '.' },
            { 'u' }, { 'n' }, { 0, '.' }
         };

         final int[][] states3 = {
            { 1, 6, 19 }, { 2 }, { 3 }, { 4 }, { 5 },
            { END_STATE, WILDCARD_STATE },
            { 7 }, { 8 }, { 9 }, { 10 }, { 11 }, { 12 }, { 13 }, { 14 },
            { 15 }, { 16 }, { END_STATE, WILDCARD_STATE, 17 },
            { 18 }, { END_STATE, WILDCARD_STATE },
            { 20 }, { 21 }, { END_STATE, WILDCARD_STATE }
         };

         testTheTest(packages3, chars3, states3);
    }

    private static volatile boolean sanityTesting = false;

    public static void main(String[] args) {
        System.setSecurityManager(new SecurityManager());

        // Some smoke tests.
        smokeTest();
        System.out.println("Smoke tests passed.");

        // Test our own pattern matching algorithm. Here we actually test
        // the PackageMatcher class from our own test code.
        sanityTesting = true;
        try {
            testTheTest();
            System.out.println("Sanity tests passed.");
        } finally {
            sanityTesting = false;
        }

        // Now test the package matching in the security manager.
        PackageMatcher matcher = new TestPackageMatcher(packages);

        // These should not match.
        for (String pkg : new String[] {"gloups.machin", "su",
                                        "org.jcp.xml.dsig.inter",
                                        "com.sun.jm", "com.sun.jmxa"}) {
            testMatch(matcher, pkg, false, true);
        }

        // These should match.
        for (String pkg : Arrays.asList(
                new String[] {"sun.misc.gloups.machin", "sun.misc",
                              "sun.reflect"})) {
            testMatch(matcher, pkg, true, true);
        }

        // Derive a list of packages that should match or not match from
        // the list in 'packages' - and check that the security manager
        // throws the appropriate exception.
        testMatches(matcher, packages);
    }

    private static void testMatches(PackageMatcher matcher, String[] pkgs) {
        Collection<String> pkglist = Arrays.asList(pkgs);
        PackageMatcher ref = new TestPackageMatcher(packages);

        for (String pkg : pkgs) {
            String candidate = pkg + "toto";
            boolean expected = true;
            testMatch(matcher, candidate, expected,
                      ref.matches(candidate) == expected);
        }

        for (String pkg : pkgs) {
            String candidate = pkg.substring(0, pkg.length() - 1);
            boolean expected = pkglist.contains(candidate + ".");
            testMatch(matcher, candidate, expected,
                      ref.matches(candidate) == expected);
        }

        for (String pkg : pkgs) {
            String candidate = pkg.substring(0, pkg.length() - 2);
            boolean expected = pkglist.contains(candidate + ".");
            testMatch(matcher, candidate, expected,
                      ref.matches(candidate) == expected);
        }
    }

    private static void testMatch(PackageMatcher matcher, String candidate,
                                  boolean expected, boolean testSecurityManager)
    {
        final boolean m = matcher.matches(candidate);
        if (m != expected) {
            final String msg = "\"" + candidate + "\": " +
                (m ? "matches" : "does not match");
            throw new Error("PackageMatcher does not give expected results: "
                            + msg);
        }

        if (sanityTesting) {
            testSecurityManager = false;
        }

        if (testSecurityManager) {
            System.out.println("Access to " + candidate + " should be " +
                               (expected ? "rejected" : "granted"));
            final String errormsg = "\"" + candidate + "\" : " +
                (expected ? "granted" : "not granted");
            try {
                System.getSecurityManager().checkPackageAccess(candidate);
                if (expected) {
                    System.err.println(errormsg);
                    throw new Error("Expected exception not thrown: " +
                                    errormsg);
                }
            } catch (SecurityException x) {
                if (!expected) {
                    System.err.println(errormsg);
                    throw new Error(errormsg + " - unexpected exception: " +
                                    x, x);
                } else {
                    System.out.println("Got expected exception: " + x);
                }
            }
        }
    }
}
