<!--

Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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


JDeprScan Internals
-----

**EXPERIMENTAL OPTIONS**

    --Xload-class CLASSNAME

        Loads deprecation data from the class named CLASSNAME instead of from
        the JDK image.

    --Xload-csv CVSFILE

        Loads deprecation data from file CSVFILE.

    --Xload-dir DIR

        Loads deprecation data from the class hierarchy rooted
        at the directory named DIR.

    --Xload-jar JARFILE

        Loads deprecation data from the classes contained in the
        jar file named JARFILE.

    --Xload-jdk9 JAVA_HOME

        Loads deprecation data from a modular JDK whose home
        directory is at JAVA_HOME. This essentially adds the given
        path to the system-modules location.

    --Xload-old-jdk JAVA_HOME

        Loads deprecation data from an old (non-modular) JDK whose
        home directory is at JAVA_HOME. This essentially scans the
        rt.jar file from that JDK.

    --Xload-self

        Loads deprecation data from the running JDK image by
        traversing the entire jrt: filesystem. This differs from
        -release 9, which traverses modules, packages, and classes by
        starting from a set of root modules and using javax.lang.model
        mechanisms (as opposed to filesystem mechanisms) for
        traversing contained elements recursively.

    --Xcompiler-arg ARG

        Adds ARG to the list of arguments passed to the compiler.

    --Xcsv-comment COMMENT

        Adds a comment line containing COMMENT to the top of the CSV
        that is emitted.  Valid only when --Xprint-csv is
        specified. More than one --Xcsv-comment option is permitted,
        which will cause a corresponding number of comment lines to be
        emitted to the CSV file.

    --Xprint-csv

        Prints out the loaded deprecation information in CSV format
        to standard output. In this mode, no scanning is done, so
        there must not be any additional directory, jar, or classname
        arguments.

**CSV FILE SYNTAX**

The `-Xprint-csv` option causes **jdeprscan** to emit the loaded
deprecation data in CSV (comma-separated value) format.  The general
syntax of CSV is documented in [RFC 4180][RFC] with supplemental
information in a [Wikipedia article][wiki].

The file is encoded in UTF-8.

The file consists of a series of lines. Any of the standard line
separators CR (U+000D), LF (U+000A), or a CR immediately followed by
LF, are supported. Newlines are only supported between records and
are not supported within field values.

Comment lines start with a `#` (U+0023) character in the first
column. The entire line is ignored up until the next line separator
sequence.

Each line is divided into fields separated by the comma `,` (U+002C)
character.  Horizontal whitespace is not treated specially; that is,
it is considered part of a field's value. An empty line is considered
to have one field which is the empty string.

A field value that contains a comma or a quote quotation mark `"`
(U+0022) must be surrounded by quotation marks. The surrounding
quotation marks are not considered part of the field value. Any
quotation marks that are part of a field value must be repeated in
addition to being surrounded by quotation marks.

It is a syntax error if a quotation mark appears within an unquoted field;
if a quoted field isn't immediately followed by a comma or line
separator; or if a quoted field is left unclosed at the end of the line.

For example, a record with the following four fields:

1. abcd
2. ef,gh
3. ij"kl
4. mnop

would be encoded as follows:

        abcd,"ef,gh","ij""kl",mnop

**CSV FILE DATA**

The first line of output must be the following:

        #jdepr1

This is strictly a comment line, but it serves as a file
identifier. The "1" indicates version 1 of this file.

Zero or more comment lines follow, containing text that is specified
by the `-Xcsv-comment` options.

Subsequent non-comment lines must have the following five fields:

        kind,typeName,descOrName,since,forRemoval

Fields are defined as follows:

 * _kind_ - one of CONSTRUCTOR, FIELD, METHOD, ENUM\_CONSTANT,
   CLASS, INTERFACE, ENUM, or ANNOTATION\_TYPE. These correspond to
   enumeration constants from the `javax.lang.model.element.ElementKind`
   enum.

 * _typeName_ - the fully qualified name of the type (if *kind* is
   CLASS, INTERFACE, ENUM, or ANNOTATION\_TYPE) or of the enclosing
   type (if _kind_ is CONSTRUCTOR, FIELD, METHOD, or
   ENUM\_CONSTANT). This value is a _binary name_ [JLS 13.1][jls131]
   using a slash character `/` (U+002F) to separate package and
   top-level name components, and a dollar sign `$` (U+0024) to
   separate nested name components. For example, the `Thread.State`
   enum that appears in Java SE would have the following typeName:

            java/lang/Thread$State

 * _descOrName_ - if _kind_ is METHOD or CONSTRUCTOR, this is the method's
   or constructor's descriptor [JVMS 4.3.3][jvms433]; if _kind_ is FIELD or
   ENUM\_CONSTANT, this is its name; otherwise this field is empty.
   A method's descriptor includes its name, parameter types, and return
   type. For example, the method

           public void String.getBytes(int srcBegin,
                                       int srcEnd,
                                       byte[] dst,
                                       int dstBegin)

   has the descriptor

           getBytes(II[BI)V

 * _since_ - the value of the `since` element of the `@Deprecated`
   annotation, or empty if this element is not present.

 * _forRemoval_ - the value of the `forRemoval` element of the
   `@Deprecated` annotation, a boolean, either "true" or "false".

Note that the _since_ field can have arbitrary text (excluding
line separators) and is thus subject to quoting.

**EXAMPLE OUTPUT**

Given the following method declaration and annotation from the
`java.lang.Character` class,

            @Deprecated(since="1.1")
            public static boolean isJavaLetter(char ch)

the following line will be emitted from **jdeprscan -Xprint-csv**:

            METHOD,java/lang/Character,isJavaLetter(C)Z,1.1,false


[RFC]: https://www.ietf.org/rfc/rfc4180.txt

[wiki]: https://en.wikipedia.org/wiki/Comma-separated_values

[jls131]: http://docs.oracle.com/javase/specs/jls/se8/html/jls-13.html#jls-13.1

[jvms433]: http://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.3.3
