/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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
package jdk.javadoc.internal.doclets.toolkit;

import javax.lang.model.element.Element;
import javax.tools.Diagnostic;
import javax.tools.FileObject;

import com.sun.source.util.DocTreePath;
import jdk.javadoc.doclet.Reporter;

import static javax.tools.Diagnostic.Kind.*;

/**
 * Provides standardized access to the diagnostic reporting facilities
 * for a doclet.
 *
 * Messages are specified by resource keys to be found in the doclet's
 * {@link Resources resources}.  Values can be substituted into the
 * strings obtained from the resource files.
 *
 * Messages are reported to the doclet's {@link Reporter reporter}.
 */
public class Messages {
    private final BaseConfiguration configuration;
    private final Resources resources;
    private final Reporter reporter;

    /**
     * Creates a {@code Messages} object to provide standardized access to
     * the doclet's diagnostic reporting mechanisms.
     *
     * @param configuration the doclet's configuration, used to access the doclet's
     *                      reporter, and additional methods and state used to
     *                      filter out messages, if any, which should be suppressed.
     * @param resources     resources for console messages and exceptions
     */
    public Messages(BaseConfiguration configuration, Resources resources) {
        this.configuration = configuration;
        this.resources = resources;
        reporter = configuration.getReporter();
    }

    /**
     * Returns the resources being used when generating messages.
     *
     * @return the resources
     */
    public Resources getResources() {
        return resources;
    }

    // ***** Errors *****

    /**
     * Reports an error message to the doclet's reporter.
     *
     * @param key  the name of a resource containing the message to be printed
     * @param args optional arguments to be replaced in the message
     */
    public void error(String key, Object... args) {
        report(ERROR, resources.getText(key, args));
    }

    /**
     * Reports an error message to the doclet's reporter.
     *
     * @param path a path identifying the position to be included with the message
     * @param key  the name of a resource containing the message to be printed
     * @param args optional arguments to be replaced in the message
     */
    public void error(DocTreePath path, String key, Object... args) {
        report(ERROR, path, resources.getText(key, args));
    }

    /**
     * Reports an error message to the doclet's reporter.
     *
     * @param path  a path identifying the position to be included with the message
     * @param start the start of a range of characters to be associated with the message
     * @param pos   the position to be associated with the message
     * @param end   the end of a range of characters to be associated with the message
     * @param key   the name of a resource containing the message to be printed
     * @param args  optional arguments to be replaced in the message
     */
    public void error(DocTreePath path, int start, int pos, int end, String key, Object... args) {
        report(ERROR, path, start, pos, end, resources.getText(key, args));
    }

    /**
     * Reports an error message to the doclet's reporter.
     *
     * @param fo    the file object to be associated with the message
     * @param start the start of a range of characters to be associated with the message
     * @param pos   the position to be associated with the message
     * @param end   the end of a range of characters to be associated with the message
     * @param key   the name of a resource containing the message to be printed
     * @param args  optional arguments to be replaced in the message
     */
    public void error(FileObject fo, int start, int pos, int end, String key, Object... args) {
        report(ERROR, fo, start, pos, end, resources.getText(key, args));
    }

    // ***** Warnings *****

    /**
     * Reports a warning message to the doclet's reporter.
     *
     * @param key  the name of a resource containing the message to be printed
     * @param args optional arguments to be replaced in the message
     */
    public void warning(String key, Object... args) {
        report(WARNING, resources.getText(key, args));
    }

    /**
     * Reports a warning message to the doclet's reporter.
     *
     * @param path a path identifying the position to be included with the message
     * @param key  the name of a resource containing the message to be printed
     * @param args optional arguments to be replaced in the message
     */
    public void warning(DocTreePath path, String key, Object... args) {
        if (configuration.showMessage(path, key)) {
            report(WARNING, path, resources.getText(key, args));
        }
    }

    /**
     * Reports a warning message to the doclet's reporter.
     *
     * @param path  a path identifying the position to be included with the message
     * @param start the start of a range of characters to be associated with the message
     * @param pos   the position to be associated with the message
     * @param end   the end of a range of characters to be associated with the message
     * @param key   the name of a resource containing the message to be printed
     * @param args  optional arguments to be replaced in the message
     */
    public void warning(DocTreePath path, int start, int pos, int end, String key, Object... args) {
        report(WARNING, path, start, pos, end, resources.getText(key, args));
    }

    /**
     * Reports a warning message to the doclet's reporter.
     *
     * @param e    an element identifying the position to be included with the message
     * @param key  the name of a resource containing the message to be printed
     * @param args optional arguments to be replaced in the message
     */
    public void warning(Element e, String key, Object... args) {
        if (configuration.showMessage(e, key)) {
            report(WARNING, e, resources.getText(key, args));
        }
    }

    /**
     * Reports a warning message to the doclet's reporter.
     *
     * @param fo    the file object to be associated with the message
     * @param start the start of a range of characters to be associated with the message
     * @param pos   the position to be associated with the message
     * @param end   the end of a range of characters to be associated with the message
     * @param key   the name of a resource containing the message to be printed
     * @param args  optional arguments to be replaced in the message
     */
    public void warning(FileObject fo, int start, int pos, int end, String key, Object... args) {
        report(WARNING, fo, start, pos, end, resources.getText(key, args));
    }

    // ***** Notices *****

    /**
     * Reports an informational notice to the doclet's reporter.
     * The message is written directly to the reporter's diagnostic stream.
     *
     * @param key  the name of a resource containing the message to be printed
     * @param args optional arguments to be replaced in the message
     */
    public void notice(String key, Object... args) {
        if (!configuration.getOptions().quiet()) {
            // Note: we do not use report(NOTE, ...) which would prefix the output with "Note:"
            reporter.getDiagnosticWriter().println(resources.getText(key, args));
        }
    }

    // ***** Internal support *****

    private void report(Diagnostic.Kind k, String msg) {
        reporter.print(k, msg);
    }

    private void report(Diagnostic.Kind k, DocTreePath p, String msg) {
        reporter.print(k, p, msg);
    }

    private void report(Diagnostic.Kind k, Element e, String msg) {
        reporter.print(k, e, msg);
    }

    private void report(Diagnostic.Kind k, FileObject fo, int start, int pos, int end, String msg) {
        reporter.print(k, fo, start, pos, end, msg);
    }

    private void report(Diagnostic.Kind k, DocTreePath path, int start, int pos, int end, String msg) {
        reporter.print(k, path, start, pos, end, msg);
    }
}
