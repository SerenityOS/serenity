/*
 * Copyright (c) 2002-2016, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package jdk.internal.org.jline.utils;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.Objects;

/**
 * Helper methods for running unix commands.
 */
public final class ExecHelper {

    private ExecHelper() {
    }

    public static String exec(boolean redirectInput, final String... cmd) throws IOException {
        Objects.requireNonNull(cmd);
        try {
            Log.trace("Running: ", cmd);
            ProcessBuilder pb = new ProcessBuilder(cmd);
            if (OSUtils.IS_AIX) {
                Map<String,String> env = pb.environment();
                env.put("PATH", "/opt/freeware/bin:" + env.get("PATH"));
                env.put("LANG", "C");
                env.put("LC_ALL", "C");
            }
            if (redirectInput) {
                pb.redirectInput(ProcessBuilder.Redirect.INHERIT);
            }
            Process p = pb.start();
            String result = waitAndCapture(p);
            Log.trace("Result: ", result);
            if (p.exitValue() != 0) {
                if (result.endsWith("\n")) {
                    result = result.substring(0, result.length() - 1);
                }
                throw new IOException("Error executing '" + String.join(" ", (CharSequence[]) cmd) + "': " + result);
            }
            return result;
        } catch (InterruptedException e) {
            throw (IOException) new InterruptedIOException("Command interrupted").initCause(e);
        }
    }

    public static String waitAndCapture(Process p) throws IOException, InterruptedException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        InputStream in = null;
        InputStream err = null;
        OutputStream out = null;
        try {
            int c;
            in = p.getInputStream();
            while ((c = in.read()) != -1) {
                bout.write(c);
            }
            err = p.getErrorStream();
            while ((c = err.read()) != -1) {
                bout.write(c);
            }
            out = p.getOutputStream();
            p.waitFor();
        } finally {
            close(in, out, err);
        }

        return bout.toString();
    }

    private static void close(final Closeable... closeables) {
        for (Closeable c : closeables) {
            if (c != null) {
                try {
                    c.close();
                } catch (Exception e) {
                    // Ignore
                }
            }
        }
    }
}

