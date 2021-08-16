/*
 * Copyright (c) 2005, 2016, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

package build.tools.projectcreator;

class ArgIterator {
    String[] args;
    int i;
    ArgIterator(String[] args) {
        this.args = args;
        this.i = 0;
    }
    String get() { return args[i]; }
    boolean hasMore() { return args != null && i  < args.length; }
    boolean next() { return ++i < args.length; }
}

abstract class ArgHandler {
    public abstract void handle(ArgIterator it);

}

class ArgRule {
    String arg;
    ArgHandler handler;
    ArgRule(String arg, ArgHandler handler) {
        this.arg = arg;
        this.handler = handler;
    }

    boolean process(ArgIterator it) {
        if (match(it.get(), arg)) {
            handler.handle(it);
            return true;
        }
        return false;
    }
    boolean match(String rule_pattern, String arg) {
        return arg.equals(rule_pattern);
    }
}

class ArgsParser {
    ArgsParser(String[] args,
               ArgRule[] rules,
               ArgHandler defaulter) {
        ArgIterator ai = new ArgIterator(args);
        while (ai.hasMore()) {
            boolean processed = false;
            for (int i=0; i<rules.length; i++) {
                processed |= rules[i].process(ai);
                if (processed) {
                    break;
                }
            }
            if (!processed) {
                if (defaulter != null) {
                    defaulter.handle(ai);
                } else {
                    System.err.println("ERROR: unparsed \""+ai.get()+"\"");
                    ai.next();
                }
            }
        }
    }
}
