/*
 * Copyright (c) 2010, 2016, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 6998583 8141039
 * @summary NativeSeedGenerator is making 8192 byte read requests from
 *             entropy pool on each init.
 * @run main/othervm -Djava.security.egd=file:/dev/random SeedGeneratorChoice
 * @run main/othervm -Djava.security.egd=file:filename  SeedGeneratorChoice
 */

/*
 * Side testcase introduced to ensure changes for 6998583 will always
 * succeed in falling back to ThreadedSeedGenerator if issues are found
 * with the native OS generator request. We should never see an exception
 * causing exit.
 * We should always fall back to the ThreadedSeedGenerator if exceptions
 * are encountered with user defined source of entropy.
 */
import java.security.SecureRandom;
import java.security.Security;

public class SeedGeneratorChoice {

    public static void main(String... arguments) throws Exception {
        for (String mech : new String[]{"SHA1PRNG", "Hash_DRBG", "HMAC_DRBG",
            "CTR_DRBG"}) {

            SecureRandom prng = null;
            if (!mech.contains("_DRBG")) {
                prng = SecureRandom.getInstance(mech);
            } else {
                Security.setProperty("securerandom.drbg.config", mech);
                prng = SecureRandom.getInstance("DRBG");
            }
            prng.generateSeed(1);
        }
    }

}
