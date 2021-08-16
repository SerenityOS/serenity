#!/bin/sh

#
# Copyright (c) 2003, 2011, Oracle and/or its affiliates. All rights reserved.
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
# @bug 4212732
# @summary Test handling of the Class-Path attribute in jar file manifests
# @author Martin Buchholz
#
# @run shell Class-Path.sh

# To run this test manually, simply do ./Class-Path.sh

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

Sys "$javac" ${TESTTOOLVMOPTS} pkg/A.java pkg/B.java

MkManifestWithClassPath "B.zip"
Sys "$jar" cmf MANIFEST.MF A.jar pkg/A.class

MkManifestWithClassPath "A.jar"
Sys "$jar" cmf MANIFEST.MF B.zip pkg/B.class

cat >Main.java <<EOF
import pkg.*;
public class Main {
    public static void main(String []a) { System.exit(A.f() + B.f()); }
}
EOF

Success "$javac" ${TESTTOOLVMOPTS} -cp "A.jar" Main.java
Success "$javac" ${TESTTOOLVMOPTS} -cp "B.zip" Main.java
Success "$java" ${TESTVMOPTS}  -cp "A.jar${PS}." Main
Success "$java" ${TESTVMOPTS}  -cp "B.zip${PS}." Main

#----------------------------------------------------------------
# Jar file Class-Path expanded only for jars found on user class path
#----------------------------------------------------------------
Sys mkdir jars
Sys mv A.jar B.zip jars/.
Success "$javac" ${TESTTOOLVMOPTS} -cp "jars/A.jar"       Main.java
Success "$java" ${TESTVMOPTS}  -cp "jars/A.jar${PS}." Main

Success "$javac" ${TESTTOOLVMOPTS} -cp "jars/B.zip"       Main.java
Success "$java" ${TESTVMOPTS}  -cp "jars/B.zip${PS}." Main

# Success "$javac" ${TESTTOOLVMOPTS} -extdirs "jars"        -cp None Main.java
# Success "$javac" ${TESTTOOLVMOPTS} -Djava.ext.dirs="jars" -cp None Main.java
# Success "$java" ${TESTVMOPTS}  -Djava.ext.dirs="jars" -cp .    Main

# Success "$javac" ${TESTTOOLVMOPTS} -endorseddirs "jars"        -cp None Main.java
# Success "$javac" ${TESTTOOLVMOPTS} -Djava.endorsed.dirs="jars" -cp None Main.java
# Success "$java" ${TESTVMOPTS}  -Djava.endorsed.dirs="jars" -cp .    Main

Failure "$java" ${TESTVMOPTS}  -Xbootclasspath/p:"jars/A.jar" -cp .    Main
Failure "$java" ${TESTVMOPTS}  -Xbootclasspath/a:"jars/B.zip" -cp .    Main
Failure "$javac" ${TESTTOOLVMOPTS} -Xbootclasspath/p:"jars/A.jar" -cp None Main.java
Failure "$javac" ${TESTTOOLVMOPTS} -Xbootclasspath/a:"jars/B.zip" -cp None Main.java
Sys mv jars/A.jar jars/B.zip .

MkManifestWithClassPath "A.jar"
echo "Main-Class: Main" >> MANIFEST.MF
Sys "$jar" cmf MANIFEST.MF Main.jar Main.class

Success "$java" ${TESTVMOPTS} -jar Main.jar

MkManifestWithClassPath "."
Sys "$jar" cmf MANIFEST.MF A.jar pkg/A.class

Success "$javac" ${TESTTOOLVMOPTS} -cp "A.jar" Main.java
Success "$java" ${TESTVMOPTS} -jar Main.jar

MkManifestWithClassPath ""
Sys "$jar" cmf MANIFEST.MF A.jar pkg/A.class

Failure "$javac" ${TESTTOOLVMOPTS} -cp "A.jar" Main.java
Failure "$java" ${TESTVMOPTS} -jar Main.jar

#----------------------------------------------------------------
# Test new flag -e (application entry point)
#----------------------------------------------------------------

cat > Hello.java <<EOF
import pkg.*;
public class Hello {
    public static void main(String []a) { System.out.println("Hello World!"); }
}
EOF

cat > Bye.java <<EOF
import pkg.*;
public class Bye {
    public static void main(String []a) { System.out.println("Good Bye!"); }
}
EOF

Success "$javac" ${TESTTOOLVMOPTS} Hello.java Bye.java

# test jar creation without manifest
#
Success "$jar" cfe "Hello.jar" "Hello" Hello.class
Success "$java" ${TESTVMOPTS} -jar Hello.jar

# test for overriding the manifest during jar creation
#
echo "Main-Class: Hello" >> MANIFEST.MF

# test for error: " 'e' flag and manifest with the 'Main-Class' 
# attribute cannot be specified together, during creation
Failure "$jar" cmfe  MANIFEST.MF "Bye.jar" "Bye" Bye.class

# test for overriding the manifest when updating the jar
#
Success "$jar" cfe "greetings.jar" "Hello" Hello.class
Success "$jar" ufe "greetings.jar" "Bye" Bye.class
Success "$java" ${TESTVMOPTS} -jar greetings.jar

# test for error: " 'e' flag and manifest with the 'Main-Class'
# attribute cannot be specified together, during update
Failure "$jar" umfe  MANIFEST.MF "greetings.jar" "Hello"

# test jar updation when there are no inputfiles 
#
Success "$jar" ufe "Hello.jar" "Bye"
Failure "$java" ${TESTVMOPTS} -jar Hello.jar
Success "$jar" umf  MANIFEST.MF "Hello.jar"

# test creating jar when the to-be-archived files
# do not contain the specified main class, there is no check done
# for the presence of the main class, so the test will pass
#
Success "$jar" cfe "Hello.jar" "Hello" Bye.class

# Jar creation and update when there is no manifest and inputfiles 
# specified
Failure "$jar" cvf "A.jar"
Failure "$jar" uvf "A.jar"

# error: no such file or directory
Failure "$jar" cvf "A.jar" non-existing.file
Failure "$jar" uvf "A.jar" non-existing.file

Cleanup

Bottom Line
