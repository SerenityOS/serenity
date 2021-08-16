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

package jdk.test.lib.process;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public interface OutputBuffer {
  public static class OutputBufferException extends RuntimeException {
    private static final long serialVersionUID = 8528687792643129571L;

    public OutputBufferException(Throwable cause) {
      super(cause);
    }
  }

  /**
   * Returns the stdout result
   *
   * @return stdout result
   */
  public String getStdout();

  /**
   * Returns the stdout as a list.
   * Empty List if application produced no output.
   */
  default public List<String> getStdoutAsList() {
    return new BufferedReader(new StringReader(getStdout()))
            .lines()
            .collect(Collectors.toList());
  }

  /**
   * Returns the stderr result
   *
   * @return stderr result
   */
  public String getStderr();
  public int getExitValue();

  /**
   * Returns the pid if available
   *
   * @return pid
   */
  public long pid();

  public static OutputBuffer of(Process p, Charset cs) {
    return new LazyOutputBuffer(p, cs);
  }

  public static OutputBuffer of(Process p) {
    return new LazyOutputBuffer(p, null);
  }

  public static OutputBuffer of(String stdout, String stderr, int exitValue) {
    return new EagerOutputBuffer(stdout, stderr, exitValue);
  }

  public static OutputBuffer of(String stdout, String stderr) {
    return of(stdout, stderr, -1);
  }

  class LazyOutputBuffer implements OutputBuffer {
    private static class StreamTask {
      private final ByteArrayOutputStream buffer;
      private final Future<Void> future;
      private final Charset cs;

      private StreamTask(InputStream stream, Charset cs) {
        this.buffer = new ByteArrayOutputStream();
        this.cs = cs;
        this.future = new StreamPumper(stream, buffer).process();
      }

      public String get() {
        try {
          future.get();
          return cs == null ? buffer.toString() : buffer.toString(cs);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          throw new OutputBufferException(e);
        } catch (ExecutionException | CancellationException e) {
          throw new OutputBufferException(e);
        }
      }
    }

    private final StreamTask outTask;
    private final StreamTask errTask;
    private final Process p;

    private final void logProgress(String state) {
        System.out.println("[" + Instant.now().toString() + "] " + state
                           + " for process " + p.pid());
        System.out.flush();
    }

    private LazyOutputBuffer(Process p, Charset cs) {
      this.p = p;
      logProgress("Gathering output");
      outTask = new StreamTask(p.getInputStream(), cs);
      errTask = new StreamTask(p.getErrorStream(), cs);
    }

    @Override
    public String getStdout() {
      return outTask.get();
    }

    @Override
    public String getStderr() {
      return errTask.get();
    }

    @Override
    public int getExitValue() {
      try {
          logProgress("Waiting for completion");
          boolean aborted = true;
          try {
              int result = p.waitFor();
              logProgress("Waiting for completion finished");
              aborted = false;
              return result;
          } finally {
              if (aborted) {
                  logProgress("Waiting for completion FAILED");
              }
          }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new OutputBufferException(e);
      }
    }

    @Override
    public long pid() {
      return p.pid();
    }
  }

  class EagerOutputBuffer implements OutputBuffer {
    private final String stdout;
    private final String stderr;
    private final int exitValue;

    private EagerOutputBuffer(String stdout, String stderr, int exitValue) {
      this.stdout = stdout;
      this.stderr = stderr;
      this.exitValue = exitValue;
    }

    @Override
    public String getStdout() {
      return stdout;
    }

    @Override
    public String getStderr() {
      return stderr;
    }

    @Override
    public int getExitValue() {
      return exitValue;
    }

    @Override
    public long pid() {
      throw new RuntimeException("no process");
    }
  }
}
