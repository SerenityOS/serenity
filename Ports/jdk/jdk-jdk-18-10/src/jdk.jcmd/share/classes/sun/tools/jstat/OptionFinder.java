/*
 * Copyright (c) 2004, 2010, Oracle and/or its affiliates. All rights reserved.
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

package sun.tools.jstat;

import java.util.*;
import java.net.*;
import java.io.*;

/**
 * A class for finding a specific special option in the jstat_options file.
 *
 * @author Brian Doherty
 * @since 1.5
 */
public class OptionFinder {

    private static final boolean debug = false;

    List<URL> optionsSources;

    public OptionFinder(List<URL> optionsSources) {
        this.optionsSources = optionsSources;
    }

    public OptionFormat getOptionFormat(String option, boolean useTimestamp) {
        OptionFormat of = getOptionFormat(option, optionsSources);
        OptionFormat tof = null;
        if ((of != null) && (useTimestamp)) {
            // prepend the timestamp column as first column
            tof = getOptionFormat("timestamp", optionsSources);
            if (tof != null) {
                ColumnFormat cf = (ColumnFormat)tof.getSubFormat(0);
                of.insertSubFormat(0, cf);
            }
        }
        return of;
    }

    protected OptionFormat getOptionFormat(String option, List<URL> sources) {
        OptionFormat of = null;
        for (URL u : sources) {
            try {
                Reader r = new BufferedReader(
                        new InputStreamReader(u.openStream()));
                of = new Parser(r).parse(option);
                if (of != null)
                    break;
            } catch (IOException e) {
                if (debug) {
                    System.err.println("Error processing " + u
                                       + " : " + e.getMessage());
                    e.printStackTrace();
                }
            } catch (ParserException e) {
                // Exception in parsing the options file.
                System.err.println(u + ": " + e.getMessage());
                System.err.println("Parsing of " + u + " aborted");
            }
        }
        return of;
    }
}
