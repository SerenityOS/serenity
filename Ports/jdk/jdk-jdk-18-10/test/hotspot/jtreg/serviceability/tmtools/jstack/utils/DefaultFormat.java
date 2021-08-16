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
package utils;

import java.util.Map;
import java.util.Scanner;
import java.util.regex.MatchResult;

/**
 *
 * jstack default format 2008-03-05 18:36:26 Full thread dump Java HotSpot(TM)
 * Client VM (11.0-b11 mixed mode):
 *
 * "Thread-16" #10 daemon prio=3 os_prio=0 tid=0x0814d800 nid=0x1d runnable
 * [0xf394d000..0xf394d9f0] java.lang.Thread.State: RUNNABLE at
 * java.net.SocketInputStream.socketRead0(Native Method) at
 * java.net.SocketInputStream.read(SocketInputStream.java:129) at
 * java.net.SocketInputStream.read(SocketInputStream.java:182) at
 * java.io.ObjectInputStream$PeekInputStream.peek(ObjectInputStream.java:2249)
 * at
 * java.io.ObjectInputStream$BlockDataInputStream.peek(ObjectInputStream.java:2542)
 * at
 * java.io.ObjectInputStream$BlockDataInputStream.peekByte(ObjectInputStream.java:2552)
 * at java.io.ObjectInputStream.readObject0(ObjectInputStream.java:1297) at
 * java.io.ObjectInputStream.readObject(ObjectInputStream.java:351) at
 * tmtools.share.debuggee.DebuggeeProtocolHandler.run(DebuggeeProtocolHandler.java:32)
 *
 * Locked ownable synchronizers: - None ....
 *
 * Note that os_prio field is optional and will be printed only if JVM was able
 * to get native thread priority.
 */
public class DefaultFormat implements Format {

    protected String threadInfoPattern() {
        return "^\"(.*)\"\\s(#\\d+\\s|)(daemon\\s|)prio=(.+)\\s(os_prio=(.+)\\s|)tid=(.+)\\snid=(.+)\\s("
                + Consts.UNKNOWN
                + "|runnable|sleeping|waiting\\son\\scondition|in\\sObject\\.wait\\(\\)|waiting\\sfor\\smonitor\\sentry)((.*))$";
    }

    protected String methodInfoPattern() {
        return "^\\s+at\\s(.+)\\((.*?)(\\:|\\))((.*?))\\)?$";
    }

    protected String extendedStatusPattern() {
        return "\\s+java\\.lang\\.Thread\\.State\\:\\s((.+))$";
    }

    protected String jniGlobalRefInfoPattern() {
        return "^JNI\\sglobal\\sreferences:\\s((.+))$";
    }

    // Sample string that matches the pattern:
    // waiting on <0x000000008f64e6d0> (a java.lang.Object)
    protected String monitorInfoPattern() {
        return "^\\s+\\-\\s(locked|waiting\\son|waiting\\sto\\slock)\\s\\<(.*)\\>\\s\\(((.*))\\)$";
    }

    // Sample string that matches the pattern:
    // waiting on <no object reference available>
    protected String monitorInfoNoObjectRefPattern() {
        return "^\\s+\\-\\s(locked|waiting\\son|waiting\\sto\\slock)\\s\\<(.*)\\>$";
    }

    protected String vmVersionInfoPattern() {
        return "Full\\sthread\\sdump\\s.*";
    }

    protected String ownableSynchronizersPattern() {
        return "^\\s+\\-\\s(\\<.*\\>\\s\\(((.*))\\)|None)$";
    }

    public JStack parse(String stack) {
        JStack result = new JStack();
        Scanner scanner = new Scanner(stack);

        // parsing thread stacks
        ThreadStack currentThreadStack = null;
        MethodInfo currentMethodInfo = null;

        try {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (line.matches(threadInfoPattern())) {
                    currentThreadStack = parseThreadInfo(line);
                    result.addThreadStack(currentThreadStack.getThreadName(), currentThreadStack);
                } else if (line.matches(methodInfoPattern())) {
                    currentMethodInfo = parseMethodInfo(line);
                    currentThreadStack.addMethod(currentMethodInfo);
                } else if (line.matches(monitorInfoPattern())) {
                    MonitorInfo mi = parseMonitorInfo(line, monitorInfoPattern());
                    currentMethodInfo.getLocks().add(mi);
                } else if (line.matches(monitorInfoNoObjectRefPattern())) {
                    MonitorInfo mi = parseMonitorInfo(line, monitorInfoNoObjectRefPattern());
                    currentMethodInfo.getLocks().add(mi);
                } else if (line.matches(extendedStatusPattern())) {
                    currentThreadStack.setExtendedStatus(parseExtendedStatus(line));
                } else if (line.matches(vmVersionInfoPattern())) {
                    result.setVmVersion(line);
                } else if (line.matches(ownableSynchronizersPattern())) {
                    currentThreadStack.getLockOSList().add(parseLockInfo(line));
                } else if (line.matches(jniGlobalRefInfoPattern())) {
                    result.setJniGlobalReferences(parseJNIGlobalRefs(line));
                } else if (line.length() != 0) {
                    System.err.println("[Warning] Unknown string: " + line);
                }
            }

            scanner.close();

        } catch (NullPointerException e) {
            e.printStackTrace();
            throw new RuntimeException("Unexpected format in jstack output");
        }

        return result;
    }

    private MonitorInfo parseMonitorInfo(String line, String pattern) {
        Scanner s = new Scanner(line);
        s.findInLine(pattern);
        MonitorInfo mi = new MonitorInfo();
        MatchResult res = s.match();

        mi.setType(res.group(1));
        mi.setMonitorAddress(res.group(2));
        if (res.groupCount() > 2) {
            mi.setMonitorClass(res.group(3));
        }
        return mi;
    }

    protected String parseExtendedStatus(String line) {
        Scanner s = new Scanner(line);
        s.findInLine(extendedStatusPattern());
        String result = s.match().group(1);
        s.close();
        return result;
    }

    protected String parseJNIGlobalRefs(String line) {
        Scanner s = new Scanner(line);
        s.findInLine(jniGlobalRefInfoPattern());
        String result = s.match().group(1);
        s.close();
        return result;
    }

    protected ThreadStack parseThreadInfo(String threadInfo) {
        Scanner s = new Scanner(threadInfo);
        ThreadStack result = new ThreadStack();

        // parsing thread info
        s.findInLine(threadInfoPattern());
        MatchResult res = s.match();

        result.setThreadName(res.group(1));

        result.setType(res.group(3));

        result.setPriority(res.group(4));
        result.setTid(res.group(7));
        result.setNid(res.group(8));
        result.setStatus(res.group(9));

        s.close();
        return result;
    }

    protected MethodInfo parseMethodInfo(String line) {

        MethodInfo result = new MethodInfo();
        Scanner s = new Scanner(line);

        s.findInLine(methodInfoPattern());
        MatchResult rexp = s.match();
        if (rexp.group(4) != null && rexp.group(4).length() > 0) {
            // line "  at tmtools.jstack.share.utils.Utils.sleep(Utils.java:29)"
            result.setName(rexp.group(1));
            result.setCompilationUnit(rexp.group(2));
            result.setLine(rexp.group(4));

        } else {
            // line "  at java.lang.Thread.sleep(Native Method)"
            result.setName(rexp.group(1));
        }

        s.close();
        return result;
    }

    public String dumpStackTraces() {
        StringBuffer result = new StringBuffer();
        Map<Thread, StackTraceElement[]> stacks = Thread.getAllStackTraces();

        // adding data and vm version
        result.append(Consts.UNKNOWN + "\n");
        result.append(Consts.UNKNOWN + "\n\n");

        for (Thread t : stacks.keySet()) {

            result.append("\"" + t.getName() + "\"");
            result.append(Consts.SEPARATOR);

            // status
            if (t.isDaemon()) {
                result.append("daemon");
                result.append(Consts.SEPARATOR);
            }

            // priority
            result.append("prio=" + t.getPriority());
            result.append(Consts.SEPARATOR);

            // tid
            result.append("tid=" + Consts.UNKNOWN);
            result.append(Consts.SEPARATOR);

            // nid
            result.append("nid=" + Consts.UNKNOWN);
            result.append(Consts.SEPARATOR);

            // status
            result.append(Consts.UNKNOWN);
            result.append(Consts.SEPARATOR);

            result.append("\n");

            // extended status
            result.append("   java.lang.Thread.State: "
                    + String.valueOf(Thread.currentThread().getState()));
            result.append(Consts.SEPARATOR);
            result.append("\n");

            for (StackTraceElement st : stacks.get(t)) {
                result.append("  at " + st.toString() + "\n");
            }
            result.append("\n");
        }

        result.append(Consts.JNI_GLOBAL_REF + Consts.UNKNOWN + "\n");
        return result.toString();
    }

    protected LockInfo parseLockInfo(String line) {
        LockInfo res = new LockInfo();

        Scanner s = new Scanner(line);
        s.findInLine(ownableSynchronizersPattern());

        MatchResult matchRes = s.match();
        String lock = matchRes.group(1).equals("None") ? matchRes.group(1) : matchRes.group(2);
        res.setLock(lock);

        return res;
    }

}
