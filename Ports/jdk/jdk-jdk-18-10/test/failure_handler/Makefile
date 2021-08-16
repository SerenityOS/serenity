#
# Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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
# This is a temporary standalone makefile
#

BUILD_DIR := $(shell pwd)/build
CLASSES_DIR := ${BUILD_DIR}/classes
IMAGE_DIR := ${BUILD_DIR}/image
RUN_DIR := $(shell pwd)/run
CLASSPATH := ${JTREG_HOME}/lib/jtreg.jar:${JAVA_HOME}/lib/tools.jar
SRC_DIR := src/share/classes/
SOURCES := ${SRC_DIR}/jdk/test/failurehandler/*.java                   \
           ${SRC_DIR}/jdk/test/failurehandler/action/*.java            \
           ${SRC_DIR}/jdk/test/failurehandler/jtreg/*.java             \
           ${SRC_DIR}/jdk/test/failurehandler/value/*.java

CONF_DIR = src/share/conf

JAVA_RELEASE = 15

TARGET_JAR = ${IMAGE_DIR}/lib/jtregFailureHandler.jar

OS_NAME := $(shell uname -o 2>&1)

ifeq ("${OS_NAME}", "Cygwin")
BUILD_DIR := $(shell cygpath -m "${BUILD_DIR}")
CLASSES_DIR := $(shell cygpath -m "${CLASSES_DIR}")
IMAGE_DIR := $(shell cygpath -m "${IMAGE_DIR}")
RUN_DIR := $(shell cygpath -m "${RUN_DIR}")
SRC_DIR := $(shell cygpath -m "${SRC_DIR}")
JAVA_HOME := $(shell cygpath -m "${JAVA_HOME}")
JTREG_HOME := $(shell cygpath -m "${JTREG_HOME}")
CLASSPATH := $(shell cygpath -pm "${CLASSPATH}")
endif

all: clean test

check_defined = $(foreach 1,$1,$(__check_defined))
__check_defined = $(if $(value $1),, $(error $1 is not set))

classes: require_env
	mkdir -p ${IMAGE_DIR}/bin ${IMAGE_DIR}/lib ${CLASSES_DIR}
	"${JAVA_HOME}"/bin/javac -target ${JAVA_RELEASE} -source ${JAVA_RELEASE} \
		-sourcepath "$(shell pwd)"                                           \
		-cp "${CLASSPATH}" 													 \
		-d ${CLASSES_DIR}                                  					 \
        ${SOURCES}
	"${JAVA_HOME}"/bin/jar cf "${TARGET_JAR}" -C "${CLASSES_DIR}" .
	"${JAVA_HOME}"/bin/jar uf "${TARGET_JAR}" -C "${CONF_DIR}" .

#
# Use JTREG_TEST_OPTS for test VM options
# Use JTREG_TESTS for jtreg tests parameter
#
test: require_env build
	rm -rf "${RUN_DIR}"
	mkdir -p "${RUN_DIR}"
	"${JTREG_HOME}"/bin/jtreg                                           \
        -jdk:"${JAVA_HOME}"                                                 \
        ${JTREG_TEST_OPTS}                                                  \
        -timeout:0.1 -va -retain:all                                        \
        -noreport                                                           \
        -agentvm                                                            \
        -thd:"${TARGET_JAR}"                                                \
        -th:jdk.test.failurehandler.jtreg.GatherProcessInfoTimeoutHandler   \
        -thtimeout:0                                                        \
        -od:"${TARGET_JAR}"                                                 \
        -o:jdk.test.failurehandler.jtreg.GatherDiagnosticInfoObserver       \
        -w:"${RUN_DIR}/JTwork"                                              \
        -r:"${RUN_DIR}/JTreport"                                            \
        $(if ${JTREG_TESTS}, ${JTREG_TESTS}, test)                          \
        && false || true

debug: JTREG_TEST_OPTS += "-J-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005'"
debug: test

require_env:
	$(call check_defined, JAVA_HOME)
	$(call check_defined, JTREG_HOME)

clean:
	rm -rf "${BUILD_DIR}" "${RUN_DIR}"

build: classes

.PHONY: all build classes test require_env clean
.DEFAULT: all
