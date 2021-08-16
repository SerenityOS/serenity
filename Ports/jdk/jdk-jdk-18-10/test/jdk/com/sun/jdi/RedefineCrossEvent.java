/*
 * Copyright (c) 2002, 2018, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 4628726
 * @summary Test class redefinition at each event cross tested with other tests
 * @author Robert Field
 *
 * @run build TestScaffold VMConnection TargetAdapter TargetListener
 * @run compile -g AccessSpecifierTest.java
 * @run compile -g AfterThreadDeathTest.java
 * @run compile -g ArrayRangeTest.java
 * @run compile -g BacktraceFieldTest.java
 * @run compile -g ClassesByName2Test.java
 * @run compile -g DebuggerThreadTest.java
 * @run compile -g DeleteEventRequestsTest.java
 * @run compile -g ExceptionEvents.java
 * @run compile -g ExpiredRequestDeletionTest.java
 * @run compile -g FieldWatchpoints.java
 * @run build InstanceFilter
 * @run compile -g LocationTest.java
 * @run compile -g NewInstanceTest.java
 * @run compile -g PopSynchronousTest.java
 * @run compile -g RepStepTarg.java
 * @run compile -g RequestReflectionTest.java
 *
 * @run driver AccessSpecifierTest -redefstart -redefevent
 * @run driver AfterThreadDeathTest -redefstart -redefevent
 * @run driver ArrayRangeTest -redefstart -redefevent
 * @run driver BacktraceFieldTest -redefstart -redefevent
 * @run driver ClassesByName2Test -redefstart -redefevent
 * @run driver DebuggerThreadTest -redefstart -redefevent
 * @run driver DeleteEventRequestsTest -redefstart -redefevent
 * @run driver ExceptionEvents -redefstart -redefevent N A StackOverflowCaughtTarg java.lang.Exception
 * @run driver ExceptionEvents -redefstart -redefevent C A StackOverflowCaughtTarg null
 * @run driver ExceptionEvents -redefstart -redefevent C A StackOverflowCaughtTarg java.lang.StackOverflowError
 * @run driver ExceptionEvents -redefstart -redefevent N A StackOverflowCaughtTarg java.lang.NullPointerException
 * @run driver ExceptionEvents -redefstart -redefevent C T StackOverflowCaughtTarg java.lang.Error
 * @run driver ExceptionEvents -redefstart -redefevent N T StackOverflowCaughtTarg java.lang.NullPointerException
 * @run driver ExceptionEvents -redefstart -redefevent N N StackOverflowCaughtTarg java.lang.Exception
 * @run driver ExceptionEvents -redefstart -redefevent C N StackOverflowCaughtTarg java.lang.Error
 * @run driver ExceptionEvents -redefstart -redefevent N A StackOverflowUncaughtTarg java.lang.Exception
 * @run driver ExpiredRequestDeletionTest -redefstart -redefevent
 * @run driver FieldWatchpoints -redefstart -redefevent
 * @run driver InstanceFilter -redefstart -redefevent
 * @run driver LocationTest -redefstart -redefevent
 * @run driver NewInstanceTest -redefstart -redefevent
 * @run driver RequestReflectionTest -redefstart -redefevent
 */
