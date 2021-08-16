/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
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
package vm.share.gc;

import nsk.share.test.ExecutionController;
import nsk.share.gc.gp.classload.GeneratedClassProducer;

public class TriggerUnloadingByFillingMetaspace implements
        TriggerUnloadingHelper {

    private volatile boolean gotOOME = false;
    private ExecutionController stresser;
    private final ThreadLocal<GeneratedClassProducer> generatedClassProducer =
        new ThreadLocal<GeneratedClassProducer>() {
          @Override
          protected GeneratedClassProducer initialValue() {
            return new GeneratedClassProducer("metaspace.stressHierarchy.common.HumongousClass");
          }
        };

    private static boolean isInMetaspace(Throwable error) {
        return (error.getMessage().trim().toLowerCase().contains("metaspace"));
    }

    @Override
    public void triggerUnloading(ExecutionController stresser) {
        while (stresser.continueExecution() && !gotOOME) {
            try {
                generatedClassProducer.get().create(-100500); //argument is not used.
            } catch (Throwable oome) {
                if (!isInMetaspace(oome)) {
                    throw new HeapOOMEException("Got OOME in heap while triggering OOME in metaspace. Test result can't be valid.");
                }
                gotOOME = true;
            }
        }
    }
}
