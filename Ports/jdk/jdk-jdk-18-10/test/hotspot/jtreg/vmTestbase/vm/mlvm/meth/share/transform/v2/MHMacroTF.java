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

package vm.mlvm.meth.share.transform.v2;

import java.io.*;
import java.util.*;

import vm.mlvm.share.Env;

import nsk.share.test.TestUtils;


public class MHMacroTF extends MHTF {

    private final String name;
    private final Collection<MHCall> calls = new HashSet<>();
    private final Collection<MHCall> outboundCalls = new LinkedHashSet<>();
    private MHCall inboundCall = null;
    private final Collection<MHTF> tfs = new LinkedHashSet<>();

    public MHMacroTF(String name) {
        this.name = name;
    }

    private void addNewInboundCall(MHCall call) {
        TestUtils.assertNotInCollection(this.calls, call);

        this.inboundCall = call;
        this.calls.add(call);
    }

    public void addOutboundCall(MHCall call) {
        TestUtils.assertNotInCollection(this.calls, call);

        this.calls.add(call);
        this.outboundCalls.add(call);
    }

    public MHCall addTransformation(MHTF tf) throws IllegalArgumentException,
            NoSuchMethodException, IllegalAccessException {
        TestUtils.assertNotInCollection(this.tfs, tf);
        for (MHCall c : tf.getOutboundCalls()) {
            TestUtils.assertInCollection(this.calls, c);
        }

        Env.traceDebug("MHMacroTF: adding %s", tf);

        this.tfs.add(tf);
        MHCall inboundCall = tf.computeInboundCall();
        addNewInboundCall(inboundCall);

        Env.traceDebug("MHMacroTF: current inbound call: %s", inboundCall);

        return inboundCall;
    }

    @Override
    public MHCall computeInboundCall() {
        return inboundCall;
    }

    @Override
    public MHCall[] getOutboundCalls() {
        return this.outboundCalls.toArray(new MHCall[0]);
    }

    @Override
    public MHTF[] getSubTFs() {
        return this.tfs.toArray(new MHTF[0]);
    }

    @Override
    protected String getName() {
        return name + " graph";
    }

    @Override
    protected String getDescription() {
        StringBuilder result = new StringBuilder("\n");
        Deque<PrettyPrintElement> printElements = new ArrayDeque<>();
        printElements.add(new PrettyPrintElement("", "  ", inboundCall, true));
        PrettyPrintElement current;
        while (!printElements.isEmpty()) {
            current = printElements.pop();
            appendElement(result, printElements, current.topCallPrefix,
                    current.subCallPrefix, current.topCall,
                    current.isRecursive);
        }
        String filename = getName() + "-"
                + Long.toString(System.currentTimeMillis(), Character.MAX_RADIX)
                + ".txt";
        try (Writer writer = new FileWriter(filename)) {
            writer.write(result.toString());
        } catch (IOException e) {
            return result.toString();
        }
        return "see " + filename;
    }

    private void appendElement(StringBuilder result,
            Deque<PrettyPrintElement> deque, String topCallPrefix,
            String subCallPrefix, MHCall topCall, boolean isRecursive) {
        MHCall[] outCalls = topCall.getTarget().getOutboundCalls();
        boolean printSubTree = (isRecursive && outCalls.length > 0);
        result.append(topCall.prettyPrint(topCallPrefix + "->",
                subCallPrefix + (printSubTree ? "|" : " ") + " ") + "\n");
        if (printSubTree) {
            for (int n = outCalls.length - 1, i = n; i >= 0; --i) {
                MHCall outCall = outCalls[i];
                boolean isLastSubtree = (i == n);
                String curTopCallPrefix = subCallPrefix
                        + (isLastSubtree ? "\\" : "|")
                        + "--";
                String curSubCallPrefix = subCallPrefix
                        + (isLastSubtree ? " " : "|")
                        + "  ";
                deque.addFirst(new PrettyPrintElement(curTopCallPrefix,
                        curSubCallPrefix, outCall,
                        !outboundCalls.contains(outCall)));
            }
        }
    }


    private static class PrettyPrintElement {
        final String topCallPrefix;
        final String subCallPrefix;
        final MHCall topCall;
        final boolean isRecursive;

        private PrettyPrintElement(String topCallPrefix,
                String subCallPrefix, MHCall topCall, boolean isRecursive) {
            this.topCallPrefix = topCallPrefix;
            this.subCallPrefix = subCallPrefix;
            this.topCall = topCall;
            this.isRecursive = isRecursive;
        }
    }

}
