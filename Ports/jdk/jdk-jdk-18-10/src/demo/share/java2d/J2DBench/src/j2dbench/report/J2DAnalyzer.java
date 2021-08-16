/*
 * Copyright (c) 2002, 2018, Oracle and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Oracle nor the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/*
 * This source code is provided to illustrate the usage of a given feature
 * or technique and has been deliberately simplified. Additional steps
 * required for a production-quality application, such as security checks,
 * input validation and proper error handling, might not be present in
 * this sample code.
 */


package j2dbench.report;

import java.util.Vector;
import java.util.Hashtable;
import java.util.Enumeration;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;

public class J2DAnalyzer {
    static Vector results = new Vector();
    static GroupResultSetHolder groupHolder;

    static final int BEST = 1;    /* The best score */
    static final int WORST = 2;   /* The worst score */
    static final int AVERAGE = 3; /* Average of all scores */
    static final int MIDAVG = 4;  /* Average of all but the best and worst */

    static int mode = MIDAVG;

    public static void usage(PrintStream out) {
        out.println("usage:");
        out.println("    java -jar J2DAnalyzer.jar [Option]*");
        out.println();
        out.println("where options are any of the following in any order:");
        out.println("   -Help|-Usage          "+
                    "print out this usage statement");
        out.println("   -Group:<groupname>    "+
                    "the following result sets are combined into a group");
        out.println("   -NoGroup              "+
                    "the following result sets stand on their own");
        out.println("   -ShowUncontested      "+
                    "show results even when only result set has a result");
        out.println("   -Graph                "+
                    "graph the results visually (using lines of *'s)");
        out.println("   -Best                 "+
                    "use best time within a resultset");
        out.println("   -Worst                "+
                    "use worst time within a resultset");
        out.println("   -Average|-Avg         "+
                    "use average of all times within a resultset");
        out.println("   -MidAverage|-MidAvg   "+
                    "like -Average but ignore best and worst times");
        out.println("   <resultfilename>      "+
                    "load in results from named file");
        out.println();
        out.println("results within a result set "+
                    "use Best/Worst/Average mode");
        out.println("results within a group "+
                    "are best of all result sets in that group");
    }

    public static void main(String[] argv) {
        boolean gavehelp = false;
        boolean graph = false;
        boolean ignoreuncontested = true;
        if (argv.length > 0 && argv[0].equalsIgnoreCase("-html")) {
            String[] newargs = new String[argv.length-1];
            System.arraycopy(argv, 1, newargs, 0, newargs.length);
            HTMLSeriesReporter.main(newargs);
            return;
        }
        for (int i = 0; i < argv.length; i++) {
            String arg = argv[i];
            if (arg.regionMatches(true, 0, "-Group:", 0, 7)) {
                groupHolder = new GroupResultSetHolder();
                groupHolder.setTitle(arg.substring(7));
                results.add(groupHolder);
            } else if (arg.equalsIgnoreCase("-NoGroup")) {
                groupHolder = null;
            } else if (arg.equalsIgnoreCase("-ShowUncontested")) {
                ignoreuncontested = false;
            } else if (arg.equalsIgnoreCase("-Graph")) {
                graph = true;
            } else if (arg.equalsIgnoreCase("-Best")) {
                mode = BEST;
            } else if (arg.equalsIgnoreCase("-Worst")) {
                mode = WORST;
            } else if (arg.equalsIgnoreCase("-Average") ||
                       arg.equalsIgnoreCase("-Avg"))
            {
                mode = AVERAGE;
            } else if (arg.equalsIgnoreCase("-MidAverage") ||
                       arg.equalsIgnoreCase("-MidAvg"))
            {
                mode = MIDAVG;
            } else if (arg.equalsIgnoreCase("-Help") ||
                       arg.equalsIgnoreCase("-Usage"))
            {
                usage(System.out);
                gavehelp = true;
            } else {
                readResults(argv[i]);
            }
        }

        if (results.size() == 0) {
            if (!gavehelp) {
                System.err.println("No results loaded");
                usage(System.err);
            }
            return;
        }

        int numsets = results.size();
        double[] totalscore = new double[numsets];
        int[] numwins = new int[numsets];
        int[] numties = new int[numsets];
        int[] numloss = new int[numsets];
        int[] numtests = new int[numsets];
        double[] bestscore = new double[numsets];
        double[] worstscore = new double[numsets];
        double[] bestspread = new double[numsets];
        double[] worstspread = new double[numsets];
        for (int i = 0; i < numsets; i++) {
            bestscore[i] = Double.NEGATIVE_INFINITY;
            worstscore[i] = Double.POSITIVE_INFINITY;
            bestspread[i] = Double.POSITIVE_INFINITY;
            worstspread[i] = Double.NEGATIVE_INFINITY;
        }

        ResultSetHolder base = (ResultSetHolder) results.elementAt(0);
        Enumeration enum_ = base.getKeyEnumeration();
        Vector keyvector = new Vector();
        while (enum_.hasMoreElements()) {
            keyvector.add(enum_.nextElement());
        }
        String[] keys = new String[keyvector.size()];
        keyvector.copyInto(keys);
        sort(keys);
        enum_ = ResultHolder.commonkeys.keys();
        System.out.println("Options common across all tests:");
        if (ResultHolder.commonname != null &&
            ResultHolder.commonname.length() != 0)
        {
            System.out.println("  testname="+ResultHolder.commonname);
        }
        while (enum_.hasMoreElements()) {
            Object key = enum_.nextElement();
            System.out.println("  "+key+"="+ResultHolder.commonkeymap.get(key));
        }
        System.out.println();
        for (int k = 0; k < keys.length; k++) {
            String key = keys[k];
            ResultHolder rh = base.getResultByKey(key);
            double score = rh.getScore();
            double maxscore = score;
            int numcontesting = 0;
            for (int i = 0; i < numsets; i++) {
                ResultSetHolder rsh =
                    (ResultSetHolder) results.elementAt(i);
                ResultHolder rh2 = rsh.getResultByKey(key);
                if (rh2 != null) {
                    if (graph) {
                        maxscore = Math.max(maxscore, rh2.getBestScore());
                    }
                    numcontesting++;
                }
            }
            if (ignoreuncontested && numcontesting < 2) {
                continue;
            }
            System.out.println(rh.getShortKey()+":");
            for (int i = 0; i < numsets; i++) {
                ResultSetHolder rsh = (ResultSetHolder) results.elementAt(i);
                System.out.print(rsh.getTitle()+": ");
                ResultHolder rh2 = rsh.getResultByKey(key);
                if (rh2 == null) {
                    System.out.println("not run");
                } else {
                    double score2 = rh2.getScore();
                    double percent = calcPercent(score, score2);
                    numtests[i]++;
                    if (percent < 97.5) {
                        numloss[i]++;
                    } else if (percent > 102.5) {
                        numwins[i]++;
                    } else {
                        numties[i]++;
                    }
                    totalscore[i] += score2;
                    if (bestscore[i] < percent) {
                        bestscore[i] = percent;
                    }
                    if (worstscore[i] > percent) {
                        worstscore[i] = percent;
                    }
                    double spread = rh2.getSpread();
                    if (bestspread[i] > spread) {
                        bestspread[i] = spread;
                    }
                    if (worstspread[i] < spread) {
                        worstspread[i] = spread;
                    }
                    System.out.print(format(score2));
                    System.out.print(" (var="+spread+"%)");
                    System.out.print(" ("+percent+"%)");
                    System.out.println();
                    if (graph) {
                        int maxlen = 60;
                        int avgpos =
                            (int) Math.round(maxlen * score / maxscore);
                        Vector scores = rh2.getAllScores();
                        for (int j = 0; j < scores.size(); j++) {
                            double s = ((Double) scores.get(j)).doubleValue();
                            int len = (int) Math.round(maxlen * s / maxscore);
                            int pos = 0;
                            while (pos < len) {
                                System.out.print(pos == avgpos ? '|' : '*');
                                pos++;
                            }
                            while (pos <= avgpos) {
                                System.out.print(pos == avgpos ? '|' : ' ');
                                pos++;
                            }
                            System.out.println();
                        }
                    }
                }
            }
        }
        System.out.println();
        System.out.println("Summary:");
        for (int i = 0; i < numsets; i++) {
            ResultSetHolder rsh = (ResultSetHolder) results.elementAt(i);
            System.out.println("  "+rsh.getTitle()+": ");
            if (numtests[i] == 0) {
                System.out.println("    No tests matched reference results");
            } else {
                double overallscore = totalscore[i]/numtests[i];
                System.out.println("    Number of tests:  "+numtests[i]);
                System.out.println("    Overall average:  "+overallscore);
                System.out.println("    Best spread:      "+bestspread[i]+
                                   "% variance");
                System.out.println("    Worst spread:     "+worstspread[i]+
                                   "% variance");
                if (i == 0) {
                    System.out.println("    (Basis for results comparison)");
                } else {
                    System.out.println("    Comparison to basis:");
                    System.out.println("      Best result:      "+bestscore[i]+
                                       "% of basis");
                    System.out.println("      Worst result:     "+worstscore[i]+
                                       "% of basis");
                    System.out.println("      Number of wins:   "+numwins[i]);
                    System.out.println("      Number of ties:   "+numties[i]);
                    System.out.println("      Number of losses: "+numloss[i]);
                }
            }
            System.out.println();
        }
    }

    public static void readResults(String filename) {
        BufferedReader in;
        try {
            in = new BufferedReader(new FileReader(filename));
            readResults(in);
        } catch (IOException e) {
            System.out.println(e);
            return;
        }
    }

    public static void addResultSet(ResultSetHolder rs) {
        if (groupHolder == null) {
            results.add(rs);
        } else {
            groupHolder.addResultSet(rs);
        }
    }

    public static void readResults(BufferedReader in)
        throws IOException
    {
        String xmlver = in.readLine();
        if (xmlver == null || !xmlver.startsWith("<?xml version=\"1.0\"")) {
            return;
        }
        while (true) {
            String rsline = in.readLine();
            if (rsline == null) {
                break;
            }
            rsline = rsline.trim();
            if (rsline.startsWith("<result-set version=")) {
                String title = getStringAttribute(rsline, "name");
                if (title == null) {
                    title = "No title";
                }
                SingleResultSetHolder srs = new SingleResultSetHolder();
                srs.setTitle(title);
                readResultSet(in, srs);
                addResultSet(srs);
            }
        }
    }

    public static void readResultSet(BufferedReader in,
                                     SingleResultSetHolder srs)
        throws IOException
    {
        String line;
        while ((line = in.readLine()) != null) {
            line = line.trim();
            if (line.startsWith("<test-desc>")) {
                int index = line.indexOf("<", 11);
                if (index < 0) {
                    index = line.length();
                }
                line = line.substring(11, index);
                srs.setDescription(line);
            } else if (line.startsWith("<sys-prop")) {
                String key = getStringAttribute(line, "key");
                String val = getStringAttribute(line, "value");
                if (key != null && val != null) {
                    srs.setProperty(key, val);
                }
            } else if (line.startsWith("<test-date")) {
                srs.setStartTime(getLongAttribute(line, "start"));
                srs.setEndTime(getLongAttribute(line, "end"));
            } else if (line.startsWith("<result")) {
                int numreps = getIntAttribute(line, "num-reps");
                int numunits = getIntAttribute(line, "num-units");
                String name = getStringAttribute(line, "name");
                if (numreps > 0 && numunits >= 0 && name != null) {
                    ResultHolder rh = new ResultHolder(srs);
                    rh.setName(name);
                    rh.setReps(numreps);
                    rh.setUnits(numunits);
                    readResult(in, rh);
                    srs.addResult(rh);
                }
            } else if (line.equals("</result-set>")) {
                break;
            } else {
                System.err.println("Unrecognized line in Result-Set: "+line);
            }
        }
    }

    public static void readResult(BufferedReader in, ResultHolder rh)
        throws IOException
    {
        String line;
        while ((line = in.readLine()) != null) {
            line = line.trim();
            if (line.startsWith("<option")) {
                String key = getStringAttribute(line, "key");
                String val = getStringAttribute(line, "value");
                if (key != null && val != null) {
                    rh.addOption(key, val);
                }
            } else if (line.startsWith("<time")) {
                long ms = getLongAttribute(line, "value");
                if (ms >= 0) {
                    rh.addTime(ms);
                }
            } else if (line.equals("</result>")) {
                break;
            } else {
                System.err.println("Unrecognized line in Result: "+line);
            }
        }
    }

    public static String getStringAttribute(String line, String attrname) {
        int index = line.indexOf(attrname+"=");
        if (index < 0) {
            return null;
        }
        index += attrname.length()+1;
        int endindex;
        if (line.charAt(index) == '\"') {
            index++;
            endindex = line.indexOf('\"', index);
        } else {
            endindex = -1;
        }
        if (endindex < 0) {
            endindex = line.indexOf(' ', index);
        }
        if (endindex < 0) {
            endindex = line.indexOf('>', index);
        }
        if (endindex < 0) {
            endindex = line.length();
        }
        return line.substring(index, endindex);
    }

    public static long getLongAttribute(String line, String attrname) {
        String val = getStringAttribute(line, attrname);
        if (val == null) {
            return -1;
        }
        try {
            return Long.parseLong(val);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public static int getIntAttribute(String line, String attrname) {
        String val = getStringAttribute(line, attrname);
        if (val == null) {
            return -1;
        }
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public abstract static class ResultSetHolder {
        private String title;

        public void setTitle(String title) {
            this.title = title;
        }

        public String getTitle() {
            return title;
        }

        public abstract Enumeration getKeyEnumeration();

        public abstract Enumeration getResultEnumeration();

        public abstract ResultHolder getResultByKey(String key);
    }

    public static class GroupResultSetHolder extends ResultSetHolder {
        private Vector members = new Vector();
        private Hashtable allresultkeys = new Hashtable();

        public void addResultSet(ResultSetHolder rsh) {
            members.add(rsh);
            Enumeration enum_ = rsh.getResultEnumeration();
            while (enum_.hasMoreElements()) {
                ResultHolder rh = (ResultHolder) enum_.nextElement();
                String key = rh.getKey();
                allresultkeys.put(key, key);
            }
        }

        private ResultSetHolder getResultSet(int index) {
            return (ResultSetHolder) members.elementAt(index);
        }

        public Enumeration getKeyEnumeration() {
            return allresultkeys.keys();
        }

        public Enumeration getResultEnumeration() {
            return new Enumerator();
        }

        public ResultHolder getResultByKey(String key) {
            ResultHolder best = null;
            double bestscore = 0.0;
            for (int i = 0; i < members.size(); i++) {
                ResultHolder cur = getResultSet(i).getResultByKey(key);
                if (cur != null) {
                    double curscore = cur.getScore();
                    if (best == null || curscore > bestscore) {
                        best = cur;
                        bestscore = curscore;
                    }
                }
            }
            return best;
        }

        public class Enumerator implements Enumeration {
            Enumeration raw = getKeyEnumeration();

            public boolean hasMoreElements() {
                return raw.hasMoreElements();
            }

            public Object nextElement() {
                return getResultByKey((String) raw.nextElement());
            }
        }
    }

    public static class SingleResultSetHolder extends ResultSetHolder {
        private String desc;
        private long start;
        private long end;
        private Hashtable props = new Hashtable();
        private Vector results = new Vector();
        private Hashtable resultsbykey = new Hashtable();

        public void setDescription(String desc) {
            this.desc = desc;
        }

        public String getDescription() {
            return desc;
        }

        public void setStartTime(long ms) {
            start = ms;
        }

        public long getStartTime() {
            return start;
        }

        public void setEndTime(long ms) {
            end = ms;
        }

        public long getEndTime() {
            return end;
        }

        public void setProperty(String key, String value) {
            props.put(key, value);
        }

        public Hashtable getProperties() {
            return this.props;
        }

        public void addResult(ResultHolder rh) {
            results.add(rh);
            resultsbykey.put(rh.getKey(), rh);
        }

        public Enumeration getKeyEnumeration() {
            return new Enumerator();
        }

        public Enumeration getResultEnumeration() {
            return results.elements();
        }

        public ResultHolder getResultByKey(String key) {
            return (ResultHolder) resultsbykey.get(key);
        }

        public class Enumerator implements Enumeration {
            Enumeration raw = getResultEnumeration();

            public boolean hasMoreElements() {
                return raw.hasMoreElements();
            }

            public Object nextElement() {
                return ((ResultHolder) raw.nextElement()).getKey();
            }
        }
    }

    public static class ResultHolder {
        public static Hashtable commonkeymap = new Hashtable();
        public static Hashtable commonkeys = new Hashtable();
        public static String commonname;

        ResultSetHolder rsh;
        private String name;
        private String key;
        private String shortkey;
        private int numreps;
        private int numunits;
        private int numruns;
        private long total;
        private long longest;
        private long shortest;
        private Hashtable options = new Hashtable();
        private Vector times = new Vector();

        public ResultHolder(ResultSetHolder rsh) {
            this.rsh = rsh;
        }

        public void setName(String name) {
            this.name = name;
            if (commonname == null) {
                commonname = name;
            } else if (!commonname.equals(name)) {
                commonname = "";
            }
        }

        public String getName() {
            return name;
        }

        public String getKey() {
            if (key == null) {
                key = makeKey(false);
            }
            return key;
        }

        public String getShortKey() {
            if (shortkey == null) {
                shortkey = makeKey(true);
            }
            return shortkey;
        }

        private String makeKey(boolean prunecommon) {
            String[] keys = new String[options.size()];
            Enumeration enum_ = options.keys();
            int i = 0;
            while (enum_.hasMoreElements()) {
                keys[i++] = (String) enum_.nextElement();
            }
            sort(keys);
            String key = (prunecommon && commonname.equals(name)) ? "" : name;
            for (i = 0; i < keys.length; i++) {
                if (!prunecommon || !commonkeys.containsKey(keys[i])) {
                    key = key+","+keys[i]+"="+options.get(keys[i]);
                }
            }
            if (key.length() == 0) {
                key = name;
            } else if (key.startsWith(",")) {
                key = key.substring(1);
            }
            return key;
        }

        public void setReps(int numreps) {
            this.numreps = numreps;
        }

        public int getReps() {
            return numreps;
        }

        public void setUnits(int numunits) {
            this.numunits = numunits;
        }

        public int getUnits() {
            return numunits;
        }

        public void addOption(String key, String value) {
            if (this.key != null) {
                throw new InternalError("option added after key was made!");
            }
            options.put(key, value);
            Object commonval = commonkeymap.get(key);
            if (commonval == null) {
                commonkeymap.put(key, value);
                commonkeys.put(key, key);
            } else if (!commonval.equals(value)) {
                commonkeys.remove(key);
            }
        }

        public Hashtable getOptions() {
            return options;
        }

        public void addTime(long ms) {
            times.add(new Long(ms));
            if (numruns == 0) {
                longest = shortest = ms;
            } else {
                if (longest < ms) longest = ms;
                if (shortest > ms) shortest = ms;
            }
            total += ms;
            numruns++;
        }

        public double getSpread() {
            return calcPercent(shortest, longest - shortest);
        }

        public double getScore() {
            double score = numreps;
            if (numunits > 0) {
                score *= numunits;
            }
            long divisor;
            if (mode == BEST) {
                divisor = shortest;
            } else if (mode == WORST) {
                divisor = longest;
            } else if (mode == AVERAGE || numruns < 3) {
                score *= numruns;
                divisor = total;
            } else {
                score *= (numruns-2);
                divisor = (total - longest - shortest);
            }
            score /= divisor;
            return score;
        }

        public double getBestScore() {
            double score = numreps;
            if (numunits > 0) {
                score *= numunits;
            }
            return score / shortest;
        }

        public Vector getAllScores() {
            Vector scores = new Vector();

            double score = numreps;
            if (numunits > 0) {
                score *= numunits;
            }
            if (mode == BEST) {
                scores.add(new Double(score / shortest));
            } else if (mode == WORST) {
                scores.add(new Double(score / longest));
            } else {
                long elimshort, elimlong;
                if (mode == AVERAGE || numruns < 3) {
                    elimshort = elimlong = -1;
                } else {
                    elimshort = shortest;
                    elimlong = longest;
                }
                for (int i = 0; i < times.size(); i++) {
                    long time = ((Long) times.get(i)).longValue();
                    if (time == elimshort) {
                        elimshort = -1;
                        continue;
                    }
                    if (time == elimlong) {
                        elimlong = -1;
                        continue;
                    }
                    scores.add(new Double(score / time));
                }
            }
            return scores;
        }
    }

    public static double calcPercent(double base, double val) {
        val /= base;
        val *= 10000;
        val = Math.rint(val);
        return val / 100;
    }

    public static String format(double val) {
        long lval = (long) val;
        String ret = String.valueOf(lval);
        int digits = ret.length();
        if (digits > 17) {
            ret = String.valueOf(val);
        } else {
            val -= lval;
            String fraction = String.valueOf(val);
            fraction = fraction.substring(fraction.indexOf('.'));
            ret += fraction;
            int len = digits+5;
            if (len < 10) len = 10;
            len++;
            if (ret.length() > len) {
                ret = ret.substring(0, len);
            }
        }
        return ret;
    }

    public static void sort(String[] strs) {
        for (int i = 1; i < strs.length; i++) {
            for (int j = i; j > 0; j--) {
                if (strs[j].compareTo(strs[j-1]) >= 0) {
                    break;
                }
                String tmp = strs[j-1];
                strs[j-1] = strs[j];
                strs[j] = tmp;
            }
        }
    }

    public static void setMode(int mode) {
        if(mode >= BEST && mode <= MIDAVG) {
            J2DAnalyzer.mode = mode;
        }
        else {
            J2DAnalyzer.mode = MIDAVG;
        }
    }
}
