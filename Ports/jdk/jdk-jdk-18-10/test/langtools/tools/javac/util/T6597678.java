/*
 * Copyright (c) 2011, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6597678 6449184
 * @summary Ensure Messages propogated between rounds
 * @library /tools/javac/lib
 * @modules jdk.compiler/com.sun.tools.javac.processing
 *          jdk.compiler/com.sun.tools.javac.util
 * @build JavacTestingAbstractProcessor T6597678
 * @run main T6597678
 */

import java.io.*;
import java.util.*;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedOptions;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;

import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.JavacMessages;
import com.sun.tools.javac.util.Log;

@SupportedOptions("WriterString")
public class T6597678 extends JavacTestingAbstractProcessor {
    public static void main(String... args) throws Exception {
        new T6597678().run();
    }

    void run() throws Exception {
        String myName = T6597678.class.getSimpleName();
        File testSrc = new File(System.getProperty("test.src"));
        File file = new File(testSrc, myName + ".java");

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        compile(sw, pw,
            "--add-exports", "jdk.compiler/com.sun.tools.javac.processing=ALL-UNNAMED",
            "--add-exports", "jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED",
            "-XDaccessInternalAPI",
            "-proc:only",
            "-processor", myName,
            "-AWriterString=" + pw.toString(),
            file.getPath());
    }

    void compile(StringWriter sw, PrintWriter pw, String... args) throws Exception {
        int rc = com.sun.tools.javac.Main.compile(args, pw);
        pw.close();
        String out = sw.toString();
        if (!out.isEmpty())
            System.err.println(out);
        if (rc != 0)
            throw new Exception("compilation failed unexpectedly: rc=" + rc);
    }

    //---------------

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Context context = ((JavacProcessingEnvironment) processingEnv).getContext();
        Log log = Log.instance(context);
        PrintWriter noteOut = log.getWriter(Log.WriterKind.NOTICE);
        PrintWriter warnOut = log.getWriter(Log.WriterKind.WARNING);
        PrintWriter errOut  = log.getWriter(Log.WriterKind.ERROR);
        Locale locale = context.get(Locale.class);
        JavacMessages messages = context.get(JavacMessages.messagesKey);

        round++;
        if (round == 1) {
            initialLocale = locale;
            initialMessages = messages;
            initialNoteWriter = noteOut;
            initialWarnWriter = warnOut;
            initialErrWriter  = errOut;

            String writerStringOpt = options.get("WriterString").intern();
            checkEqual("noteWriterString", noteOut.toString().intern(), writerStringOpt);
            checkEqual("warnWriterString", warnOut.toString().intern(), writerStringOpt);
            checkEqual("errWriterString",  errOut.toString().intern(),  writerStringOpt);
        } else {
            checkEqual("locale", locale, initialLocale);
            checkEqual("messages", messages, initialMessages);
            checkEqual("noteWriter", noteOut, initialNoteWriter);
            checkEqual("warnWriter", warnOut, initialWarnWriter);
            checkEqual("errWriter",  errOut,  initialErrWriter);
        }

        return true;
    }

    <T> void checkEqual(String label, T actual, T expected) {
        if (actual != expected)
            messager.printMessage(Diagnostic.Kind.ERROR,
                    "Unexpected value for " + label
                    + "; expected: " + expected
                    + "; found: " + actual);
    }

    int round = 0;
    Locale initialLocale;
    JavacMessages initialMessages;
    PrintWriter initialNoteWriter;
    PrintWriter initialWarnWriter;
    PrintWriter initialErrWriter;
}
