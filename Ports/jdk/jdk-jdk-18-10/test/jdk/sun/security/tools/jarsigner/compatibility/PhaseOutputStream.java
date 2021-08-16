/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/*
 * A custom output stream that saves the testing details to different files
 * according to the current testing phase.
 */
public class PhaseOutputStream extends OutputStream {

    public enum Phase {
        PRE_SIGNING,        // before jar signing
        SIGNING,            // jar signing
        VERIFYING,          // jar verifying
        DELAY_VERIFYING,    // jar verifying after certificates expire
        POST_VERIFYING;     // after jar verifying
    }

    private OutputStream signingOut = null;
    private OutputStream verifyingOut = null;
    private OutputStream delayVerifyingOut = null;

    private Phase currentPhase = Phase.PRE_SIGNING;

    public void transfer() {
        switch (currentPhase) {
        case PRE_SIGNING:
            currentPhase = Phase.SIGNING;
            break;
        case SIGNING:
            currentPhase = Phase.VERIFYING;
            break;
        case VERIFYING:
            currentPhase = Compatibility.DELAY_VERIFY
                    ? Phase.DELAY_VERIFYING
                    : Phase.POST_VERIFYING;
            break;
        case DELAY_VERIFYING:
            currentPhase = Phase.POST_VERIFYING;
            break;
        case POST_VERIFYING:
            currentPhase = Phase.POST_VERIFYING;
            break;
        }
    }

    // The core phases are SIGNING, VERIFYING and DELAY_VERIFYING.
    public boolean isCorePhase() {
        return currentPhase != PhaseOutputStream.Phase.PRE_SIGNING
                && currentPhase != PhaseOutputStream.Phase.POST_VERIFYING;
    }

    public Phase currentPhase() {
        return currentPhase;
    }

    @Override
    public void write(int b) throws IOException {
        OutputStream output = phaseOut();
        if (output != null) {
            output.write(b);
        }
    }

    @Override
    public void write(byte[] b) throws IOException {
        OutputStream output = phaseOut();
        if (output != null) {
            output.write(b);
        }
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        OutputStream output = phaseOut();
        if (output != null) {
            output.write(b, off, len);
        }
    }

    public void write(String str) throws IOException {
        write(str.getBytes());
    }

    private OutputStream phaseOut() throws FileNotFoundException {
        switch (currentPhase) {
        case SIGNING:
            return signingOut == null
                    ? signingOut = createOutput(Phase.SIGNING)
                    : signingOut;
        case VERIFYING:
            return verifyingOut == null
                    ? verifyingOut = createOutput(Phase.VERIFYING)
                    : verifyingOut;
        case DELAY_VERIFYING:
            return delayVerifyingOut == null
                    ? delayVerifyingOut = createOutput(Phase.DELAY_VERIFYING)
                    : delayVerifyingOut;
        default:
            return null;
        }
    }

    @Override
    public void flush() throws IOException {
        flush(signingOut);
        flush(verifyingOut);
        flush(delayVerifyingOut);
    }

    private void flush(OutputStream output) throws IOException {
        if (output != null) {
            output.flush();
        }
    }

    @Override
    public void close() throws IOException {
        close(signingOut);
        close(verifyingOut);
        close(delayVerifyingOut);
    }

    private void close(OutputStream output) throws IOException {
        if (output != null) {
            output.close();
        }
    }

    private static OutputStream createOutput(Phase phase)
            throws FileNotFoundException {
        return new FileOutputStream(fileName(phase), true);
    }

    public static String fileName(Phase phase) {
        return phase.name() + ".html";
    }
}
