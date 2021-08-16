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

package combo;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A combo parameter represents an 'hole' in a template that can be replaced with a given string.
 * The schema of such holes is defined in {@link ComboParameter#pattern}; the main routine for
 * replacing holes in a template scheme is {@link ComboParameter#expandTemplate(String, Resolver)}.
 */
public interface ComboParameter {

    /**
     * A combo parameter can take the form:
     * <p>
     * #{MAJOR}
     * #{MAJOR.}
     * #{MAJOR.MINOR}
     * <p>
     * where MAJOR can be IDENTIFIER or IDENTIFIER[NUMERIC_INDEX]
     * and MINOR can be an identifier.
     */
    Pattern pattern = Pattern.compile("#\\{([A-Z_][A-Z0-9_]*(?:\\[\\d+\\])?)(?:\\.([A-Z0-9_]*))?\\}");

    /**
     * Entry point for the customizable replacement logic. Subclasses must implement this method to
     * specify how a given template hole should be expanded. An optional contextual argument is passed
     * in as parameter, to make expansion more flexible.
     */
    String expand(String optParameter);

    /**
     * Helper class for defining 'constant' combo parameters - i.e. parameters that always expand
     * as a given string value - regardless of the context.
     */
    class Constant<D> implements ComboParameter {

        D data;

        public Constant(D data) {
            this.data = data;
        }

        @Override
        public String expand(String _unused) {
            return String.valueOf(data);
        }
    }

    /**
     * Helper interface used to lookup parameters given a parameter name.
     */
    interface Resolver {
        ComboParameter lookup(String name);
    }

    /**
     * Main routine for replacing holes in a template string. Holes are repeatedly searches, their
     * corresponding parameters retrieved, and replaced through expansion; since an expansion can
     * lead to more holes, the process has to be applied until a fixed point is reached.
     */
    static String expandTemplate(String template, Resolver resolver) {
        CharSequence in = template;
        StringBuffer out = new StringBuffer();
        while (true) {
            boolean more = false;
            Matcher m = pattern.matcher(in);
            while (m.find()) {
                String parameterName = m.group(1);
                String minor = m.group(2);
                ComboParameter parameter = resolver.lookup(parameterName);
                if (parameter == null) {
                    throw new IllegalStateException("Unhandled parameter name " + parameterName);
                }

                String replacement = parameter.expand(minor);
                more |= pattern.matcher(replacement).find();
                m.appendReplacement(out, replacement);
            }
            m.appendTail(out);
            if (!more)
                return out.toString();
            else {
                in = out;
                out = new StringBuffer();
            }
        }
    }
}
