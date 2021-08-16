/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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

package compiler.compilercontrol.share.scenario;

import compiler.compilercontrol.share.method.MethodDescriptor;

public class JcmdCommand extends CompileCommand {
    public final Scenario.JcmdType jcmdType;

    public JcmdCommand(Command command,
                       MethodDescriptor methodDescriptor,
                       Scenario.Compiler compiler,
                       Scenario.Type type,
                       Scenario.JcmdType jcmdType) {
        super(command, methodDescriptor, compiler, type);
        this.jcmdType = jcmdType;
    }

    public JcmdCommand(Command command,
                       MethodDescriptor methodDescriptor,
                       Scenario.Compiler compiler,
                       Scenario.Type type,
                       Scenario.JcmdType jcmdType,
                       String argument) {
        super(command, methodDescriptor, compiler, type, argument);
        this.jcmdType = jcmdType;
    }


    /**
     * Enchances parent's class method with the the JCMDtype printing:
     * {@code ... JCMDType: <jcmd_type>}
     */
    protected String formatFields() {
        return super.formatFields() + " JCMDType: " + jcmdType;
    }

}
