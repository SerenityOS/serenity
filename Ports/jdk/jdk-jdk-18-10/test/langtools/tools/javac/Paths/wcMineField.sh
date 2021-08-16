#!/bin/sh

#
# Copyright (c) 2005, 2015, Oracle and/or its affiliates. All rights reserved.
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

#
# @test
# @summary Test classpath wildcards for javac and java -classpath option.
# @bug 6268383 8172309
# @run shell/timeout=600 wcMineField.sh

# To run this test manually, simply do ./wcMineField.sh
#----------------------------------------------------------------

. ${TESTSRC-.}/Util.sh

set -u

#----------------------------------------------------------------
# Note that, on Windows only, the launcher also includes another
# kind of command-line wildcard expansion, via setargv.obj
# http://msdn.microsoft.com/library/default.asp?url=/library/en-us/vccelng/htm/progs_11.asp
# Therefore, on Windows, -classpath "foo/*" is treated differently
# from -classpath ".${PS}foo/*"
#----------------------------------------------------------------

#----------------------------------------------------------------
# Prepare the "Minefield"
#----------------------------------------------------------------
Cleanup() {
    Sys rm -rf GooSrc GooJar GooZip GooClass
         Sys rm -rf SpeSrc SpeJar SpeZip SpeClass
    Sys rm -rf BadSrc BadJar BadZip BadClass
         Sys rm -rf JarNClass StarJar MixJar StarDir
    Sys rm -rf OneDir *.class Main*.java MANIFEST.MF
}

Cleanup
Sys mkdir  GooSrc GooJar GooZip GooClass GooJar/SubDir
Sys mkdir  BadSrc BadJar BadZip BadClass
Sys mkdir  SpeSrc SpeJar SpeZip SpeClass
Sys mkdir  JarNClass StarJar MixJar
echo 'public class Lib  {public static void f(){}}' > Lib.java
echo 'public class Lib2 {public static void g(){}}' > Lib2.java
echo 'public class Lib3 {public static void h(){}}' > Lib3.java
Sys "$javac" ${TESTTOOLVMOPTS} Lib.java Lib2.java Lib3.java
Sys cp Lib.class JarNClass/.
Sys "$jar" cf GooJar/Lib.jar Lib.class
Sys "$jar" cf GooJar/SubDir/Lib2.jar Lib2.class
Sys "$jar" cf JarNClass/Lib.jar Lib.class

Sys "$jar" cf GooZip/Lib.zip Lib.class
Sys mv Lib.class GooClass/.
Sys mv Lib2.class GooClass/.
Sys mv Lib3.class GooClass/.
Sys mv Lib.java GooSrc/.
Sys mv Lib2.java GooSrc/.
Sys mv Lib3.java GooSrc
CheckFiles GooZip/Lib.zip GooJar/Lib.jar GooSrc/Lib.java
CheckFiles GooSrc/Lib2.java GooSrc/Lib3.java GooJar/SubDir/Lib2.jar

echo 'public class Spe1 {public static void f(){}}' > Spe1.java
echo 'public class Spe2 {public static void f(){}}' > Spe2.java
echo 'public class Spe3 {public static void f(){}}' > Spe3.java
echo 'public class Spe4 {public static void f(){}}' > Spe4.java
Sys "$javac" ${TESTTOOLVMOPTS} Spe1.java
Sys "$javac" ${TESTTOOLVMOPTS} Spe2.java
Sys "$javac" ${TESTTOOLVMOPTS} Spe3.java
Sys "$javac" ${TESTTOOLVMOPTS} Spe4.java

UnixOnly Sys "$jar" cf "SpeJar/Spe:Colon.jar" Spe1.class
UnixOnly Sys "$jar" cf "SpeJar/Spe*wc.jar" Spe4.class
UnixOnly CheckFiles "SpeJar/Spe*wc.jar"

UnixOnly Sys "$jar" cf "StarJar/*jar.jar" Spe2.class
UnixOnly Sys "$jar" cf "StarJar/jar*.jar" Spe3.class
UnixOnly Sys "$jar" cf "StarJar/*jar*.jar" Spe4.class
UnixOnly CheckFiles "StarJar/*jar.jar" "StarJar/jar*.jar" "StarJar/*jar*.jar"

Sys "$jar" cf "SpeJar/Spe,Comma.jar" Spe2.class
Sys "$jar" cf "SpeJar/Spe;Semi.jar" Spe3.class

Sys "$jar" cf "MixJar/mix.jAr" Spe1.class
Sys "$jar" cf "MixJar/mix2.JAR" Spe2.class
Sys "$jar" cf "MixJar/mix3.zip" Spe3.class
Sys "$jar" cf "MixJar/.hiddenjar.jar" Spe4.class

Sys mv Spe*.class SpeClass/.
Sys mv Spe*.java SpeSrc/.
CheckFiles "SpeJar/Spe,Comma.jar" "SpeJar/Spe;Semi.jar" "SpeSrc/Spe2.java" "SpeSrc/Spe3.java" "SpeSrc/Spe4.java"
CheckFiles "MixJar/mix.jAr" "MixJar/mix2.JAR" "MixJar/mix3.zip" "MixJar/.hiddenjar.jar"

echo 'public class Main {public static void main(String[] a) {Lib.f();}}' > Main.java
echo 'public class Main1 {public static void main(String[] a) {Lib2.g();}}' > Main1.java
echo 'public class Main1b {public static void main(String[] a) {Spe1.f();}}' > Main1b.java
echo 'public class Main2 {public static void main(String[] a) {Spe2.f();}}' > Main2.java
echo 'public class Main3 {public static void main(String[] a) {Spe3.f();}}' > Main3.java
echo 'public class Main4 {public static void main(String[] a) {Spe4.f();}}' > Main4.java
echo 'public class Main5 {public static void main(String[] a) {Spe2.f(); Lib.f();}}' > Main5.java
echo 'public class Main6 {public static void main(String[] a) {Lib3.h();}}' > Main6.java


#----------------------------------------------------------------
# Verify expected behaviour with directory named "*"
#----------------------------------------------------------------
starDir() {
    printf "Running tests with directory named \"*\"\n"
    Sys rm -rf ./StarDir
    Sys mkdir -p StarDir/"*"
    Sys cp "GooClass/Lib2.class" "StarDir/*/Lib2.class"
    Sys "$jar" cf "StarDir/Lib3.jar" -C GooClass "Lib3.class"
    Sys "$jar" cf "StarDir/*/Lib.jar" -C GooClass "Lib.class"
    CheckFiles "StarDir/*/Lib.jar" "StarDir/*/Lib2.class" "StarDir/Lib3.jar"
    Sys cp Main6.java ./StarDir/.
    Sys cp Main.java  ./StarDir/"*"/.
    Sys cp Main1.java ./StarDir/"*"/.
    CPWC_DIR=`pwd`
    Sys cd StarDir
    Failure "$javac" ${TESTTOOLVMOPTS} -classpath "*" Main6.java
    Failure "$javac" ${TESTTOOLVMOPTS} -classpath "./*" Main6.java
    Sys rm -f Main6.*
    Sys cd "*"
    Success "$javac" ${TESTTOOLVMOPTS} -classpath "*" Main.java
    Success "$java" ${TESTVMOPTS} -classpath .${PS}"*" Main
    Success "$javac" ${TESTTOOLVMOPTS} Main1.java
    Success "$java" ${TESTVMOPTS} -classpath "." Main1
    Sys cd $CPWC_DIR

    Failure "$javac" ${TESTTOOLVMOPTS} -classpath "StarDir/*" Main6.java

    Success "$javac" ${TESTTOOLVMOPTS} -classpath StarDir/\* Main1.java
    Success "$java" ${TESTVMOPTS}  -classpath StarDir/\*:. Main1

    Success "$javac" ${TESTTOOLVMOPTS} -classpath "StarDir/*" Main1.java
    Success "$java" ${TESTVMOPTS}  -classpath ".${PS}StarDir/*" Main1

    Failure "$javac" ${TESTTOOLVMOPTS} -classpath StarDir/"\*/*" Main.java
    Success "$javac" ${TESTTOOLVMOPTS} -classpath StarDir/"*/*" Main.java

    Success "$java" ${TESTVMOPTS}  -classpath .${PS}StarDir/"*/*" Main
    Failure "$java" ${TESTVMOPTS}  -classpath .${PS}StarDir/"\*/*" Main

    Success "$javac" ${TESTTOOLVMOPTS} -classpath "StarDir/Lib3.jar" Main6.java
    Success "$java" ${TESTVMOPTS}  -classpath ".${PS}StarDir/Lib3.jar" Main6

    Success "$javac" ${TESTTOOLVMOPTS} -classpath StarDir/"*"/Lib.jar Main.java
    Success "$java" ${TESTVMOPTS}  -classpath .${PS}StarDir/"*"/Lib.jar Main
}
UnixOnly starDir

#----------------------------------------------------------------
# Verify the basic jar file works
#----------------------------------------------------------------
#baseline test to verify it works.
Success "$javac" ${TESTTOOLVMOPTS} -cp "GooJar/Lib.jar" Main.java
Success "$javac" ${TESTTOOLVMOPTS} -classpath "GooJar/Lib.jar" Main.java
Success "$java" ${TESTVMOPTS}  -classpath "GooJar/Lib.jar${PS}." Main
Success "$java" ${TESTVMOPTS}  -cp "GooJar/Lib.jar${PS}." Main

#basic test of one jar to be loaded
UnixOnly Success "$javac" ${TESTTOOLVMOPTS} -classpath "GooJar/*"       Main.java
         Success "$javac" ${TESTTOOLVMOPTS} -classpath "GooJar/*${PS}." Main.java
Success "$java" ${TESTVMOPTS}  -classpath "GooJar/*${PS}." Main
#in a subdir. First * should not load jars in subdirectories unless specified
Failure "$javac" ${TESTTOOLVMOPTS} -classpath "GooJar/*" Main1.java
Failure "$javac" ${TESTTOOLVMOPTS} -classpath "GooJar/*${PS}." Main1.java
Success "$javac" ${TESTTOOLVMOPTS} -cp "GooJar/SubDir/*" Main1.java
Success "$javac" ${TESTTOOLVMOPTS} -classpath "GooJar/SubDir/*" Main1.java
Success "$javac" ${TESTTOOLVMOPTS} --class-path "GooJar/SubDir/*" Main1.java
Success "$javac" ${TESTTOOLVMOPTS} --class-path="GooJar/SubDir/*" Main1.java
#Same with launcher. Should not load jar in subdirectories unless specified
Failure "$java" ${TESTVMOPTS}  -classpath "GooJar/*${PS}." Main1
Success "$java" ${TESTVMOPTS}  -classpath "GooJar/SubDir/*${PS}." Main1
Success "$java" ${TESTVMOPTS}  -cp "GooJar/SubDir/*${PS}." Main1

Success env CLASSPATH="GooJar/SubDir/*" "$javac" ${TESTTOOLVMOPTS} Main1.java
Success env CLASSPATH="GooJar/SubDir/*${PS}." "$java" ${TESTVMOPTS} Main1
#----------------------------------------------------------------
# Verify the jar files in 2 directories
#----------------------------------------------------------------
Success "$javac" ${TESTTOOLVMOPTS} -classpath "GooJar/Lib.jar${PS}SpeJar/Spe,Comma.jar" Main5.java
Success "$java" ${TESTVMOPTS}  -classpath "GooJar/Lib.jar${PS}SpeJar/Spe,Comma.jar${PS}." Main5

Success "$javac" ${TESTTOOLVMOPTS} -classpath "GooJar/*${PS}SpeJar/*" Main5.java
Success "$java" ${TESTVMOPTS}  -classpath "GooJar/*${PS}SpeJar/*${PS}." Main5

#----------------------------------------------------------------
# Verify jar file and class file in same directory.
#----------------------------------------------------------------
Success "$javac" ${TESTTOOLVMOPTS} -classpath "JarNClass/*${PS}" Main.java
Success "$java" ${TESTVMOPTS}  -classpath "JarNClass/*${PS}." Main

#----------------------------------------------------------------
# Verify these odd jar files work explicitly on classpath, kind of
# a baseline. Last one is also a test with * in a jar name.
#----------------------------------------------------------------
Failure "$javac" ${TESTTOOLVMOPTS} -classpath "SpeJar/Spe:Colon.jar" Main1.java

Success "$javac" ${TESTTOOLVMOPTS} -classpath "SpeJar/Spe,Comma.jar" Main2.java
Success "$java" ${TESTVMOPTS}  -classpath "SpeJar/Spe,Comma.jar${PS}." Main2

UnixOnly Success "$javac" ${TESTTOOLVMOPTS} -classpath "SpeJar/Spe;Semi.jar" Main3.java
UnixOnly Success "$java" ${TESTVMOPTS}  -classpath "SpeJar/Spe;Semi.jar${PS}." Main3

UnixOnly Success "$javac" ${TESTTOOLVMOPTS} -classpath "SpeJar/Spe*wc.jar" Main4.java
UnixOnly Success "$java" ${TESTVMOPTS}  -classpath "SpeJar/Spe*wc.jar${PS}." Main4
#----------------------------------------------------------------
# Verify these odd jar files work with classpath wildcard.
#----------------------------------------------------------------

speJar() {
    printf "Running tests with jar file names containing special characters\n"
#     Failure "$javac" ${TESTTOOLVMOPTS} -classpath "SpeJar/*" Main1.java
#     Success "$java" ${TESTVMOPTS}  -classpath "SpeJar/*" Main1

    Success "$javac" ${TESTTOOLVMOPTS} -classpath "SpeJar/*" Main2.java
    Success "$java" ${TESTVMOPTS}  -classpath "SpeJar/*${PS}." Main2

    Success "$javac" ${TESTTOOLVMOPTS} -classpath "SpeJar/*" Main3.java
    Success "$java" ${TESTVMOPTS}  -classpath "SpeJar/*${PS}." Main3

    Success "$javac" ${TESTTOOLVMOPTS} -classpath "SpeJar/*" Main4.java
    Success "$java" ${TESTVMOPTS}  -classpath "SpeJar/*${PS}." Main4
}
UnixOnly speJar

#----------------------------------------------------------------
# Verify these jar files with asterisk in jar file name
#----------------------------------------------------------------
starJar() {
    printf "Running tests with jar file names containing \"*\"\n"
    Success "$javac" ${TESTTOOLVMOPTS} -classpath "StarJar/*jar.jar" Main2.java
    Success "$java" ${TESTVMOPTS}  -classpath "StarJar/*jar.jar${PS}." Main2

    Success "$javac" ${TESTTOOLVMOPTS} -classpath "StarJar/jar*.jar" Main3.java
    Success "$java" ${TESTVMOPTS}  -classpath "StarJar/jar*.jar${PS}." Main3

    Success "$javac" ${TESTTOOLVMOPTS} -classpath "StarJar/*jar*.jar" Main4.java
    Success "$java" ${TESTVMOPTS}  -classpath "StarJar/*jar*.jar${PS}." Main4

    Success "$javac" ${TESTTOOLVMOPTS} -classpath "StarJar/*" Main2.java
    Success "$java" ${TESTVMOPTS}  -classpath "StarJar/*${PS}." Main2

    Success "$javac" ${TESTTOOLVMOPTS} -classpath "StarJar/*" Main3.java
    Success "$java" ${TESTVMOPTS}  -classpath "StarJar/*${PS}." Main3

    Success "$javac" ${TESTTOOLVMOPTS} -classpath "StarJar/*" Main4.java
    Success "$java" ${TESTVMOPTS}  -classpath "StarJar/*${PS}." Main4
}
UnixOnly starJar

#----------------------------------------------------------------
# Verify these jar files with varying extensions
#----------------------------------------------------------------
# Mixed case extensions should not be loaded.
Failure "$javac" ${TESTTOOLVMOPTS} -classpath "MixJar/*" Main1b.java
Success "$javac" ${TESTTOOLVMOPTS} -classpath "MixJar/mix.jAr" Main1b.java
Failure "$javac" ${TESTTOOLVMOPTS} -classpath "MixJar/*" Main1b

#upper case, .JAR, extension should be loaded
UnixOnly Success "$javac" ${TESTTOOLVMOPTS} -classpath       "MixJar/*" Main2.java
         Success "$javac" ${TESTTOOLVMOPTS} -classpath ".${PS}MixJar/*" Main2.java

Success "$java" ${TESTVMOPTS}  -classpath "MixJar/*${PS}." Main2
# zip extensions should not be loaded
Failure "$javac" ${TESTTOOLVMOPTS} -classpath "MixJar/*" Main3.java
Success "$javac" ${TESTTOOLVMOPTS} -classpath "MixJar/mix3.zip" Main3.java
Failure "$java" ${TESTVMOPTS}  -classpath "MixJar/*${PS}." Main3
# unix "hidden" file
UnixOnly Success "$javac" ${TESTTOOLVMOPTS} -classpath "MixJar/*" Main4.java
UnixOnly Success "$java" ${TESTVMOPTS}  -classpath "MixJar/*${PS}." Main4

Cleanup

Bottom Line
#----------------------------------------------------------------
