/*
 * Copyright (c) 2011, 2021, Oracle and/or its affiliates. All rights reserved.
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
package gc.gctests.PhantomReference;

import java.lang.ref.ReferenceQueue;
import java.util.Date;
import java.util.HashMap;

/**
 * Helper class that tracks the original hash code for the
 * object.
 */
public final class PhantomHelper {

    private int originalHashCode;
    private int hashCounter;

    /**
     * Constructor for helper class that tracks the hash code.
     *
     * @param originalHashCode Referred objects hash code
     */
    public PhantomHelper(int originalHashCode) {
        this.originalHashCode = originalHashCode;
        hashCounter = 1;
    }

    /**
     * Get the referred objects original hash code.
     *
     * @return Original referred objects hash code
     */
    public int getOriginalHashCode() {
        return originalHashCode;
    }

    /**
     * Increase the counter for the number of objects
     * using this hash code.
     */
    public void increaseHashCounter() {
        hashCounter++;
    }

    /**
     * Decrease the counter for the number of objects
     * using this hash code.
     */
    public void decreaseHashCounter() {
        hashCounter--;
    }

    /**
     * Retreive the hash code counter.
     *
     * @return Hash code counter value
     */
    public int getHashCounter() {
        return hashCounter;
    }

    /**
     * Verify all the hash codes from the objects in the reference
     * queue against the hash map.
     *
     * @param rq Reference queue for the phantom references.
     * @param hmHelper Hashmap that contains all the hash codes
     * @param maxWaitTime Maximum time to wait for completion of deref:ing
     * from the reference queue.
     * @return True if all hash codes matches
     */
    public static final String checkAllHashCodes(ReferenceQueue rq,
                                                 HashMap hmHelper,
                                                 long maxWaitTime) {
        // Check all the phantom references
        long startTime = new Date().getTime();
        boolean keepRunning = true;

        while (keepRunning) {
            try {
                PRHelper prh = (PRHelper) rq.remove(1000);

                if (prh != null) {
                    Integer ik = Integer.valueOf(prh.getReferentHashCode());
                    PhantomHelper ph = (PhantomHelper) hmHelper.get(ik);

                    if (ph != null) {
                        if (ph.getOriginalHashCode()
                                == prh.getReferentHashCode()) {
                            ph.decreaseHashCounter();
                            if (ph.getHashCounter() == 0) {
                                hmHelper.remove(
                                        Integer.valueOf(prh.getReferentHashCode()));
                            } else {
                                hmHelper.put(ik, ph);
                            }
                            prh.clear();
                        }
                    } else {
                        return "Unmapped hash code detected. The test is faulty.";
                    }

                    prh = null;
                }
            } catch (InterruptedException e) {
                ; // Checkstyle wants at least one line here...
            }

            if (new Date().getTime() - startTime > maxWaitTime) {
                return "All phantom references weren't processed "
                        + "in the configured max time ("
                        + (maxWaitTime / 1000) + " secs)";
            }

            if (hmHelper.size() == 0) {
                keepRunning = false;
            }
        }

        return null;
    }
}
