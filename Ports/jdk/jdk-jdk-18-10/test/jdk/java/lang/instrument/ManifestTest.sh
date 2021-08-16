#
# Copyright (c) 2008, 2015, Oracle and/or its affiliates. All rights reserved.
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
# @bug 6274276
# @summary JLI JAR manifest processing should ignore leading and trailing white space.
# @author Daniel D. Daugherty
#
# @key intermittent
# @run build ManifestTestApp ExampleForBootClassPath
# @run shell/timeout=900 ManifestTest.sh
#

make_a_JAR() {
    # version_line and premain_line are required
    version_line="Manifest-Version: 1.0"
    premain_line="Premain-Class: ${AGENT}"
    boot_cp_line=""
    expect_boot_cp_line="ExampleForBootClassPath was not loaded."
    can_redef_line=""
    expect_redef_line="isRedefineClassesSupported()=false"
    can_retrans_line=""
    expect_retrans_line="isRetransformClassesSupported()=false"
    can_set_nmp_line=""
    expect_set_nmp_line="isNativeMethodPrefixSupported()=false"
    # some tests create directories with spaces in their name,
    # explicitly delete these.
    to_be_deleted=""

    while [ $# != 0 ] ; do
        case "$1" in
        defaults)
            # just use the defaults for the test
            ;;

        boot_cp_line1)
            boot_cp_line="Boot-Class-Path: no_white_space"
            expect_boot_cp_line="ExampleForBootClassPath was loaded."
            mkdir -p no_white_space
            cp -p $OUT_OF_THE_WAY/ExampleForBootClassPath.class no_white_space
            ;;

        boot_cp_line2)
            boot_cp_line="Boot-Class-Path:  has_leading_blank"
            expect_boot_cp_line="ExampleForBootClassPath was loaded."
            to_be_deleted=" has_leading_blank"
            mkdir -p has_leading_blank " has_leading_blank"
            # the good class is in the directory without the blank
            cp -p $OUT_OF_THE_WAY/ExampleForBootClassPath.class \
                has_leading_blank
            # the bad class is in the directory with the blank
            cp -p $OUT_OF_THE_WAY/ExampleForBootClassPath.class.bad \
                " has_leading_blank/ExampleForBootClassPath.class"
            ;;

        boot_cp_line3)
            boot_cp_line="Boot-Class-Path: has_trailing_blank "
            expect_boot_cp_line="ExampleForBootClassPath was loaded."
            to_be_deleted="has_trailing_blank "
            mkdir -p has_trailing_blank "has_trailing_blank "
            # the good class is in the directory without the blank
            cp -p $OUT_OF_THE_WAY/ExampleForBootClassPath.class \
                has_trailing_blank
            # the bad class is in the directory with the blank
            cp -p $OUT_OF_THE_WAY/ExampleForBootClassPath.class.bad \
                "has_trailing_blank /ExampleForBootClassPath.class"
            ;;

        boot_cp_line4)
            boot_cp_line="Boot-Class-Path:  has_leading_and_trailing_blank "
            expect_boot_cp_line="ExampleForBootClassPath was loaded."
            to_be_deleted=" has_leading_and_trailing_blank "
            mkdir -p has_leading_and_trailing_blank \
                " has_leading_and_trailing_blank "
            # the good class is in the directory without the blanks
            cp -p $OUT_OF_THE_WAY/ExampleForBootClassPath.class \
                has_leading_and_trailing_blank
            # the bad class is in the directory with the blanks
            cp -p $OUT_OF_THE_WAY/ExampleForBootClassPath.class.bad \
                " has_leading_and_trailing_blank /ExampleForBootClassPath.class"
            ;;

        boot_cp_line5)
            boot_cp_line="Boot-Class-Path: has_embedded blank"
            expect_boot_cp_line="ExampleForBootClassPath was loaded."
            to_be_deleted="has_embedded blank"
            mkdir -p has_embedded "has_embedded blank"
            # the good class is in the first blank separated word
            cp -p $OUT_OF_THE_WAY/ExampleForBootClassPath.class has_embedded
            # the bad class is in the directory with the blank
            cp -p $OUT_OF_THE_WAY/ExampleForBootClassPath.class.bad \
                "has_embedded blank/ExampleForBootClassPath.class"
            ;;

        can_redef_line1)
            can_redef_line="Can-Redefine-Classes: true"
            expect_redef_line="isRedefineClassesSupported()=true"
            ;;

        can_redef_line2)
            can_redef_line="Can-Redefine-Classes:  true"
            expect_redef_line="isRedefineClassesSupported()=true"
            ;;

        can_redef_line3)
            can_redef_line="Can-Redefine-Classes: true "
            expect_redef_line="isRedefineClassesSupported()=true"
            ;;

        can_redef_line4)
            can_redef_line="Can-Redefine-Classes:  true "
            expect_redef_line="isRedefineClassesSupported()=true"
            ;;

        can_redef_line5)
            can_redef_line="Can-Redefine-Classes: false"
            ;;

        can_redef_line6)
            can_redef_line="Can-Redefine-Classes:  false"
            ;;

        can_redef_line7)
            can_redef_line="Can-Redefine-Classes: false "
            ;;

        can_redef_line8)
            can_redef_line="Can-Redefine-Classes:  false "
            ;;

        can_redef_line9)
            # this line makes the jar command unhappy and that's
            # not what we're testing so skip this case
            can_redef_line="Can-Redefine-Classes:"
            ;;

        can_redef_line10)
            can_redef_line="Can-Redefine-Classes: "
            ;;

        can_redef_line11)
            can_redef_line="Can-Redefine-Classes:  "
            ;;

        can_retrans_line1)
            can_retrans_line="Can-Retransform-Classes: true"
            expect_retrans_line="isRetransformClassesSupported()=true"
            ;;

        can_retrans_line2)
            can_retrans_line="Can-Retransform-Classes:  true"
            expect_retrans_line="isRetransformClassesSupported()=true"
            ;;

        can_retrans_line3)
            can_retrans_line="Can-Retransform-Classes: true "
            expect_retrans_line="isRetransformClassesSupported()=true"
            ;;

        can_retrans_line4)
            can_retrans_line="Can-Retransform-Classes:  true "
            expect_retrans_line="isRetransformClassesSupported()=true"
            ;;

        can_retrans_line5)
            can_retrans_line="Can-Retransform-Classes: false"
            ;;

        can_retrans_line6)
            can_retrans_line="Can-Retransform-Classes:  false"
            ;;

        can_retrans_line7)
            can_retrans_line="Can-Retransform-Classes: false "
            ;;

        can_retrans_line8)
            can_retrans_line="Can-Retransform-Classes:  false "
            ;;

        can_retrans_line9)
            # this line makes the jar command unhappy and that's
            # not what we're testing so skip this case
            can_retrans_line="Can-Retransform-Classes:"
            ;;

        can_retrans_line10)
            can_retrans_line="Can-Retransform-Classes: "
            ;;

        can_retrans_line11)
            can_retrans_line="Can-Retransform-Classes:  "
            ;;

        can_set_nmp_line1)
            can_set_nmp_line="Can-Set-Native-Method-Prefix: true"
            expect_set_nmp_line="isNativeMethodPrefixSupported()=true"
            ;;

        can_set_nmp_line2)
            can_set_nmp_line="Can-Set-Native-Method-Prefix:  true"
            expect_set_nmp_line="isNativeMethodPrefixSupported()=true"
            ;;

        can_set_nmp_line3)
            can_set_nmp_line="Can-Set-Native-Method-Prefix: true "
            expect_set_nmp_line="isNativeMethodPrefixSupported()=true"
            ;;

        can_set_nmp_line4)
            can_set_nmp_line="Can-Set-Native-Method-Prefix:  true "
            expect_set_nmp_line="isNativeMethodPrefixSupported()=true"
            ;;

        can_set_nmp_line5)
            can_set_nmp_line="Can-Set-Native-Method-Prefix: false"
            ;;

        can_set_nmp_line6)
            can_set_nmp_line="Can-Set-Native-Method-Prefix:  false"
            ;;

        can_set_nmp_line7)
            can_set_nmp_line="Can-Set-Native-Method-Prefix: false "
            ;;

        can_set_nmp_line8)
            can_set_nmp_line="Can-Set-Native-Method-Prefix:  false "
            ;;

        can_set_nmp_line9)
            # this line makes the jar command unhappy and that's
            # not what we're testing so skip this case
            can_set_nmp_line="Can-Set-Native-Method-Prefix:"
            ;;

        can_set_nmp_line10)
            can_set_nmp_line="Can-Set-Native-Method-Prefix: "
            ;;

        can_set_nmp_line11)
            can_set_nmp_line="Can-Set-Native-Method-Prefix:  "
            ;;

        premain_line1)
            premain_line="Premain-Class:  ${AGENT}"
            ;;

        premain_line2)
            premain_line="Premain-Class: ${AGENT} "
            ;;

        premain_line3)
            premain_line="Premain-Class:  ${AGENT} "
            ;;

        version_line1)
            version_line="Manifest-Version:  1.0"
            ;;

        version_line2)
            version_line="Manifest-Version: 1.0 "
            ;;

        version_line3)
            version_line="Manifest-Version:  1.0 "
            ;;

        *)
            echo "ERROR: invalid test token"
            exit 1
        esac
        shift
    done

    echo "${version_line}" >  ${AGENT}.mf
    echo "${premain_line}" >> ${AGENT}.mf

    if [ -n "$boot_cp_line" ]; then
        echo "${boot_cp_line}" >> ${AGENT}.mf
    fi

    if [ -n "$can_redef_line" ]; then
        echo "${can_redef_line}" >> ${AGENT}.mf
    fi

    if [ -n "$can_retrans_line" ]; then
        echo "${can_retrans_line}" >> ${AGENT}.mf
    fi

    if [ -n "$can_set_nmp_line" ]; then
        echo "${can_set_nmp_line}" >> ${AGENT}.mf
    fi

    rm -f ${AGENT}.jar
    ${JAR} ${TESTTOOLVMOPTS} cvfm ${AGENT}.jar ${AGENT}.mf ${AGENT}.class

    echo "$expect_boot_cp_line" > expect_boot_cp_line
    echo "$expect_redef_line"   > expect_redef_line
    echo "$expect_retrans_line" > expect_retrans_line
    echo "$expect_set_nmp_line" > expect_set_nmp_line
}

if [ "${TESTJAVA}" = "" ]
then
  echo "TESTJAVA not set.  Test cannot execute.  Failed."
  exit 1
fi

if [ "${COMPILEJAVA}" = "" ]
then
  COMPILEJAVA="${TESTJAVA}"
fi
echo "COMPILEJAVA=${COMPILEJAVA}"

if [ "${TESTSRC}" = "" ]
then
  echo "TESTSRC not set.  Test cannot execute.  Failed."
  exit 1
fi

if [ "${TESTCLASSES}" = "" ]
then
  echo "TESTCLASSES not set.  Test cannot execute.  Failed."
  exit 1
fi

JAR="${COMPILEJAVA}/bin/jar"
JAVAC="${COMPILEJAVA}"/bin/javac
JAVA="${TESTJAVA}"/bin/java

# Now that ManifestTestApp.class is built, we move
# ExampleForBootClassPath.class so that it cannot be found
# by default
OUT_OF_THE_WAY=out_of_the_way
mkdir $OUT_OF_THE_WAY
mv "${TESTCLASSES}/ExampleForBootClassPath.class" $OUT_OF_THE_WAY

# create a bad version of ExampleForBootClassPath.class
# so we can tell when the wrong version is run
sed 's/return 15/return 42/' "${TESTSRC}"/ExampleForBootClassPath.java \
    > ExampleForBootClassPath.java
"$JAVAC" ${TESTJAVACOPTS} ${TESTTOOLVMOPTS} ExampleForBootClassPath.java
mv ExampleForBootClassPath.class \
    $OUT_OF_THE_WAY/ExampleForBootClassPath.class.bad
mv ExampleForBootClassPath.java \
    $OUT_OF_THE_WAY/ExampleForBootClassPath.java.bad

AGENT=ManifestTestAgent
# We compile the agent in the working directory instead of with
# a build task because we construct a different agent JAR file
# for each test case.
${JAVAC} ${TESTJAVACOPTS} ${TESTTOOLVMOPTS} -d . ${TESTSRC}/${AGENT}.java

FAIL_MARKER=fail_marker
rm -f $FAIL_MARKER

while read token; do
    echo
    echo "===== begin test case: $token ====="
    make_a_JAR "$token"

    "${JAVA}" ${TESTVMOPTS} -javaagent:${AGENT}.jar \
        -classpath "${TESTCLASSES}" ManifestTestApp > output.log 2>&1
    result=$?

    cat output.log

    if [ "$result" = 0 ]; then
        echo "PASS: ManifestTestApp exited with status of 0."
    else
        echo "FAIL: ManifestTestApp exited with status of $result"
        touch $FAIL_MARKER
    fi

    MESG="Hello from ${AGENT}!"
    grep -s "$MESG" output.log > /dev/null
    result=$?
    if [ "$result" = 0 ]; then
        echo "PASS: found '$MESG' in the test output"
    else
        echo "FAIL: did NOT find '$MESG' in the test output"
        touch $FAIL_MARKER
    fi

    MESG=`cat expect_boot_cp_line | tr -d '\n\r'`
    grep -s "$MESG" output.log > /dev/null
    result=$?
    if [ "$result" = 0 ]; then
        echo "PASS: found '$MESG' in the test output"
    else
        echo "FAIL: did NOT find '$MESG' in the test output"
        touch $FAIL_MARKER
    fi

    MESG=`cat expect_redef_line | tr -d '\n\r'`
    grep -s "$MESG" output.log > /dev/null
    result=$?
    if [ "$result" = 0 ]; then
        echo "PASS: found '$MESG' in the test output"
    else
        echo "FAIL: did NOT find '$MESG' in the test output"
        touch $FAIL_MARKER
    fi

    MESG=`cat expect_retrans_line | tr -d '\n\r'`
    grep -s "$MESG" output.log > /dev/null
    result=$?
    if [ "$result" = 0 ]; then
        echo "PASS: found '$MESG' in the test output"
    else
        echo "FAIL: did NOT find '$MESG' in the test output"
        touch $FAIL_MARKER
    fi

    MESG=`cat expect_set_nmp_line | tr -d '\n\r'`
    grep -s "$MESG" output.log > /dev/null
    result=$?
    if [ "$result" = 0 ]; then
        echo "PASS: found '$MESG' in the test output"
    else
        echo "FAIL: did NOT find '$MESG' in the test output"
        touch $FAIL_MARKER
    fi

    #clean up any problematic directories
    if [ -n "$to_be_deleted" ]; then
        echo "Test removing [$to_be_deleted]"
        rm -rf "$to_be_deleted"
    fi

    echo "===== end test case: $token ====="
    echo
done << EOF
defaults
version_line1
version_line2
version_line3
premain_line1
premain_line2
premain_line3
boot_cp_line1
boot_cp_line2
boot_cp_line3
boot_cp_line4
boot_cp_line5
can_redef_line1
can_redef_line2
can_redef_line3
can_redef_line4
can_redef_line5
can_redef_line6
can_redef_line7
can_redef_line8
can_redef_line10
can_redef_line11
can_retrans_line1
can_retrans_line2
can_retrans_line3
can_retrans_line4
can_retrans_line5
can_retrans_line6
can_retrans_line7
can_retrans_line8
can_retrans_line10
can_retrans_line11
can_set_nmp_line1
can_set_nmp_line2
can_set_nmp_line3
can_set_nmp_line4
can_set_nmp_line5
can_set_nmp_line6
can_set_nmp_line7
can_set_nmp_line8
can_set_nmp_line10
can_set_nmp_line11
EOF

if [ -f $FAIL_MARKER ]; then
    exit 1
else
    exit 0
fi
