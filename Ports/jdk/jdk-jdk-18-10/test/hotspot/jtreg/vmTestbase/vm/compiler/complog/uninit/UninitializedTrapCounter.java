/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
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
package vm.compiler.complog.uninit;

import java.util.*;
import java.util.regex.*;
import java.io.*;

import nsk.share.TestFailure;
import vm.compiler.complog.share.LogCompilationParser;

/**
 * Parser that finds uninitialized traps for each method and throws
 * and exception if there are more then 1 uninitialized trap for at least 1 method.
 *
 * Parser supports following options:
 * <ul>
 * <li>-classFilter=<comma separated list> - list of classes for which uncommon traps will be checked. If option is not presented or list is empty then all uncommon traps for all classes will be checked.
 * <li>-methodFilter=<comma separated list> - list of methods for which uncommon traps will be checked. If option is not presented or list is empty then all uncommon traps for all methods will be checked.
 * </ul>
 * If compilation log contains uncommon trap with reason uninialized that was fired for method M in class K, then it will be checked only if classFilter contains K and methodFilter contains M.
 */
public class UninitializedTrapCounter extends LogCompilationParser {
    private Map<String,Integer> methods = new LinkedHashMap<String,Integer>();

    private static final String JVMS_ELEMENT = "<jvms [^>]*>";
    private static final String METHOD_INFO = "method='(([^ ']+) ([^ ']+) [^']+)'.*uninitialized_traps='([0-9]+)'";
    private static final String CLASS_FILTER = "-classFilter=([^ ]+)";
    private static final String METHOD_FILTER = "-methodFilter=([^ ]+)";
    private static Pattern pattern = Pattern.compile(METHOD_INFO);

    private List<String> classFilter = new ArrayList<String>();
    private List<String> methodFilter = new ArrayList<String>();

    public void setOptions(String optionString) {
        if(optionString == null) return;
        Matcher methodFilterMatcher = Pattern.compile(METHOD_FILTER).matcher(optionString);
        Matcher classFilterMatcher = Pattern.compile(CLASS_FILTER).matcher(optionString);
        if(methodFilterMatcher.find()) {
            methodFilter = Arrays.asList(methodFilterMatcher.group(1).split(","));
        }
        if(classFilterMatcher.find()) {
            classFilter = Arrays.asList(classFilterMatcher.group(1).split(","));
        }
    }

    /**
     * Find uninitialized traps count.
     */
    public void parse(File logFile) throws Throwable {
        Scanner scanner = new Scanner(logFile);
        String jvms = scanner.findWithinHorizon(JVMS_ELEMENT,0);
        while(jvms != null) {
            parseJVMSElement(jvms);
            jvms = scanner.findWithinHorizon(JVMS_ELEMENT,0);
        }

        boolean failed = false;
        for(Map.Entry<String,Integer> method : methods.entrySet()) {
            if(method.getValue() > 1) {
                failed = true;
                log.error(method.getValue() +
                          " uninitizlied traps found for method '" +
                          method.getKey() + "'.");

            }
        }
        if(failed) {
            throw new TestFailure("More than 1 uncommon trap with reason 'uninitialized'"+
                                  " occurred at least for 1 method.");
        }
    }

    private void parseJVMSElement(String jvms) {
        Matcher matcher = pattern.matcher(jvms);
        if(!matcher.find()) return;

        String methodID = matcher.group(1);
        String trapsCountStr = matcher.group(4);
        Integer trapsCount = 0;
        Integer oldTrapsCount = 0;
        String className = matcher.group(2);
        String methodName = matcher.group(3);

        if((classFilter.size() > 0 && !findMatches(classFilter,className)) ||
           (methodFilter.size() > 0 && !findMatches(methodFilter,methodName))) {
            //filtering out uncommon trap we are not interested in
            return;
        }

        try {
            trapsCount = Integer.valueOf(trapsCountStr);
        } catch (NumberFormatException nfe) {
            trapsCount = 0;
        }

        oldTrapsCount = methods.get(methodID);
        if(oldTrapsCount == null) oldTrapsCount = -1;

        methods.put(methodID, Math.max(trapsCount, oldTrapsCount));
    }

    private static boolean findMatches(List<String> patterns, String str) {
        for(String pattern : patterns) {
            if(Pattern.matches(pattern,str)) {
                return true;
            }
        }
        return false;
    }

}
