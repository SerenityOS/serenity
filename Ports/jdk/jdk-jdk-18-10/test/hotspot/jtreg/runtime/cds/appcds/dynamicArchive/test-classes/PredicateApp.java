/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

public class PredicateApp {
    public static void main(String[] args){
        boolean isRuntime = (args.length == 1 && args[0].equals("run")) ? true : false;
        List languages = Arrays.asList("Java", "Scala", "C++", "Haskell", "Lisp");

        doit(() -> {
            System.out.println("Languages which starts with J :");
            filter(languages, (str)->((String)str).startsWith("J"));
            LambdaVerification.verifyCallerIsArchivedLambda(isRuntime);
        });

        doit(() -> {
            System.out.println("Languages which ends with a ");
            filter(languages, (str)->((String)str).endsWith("a"));
            LambdaVerification.verifyCallerIsArchivedLambda(isRuntime);
        });

        doit(() -> {
            System.out.println("Print all languages :");
            filter(languages, (str)->true);
            LambdaVerification.verifyCallerIsArchivedLambda(isRuntime);
        });

        doit(() -> {
            System.out.println("Print no language : ");
            filter(languages, (str)->false);
            LambdaVerification.verifyCallerIsArchivedLambda(isRuntime);
        });

        doit(() -> {
            System.out.println("Print language whose length greater than 4:");
            filter(languages, (str)->((String)str).length() > 4);
            LambdaVerification.verifyCallerIsArchivedLambda(isRuntime);
        });
    }

    public static void filter(List names, Predicate condition) {
        names.stream().filter((name) -> (condition.test(name))).forEach((name) -> {
           System.out.println(name + " ");
       });
    }

    static void doit(Runnable t) {
        t.run();
    }
}
