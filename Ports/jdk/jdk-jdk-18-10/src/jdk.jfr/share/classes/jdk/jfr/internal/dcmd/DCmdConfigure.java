/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package jdk.jfr.internal.dcmd;



import jdk.jfr.FlightRecorder;
import jdk.jfr.internal.LogLevel;
import jdk.jfr.internal.LogTag;
import jdk.jfr.internal.Logger;
import jdk.jfr.internal.Options;
import jdk.jfr.internal.PrivateAccess;
import jdk.jfr.internal.Repository;
import jdk.jfr.internal.SecuritySupport.SafePath;

/**
 * JFR.configure - invoked from native
 *
 */
//Instantiated by native
final class DCmdConfigure extends AbstractDCmd {
    /**
     * Execute JFR.configure.
     *
     * @param repositoryPath the path
     * @param dumpPath path to dump to on fatal error (oom)
     * @param stackDepth depth of stack traces
     * @param globalBufferCount number of global buffers
     * @param globalBufferSize size of global buffers
     * @param threadBufferSize size of thread buffer for events
     * @param maxChunkSize threshold at which a new chunk is created in the disk repository
     * @param sampleThreads if thread sampling should be enabled
     *
     * @return result

     * @throws DCmdException
     *             if the dump could not be completed
     */
    public String[] execute
    (
            boolean verbose,
            String repositoryPath,
            String dumpPath,
            Integer stackDepth,
            Long globalBufferCount,
            Long globalBufferSize,
            Long threadBufferSize,
            Long memorySize,
            Long maxChunkSize,
            Boolean sampleThreads

    ) throws DCmdException {
        if (Logger.shouldLog(LogTag.JFR_DCMD, LogLevel.DEBUG)) {
            Logger.log(LogTag.JFR_DCMD, LogLevel.DEBUG, "Executing DCmdConfigure: repositorypath=" + repositoryPath +
                    ", dumppath=" + dumpPath +
                    ", stackdepth=" + stackDepth +
                    ", globalbuffercount=" + globalBufferCount +
                    ", globalbuffersize=" + globalBufferSize +
                    ", thread_buffer_size=" + threadBufferSize +
                    ", memorysize=" + memorySize +
                    ", maxchunksize=" + maxChunkSize +
                    ", samplethreads=" + sampleThreads);
        }


        boolean updated = false;
        if (repositoryPath != null) {
            try {
                SafePath s = new SafePath(repositoryPath);
                if (FlightRecorder.isInitialized()) {
                    PrivateAccess.getInstance().getPlatformRecorder().migrate(s);
                } else {
                    Repository.getRepository().setBasePath(s);
                }
                Logger.log(LogTag.JFR, LogLevel.INFO, "Base repository path set to " + repositoryPath);
            } catch (Exception e) {
                throw new DCmdException("Could not use " + repositoryPath + " as repository. " + e.getMessage(), e);
            }
            if (verbose) {
                printRepositoryPath();
            }
            updated = true;
        }

        if (dumpPath != null)  {
            Options.setDumpPath(new SafePath(dumpPath));
            Logger.log(LogTag.JFR, LogLevel.INFO, "Emergency dump path set to " + dumpPath);
           if (verbose) {
               printDumpPath();
           }
            updated = true;
        }

        if (stackDepth != null)  {
            Options.setStackDepth(stackDepth);
            Logger.log(LogTag.JFR, LogLevel.INFO, "Stack depth set to " + stackDepth);
            if (verbose) {
                printStackDepth();
            }
            updated = true;
        }

        if (globalBufferCount != null)  {
            Options.setGlobalBufferCount(globalBufferCount);
            Logger.log(LogTag.JFR, LogLevel.INFO, "Global buffer count set to " + globalBufferCount);
            if (verbose) {
                printGlobalBufferCount();
            }
            updated = true;
        }

        if (globalBufferSize != null)  {
            Options.setGlobalBufferSize(globalBufferSize);
            Logger.log(LogTag.JFR, LogLevel.INFO, "Global buffer size set to " + globalBufferSize);
            if (verbose) {
                printGlobalBufferSize();
            }
            updated = true;
        }

        if (threadBufferSize != null)  {
            Options.setThreadBufferSize(threadBufferSize);
            Logger.log(LogTag.JFR, LogLevel.INFO, "Thread buffer size set to " + threadBufferSize);
            if (verbose) {
                printThreadBufferSize();
            }
            updated = true;
        }

        if (memorySize != null) {
            Options.setMemorySize(memorySize);
            Logger.log(LogTag.JFR, LogLevel.INFO, "Memory size set to " + memorySize);
            if (verbose) {
                printMemorySize();
            }
            updated = true;
        }

        if (maxChunkSize != null)  {
            Options.setMaxChunkSize(maxChunkSize);
            Logger.log(LogTag.JFR, LogLevel.INFO, "Max chunk size set to " + maxChunkSize);
            if (verbose) {
                printMaxChunkSize();
            }
            updated = true;
        }

        if (sampleThreads != null)  {
            Options.setSampleThreads(sampleThreads);
            Logger.log(LogTag.JFR, LogLevel.INFO, "Sample threads set to " + sampleThreads);
            if (verbose) {
                printSampleThreads();
            }
            updated = true;
        }
        if (!verbose) {
            return new String[0];
        }
        if (!updated) {
            println("Current configuration:");
            println();
            printRepositoryPath();
            printStackDepth();
            printGlobalBufferCount();
            printGlobalBufferSize();
            printThreadBufferSize();
            printMemorySize();
            printMaxChunkSize();
            printSampleThreads();
        }
        return getResult();
    }

    private void printRepositoryPath() {
        print("Repository path: ");
        printPath(Repository.getRepository().getRepositoryPath());
        println();
    }

    private void printDumpPath() {
        print("Dump path: ");
        printPath(Options.getDumpPath());
        println();
    }

    private void printSampleThreads() {
        println("Sample threads: " + Options.getSampleThreads());
    }

    private void printStackDepth() {
        println("Stack depth: " +  Options.getStackDepth());
    }

    private void printGlobalBufferCount() {
        println("Global buffer count: " +  Options.getGlobalBufferCount());
    }

    private void printGlobalBufferSize() {
        print("Global buffer size: ");
        printBytes(Options.getGlobalBufferSize());
        println();
    }

    private void printThreadBufferSize() {
        print("Thread buffer size: ");
        printBytes(Options.getThreadBufferSize());
        println();
    }

    private void printMemorySize() {
        print("Memory size: ");
        printBytes(Options.getMemorySize());
        println();
    }

    private void printMaxChunkSize() {
        print("Max chunk size: ");
        printBytes(Options.getMaxChunkSize());
        println();
    }

    @Override
    public String[] printHelp() {
        throw new InternalError("Should not reach here!");
    }

    @Override
    public Argument[] getArgumentInfos() {
        throw new InternalError("Should not reach here!");
    }

    @Override
    protected void execute(ArgumentParser parser) throws DCmdException {
        throw new InternalError("Should not reach here!");
    }
}
