/*
 * Copyright (c) 2004, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 5008856 5023573 5024917 5062569 7172176
 * @summary 'missing resource key' error for key = "Operating system"
 *
 * @modules jdk.jconsole/sun.tools.jconsole
 *          jdk.jconsole/sun.tools.jconsole.resources:open
 *
 * @run main ResourceCheckTest
 */

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import sun.tools.jconsole.Messages;
import sun.tools.jconsole.Resources;

/*
 * Ensures that there is a one-to-one mapping between constants in the
 * Message class and the keys in the sun.tools.jconsole.resources.messages
 * bundle.
 *
 * An error will be thrown if there is a:
 *
 * - key in the resource bundle that doesn't have a public static field with
 *   the same name in the Message class.
 *
 * - public static field in the Message class that doesn't have a key with
 *   the same name in the resource bundle.
 *
 * - message with a mnemonic identifier(&) for which a mnemonic can't
 *   be looked up using Resources#getMnemonicInt().
 *
 */
public class ResourceCheckTest {
    private static final String MISSING_RESOURCE_KEY_PREFIX = "missing message for";
    private static final String RESOURCE_BUNDLE = "sun.tools.jconsole.resources.messages";
    private static final String NEW_LINE = String.format("%n");

    public static void main(String... args) {
        List<String> errors = new ArrayList<>();
        // Ensure that all Message fields have a corresponding key/value
        // in the resource bundle and that mnemonics can be looked
        // up where applicable.
        Module module = sun.tools.jconsole.Messages.class.getModule();
        ResourceBundle rb = ResourceBundle.getBundle(RESOURCE_BUNDLE, module);
        for (Field field : Messages.class.getFields()) {
            if (isResourceKeyField(field)) {
                String resourceKey = field.getName();
                String message = readField(field);
                if (message.startsWith(MISSING_RESOURCE_KEY_PREFIX)) {
                    errors.add("Can't find message (and perhaps mnemonic) for "
                            + Messages.class.getSimpleName() + "."
                            + resourceKey + " in resource bundle.");
                } else {
                    String resourceMessage = rb.getString(resourceKey);
                    if (hasMnemonicIdentifier(resourceMessage)) {
                        int mi = Resources.getMnemonicInt(message);
                        if (mi == 0) {
                            errors.add("Could not look up mnemonic for message '"
                                    + message + "'.");
                        }
                    }
                }
            }
        }

        // Ensure that there is Message class field for every resource key.
        for (String key : Collections.list(rb.getKeys())) {
            try {
                Messages.class.getField(key);
            } catch (NoSuchFieldException nfe) {
                errors.add("Can't find static field ("
                        + Messages.class.getSimpleName() + "." + key
                        + ") matching '" + key
                        + "' in resource bundle. Unused message?");
            }
        }

        if (errors.size() > 0) {
            throwError(errors);
        }
    }

    private static String readField(Field field) {
        try {
            return (String) field.get(null);
        } catch (IllegalArgumentException | IllegalAccessException e) {
            throw new Error("Could not access field " + field.getName()
                    + " when trying to read resource message.");
        }
    }

    private static boolean isResourceKeyField(Field field) {
        int modifiers = field.getModifiers();
        return Modifier.isPublic(modifiers) && Modifier.isStatic(modifiers);
    }

    private static boolean hasMnemonicIdentifier(String s) {
        for (int i = 0; i < s.length() - 1; i++) {
            if (s.charAt(i) == '&') {
                if (s.charAt(i + 1) != '&') {
                    return true;
                } else {
                    i++;
                }
            }
        }
        return false;
    }

    private static void throwError(List<String> errors) {
        StringBuffer buffer = new StringBuffer();
        buffer.append("Found ");
        buffer.append(errors.size());
        buffer.append(" error(s) when checking one-to-one mapping ");
        buffer.append("between Message and resource bundle keys in ");
        buffer.append(RESOURCE_BUNDLE);
        buffer.append(" with ");
        buffer.append(Locale.getDefault());
        buffer.append(" locale.");
        buffer.append(NEW_LINE);
        int errorIndex = 1;
        for (String error : errors) {
            buffer.append("Error ");
            buffer.append(errorIndex);
            buffer.append(": ");
            buffer.append(error);
            buffer.append(NEW_LINE);
            errorIndex++;
        }
        throw new Error(buffer.toString());
    }
}
