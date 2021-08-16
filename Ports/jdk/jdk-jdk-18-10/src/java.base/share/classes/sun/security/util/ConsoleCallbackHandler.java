/*
 * Copyright (c) 2014, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package sun.security.util;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.ConfirmationCallback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.TextOutputCallback;
import javax.security.auth.callback.UnsupportedCallbackException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * A {@code CallbackHandler} that prompts and reads from the command line
 * for answers to authentication questions.
 */
public class ConsoleCallbackHandler implements CallbackHandler {

    /**
     * Creates a callback handler that prompts and reads from the
     * command line for answers to authentication questions.
     */
    public ConsoleCallbackHandler() { }

    /**
     * Handles the specified set of callbacks.
     *
     * @param callbacks the callbacks to handle
     * @throws IOException if an input or output error occurs.
     * @throws UnsupportedCallbackException if the callback is not an
     * instance of NameCallback or PasswordCallback
     */
    public void handle(Callback[] callbacks)
        throws IOException, UnsupportedCallbackException
    {
        ConfirmationCallback confirmation = null;

        for (int i = 0; i < callbacks.length; i++) {
            if (callbacks[i] instanceof TextOutputCallback) {
                TextOutputCallback tc = (TextOutputCallback) callbacks[i];

                String text;
                switch (tc.getMessageType()) {
                case TextOutputCallback.INFORMATION:
                    text = "";
                    break;
                case TextOutputCallback.WARNING:
                    text = "Warning: ";
                    break;
                case TextOutputCallback.ERROR:
                    text = "Error: ";
                    break;
                default:
                    throw new UnsupportedCallbackException(
                        callbacks[i], "Unrecognized message type");
                }

                String message = tc.getMessage();
                if (message != null) {
                    text += message;
                }
                if (text != null) {
                    System.err.println(text);
                }

            } else if (callbacks[i] instanceof NameCallback) {
                NameCallback nc = (NameCallback) callbacks[i];

                if (nc.getDefaultName() == null) {
                    System.err.print(nc.getPrompt());
                } else {
                    System.err.print(nc.getPrompt() +
                                " [" + nc.getDefaultName() + "] ");
                }
                System.err.flush();

                String result = readLine();
                if (result.isEmpty()) {
                    result = nc.getDefaultName();
                }

                nc.setName(result);

            } else if (callbacks[i] instanceof PasswordCallback) {
                PasswordCallback pc = (PasswordCallback) callbacks[i];

                System.err.print(pc.getPrompt());
                System.err.flush();

                pc.setPassword(Password.readPassword(System.in, pc.isEchoOn()));

            } else if (callbacks[i] instanceof ConfirmationCallback) {
                confirmation = (ConfirmationCallback) callbacks[i];

            } else {
                throw new UnsupportedCallbackException(
                    callbacks[i], "Unrecognized Callback");
            }
        }

        /* Do the confirmation callback last. */
        if (confirmation != null) {
            doConfirmation(confirmation);
        }
    }

    /* Reads a line of input */
    private String readLine() throws IOException {
        String result = new BufferedReader
            (new InputStreamReader(System.in)).readLine();
        if (result == null) {
            throw new IOException("Cannot read from System.in");
        }
        return result;
    }

    private void doConfirmation(ConfirmationCallback confirmation)
        throws IOException, UnsupportedCallbackException
    {
        String prefix;
        int messageType = confirmation.getMessageType();
        switch (messageType) {
        case ConfirmationCallback.WARNING:
            prefix =  "Warning: ";
            break;
        case ConfirmationCallback.ERROR:
            prefix = "Error: ";
            break;
        case ConfirmationCallback.INFORMATION:
            prefix = "";
            break;
        default:
            throw new UnsupportedCallbackException(
                confirmation, "Unrecognized message type: " + messageType);
        }

        class OptionInfo {
            String name;
            int value;
            OptionInfo(String name, int value) {
                this.name = name;
                this.value = value;
            }
        }

        OptionInfo[] options;
        int optionType = confirmation.getOptionType();
        switch (optionType) {
        case ConfirmationCallback.YES_NO_OPTION:
            options = new OptionInfo[] {
                new OptionInfo("Yes", ConfirmationCallback.YES),
                new OptionInfo("No", ConfirmationCallback.NO)
            };
            break;
        case ConfirmationCallback.YES_NO_CANCEL_OPTION:
            options = new OptionInfo[] {
                new OptionInfo("Yes", ConfirmationCallback.YES),
                new OptionInfo("No", ConfirmationCallback.NO),
                new OptionInfo("Cancel", ConfirmationCallback.CANCEL)
            };
            break;
        case ConfirmationCallback.OK_CANCEL_OPTION:
            options = new OptionInfo[] {
                new OptionInfo("OK", ConfirmationCallback.OK),
                new OptionInfo("Cancel", ConfirmationCallback.CANCEL)
            };
            break;
        case ConfirmationCallback.UNSPECIFIED_OPTION:
            String[] optionStrings = confirmation.getOptions();
            options = new OptionInfo[optionStrings.length];
            for (int i = 0; i < options.length; i++) {
                options[i] = new OptionInfo(optionStrings[i], i);
            }
            break;
        default:
            throw new UnsupportedCallbackException(
                confirmation, "Unrecognized option type: " + optionType);
        }

        int defaultOption = confirmation.getDefaultOption();

        String prompt = confirmation.getPrompt();
        if (prompt == null) {
            prompt = "";
        }
        prompt = prefix + prompt;
        if (!prompt.isEmpty()) {
            System.err.println(prompt);
        }

        for (int i = 0; i < options.length; i++) {
            if (optionType == ConfirmationCallback.UNSPECIFIED_OPTION) {
                // defaultOption is an index into the options array
                System.err.println(
                    i + ". " + options[i].name +
                    (i == defaultOption ? " [default]" : ""));
            } else {
                // defaultOption is an option value
                System.err.println(
                    i + ". " + options[i].name +
                    (options[i].value == defaultOption ? " [default]" : ""));
            }
        }
        System.err.print("Enter a number: ");
        System.err.flush();
        int result;
        try {
            result = Integer.parseInt(readLine());
            if (result < 0 || result > (options.length - 1)) {
                result = defaultOption;
            } else {
                result = options[result].value;
            }
        } catch (NumberFormatException e) {
            result = defaultOption;
        }

        confirmation.setSelectedIndex(result);
    }
}
