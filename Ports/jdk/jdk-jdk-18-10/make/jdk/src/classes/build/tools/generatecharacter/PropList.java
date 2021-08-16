/*
 * Copyright (c) 2011, 2019, Oracle and/or its affiliates. All rights reserved.
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

package build.tools.generatecharacter;

import java.util.regex.*;
import java.util.*;
import java.io.*;

/**
 * A PropList object contains the lists of code points that have
 * the same Unicode property defined in PropList.txt and
 * DerivedCoreProperties.txt
 *
 * @author Xueming Shen
 */
public class PropList {

    public static PropList readSpecFile(File file, int plane)
        throws IOException
    {
        return new PropList(file, plane);
    }

    public List<Integer> codepoints(String name) {
        return propMap.get(name);
    }

    public Set<String> names() {
        return propMap.keySet();
    }

    public void putAll(PropList pl) {
        pl.names().stream()
            .forEach(name -> propMap.put(name, pl.codepoints(name)));
    }

    private Map<String, List<Integer>> propMap =
        new LinkedHashMap<String, List<Integer>>();

    private PropList(File file, int plane) throws IOException {

        int i, j;
        BufferedReader sbfr = new BufferedReader(new FileReader(file));
        Matcher m = Pattern.compile("(\\p{XDigit}+)(?:\\.{2}(\\p{XDigit}+))?\\s*;\\s+(\\w+)\\s+#.*").matcher("");
        String line = null;
        int lineNo = 0;
        while ((line = sbfr.readLine()) != null) {
            lineNo++;
            if (line.length() <= 1 || line.charAt(0) == '#') {
                continue;
            }
            m.reset(line);
            if (m.matches()) {
                int start = Integer.parseInt(m.group(1), 16);
                if ((start >> 16) != plane)
                    continue;
                int end = (m.group(2)==null)?start
                          :Integer.parseInt(m.group(2), 16);
                String name = m.group(3);

                start &= 0xffff;
                end &= 0xffff;

                List<Integer> list = propMap.get(name);
                if (list == null) {
                    list = new ArrayList<Integer>();
                    propMap.put(name, list);
                }
                while (start <= end)
                    list.add(start++);
            } else {
                System.out.printf("Warning: Unrecognized line %d <%s>%n", lineNo, line);
            }
        }
        sbfr.close();

        //for (String name: propMap.keySet()) {
        //    System.out.printf("%s    %d%n", name, propMap.get(name).size());
        //}
    }

    public static void main(String[] args) throws IOException {
        readSpecFile(new File(args[0]), Integer.decode(args[1]));
    }
}
