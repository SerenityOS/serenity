/*
 * Copyright (c) 2011, 2018, Oracle and/or its affiliates. All rights reserved.
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

package vm.mlvm.share.jpda;

import nsk.share.ArgumentParser;
import vm.mlvm.share.Env;

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.Location;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.StackFrame;

public class StratumUtils {

    public static boolean checkStratum(Location location, StratumInfo si) throws AbsentInformationException {
        // Check stratum information
        try {
            Env.traceVerbose(String.format("Stratum [%s]: required=[%s:%d]", si.stratum, si.stratumSourceName, si.stratumSourceLine));
            Env.traceVerbose(String.format("Default stratum: location=[%s:%d]", location.sourceName(), location.lineNumber()));

            String stratumSourceName = location.sourceName(si.stratum);
            int stratumSourceLine = location.lineNumber(si.stratum);
            String stratumInfo = String.format("actual=[%s:%d]", stratumSourceName, stratumSourceLine);

            if (stratumSourceName.equals(si.stratumSourceName) && stratumSourceLine == si.stratumSourceLine) {
                Env.traceVerbose("Stratum matches: " + stratumInfo);
                return true;
            } else {
                Env.complain("Stratum mismatch: " + stratumInfo);
                return false;
            }
        } catch (AbsentInformationException e) {
            Env.complain(e, "Strata information is absent. Available strata: " + getStrataStr(location.declaringType()));

            return false;
        }
    }

    public static StratumInfo parseStratumInfo(String stratumInfo) throws Exception {
        int sourceNamePos = stratumInfo.indexOf('=');
        int sourceLinePos = stratumInfo.indexOf(':');

        if (sourceNamePos == -1 || sourceLinePos == -1 || sourceNamePos >= sourceLinePos) {
            throw new ArgumentParser.BadOption("Wrong syntax of stratum information: [" + stratumInfo + "]");
        }

        return new StratumInfo(
                stratumInfo.substring(0, sourceNamePos),
                stratumInfo.substring(sourceNamePos + 1, sourceLinePos),
                Integer.parseInt(stratumInfo.substring(sourceLinePos + 1)));
    }

    public static String getStrataStr(StackFrame sf) {
        ObjectReference thisObject = sf.thisObject();
        if (thisObject == null)
            return "(no strata)";
        return StratumUtils.getStrataStr(thisObject.referenceType());
    }

    public static String getStrataStr(ReferenceType ref) {
        return ref.availableStrata() + " (" + ref + ")";
    }
}
