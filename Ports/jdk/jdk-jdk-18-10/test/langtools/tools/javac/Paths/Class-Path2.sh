#!/bin/sh
#
# Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
#
# This code is free software; you can redistribute it and/or modify it
# under the terms of the GNU General Public License version 2 only, as
# published by the Free Software Foundation.
#
# This code is distributed in the hope that it will be useful, but WITHOUT
# ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
# FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
# version 2 for more details (a copy is included in the LICENSE file that
# accompanied this code).
#
# You should have received a copy of the GNU General Public License version
# 2 along with this work; if not, write to the Free Software Foundation,
# Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
#
# Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
# or visit www.oracle.com if you need additional information or have any
# questions.
#

# @test 
# @bug 4212732 6485027
# @summary Test handling of the Class-Path attribute in jar file manifests
# @author Martin Buchholz
#
# @run shell Class-Path2.sh

# To run this test manually, simply do ./Class-Path2.sh

. ${TESTSRC-.}/Util.sh

set -u

Cleanup() {
    Sys rm -rf pkg Main.java Main.class Main.jar jars
    Sys rm -rf MANIFEST.MF A.jar B.zip
}

Cleanup
Sys mkdir pkg

#----------------------------------------------------------------
# Create mutually referential jar files
#----------------------------------------------------------------
cat >pkg/A.java <<EOF
package pkg;
import pkg.B;
public class A {
    public static int f() { return B.g(); }
    public static int g() { return 0; }
}
EOF

cat >pkg/B.java <<EOF
package pkg;
import pkg.A;
public class B {
    public static int f() { return A.g(); }
    public static int g() { return 0; }
}
EOF

Sys "$javac" pkg/A.java pkg/B.java

MkManifestWithClassPath "./sub/B.zip"
Sys "$jar" cmf MANIFEST.MF A.jar pkg/A.class

MkManifestWithClassPath "../A.jar"
Sys "$jar" cmf MANIFEST.MF B.zip pkg/B.class

cat >Main.java <<EOF
import pkg.*;
public class Main {
    public static void main(String []a) { System.exit(A.f() + B.f()); }
}
EOF

Sys rm -rf pkg

Sys mkdir jars
Sys mkdir jars/sub/
Sys mv A.jar jars/.
Sys mv B.zip jars/sub/.

#
# Test 1: Compiling 
#

Success "$javac" ${TESTTOOLVMOPTS} -cp "jars/A.jar" Main.java
Success "$java"  ${TESTVMOPTS}     -cp "jars/A.jar${PS}." Main

Success "$javac" ${TESTTOOLVMOPTS} -cp "jars/sub/B.zip"       Main.java
Success "$java"  ${TESTVMOPTS}     -cp "jars/sub/B.zip${PS}." Main

#
# Test 2: Use of extension directories is incorrect
#

# Success "$javac" ${TESTTOOLVMOPTS} -extdirs jars          -cp None Main.java
# Success "$java"  ${TESTVMOPTS}     -Djava.ext.dirs="jars" -cp .    Main

# Success "$javac" ${TESTTOOLVMOPTS} -extdirs jars/sub          -cp None Main.java
# Success "$java"  ${TESTVMOPTS}     -Djava.ext.dirs="jars/sub" -cp .    Main

Cleanup

Bottom Line
