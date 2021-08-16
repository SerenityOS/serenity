#
# Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
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

ifneq "x$(ALT_BOOTDIR)" "x"
	BOOTDIR := $(ALT_BOOTDIR)
endif

ifeq "x$(BOOTDIR)" "x"
	JDK_HOME := $(shell dirname $(shell which java))/..
else
	JDK_HOME := $(BOOTDIR)
endif

SRC_DIR = src
BUILD_DIR = build
DST_DIR = dist
OUTPUT_DIR = $(BUILD_DIR)/classes
TESTLIBRARY_DIR = ../../../../../test/lib

JAVAC = $(JDK_HOME)/bin/javac
JAR = $(JDK_HOME)/bin/jar

SRC_FILES = $(shell find $(SRC_DIR) -name '*.java')
LIB_FILES = $(shell find $(TESTLIBRARY_DIR)/jdk/test/lib/ \
    $(TESTLIBRARY_DIR)/jdk/test/lib/process \
    $(TESTLIBRARY_DIR)/jdk/test/lib/util \
    $(TESTLIBRARY_DIR)/jtreg \
    -maxdepth 1 -name '*.java')
WB_SRC_FILES = $(shell find $(TESTLIBRARY_DIR)/sun/hotspot $(TESTLIBRARY_DIR)/jdk/test/whitebox -name '*.java')
EXPORTS=--add-exports java.base/jdk.internal.jimage=ALL-UNNAMED \
	--add-exports java.base/jdk.internal.misc=ALL-UNNAMED \
	--add-exports java.base/jdk.internal.reflect=ALL-UNNAMED \
	--add-exports java.base/jdk.internal.access=ALL-UNNAMED

MAIN_CLASS = sun.hotspot.tools.ctw.CompileTheWorld

.PHONY: clean cleantmp

all: $(DST_DIR)/ctw.zip cleantmp

clean: cleantmp
	@rm -rf $(DST_DIR)

cleantmp:
	@rm -rf filelist wb_filelist manifest.mf
	@rm -rf $(BUILD_DIR)

$(DST_DIR):
	@mkdir -p $@

$(DST_DIR)/ctw.sh: $(DST_DIR)
	echo '$${JAVA_HOME}/bin/java $${JAVA_OPTIONS} $(EXPORTS) -XX:-UseCounterDecay -Xbatch "-XX:CompileCommand=exclude,java/lang/invoke/MethodHandle.*" -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xbootclasspath/a:wb.jar -jar ctw.jar $$@' > $@
	chmod a+x $@

$(DST_DIR)/ctw.jar: filelist $(DST_DIR)/wb.jar
	@mkdir -p $(OUTPUT_DIR)
	$(JAVAC) $(EXPORTS) -sourcepath $(SRC_DIR) -d $(OUTPUT_DIR) -cp $(DST_DIR)/wb.jar @filelist
	$(JAR) --create --file=$@ --main-class $(MAIN_CLASS) -C $(OUTPUT_DIR) .
	@rm -rf $(OUTPUT_DIR)

$(DST_DIR)/wb.jar: wb_filelist $(DST_DIR)
	@mkdir -p $(OUTPUT_DIR)
	$(JAVAC)  -sourcepath $(TESTLIBRARY_DIR) \
		-d $(OUTPUT_DIR) \
		-cp $(OUTPUT_DIR) \
		@wb_filelist
	$(JAR) --create --file=$@ -C $(OUTPUT_DIR) .
	@rm -rf $(OUTPUT_DIR)

$(DST_DIR)/ctw.zip: $(DST_DIR)/ctw.sh $(DST_DIR)/wb.jar $(DST_DIR)/ctw.jar
	zip -j $@ $?

wb_filelist: $(WB_SRC_FILES)
	@rm -f $@
	@echo $(WB_SRC_FILES) > $@

filelist: $(SRC_FILES) $(LIB_FILES)
	@rm -f $@
	@echo $(SRC_FILES) $(LIB_FILES) > $@
