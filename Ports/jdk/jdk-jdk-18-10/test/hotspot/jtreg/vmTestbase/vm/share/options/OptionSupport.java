/*
 * Copyright (c) 2008, 2018, Oracle and/or its affiliates. All rights reserved.
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
package vm.share.options;

/**
 * This class actually provides the OptionFramework API
 * via two static {@link OptionSupport#setup} methods.
 * See also "General description" section of the package level documentation.
 */
public class OptionSupport
{
    /**
     * This method parses the commandline arguments, setups the instance fields (annotated by @Option) accordingly
     * and calls method run().
     * @param test  the instance to setup fields for
     * @param args  command line arguments array
      */
    public static void setupAndRun(Runnable test, String[] args) {
        setupAndRun(test, args, null);
    }

    /**
     * This method parses the commandline arguments and setups the instance fields (annotated by @Option) accordingly
     * @param test  the instance to setup fields for
     * @param args  command line arguments array
     */
    public static void setup(Object test, String[] args)
    {
        setup(test, args, null);
    }

    /**
     * This method parses the commandline arguments, setups the instance fields (annotated by @Option) accordingly
     * and calls method run().
     * @param test  the instance to setup fields for
     * @param args  command line arguments array
     * @param unknownOptionHandler an option handler for unknown options
      */
    public static void setupAndRun(Runnable test, String[] args, OptionHandler unknownOptionHandler) {
        setup(test, args, unknownOptionHandler);
        test.run();
    }


    /**
     *  This is an extension API which allows Test author to create and
     *  process some specific options.
     * @param test  the instance to setup fields for
     * @param args  command line arguments array
     * @param unknownOptionHandler an option handler for unknown options
     *
     */
    public static void setup(Object test, String[] args, OptionHandler unknownOptionHandler) {
                new OptionsSetup(test, args, unknownOptionHandler).run();
    }
}
