/*
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates. All rights reserved.
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
package gc.g1.unloading.check;


import gc.g1.unloading.ExecutionTask;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.util.*;

import nsk.share.test.ExecutionController;

/**
 * This thread listens to queue of phantomized classloaders and marks corresponding assertions as passed.
 */
public class PhantomizationServiceThread extends ExecutionTask {

    private static final int TIMEOUT = 100;

    private Map<Reference<?>, PhantomizedAssertion> map = new HashMap<>();

    private ReferenceQueue queue = new ReferenceQueue();

    public PhantomizationServiceThread(ExecutionController executionController) {
        super(executionController);
    }

    public void add(Reference ref, PhantomizedAssertion assertion) {
        map.put(ref, assertion);
    }

    public ReferenceQueue getQueue() {
        return queue;
    }

    @Override
    protected void task() throws Exception {
        Reference ref = queue.remove(TIMEOUT);
        PhantomizedAssertion assertion = map.remove(ref);
            if (assertion != null) {
                assertion.setPhantomized();
            }
    }

}
