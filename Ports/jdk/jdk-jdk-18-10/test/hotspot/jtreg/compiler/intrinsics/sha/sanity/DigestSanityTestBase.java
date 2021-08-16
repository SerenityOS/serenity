/*
 * Copyright (c) 2014, 2020, Oracle and/or its affiliates. All rights reserved.
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

package compiler.intrinsics.sha.sanity;

import compiler.intrinsics.sha.TestDigest;
import compiler.testlibrary.intrinsics.Verifier;
import sun.hotspot.WhiteBox;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.Properties;
import java.util.function.BooleanSupplier;

/**
 * Base class for sanity tests on SHA intrinsics support.
 */
public class DigestSanityTestBase {
    protected static final String MD5_INTRINSIC_ID
            = "_md5_implCompress";
    protected static final String SHA1_INTRINSIC_ID
            = "_sha_implCompress";
    protected static final String SHA256_INTRINSIC_ID
            = "_sha2_implCompress";
    protected static final String SHA512_INTRINSIC_ID
            = "_sha5_implCompress";
    protected static final String SHA3_INTRINSIC_ID
            = "_sha3_implCompress";
    protected static final String MB_INTRINSIC_ID
            = "_digestBase_implCompressMB";

    private static final WhiteBox WHITE_BOX = WhiteBox.getWhiteBox();
    private static final int MSG_SIZE = 1024;
    private static final int OFFSET = 0;
    private static final int ITERATIONS = 10000;
    private static final int WARMUP_ITERATIONS = 1;
    private static final String PROVIDER = "SUN";

    private final BooleanSupplier predicate;
    private final String intrinsicID;

    /**
     * Construct the new test on intrinsic with ID {@code intrinsicID},
     * which is expected to be emitted if {@code predicate} is evaluated to
     * {@code true}.
     *
     * @param predicate The predicate indicating if the intrinsic is expected to
     *                  be used.
     * @param intrinsicID The ID of the intrinsic to be tested.
     */
    protected DigestSanityTestBase(BooleanSupplier predicate, String intrinsicID) {
        this.predicate = predicate;
        this.intrinsicID = intrinsicID;
    }

    /**
     * Run the test and dump properties to file.
     *
     * @throws Exception when something went wrong.
     */
    public final void test() throws Exception {
        String algorithm = Objects.requireNonNull(
                System.getProperty("algorithm"),
                "Algorithm name should be specified.");

        dumpProperties();

        TestDigest.testDigest(DigestSanityTestBase.PROVIDER, algorithm,
                DigestSanityTestBase.MSG_SIZE, DigestSanityTestBase.OFFSET,
                DigestSanityTestBase.ITERATIONS,
                DigestSanityTestBase.WARMUP_ITERATIONS);
    }

    /**
     * Dump properties containing information about the tested intrinsic name
     * and whether or not is should be used to the file
     * &lt;LogFile value&gt;.verify.properties.
     *
     * @throws IOException when something went wrong during dumping to file.
     */
    private void dumpProperties() throws IOException {
        Properties properties = new Properties();
        properties.setProperty(Verifier.INTRINSIC_NAME_PROPERTY, intrinsicID);
        properties.setProperty(Verifier.INTRINSIC_IS_EXPECTED_PROPERTY,
                String.valueOf(predicate.getAsBoolean()));

        String logFileName
                = DigestSanityTestBase.WHITE_BOX.getStringVMFlag("LogFile");
        FileOutputStream fileOutputStream = new FileOutputStream(logFileName
                + Verifier.PROPERTY_FILE_SUFFIX);

        properties.store(fileOutputStream, null);
        fileOutputStream.close();
    }
}
