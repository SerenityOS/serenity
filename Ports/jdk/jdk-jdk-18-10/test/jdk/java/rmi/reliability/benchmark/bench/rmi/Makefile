#
# Copyright (c) 2000, 2020, Oracle and/or its affiliates. All rights reserved.
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
#
#
# RMI benchmarks makefile
#

BUILD_DIR = ../..
JAVA_FILES = BenchServer.java \
	BenchServerImpl.java \
	BooleanArrayCalls.java \
	BooleanCalls.java \
	ByteArrayCalls.java \
	ByteCalls.java \
	CharArrayCalls.java \
	CharCalls.java \
	ClassLoading.java \
	DoubleArrayCalls.java \
	DoubleCalls.java \
	ExceptionCalls.java \
	ExportObjs.java \
	FloatArrayCalls.java \
	FloatCalls.java \
	IntArrayCalls.java \
	IntCalls.java \
	LongArrayCalls.java \
	LongCalls.java \
	Main.java \
	NullCalls.java \
	ObjArrayCalls.java \
	ObjTreeCalls.java \
	ProxyArrayCalls.java \
	RemoteObjArrayCalls.java \
	ShortArrayCalls.java \
	ShortCalls.java \
	SmallObjTreeCalls.java \
	TheTerminator.java

all: .classes altroot.dir

.classes: $(JAVA_FILES)
	javac -d $(BUILD_DIR) $(JAVA_FILES)
	touch .classes

altroot.dir:
	cd altroot ; $(MAKE)

altroot.clean:
	cd altroot ; $(MAKE) clean

clean: altroot.clean
	rm -f *.class .classes

