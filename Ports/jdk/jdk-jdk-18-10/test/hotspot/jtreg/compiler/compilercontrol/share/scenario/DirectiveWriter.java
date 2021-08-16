/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

import compiler.compilercontrol.share.JSONFile;
import compiler.compilercontrol.share.method.MethodDescriptor;

import java.util.List;

/**
 * Simple directive file writer.
 */
public class DirectiveWriter implements AutoCloseable {
    private final JSONFile jsonFile;

    /**
     * Builds directive file for the given name
     *
     * @param fileName name the file to be created
     */
    public DirectiveWriter(String fileName) {
        jsonFile = new JSONFile(fileName);
    }

    /**
     * Emits match block with a given methods
     *
     * @param methods methods used for the match
     * @return this DirectiveWriter instance
     */
    public DirectiveWriter match(String... methods) {
        if (jsonFile.getElement() == null) {
            write(JSONFile.Element.ARRAY);
        }
        write(JSONFile.Element.OBJECT);
        write(JSONFile.Element.PAIR, "match");
        writeMethods(methods);
        return this;
    }

    /**
     * Emits match block with a given methods
     *
     * @param methodDescriptors method descriptors used for the match
     * @return this DirectiveWriter instance
     */
    public DirectiveWriter match(MethodDescriptor... methodDescriptors) {
        String[] methods = new String[methodDescriptors.length];
        for (int i = 0; i < methodDescriptors.length; i++) {
            methods[i] = methodDescriptors[i].getString();
        }
        match(methods);
        return this;
    }

    /**
     * Emits inline block with a given methods to be inlined or not.
     * Each method should be prepended with + or - to show if it should be
     * inlined or not.
     *
     * @param methods methods used for the inline
     * @return this DirectiveWriter instance
     */
    public DirectiveWriter inline(String... methods) {
        write(JSONFile.Element.PAIR, "inline");
        writeMethods(methods);
        return this;
    }

    /**
     * Emits inline block with a given methods to be inlined or not.
     * Each method should be prepended with + or - to show if it should be
     * inlined or not.
     *
     * @param methods methods used for the inline
     * @return this DirectiveWriter instance
     */
    public DirectiveWriter inline(List<String> methods) {
        write(JSONFile.Element.PAIR, "inline");
        writeMethods(methods.toArray(new String[methods.size()]));
        return this;
    }

    private void writeMethods(String[] methods) {
        if (methods.length == 0) {
            throw new IllegalArgumentException("ERROR: empty methods array");
        }
        if (methods.length > 1) {
            write(JSONFile.Element.ARRAY);
            for (String method : methods) {
                write(JSONFile.Element.VALUE, "\"" + method + "\"");
            }
            end(); // ends array
        } else {
            write(JSONFile.Element.VALUE, "\"" + methods[0] + "\"");
        }
    }

    /**
     * Emits compiler blocks that makes current match to be excluded or not
     * from compilation with specified compiler
     *
     * @param compiler compiler to be excluded or null, for all
     * @param exclude  shows if compiler should be disabled for this match
     * @return this DirectiveWriter instance
     */
    public DirectiveWriter excludeCompile(Scenario.Compiler compiler,
                                          boolean exclude) {
        for (Scenario.Compiler comp : Scenario.Compiler.values()) {
            emitCompiler(comp);
            if (comp == compiler || compiler == null) {
                option(Option.EXCLUDE, exclude);
            } else {
                // just make this block be enabled
                option(Option.ENABLE, true);
            }
            end(); // end compiler block
        }
        return this;
    }

    /**
     * Emits compiler directive block
     *
     * @return this DirectiveWriter instance
     */
    public DirectiveWriter emitCompiler(Scenario.Compiler compiler) {
        write(JSONFile.Element.PAIR, compiler.name);
        write(JSONFile.Element.OBJECT);
        return this;
    }

    @Override
    public void close() {
        jsonFile.close();
    }

    /**
     * Ends current object element. It could be either a
     * c1 or c2 block, or a whole match block
     *
     * @return this DirectiveWriter instance
     */
    public DirectiveWriter end() {
        jsonFile.end();
        return this;
    }

    public DirectiveWriter write(JSONFile.Element element, String... value) {
        jsonFile.write(element, value);
        return this;
    }

    /**
     * Emits directive option with a given value
     *
     * @param option directive to be set
     * @param value value of the directive
     * @return this DirectiveWriter instance
     */
    public DirectiveWriter option(Option option, Object value) {
        write(JSONFile.Element.PAIR, option.string);
        write(JSONFile.Element.VALUE, String.valueOf(value));
        return this;
    }

    /**
     * Directive option list
     */
    public enum Option {
        PRINT_ASSEMBLY("PrintAssembly"),
        LOG("Log"),
        EXCLUDE("Exclude"),
        ENABLE("Enable"),
        INTRINSIC("ControlIntrinsic");

        public final String string;

        private Option(String directive) {
            this.string = directive;
        }
    }
}
