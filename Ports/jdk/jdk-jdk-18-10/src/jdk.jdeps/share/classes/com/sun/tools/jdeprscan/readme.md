<!--

Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.

This code is free software; you can redistribute it and/or modify it
under the terms of the GNU General Public License version 2 only, as
published by the Free Software Foundation.  Oracle designates this
particular file as subject to the "Classpath" exception as provided
by Oracle in the LICENSE file that accompanied this code.

This code is distributed in the hope that it will be useful, but WITHOUT
ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
version 2 for more details (a copy is included in the LICENSE file that
accompanied this code).

You should have received a copy of the GNU General Public License version
2 along with this work; if not, write to the Free Software Foundation,
Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.

Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
or visit www.oracle.com if you need additional information or have any
questions.

-->


JDeprScan Tool Command Reference
-----

**NAME**

        jdeprscan - Java deprecation scanner

**SYNOPSIS**

        jdeprscan [options] {dir | jar | class} ...

**OPTIONS**

        --class-path PATH

            Sets the classpath to PATH.

        --for-removal

            Limit reporting to deprecations whose forRemoval element
            is true.

        --full-version

            Prints the full version string of the tool and exits.

        -h
        --help

            Prints a help message and exits.

        -l
        --list

            Prints out the set of deprecated APIs.

        --release 6|7|8|9|10

            Specifies the Java SE release that is the source of
            the list of deprecated APIs. If no --release option is
            provided, the latest release is used.

        -v
        --verbose

            Enables additional output.

        --version

            Prints the version string of the tool and exits.

**DESCRIPTION**

**jdeprscan** scans a class library for uses of deprecated APIs.
**jdeprscan** processes one or more arguments, which can be any
combination of a directory, a jar file, or a class name.

A directory argument must specify a path to a directory hierarchy that
reflects the Java package hierarchy of the classes it contains.
**jdeprscan** will scan each class found in the directory hierarchy
and report information about how those classes use deprecated APIs.

Given a jar file, **jdeprscan** will scan the classes found within
that jar file and report information about how those classes use
deprecated APIs.

Given a class file, **jdeprscan** will scan that class and report
its use of deprecated APIs.

Given a class name, **jdeprscan** will search for that class on the
classpath, scan that class, and report information about how that
class uses deprecated APIs. The class name must use the fully
qualified binary name of the class, as described in the
[Java Language Specification, section 13.1][jls131]. This form uses
the '$' character instead of '.' as the separator for nested class names.
For example, the `Thread.State` enum would be specified using the string

        java.lang.Thread$State

The `--class-path` option specifies the classpath used for
class searching. The classpath is used for classes named on the
command line, as well as for dependencies of the classes in jar file
or directory hierarchy to be scanned.

The `--for-removal` option limits output to uses of deprecated APIs
whose `@Deprecated` annotation includes the `forRemoval` element with
the value `true`. Note: the `forRemoval` attribute of the
`@Deprecated` annotation did not exist prior to Java SE 9, so this
option cannot be used with a release value of 6, 7, or 8.

The `--release` option specifies the Java SE specification version
that determines the set of deprecated APIs for which scanning is
done. This is useful if a deprecation report is desired that lists
uses of deprecated APIs as of a particular release in the past. If no
`--release` option is given, the latest release is used.

The `--list` and `-l` options will list the known set of deprecated
APIs instead of doing any scanning. Since no scanning is done,
no directory, jar, or class arguments should be provided. The set
of deprecated APIs listed is affected by the `--release` and the
`--for-removal` options.


**EXAMPLE OUTPUT**

The output is a report that lists program elements that use deprecated
APIs. Output is subject to change.

Consider the following declarations:

        // java.lang.Boolean

        @Deprecated(since="9")
        public Boolean(boolean value)

        // java.lang.Thread

        @Deprecated(since="1.5", forRemoval=true)
        public void destroy()

Running **jdeprscan** over a class that calls these methods will result
in output something like the following:

        class Example uses method java/lang/Boolean.<init>(Z)V deprecated
        class Example uses method java/lang/Thread.destroy()V deprecated for removal

Running **jdeprscan** with the `--list` option will result in output
including something like the following:

        ...
        @Deprecated(since="9") java.lang.Boolean(boolean)
        @Deprecated(since="1.5", forRemoval=true) void java.lang.Thread.destroy()
        ...

**NOTES**

The **jdeprscan** tool operates by opening Java class files and
reading their structures directly, particularly the constant
pool. Because of this, **jdeprscan** can tell _that_ a deprecated API
is used, but it often cannot tell _where_ in the class that API is
used.

The **jdeprscan** tool doesn't follow the same set of rules for
emitting warnings as specified for Java compilers in [JLS section
9.6.4.6][jls9646]. In particular, **jdeprscan** does not respond to
the `@SuppressWarnings` annotation, as that is significant only in
source code, not in class files. In addition, **jdeprscan** emits
warnings even if the usage is within the API element that is
deprecated and when the use and declaration are within the same
outermost class.

[jls9646]: http://docs.oracle.com/javase/specs/jls/se8/html/jls-9.html#jls-9.6.4.6

[jls131]: http://docs.oracle.com/javase/specs/jls/se8/html/jls-13.html#jls-13.1
