#
# Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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


SOURCEPATH=src
CLASSES=build
DIST=dist
RESOURCES=resources

RENDERPERF_CLASSES = $(CLASSES)/renderperf/RenderPerfTest.class
RENDERPERFLCD_CLASSES = $(CLASSES)/renderperf/RenderPerfLCDTest.class
RENDERPERF_SOURCES = $(SOURCEPATH)/renderperf/RenderPerfTest.java
RENDERPERFLCD_SOURCES = $(SOURCEPATH)/renderperf/RenderPerfLCDTest.java

RENDERPERF_RESOURCES = $(CLASSES)/renderperf/images/duke.png

all: mkdirs $(DIST)/RenderPerfTest.jar $(DIST)/RenderPerfLCDTest.jar

run: mkdirs $(DIST)/RenderPerfTest.jar
	java -jar $(DIST)/RenderPerfTest.jar

$(DIST)/RenderPerfTest.jar: \
	$(RENDERPERF_CLASSES) $(RENDERPERF_RESOURCES) \
	$(CLASSES)/renderperf.manifest
	jar cvmf $(CLASSES)/renderperf.manifest $(DIST)/RenderPerfTest.jar -C $(CLASSES) .

$(DIST)/RenderPerfLCDTest.jar: \
	$(RENDERPERFLCD_CLASSES) $(RENDERPERFLCD_RESOURCES) \
	$(CLASSES)/renderperflcd.manifest
	jar cvmf $(CLASSES)/renderperflcd.manifest $(DIST)/RenderPerfLCDTest.jar -C $(CLASSES) .

$(CLASSES)/renderperf/images/%: $(RESOURCES)/renderperf/images/%
	cp -r $< $@


$(CLASSES)/renderperf.manifest:
	echo "Main-Class: renderperf.RenderPerfTest" > $@

$(CLASSES)/renderperflcd.manifest:
	echo "Main-Class: renderperf.RenderPerfLCDTest" > $@

$(DIST):
	mkdir  $(DIST)

$(CLASSES):
	mkdir $(CLASSES)
	mkdir -p $(CLASSES)/renderperf/images

mkdirs: $(DIST) $(CLASSES)

$(RENDERPERF_CLASSES): $(RENDERPERF_SOURCES)
	javac -g:none -d $(CLASSES) -sourcepath $(SOURCEPATH) $<

$(RENDERPERFLCD_CLASSES): $(RENDERPERFLCD_SOURCES)
	javac -g:none -d $(CLASSES) -sourcepath $(SOURCEPATH) $<

clean:
	rm -rf $(CLASSES)
	rm -rf $(DIST)
