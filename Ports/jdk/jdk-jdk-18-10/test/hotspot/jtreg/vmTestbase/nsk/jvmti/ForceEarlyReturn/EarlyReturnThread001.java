/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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
package nsk.jvmti.ForceEarlyReturn;

import nsk.share.Wicket;
import java.io.PrintStream;
import nsk.share.Consts;
import java.util.concurrent.locks.ReentrantLock;


enum MethodType { Object, Int, Long, Float, Double, Void };

public class EarlyReturnThread001 extends Thread {
    public volatile int state = Consts.TEST_PASSED;
    public volatile boolean earlyReturned = true;
    public volatile boolean stop = false;

    private boolean complain = true;
    private PrintStream out = System.out;

    // To check whether java.util.concurrent.locks stay locked after forced early return
    private ReentrantLock lock = new ReentrantLock();

    public Wicket startingBarrier = new Wicket();
    public Object barrier = new Object();

    private static Object ordinaryResult = new Object();

    private MethodType methodType;

    public void stopThread() {
        stop = true;
    }

    public EarlyReturnThread001 (
            MethodType _mtype
            , PrintStream _out
            )
    {
        methodType = _mtype;
        out = _out;
    }

    public void run() {

        switch(methodType) {
            case Object:
                if (methodObject() == ordinaryResult) {
                    state = Consts.TEST_FAILED;
                }
                break;
            case Int:
                if (methodInt() == 0) {
                    state = Consts.TEST_FAILED;
                }
                break;
            case Long:
                if (methodLong() == 0) {
                    state = Consts.TEST_FAILED;
                }
                break;
            case Float:
                if (methodFloat() == 0) {
                    state = Consts.TEST_FAILED;
                }
                break;
            case Double:
                if (methodDouble() == 0) {
                    state = Consts.TEST_FAILED;
                }
                break;
            case Void:
                methodVoid();
                break;
        }

        // Check whether any errors were encountered during thread execution
        if (state != Consts.TEST_PASSED) {
            out.println("Invoked method wasn't forced to early return, since it returned predefined value.");
        }

        // Check whether locks stay locked
        if (!lock.isLocked()) {
            out.println("java.util.concurrent.lock.ReentrantLock was released after method force early return.");
            state = Consts.TEST_FAILED;
        }

    }

    public Object methodObject() {
        try {
            try {
                try {
                    try {
                        try {
                            synchronized(barrier) {
                                lock.lock();
                                startingBarrier.unlock();
                            }

                            // loop until the main thread forces early return
                            int i = 0; int n = 1000;
                            while (!stop) {
                                if (n <= 0) { n = 1000; }
                                if (i > n) { i = 0; n--; }
                                i++;
                            }

                            if (earlyReturned) {
                                out.println("TEST FAILED: a tested frame wasn't returned");
                                state = Consts.TEST_FAILED;
                                complain = false;
                            }

                        } finally { if (complain) { out.println("!!! Try-Finally block was executed !!!"); state = Consts.TEST_FAILED; } }
                    } finally { if (complain) { out.println("!!! Try-Finally block was executed !!!"); state = Consts.TEST_FAILED; } }
                } finally { if (complain) { out.println("!!! Try-Finally block was executed !!!"); state = Consts.TEST_FAILED; } }
            } finally { if (complain) { out.println("!!! Try-Finally block was executed !!!"); state = Consts.TEST_FAILED; } }
        } finally { if (complain) { out.println("!!! Try-Finally block was executed !!!"); state = Consts.TEST_FAILED; } }

        return ordinaryResult;
    }

    public int methodInt() {
        try {
            try {
                try {
                    try {
                        try {
                            synchronized(barrier) {
                                lock.lock();
                                startingBarrier.unlock();
                            }

                            // loop until the main thread forces early return
                            int i = 0; int n = 1000;
                            while (!stop) {
                                if (n <= 0) { n = 1000; }
                                if (i > n) { i = 0; n--; }
                                i++;
                            }

                            if (earlyReturned) {
                                out.println("TEST FAILED: a tested frame wasn't returned");
                                state = Consts.TEST_FAILED;
                                complain = false;
                            }

                        } finally { if (complain) { out.println("!!! Try-Finally block was executed !!!"); state = Consts.TEST_FAILED; } }
                    } finally { if (complain) { out.println("!!! Try-Finally block was executed !!!"); state = Consts.TEST_FAILED; } }
                } finally { if (complain) { out.println("!!! Try-Finally block was executed !!!"); state = Consts.TEST_FAILED; } }
            } finally { if (complain) { out.println("!!! Try-Finally block was executed !!!"); state = Consts.TEST_FAILED; } }
        } finally { if (complain) { out.println("!!! Try-Finally block was executed !!!"); state = Consts.TEST_FAILED; } }

        return 0;
    }

    public long methodLong() {
        try {
            try {
                try {
                    try {
                        try {
                            synchronized(barrier) {
                                lock.lock();
                                startingBarrier.unlock();
                            }

                            // loop until the main thread forces early return
                            int i = 0; int n = 1000;
                            while (!stop) {
                                if (n <= 0) { n = 1000; }
                                if (i > n) { i = 0; n--; }
                                i++;
                            }

                            if (earlyReturned) {
                                out.println("TEST FAILED: a tested frame wasn't returned");
                                state = Consts.TEST_FAILED;
                                complain = false;
                            }

                        } finally { if (complain) { out.println("!!! Try-Finally block was executed !!!"); state = Consts.TEST_FAILED; } }
                    } finally { if (complain) { out.println("!!! Try-Finally block was executed !!!"); state = Consts.TEST_FAILED; } }
                } finally { if (complain) { out.println("!!! Try-Finally block was executed !!!"); state = Consts.TEST_FAILED; } }
            } finally { if (complain) { out.println("!!! Try-Finally block was executed !!!"); state = Consts.TEST_FAILED; } }
        } finally { if (complain) { out.println("!!! Try-Finally block was executed !!!"); state = Consts.TEST_FAILED; } }

        return 0;
    }

    public float methodFloat() {
        try {
            try {
                try {
                    try {
                        try {
                            synchronized(barrier) {
                                lock.lock();
                                startingBarrier.unlock();
                            }

                            // loop until the main thread forces early return
                            int i = 0; int n = 1000;
                            while (!stop) {
                                if (n <= 0) { n = 1000; }
                                if (i > n) { i = 0; n--; }
                                i++;
                            }

                            if (earlyReturned) {
                                out.println("TEST FAILED: a tested frame wasn't returned");
                                state = Consts.TEST_FAILED;
                                complain = false;
                            }

                        } finally { if (complain) { out.println("!!! Try-Finally block was executed !!!"); state = Consts.TEST_FAILED; } }
                    } finally { if (complain) { out.println("!!! Try-Finally block was executed !!!"); state = Consts.TEST_FAILED; } }
                } finally { if (complain) { out.println("!!! Try-Finally block was executed !!!"); state = Consts.TEST_FAILED; } }
            } finally { if (complain) { out.println("!!! Try-Finally block was executed !!!"); state = Consts.TEST_FAILED; } }
        } finally { if (complain) { out.println("!!! Try-Finally block was executed !!!"); state = Consts.TEST_FAILED; } }

        return 0;
    }

    public double methodDouble() {
        try {
            try {
                try {
                    try {
                        try {
                            synchronized(barrier) {
                                lock.lock();
                                startingBarrier.unlock();
                            }

                            // loop until the main thread forces early return
                            int i = 0; int n = 1000;
                            while (!stop) {
                                if (n <= 0) { n = 1000; }
                                if (i > n) { i = 0; n--; }
                                i++;
                            }

                            if (earlyReturned) {
                                out.println("TEST FAILED: a tested frame wasn't returned");
                                state = Consts.TEST_FAILED;
                                complain = false;
                            }

                        } finally { if (complain) { out.println("!!! Try-Finally block was executed !!!"); state = Consts.TEST_FAILED; } }
                    } finally { if (complain) { out.println("!!! Try-Finally block was executed !!!"); state = Consts.TEST_FAILED; } }
                } finally { if (complain) { out.println("!!! Try-Finally block was executed !!!"); state = Consts.TEST_FAILED; } }
            } finally { if (complain) { out.println("!!! Try-Finally block was executed !!!"); state = Consts.TEST_FAILED; } }
        } finally { if (complain) { out.println("!!! Try-Finally block was executed !!!"); state = Consts.TEST_FAILED; } }

        return 0;
    }

    public void methodVoid() {
        try {
            try {
                try {
                    try {
                        try {
                            synchronized(barrier) {
                                lock.lock();
                                startingBarrier.unlock();
                            }

                            // loop until the main thread forces early return
                            int i = 0; int n = 1000;
                            while (!stop) {
                                if (n <= 0) { n = 1000; }
                                if (i > n) { i = 0; n--; }
                                i++;
                            }

                            if (earlyReturned) {
                                out.println("TEST FAILED: a tested frame wasn't returned");
                                state = Consts.TEST_FAILED;
                                complain = false;
                            }

                        } finally { if (complain) { out.println("!!! Try-Finally block was executed !!!"); state = Consts.TEST_FAILED; } }
                    } finally { if (complain) { out.println("!!! Try-Finally block was executed !!!"); state = Consts.TEST_FAILED; } }
                } finally { if (complain) { out.println("!!! Try-Finally block was executed !!!"); state = Consts.TEST_FAILED; } }
            } finally { if (complain) { out.println("!!! Try-Finally block was executed !!!"); state = Consts.TEST_FAILED; } }
        } finally { if (complain) { out.println("!!! Try-Finally block was executed !!!"); state = Consts.TEST_FAILED; } }
    }
}
