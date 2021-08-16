/*
 * Copyright (c) 2009, 2010, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6911256 6964740
 * @summary Tests of generated TWR code.
 */

import java.util.List;
import java.util.ArrayList;

public class TwrTests {
    public static void main(String[] args) {
        testCreateFailure1();
        testCreateFailure2();
        testCreateFailure2Nested();
        testCreateFailure3();
        testCreateFailure3Nested();
        testCreateFailure4();
        testCreateFailure4Nested();
        testCreateFailure5();
        testCreateFailure5Nested();

        testCreateSuccess1();
        testCreateSuccess2();
        testCreateSuccess2Nested();
        testCreateSuccess3();
        testCreateSuccess3Nested();
        testCreateSuccess4();
        testCreateSuccess4Nested();
        testCreateSuccess5();
        testCreateSuccess5Nested();
    }

    /*
     * The following tests simulate a creation failure of every possible
     * resource in an TWR block, and check to make sure that the failure
     * prevents creation of subsequent resources, and that all created
     * resources are properly closed, even if one or more of the close
     * attempts fails.
     */

    public static void testCreateFailure1() {
        int creationFailuresDetected = 0;
        List<Integer> closedList = new ArrayList<Integer>(0);
        try (Resource r0 = createResource(0, 0, 0, closedList)) {
            throw new AssertionError("Resource creation succeeded");
        } catch (Resource.CreateFailException e) {
            creationFailuresDetected++;
            if (e.resourceId() != 0) {
                throw new AssertionError("Wrong resource creation "
                                         + e.resourceId() + " failed");
            }
        } catch (Resource.CloseFailException e) {
            throw new AssertionError("Unexpected CloseFailException: " + e.resourceId());
        }
        checkForSingleCreationFailure(creationFailuresDetected);
        checkClosedList(closedList, 0);
    }

    public static void testCreateFailure2() {
        for (int createFailureId = 0; createFailureId < 2; createFailureId++) {
            for (int bitMap = 0, n = 1 << createFailureId; bitMap < n; bitMap++) {
                int creationFailuresDetected = 0;
                List<Integer> closedList = new ArrayList<Integer>();
                try (Resource r0 = createResource(0, createFailureId, bitMap, closedList);
                     Resource r1 = createResource(1, createFailureId, bitMap, closedList)) {
                    throw new AssertionError("Entire resource creation succeeded");
                } catch (Resource.CreateFailException e) {
                    creationFailuresDetected++;
                    checkCreateFailureId(e.resourceId(), createFailureId);
                    checkSuppressedExceptions(e.getSuppressed(), bitMap);
                } catch (Resource.CloseFailException e) {
                    throw new AssertionError("Secondary exception suppression failed");
                }
                checkForSingleCreationFailure(creationFailuresDetected);
                checkClosedList(closedList, createFailureId);
            }
        }
    }

    public static void testCreateFailure2Nested() {
        for (int createFailureId = 0; createFailureId < 2; createFailureId++) {
            for (int bitMap = 0, n = 1 << createFailureId; bitMap < n; bitMap++) {
                int creationFailuresDetected = 0;
                List<Integer> closedList = new ArrayList<Integer>();
                try (Resource r0 = createResource(0, createFailureId, bitMap, closedList)) {
                    try(Resource r1 = createResource(1, createFailureId, bitMap, closedList)) {
                        throw new AssertionError("Entire resource creation succeeded");
                    }
                } catch (Resource.CreateFailException e) {
                    creationFailuresDetected++;
                    checkCreateFailureId(e.resourceId(), createFailureId);
                    checkSuppressedExceptions(e.getSuppressed(), bitMap);
                } catch (Resource.CloseFailException e) {
                    throw new AssertionError("Secondary exception suppression failed");
                }
                checkForSingleCreationFailure(creationFailuresDetected);
                checkClosedList(closedList, createFailureId);
            }
        }
    }

    public static void testCreateFailure3() {
        for (int createFailureId = 0; createFailureId < 3; createFailureId++) {
            for (int bitMap = 0, n = 1 << createFailureId; bitMap < n; bitMap++) {
                int creationFailuresDetected = 0;
                List<Integer> closedList = new ArrayList<Integer>();
                try (Resource r0 = createResource(0, createFailureId, bitMap, closedList);
                     Resource r1 = createResource(1, createFailureId, bitMap, closedList);
                     Resource r2 = createResource(2, createFailureId, bitMap, closedList)) {
                    throw new AssertionError("Entire resource creation succeeded");
                } catch (Resource.CreateFailException e) {
                    creationFailuresDetected++;
                    checkCreateFailureId(e.resourceId(), createFailureId);
                    checkSuppressedExceptions(e.getSuppressed(), bitMap);
                } catch (Resource.CloseFailException e) {
                    throw new AssertionError("Secondary exception suppression failed:" + e);
                }
                checkForSingleCreationFailure(creationFailuresDetected);
                checkClosedList(closedList, createFailureId);
            }
        }
    }

    public static void testCreateFailure3Nested() {
        for (int createFailureId = 0; createFailureId < 3; createFailureId++) {
            for (int bitMap = 0, n = 1 << createFailureId; bitMap < n; bitMap++) {
                int creationFailuresDetected = 0;
                List<Integer> closedList = new ArrayList<Integer>();
                try (Resource r0 = createResource(0, createFailureId, bitMap, closedList)) {
                    try (Resource r1 = createResource(1, createFailureId, bitMap, closedList)) {
                        try (Resource r2 = createResource(2, createFailureId, bitMap, closedList)) {
                            throw new AssertionError("Entire resource creation succeeded");
                        }
                    }
                } catch (Resource.CreateFailException e) {
                    creationFailuresDetected++;
                    checkCreateFailureId(e.resourceId(), createFailureId);
                    checkSuppressedExceptions(e.getSuppressed(), bitMap);
                } catch (Resource.CloseFailException e) {
                    throw new AssertionError("Secondary exception suppression failed:" + e);
                }
                checkForSingleCreationFailure(creationFailuresDetected);
                checkClosedList(closedList, createFailureId);
            }
        }
    }

    public static void testCreateFailure4() {
        for (int createFailureId = 0; createFailureId < 4; createFailureId++) {
            for (int bitMap = 0, n = 1 << createFailureId; bitMap < n; bitMap++) {
                int creationFailuresDetected = 0;
                List<Integer> closedList = new ArrayList<Integer>();
                try (Resource r0 = createResource(0, createFailureId, bitMap, closedList);
                     Resource r1 = createResource(1, createFailureId, bitMap, closedList);
                     Resource r2 = createResource(2, createFailureId, bitMap, closedList);
                     Resource r3 = createResource(3, createFailureId, bitMap, closedList)) {
                    throw new AssertionError("Entire resource creation succeeded");
                } catch (Resource.CreateFailException e) {
                    creationFailuresDetected++;
                    checkCreateFailureId(e.resourceId(), createFailureId);
                    checkSuppressedExceptions(e.getSuppressed(), bitMap);
                } catch (Resource.CloseFailException e) {
                    throw new AssertionError("Secondary exception suppression failed:" + e);
                }
                checkForSingleCreationFailure(creationFailuresDetected);
                checkClosedList(closedList, createFailureId);
            }
        }
    }

    public static void testCreateFailure4Nested() {
        for (int createFailureId = 0; createFailureId < 4; createFailureId++) {
            for (int bitMap = 0, n = 1 << createFailureId; bitMap < n; bitMap++) {
                int creationFailuresDetected = 0;
                List<Integer> closedList = new ArrayList<Integer>();
                try (Resource r0 = createResource(0, createFailureId, bitMap, closedList)) {
                    try (Resource r1 = createResource(1, createFailureId, bitMap, closedList)) {
                        try (Resource r2 = createResource(2, createFailureId, bitMap, closedList)) {
                            try (Resource r3 = createResource(3, createFailureId, bitMap, closedList)) {
                                throw new AssertionError("Entire resource creation succeeded");
                            }
                        }
                    }
                } catch (Resource.CreateFailException e) {
                    creationFailuresDetected++;
                    checkCreateFailureId(e.resourceId(), createFailureId);
                    checkSuppressedExceptions(e.getSuppressed(), bitMap);
                } catch (Resource.CloseFailException e) {
                    throw new AssertionError("Secondary exception suppression failed:" + e);
                }
                checkForSingleCreationFailure(creationFailuresDetected);
                checkClosedList(closedList, createFailureId);
            }
        }
    }

    public static void testCreateFailure5() {
        for (int createFailureId = 0; createFailureId < 5; createFailureId++) {
            for (int bitMap = 0, n = 1 << createFailureId; bitMap < n; bitMap++) {
                int creationFailuresDetected = 0;
                List<Integer> closedList = new ArrayList<Integer>();
                try (Resource r0 = createResource(0, createFailureId, bitMap, closedList);
                     Resource r1 = createResource(1, createFailureId, bitMap, closedList);
                     Resource r2 = createResource(2, createFailureId, bitMap, closedList);
                     Resource r3 = createResource(3, createFailureId, bitMap, closedList);
                     Resource r4 = createResource(4, createFailureId, bitMap, closedList)) {
                    throw new AssertionError("Entire resource creation succeeded");
                } catch (Resource.CreateFailException e) {
                    creationFailuresDetected++;
                    checkCreateFailureId(e.resourceId(), createFailureId);
                    checkSuppressedExceptions(e.getSuppressed(), bitMap);
                } catch (Resource.CloseFailException e) {
                    throw new AssertionError("Secondary exception suppression failed:" + e);
                }
                checkForSingleCreationFailure(creationFailuresDetected);
                checkClosedList(closedList, createFailureId);
            }
        }
    }

    public static void testCreateFailure5Nested() {
        for (int createFailureId = 0; createFailureId < 5; createFailureId++) {
            for (int bitMap = 0, n = 1 << createFailureId; bitMap < n; bitMap++) {
                int creationFailuresDetected = 0;
                List<Integer> closedList = new ArrayList<Integer>();
                try (Resource r0 = createResource(0, createFailureId, bitMap, closedList)) {
                    try (Resource r1 = createResource(1, createFailureId, bitMap, closedList)) {
                        try (Resource r2 = createResource(2, createFailureId, bitMap, closedList)) {
                            try (Resource r3 = createResource(3, createFailureId, bitMap, closedList)) {
                                try (Resource r4 = createResource(4, createFailureId, bitMap, closedList)) {
                                    throw new AssertionError("Entire resource creation succeeded");
                                }
                            }
                        }
                    }
                } catch (Resource.CreateFailException e) {
                    creationFailuresDetected++;
                    checkCreateFailureId(e.resourceId(), createFailureId);
                    checkSuppressedExceptions(e.getSuppressed(), bitMap);
                } catch (Resource.CloseFailException e) {
                    throw new AssertionError("Secondary exception suppression failed:" + e);
                }
                checkForSingleCreationFailure(creationFailuresDetected);
                checkClosedList(closedList, createFailureId);
            }
        }
    }

    /**
     * Create a resource with the specified ID. The ID must be less than createFailureId.
     * A subsequent attempt to close the resource will fail iff the corresponding bit
     * is set in closeFailureBitMap.  When an attempt is made to close this resource,
     * its ID will be added to closedList, regardless of whether the attempt succeeds.
     *
     * @param id the ID of this resource
     * @param createFailureId the ID of the resource whose creation will fail
     * @param closeFailureBitMap a bit vector describing which resources should throw an
     *        exception when close is attempted
     * @param closedList a list on which to record resource close attempts
     * @throws AssertionError if no attempt should be made to create this resource
     */
    private static Resource createResource(int id,
                                           int createFailureId,
                                           int closeFailureBitMap,
                                           List<Integer> closedList) throws Resource.CreateFailException {
        if (id > createFailureId)
            throw new AssertionError("Resource " + id + " shouldn't be created");
        boolean createSucceeds = id != createFailureId;
        boolean closeSucceeds = (closeFailureBitMap & (1 << id)) == 0;
        return new Resource(id, createSucceeds, closeSucceeds, closedList);
    }


    /**
     * Check that an observed creation failure has the expected resource ID.
     *
     * @param foundId the ID of the resource whose creation failed
     * @param expectedId the ID of the resource whose creation should have failed
     */
    private static void checkCreateFailureId(int foundId, int expectedId) {
        if (foundId != expectedId)
            throw new AssertionError("Wrong resource creation failed. Found ID "
                                     + foundId + " expected " + expectedId);
    }

    /**
     * Check for proper suppressed exceptions in proper order.
     *
     * @param suppressedExceptions the suppressed exceptions array returned by
     *        getSuppressed()
     * @bitmap a bitmap indicating which suppressed exceptions are expected.
     *         Bit i is set iff id should throw a CloseFailException.
     */
    private static void checkSuppressedExceptions(Throwable[] suppressedExceptions, int bitMap) {
        if (suppressedExceptions.length != Integer.bitCount(bitMap))
            throw new AssertionError("Expected " + Integer.bitCount(bitMap)
                                     + " suppressed exceptions, got " +  suppressedExceptions.length);

        int prevCloseFailExceptionId = Integer.MAX_VALUE;
        for (Throwable t : suppressedExceptions) {
            int id = ((Resource.CloseFailException) t).resourceId();
            if ((1 << id  & bitMap) == 0)
                throw new AssertionError("Unexpected suppressed CloseFailException: " + id);
            if (id > prevCloseFailExceptionId)
                throw new AssertionError("Suppressed CloseFailException" + id
                                         + " followed " + prevCloseFailExceptionId);
        }
    }

    /**
     * Check that exactly one resource creation failed.
     *
     * @param numCreationFailuresDetected the number of creation failures detected
     */
    private static void checkForSingleCreationFailure(int numCreationFailuresDetected) {
        if (numCreationFailuresDetected != 1)
            throw new AssertionError("Wrong number of creation failures: "
                                     + numCreationFailuresDetected);
    }

    /**
     * Check that a close was attempted on every resourced that was successfully opened,
     * and that the close attempts occurred in the proper order.
     *
     * @param closedList the resource IDs of the close attempts, in the order they occurred
     * @param the ID of the resource whose creation failed.  Close attempts should occur
     *        for all previous resources, in reverse order.
     */
    private static void checkClosedList(List<Integer> closedList, int createFailureId) {
        List<Integer> expectedList = new ArrayList<Integer>(createFailureId);
        for (int i = createFailureId - 1; i >= 0; i--)
            expectedList.add(i);
        if (!closedList.equals(expectedList))
            throw new AssertionError("Closing sequence " + closedList + " != " + expectedList);
    }

    /*
     * The following tests simulate the creation of several resources, followed
     * by success or failure of forward processing.  They test that all resources
     * are properly closed, even if one or more of the close attempts fails.
     */

    public static void testCreateSuccess1() {
        for (int bitMap = 0, n = 1 << 1; bitMap < n; bitMap++) {
            for (int failure = 0; failure < 2; failure++) {
                List<Integer> closedList = new ArrayList<Integer>();
                try (Resource r0 = createResource(0, bitMap, closedList)) {
                    if (failure != 0)
                        throw new MyKindOfException();
                } catch (Resource.CreateFailException e) {
                    throw new AssertionError(
                                             "Resource creation failed: " + e.resourceId());
                } catch (MyKindOfException e) {
                    if (failure == 0)
                        throw new AssertionError("Unexpected MyKindOfException");
                    checkSuppressedExceptions(e.getSuppressed(), bitMap);
                } catch (Resource.CloseFailException e) {
                    if (failure == 1)
                        throw new AssertionError("Secondary exception suppression failed");
                    int id = e.resourceId();
                    if (bitMap == 0)
                        throw new AssertionError("Unexpected CloseFailException: " + id);
                    int highestCloseFailBit = Integer.highestOneBit(bitMap);
                    if (1 << id != highestCloseFailBit) {
                        throw new AssertionError("CloseFailException: got id " + id
                                                 + ", expected lg(" + highestCloseFailBit +")");
                    }
                    checkSuppressedExceptions(e.getSuppressed(), bitMap & ~highestCloseFailBit);
                }
                checkClosedList(closedList, 1);
            }
        }
    }

    public static void testCreateSuccess2() {
        for (int bitMap = 0, n = 1 << 2; bitMap < n; bitMap++) {
            for (int failure = 0; failure < 2; failure++) {
                List<Integer> closedList = new ArrayList<Integer>();
                try (Resource r0 = createResource(0, bitMap, closedList);
                     Resource r1 = createResource(1, bitMap, closedList)) {
                    if (failure != 0)
                        throw new MyKindOfException();
                } catch (Resource.CreateFailException e) {
                    throw new AssertionError(
                                             "Resource creation failed: " + e.resourceId());
                } catch (MyKindOfException e) {
                    if (failure == 0)
                        throw new AssertionError("Unexpected MyKindOfException");
                    checkSuppressedExceptions(e.getSuppressed(), bitMap);
                } catch (Resource.CloseFailException e) {
                    if (failure == 1)
                        throw new AssertionError("Secondary exception suppression failed");
                    int id = e.resourceId();
                    if (bitMap == 0)
                        throw new AssertionError("Unexpected CloseFailException: " + id);
                    int highestCloseFailBit = Integer.highestOneBit(bitMap);
                    if (1 << id != highestCloseFailBit) {
                        throw new AssertionError("CloseFailException: got id " + id
                                                 + ", expected lg(" + highestCloseFailBit +")");
                    }
                    checkSuppressedExceptions(e.getSuppressed(), bitMap & ~highestCloseFailBit);
                }
                checkClosedList(closedList, 2);
            }
        }
    }

    public static void testCreateSuccess2Nested() {
        for (int bitMap = 0, n = 1 << 2; bitMap < n; bitMap++) {
            for (int failure = 0; failure < 2; failure++) {
                List<Integer> closedList = new ArrayList<Integer>();
                try (Resource r0 = createResource(0, bitMap, closedList)) {
                    try (Resource r1 = createResource(1, bitMap, closedList)) {
                        if (failure != 0)
                            throw new MyKindOfException();
                    }
                } catch (Resource.CreateFailException e) {
                    throw new AssertionError(
                                             "Resource creation failed: " + e.resourceId());
                } catch (MyKindOfException e) {
                    if (failure == 0)
                        throw new AssertionError("Unexpected MyKindOfException");
                    checkSuppressedExceptions(e.getSuppressed(), bitMap);
                } catch (Resource.CloseFailException e) {
                    if (failure == 1)
                        throw new AssertionError("Secondary exception suppression failed");
                    int id = e.resourceId();
                    if (bitMap == 0)
                        throw new AssertionError("Unexpected CloseFailException: " + id);
                    int highestCloseFailBit = Integer.highestOneBit(bitMap);
                    if (1 << id != highestCloseFailBit) {
                        throw new AssertionError("CloseFailException: got id " + id
                                                 + ", expected lg(" + highestCloseFailBit +")");
                    }
                    checkSuppressedExceptions(e.getSuppressed(), bitMap & ~highestCloseFailBit);
                }
                checkClosedList(closedList, 2);
            }
        }
    }

    public static void testCreateSuccess3() {
        for (int bitMap = 0, n = 1 << 3; bitMap < n; bitMap++) {
            for (int failure = 0; failure < 2; failure++) {
                List<Integer> closedList = new ArrayList<Integer>();
                try (Resource r0 = createResource(0, bitMap, closedList);
                     Resource r1 = createResource(1, bitMap, closedList);
                     Resource r2 = createResource(2, bitMap, closedList)) {
                    if (failure != 0)
                        throw new MyKindOfException();
                } catch (Resource.CreateFailException e) {
                    throw new AssertionError(
                                             "Resource creation failed: " + e.resourceId());
                } catch (MyKindOfException e) {
                    if (failure == 0)
                        throw new AssertionError("Unexpected MyKindOfException");
                    checkSuppressedExceptions(e.getSuppressed(), bitMap);
                } catch (Resource.CloseFailException e) {
                    if (failure == 1)
                        throw new AssertionError("Secondary exception suppression failed");
                    int id = e.resourceId();
                    if (bitMap == 0)
                        throw new AssertionError("Unexpected CloseFailException: " + id);
                    int highestCloseFailBit = Integer.highestOneBit(bitMap);
                    if (1 << id != highestCloseFailBit) {
                        throw new AssertionError("CloseFailException: got id " + id
                                                 + ", expected lg(" + highestCloseFailBit +")");
                    }
                    checkSuppressedExceptions(e.getSuppressed(), bitMap & ~highestCloseFailBit);
                }
                checkClosedList(closedList, 3);
            }
        }
    }

    public static void testCreateSuccess3Nested() {
        for (int bitMap = 0, n = 1 << 3; bitMap < n; bitMap++) {
            for (int failure = 0; failure < 2; failure++) {
                List<Integer> closedList = new ArrayList<Integer>();
                try (Resource r0 = createResource(0, bitMap, closedList)) {
                    try (Resource r1 = createResource(1, bitMap, closedList)) {
                        try (Resource r2 = createResource(2, bitMap, closedList)) {
                            if (failure != 0)
                                throw new MyKindOfException();
                        }
                    }
                } catch (Resource.CreateFailException e) {
                    throw new AssertionError(
                                             "Resource creation failed: " + e.resourceId());
                } catch (MyKindOfException e) {
                    if (failure == 0)
                        throw new AssertionError("Unexpected MyKindOfException");
                    checkSuppressedExceptions(e.getSuppressed(), bitMap);
                } catch (Resource.CloseFailException e) {
                    if (failure == 1)
                        throw new AssertionError("Secondary exception suppression failed");
                    int id = e.resourceId();
                    if (bitMap == 0)
                        throw new AssertionError("Unexpected CloseFailException: " + id);
                    int highestCloseFailBit = Integer.highestOneBit(bitMap);
                    if (1 << id != highestCloseFailBit) {
                        throw new AssertionError("CloseFailException: got id " + id
                                                 + ", expected lg(" + highestCloseFailBit +")");
                    }
                    checkSuppressedExceptions(e.getSuppressed(), bitMap & ~highestCloseFailBit);
                }
                checkClosedList(closedList, 3);
            }
        }
    }

    public static void testCreateSuccess4() {
        for (int bitMap = 0, n = 1 << 4; bitMap < n; bitMap++) {
            for (int failure = 0; failure < 2; failure++) {
                List<Integer> closedList = new ArrayList<Integer>();
                try (Resource r0 = createResource(0, bitMap, closedList);
                     Resource r1 = createResource(1, bitMap, closedList);
                     Resource r2 = createResource(2, bitMap, closedList);
                     Resource r3 = createResource(3, bitMap, closedList)) {
                    if (failure != 0)
                        throw new MyKindOfException();
                } catch (Resource.CreateFailException e) {
                    throw new AssertionError(
                                             "Resource creation failed: " + e.resourceId());
                } catch (MyKindOfException e) {
                    if (failure == 0)
                        throw new AssertionError("Unexpected MyKindOfException");
                    checkSuppressedExceptions(e.getSuppressed(), bitMap);
                } catch (Resource.CloseFailException e) {
                    if (failure == 1)
                        throw new AssertionError("Secondary exception suppression failed");
                    int id = e.resourceId();
                    if (bitMap == 0)
                        throw new AssertionError("Unexpected CloseFailException: " + id);
                    int highestCloseFailBit = Integer.highestOneBit(bitMap);
                    if (1 << id != highestCloseFailBit) {
                        throw new AssertionError("CloseFailException: got id " + id
                                                 + ", expected lg(" + highestCloseFailBit +")");
                    }
                    checkSuppressedExceptions(e.getSuppressed(), bitMap & ~highestCloseFailBit);
                }
                checkClosedList(closedList, 4);
            }
        }
    }

    public static void testCreateSuccess4Nested() {
        for (int bitMap = 0, n = 1 << 4; bitMap < n; bitMap++) {
            for (int failure = 0; failure < 2; failure++) {
                List<Integer> closedList = new ArrayList<Integer>();
                try (Resource r0 = createResource(0, bitMap, closedList)) {
                    try (Resource r1 = createResource(1, bitMap, closedList)) {
                        try (Resource r2 = createResource(2, bitMap, closedList)) {
                            try (Resource r3 = createResource(3, bitMap, closedList)) {
                                if (failure != 0)
                                    throw new MyKindOfException();
                            }
                        }
                    }
                } catch (Resource.CreateFailException e) {
                    throw new AssertionError(
                                             "Resource creation failed: " + e.resourceId());
                } catch (MyKindOfException e) {
                    if (failure == 0)
                        throw new AssertionError("Unexpected MyKindOfException");
                    checkSuppressedExceptions(e.getSuppressed(), bitMap);
                } catch (Resource.CloseFailException e) {
                    if (failure == 1)
                        throw new AssertionError("Secondary exception suppression failed");
                    int id = e.resourceId();
                    if (bitMap == 0)
                        throw new AssertionError("Unexpected CloseFailException: " + id);
                    int highestCloseFailBit = Integer.highestOneBit(bitMap);
                    if (1 << id != highestCloseFailBit) {
                        throw new AssertionError("CloseFailException: got id " + id
                                                 + ", expected lg(" + highestCloseFailBit +")");
                    }
                    checkSuppressedExceptions(e.getSuppressed(), bitMap & ~highestCloseFailBit);
                }
                checkClosedList(closedList, 4);
            }
        }
    }

    public static void testCreateSuccess5() {
        for (int bitMap = 0, n = 1 << 5; bitMap < n; bitMap++) {
            for (int failure = 0; failure < 2; failure++) {
                List<Integer> closedList = new ArrayList<Integer>();
                try (Resource r0 = createResource(0, bitMap, closedList);
                     Resource r1 = createResource(1, bitMap, closedList);
                     Resource r2 = createResource(2, bitMap, closedList);
                     Resource r3 = createResource(3, bitMap, closedList);
                     Resource r4 = createResource(4, bitMap, closedList)) {
                    if (failure != 0)
                        throw new MyKindOfException();
                } catch (Resource.CreateFailException e) {
                    throw new AssertionError("Resource creation failed: " + e.resourceId());
                } catch (MyKindOfException e) {
                    if (failure == 0)
                        throw new AssertionError("Unexpected MyKindOfException");
                    checkSuppressedExceptions(e.getSuppressed(), bitMap);
                } catch (Resource.CloseFailException e) {
                    if (failure == 1)
                        throw new AssertionError("Secondary exception suppression failed");
                    int id = e.resourceId();
                    if (bitMap == 0)
                        throw new AssertionError("Unexpected CloseFailException: " + id);
                    int highestCloseFailBit = Integer.highestOneBit(bitMap);
                    if (1 << id != highestCloseFailBit) {
                        throw new AssertionError("CloseFailException: got id " + id
                                                 + ", expected lg(" + highestCloseFailBit +")");
                    }
                    checkSuppressedExceptions(e.getSuppressed(), bitMap & ~highestCloseFailBit);
                }
                checkClosedList(closedList, 5);
            }
        }
    }

    public static void testCreateSuccess5Nested() {
        for (int bitMap = 0, n = 1 << 5; bitMap < n; bitMap++) {
            for (int failure = 0; failure < 2; failure++) {
                List<Integer> closedList = new ArrayList<Integer>();
                try (Resource r0 = createResource(0, bitMap, closedList)) {
                    try (Resource r1 = createResource(1, bitMap, closedList)) {
                        try (Resource r2 = createResource(2, bitMap, closedList)) {
                            try (Resource r3 = createResource(3, bitMap, closedList)) {
                                try (Resource r4 = createResource(4, bitMap, closedList)) {
                                    if (failure != 0)
                                        throw new MyKindOfException();
                                }
                            }
                        }
                    }
                } catch (Resource.CreateFailException e) {
                    throw new AssertionError("Resource creation failed: " + e.resourceId());
                } catch (MyKindOfException e) {
                    if (failure == 0)
                        throw new AssertionError("Unexpected MyKindOfException");
                    checkSuppressedExceptions(e.getSuppressed(), bitMap);
                } catch (Resource.CloseFailException e) {
                    if (failure == 1)
                        throw new AssertionError("Secondary exception suppression failed");
                    int id = e.resourceId();
                    if (bitMap == 0)
                        throw new AssertionError("Unexpected CloseFailException: " + id);
                    int highestCloseFailBit = Integer.highestOneBit(bitMap);
                    if (1 << id != highestCloseFailBit) {
                        throw new AssertionError("CloseFailException: got id " + id
                                                 + ", expected lg(" + highestCloseFailBit +")");
                    }
                    checkSuppressedExceptions(e.getSuppressed(), bitMap & ~highestCloseFailBit);
                }
                checkClosedList(closedList, 5);
            }
        }
    }

    private static Resource createResource(int id,
                                           int closeFailureBitMap,
                                           List<Integer> closedList) throws Resource.CreateFailException {
        boolean closeSucceeds = (closeFailureBitMap & (1 << id)) == 0;
        return new Resource(id, true, closeSucceeds, closedList);
    }

    private static class MyKindOfException extends Exception {
    }
}

class Resource implements AutoCloseable {
    /** A number identifying this resource */
    private final int resourceId;

    /** Whether the close call on this resource should succeed or fail */
    private final boolean closeSucceeds;

    /** When resource is closed, it records its ID in this list */
    private final List<Integer> closedList;

    Resource(int resourceId, boolean createSucceeds, boolean closeSucceeds,
             List<Integer> closedList) throws CreateFailException {
        if (!createSucceeds)
            throw new CreateFailException(resourceId);
        this.resourceId = resourceId;
        this.closeSucceeds = closeSucceeds;
        this.closedList = closedList;
    }

    public void close() throws CloseFailException {
        closedList.add(resourceId);
        if (!closeSucceeds)
            throw new CloseFailException(resourceId);
    }

    public static class ResourceException extends RuntimeException {
        private final int resourceId;

        public ResourceException(int resourceId) {
            super("Resource ID = " + resourceId);
            this.resourceId = resourceId;
        }

        public int resourceId() {
            return resourceId;
        }
    }

    public static class CreateFailException extends ResourceException {
        public CreateFailException(int resourceId) {
            super(resourceId);
        }
    }

    public static class CloseFailException extends ResourceException {
        public CloseFailException(int resourceId) {
            super(resourceId);
        }
    }
}
