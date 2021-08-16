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
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package vm.share.options;

/**
 * Handles options not specified via @Options annotations,
 * implemented and provided by the user.
 */
public interface OptionHandler
{
        /**
         * This method is called for every unknown option
         * @param name option name (not including '-' or '=', trimmed)
         * @param value may be null in the case of the last option with no value
         * specified.
         */
        public void option(String name, String value);

        /**
         * This method is called for every command line argument which
         * is not recognized as a part of -option_name=value pair.
         * @param value the value of an unrecognized argument
         */
        public void argument(String value);
}
