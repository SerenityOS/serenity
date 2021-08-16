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

import java.util.function.Function;
import java.util.List;
import java.util.stream.Collectors;

import static compiler.compilercontrol.share.method.MethodDescriptor.Separator.COMMA;
/**
 * Creates VM options by adding CompileCommand prefix to commands
 */
public class CommandOptionsBuilder extends AbstractCommandBuilder {
    @Override
    public List<String> getOptions() {
        Function<CompileCommand, String> mapper = cc -> {
            StringBuilder sb = new StringBuilder("-XX:CompileCommand=");
            sb.append(cc.command.name);
            sb.append(COMMA.symbol);
            sb.append(cc.methodDescriptor.getString());
            if (cc.argument != null) {
                sb.append(COMMA.symbol);
                sb.append(cc.argument);
            }
            return sb.toString();
        };

        List<String> options = compileCommands.stream()
                .map(mapper).collect(Collectors.toList());

        return options;
    }
}
