/*
 * Copyright (c) 2008, 2018, Oracle and/or its affiliates. All rights reserved.
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
package nsk.share.aod;

import nsk.share.*;
import java.util.*;

public class AODTargetArgParser extends ArgumentParser {

    public static final String agentsNumberParam = "agentsNumber";

    public static final String socketPortParam = "port";

    private int expectedAgentsNumber;

    private int port;

    private static List<String> supportedOptions;

    static {
        supportedOptions = new ArrayList<String>();
        supportedOptions.add(agentsNumberParam);
        supportedOptions.add(socketPortParam);
    }

    public AODTargetArgParser(String[] args) {
        super(args);
    }

    protected boolean checkOption(String option, String value) {
        if (super.checkOption(option, value))
            return true;

        if (!supportedOptions.contains(option))
            return false;

        if (option.equals(agentsNumberParam)) {
            expectedAgentsNumber = Integer.parseInt(value);
            if (expectedAgentsNumber < 0)
                throw new TestBug("Invalid value of '" + option + "'");
        } else if (option.equals(socketPortParam)) {
            port = Integer.parseInt(value);
            if (port <= 0 || port > 65535)
                throw new TestBug("Invalid value of '" + option + "':" + port +" (it is expected to be in the range [1..65535]");
        }

        return true;
    }

    public int getExpectedAgentsNumber() {
        if (!options.containsKey(agentsNumberParam))
            throw new TestBug("Number of expected agents isn't specified");

        return expectedAgentsNumber;
    }

    public int getPort() {
        if (!options.containsKey(socketPortParam))
            return -1;

        return port;
    }
}
