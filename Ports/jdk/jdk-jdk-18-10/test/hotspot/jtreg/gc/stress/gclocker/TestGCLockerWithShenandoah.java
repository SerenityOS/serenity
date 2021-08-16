/*
 * Copyright (c) 2017, 2018, Red Hat, Inc. All rights reserved.
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
 *
 */

package gc.stress.gclocker;

/*
 * @test id=default
 * @library /
 * @requires vm.gc.Shenandoah
 * @summary Stress Shenandoah's JNI handling by calling GetPrimitiveArrayCritical while concurrently filling up old gen.
 *
 * @run main/native/othervm/timeout=200 -Xlog:gc*=info -Xms1500m -Xmx1500m -XX:+UnlockExperimentalVMOptions -XX:+UnlockDiagnosticVMOptions
 *      -XX:+UseShenandoahGC
 *      -XX:+ShenandoahVerify
 *      gc.stress.gclocker.TestGCLockerWithShenandoah
 *
 * @run main/native/othervm/timeout=200 -Xlog:gc*=info -Xms1500m -Xmx1500m -XX:+UnlockExperimentalVMOptions -XX:+UnlockDiagnosticVMOptions
 *      -XX:+UseShenandoahGC
 *      gc.stress.gclocker.TestGCLockerWithShenandoah
 */

/*
 * @test id=aggressive
 * @library /
 * @requires vm.gc.Shenandoah
 * @summary Stress Shenandoah's JNI handling by calling GetPrimitiveArrayCritical while concurrently filling up old gen.
 *
 * @run main/native/othervm/timeout=200 -Xlog:gc*=info -Xms1500m -Xmx1500m -XX:+UnlockExperimentalVMOptions -XX:+UnlockDiagnosticVMOptions
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCHeuristics=aggressive
 *      -XX:+ShenandoahOOMDuringEvacALot
 *      gc.stress.gclocker.TestGCLockerWithShenandoah
 *
 * @run main/native/othervm/timeout=200 -Xlog:gc*=info -Xms1500m -Xmx1500m -XX:+UnlockExperimentalVMOptions -XX:+UnlockDiagnosticVMOptions
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCHeuristics=aggressive
 *      -XX:+ShenandoahAllocFailureALot
 *      gc.stress.gclocker.TestGCLockerWithShenandoah
 */
public class TestGCLockerWithShenandoah {
    public static void main(String[] args) {
        String[] testArgs = {"2", "Shenandoah", "0"};
        TestGCLocker.main(testArgs);
    }
}
