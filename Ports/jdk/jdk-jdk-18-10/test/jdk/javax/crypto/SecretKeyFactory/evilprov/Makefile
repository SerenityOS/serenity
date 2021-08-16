#
# Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

# Java paths
#JAVA_BASE=PATH_TO_JAVA_IMG_DIR
JAVABIN=$(JAVA_BASE)/bin
JAVAC=$(JAVABIN)/javac
JAVA=$(JAVABIN)/java
JAR=$(JAVABIN)/jar 
JARSIGNER=$(JAVABIN)/jarsigner

# Compile-time flags and paths
JFLAGS=-Xlint:all
SRCPATH=com/evilprovider
CLASSDST=classes

PROVJAR=evilprov.jar
KSTORE=PATH_TO_KEYSTORE
KALIAS=PLACE_SIGNING_ALIAS_HERE
MODVER=1.0

all: $(PROVJAR)

%.class: %.java
	mkdir -p $(CLASSDST)
	$(JAVAC) -d $(CLASSDST) $(JFLAGS) $<

$(PROVJAR): $(SRCPATH)/EvilHmacSHA1.class $(SRCPATH)/EvilProvider.class module-info.class
	$(JAR) --create --file $(PROVJAR) --module-version $(MODVER) -C $(CLASSDST) .

signed: $(PROVJAR)
	jarsigner -keystore $(KSTORE) $(PROVJAR).jar $(KALIAS)

clean:
	rm -rf $(CLASSDST) $(PROVJAR)
