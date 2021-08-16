#
# Copyright (c) 2002, 2020, Oracle and/or its affiliates. All rights reserved.
#
# Redistribution and use in source and binary forms, with or without
# modification, are permitted provided that the following conditions
# are met:
#
#   - Redistributions of source code must retain the above copyright
#     notice, this list of conditions and the following disclaimer.
#
#   - Redistributions in binary form must reproduce the above copyright
#     notice, this list of conditions and the following disclaimer in the
#     documentation and/or other materials provided with the distribution.
#
#   - Neither the name of Oracle nor the names of its
#     contributors may be used to endorse or promote products derived
#     from this software without specific prior written permission.
#
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
# IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
# THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
# PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
# CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
# EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
# PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
# PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
# LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
# NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
# SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
#

SOURCEPATH=src
CLASSES=build
DIST=dist
RESOURCES=resources

J2DBENCH_CLASSES = \
	$(CLASSES)/j2dbench/J2DBench.class \
	$(CLASSES)/j2dbench/Destinations.class \
	$(CLASSES)/j2dbench/Group.class \
	$(CLASSES)/j2dbench/Result.class \
	$(CLASSES)/j2dbench/Modifier.class \
	$(CLASSES)/j2dbench/Node.class \
	$(CLASSES)/j2dbench/Option.class \
	$(CLASSES)/j2dbench/ResultSet.class \
	$(CLASSES)/j2dbench/TestEnvironment.class \
	$(CLASSES)/j2dbench/Test.class \
	$(CLASSES)/j2dbench/ui/EnableButton.class \
	$(CLASSES)/j2dbench/ui/CompactLayout.class \
	$(CLASSES)/j2dbench/tests/ImageTests.class \
	$(CLASSES)/j2dbench/tests/RenderTests.class \
	$(CLASSES)/j2dbench/tests/PixelTests.class \
	$(CLASSES)/j2dbench/tests/MiscTests.class \
	$(CLASSES)/j2dbench/tests/iio/IIOTests.class \
	$(CLASSES)/j2dbench/tests/iio/InputTests.class \
	$(CLASSES)/j2dbench/tests/iio/InputImageTests.class \
	$(CLASSES)/j2dbench/tests/iio/InputStreamTests.class \
	$(CLASSES)/j2dbench/tests/iio/OutputTests.class \
	$(CLASSES)/j2dbench/tests/iio/OutputImageTests.class \
	$(CLASSES)/j2dbench/tests/iio/OutputStreamTests.class \
	$(CLASSES)/j2dbench/tests/text/TextConstructionTests.class \
	$(CLASSES)/j2dbench/tests/text/TextMeasureTests.class \
	$(CLASSES)/j2dbench/tests/text/TextRenderTests.class \
	$(CLASSES)/j2dbench/tests/text/TextTests.class \
	$(CLASSES)/j2dbench/tests/GraphicsTests.class

J2DANALYZER_CLASSES = \
	$(CLASSES)/j2dbench/report/IIOComparator.class \
	$(CLASSES)/j2dbench/report/HTMLSeriesReporter.class \
	$(CLASSES)/j2dbench/report/J2DAnalyzer.class \
	$(CLASSES)/j2dbench/report/XMLHTMLReporter.class

J2DBENCH_RESOURCES = \
	$(CLASSES)/j2dbench/tests/iio/images \
	$(CLASSES)/j2dbench/tests/cmm/images \
	$(CLASSES)/j2dbench/tests/text/textdata

SCM_DIRs = .hg .svn CVS RCS SCCS Codemgr_wsdata deleted_files

all: mkdirs J2DBench.jar J2DAnalyzer.jar

run: mkdirs J2DBench.jar
	java -jar J2DBench.jar

analyze: mkdirs J2DAnalyzer.jar
	java -jar J2DAnalyzer.jar

J2DBench.jar: \
	$(J2DBENCH_CLASSES) $(J2DBENCH_RESOURCES) \
	$(CLASSES)/j2dbench.manifest
	jar cvmf $(CLASSES)/j2dbench.manifest $(DIST)/J2DBench.jar -C $(CLASSES) j2dbench

J2DAnalyzer.jar: $(J2DANALYZER_CLASSES) $(CLASSES)/j2danalyzer.manifest
	jar cvmf $(CLASSES)/j2danalyzer.manifest \
		$(DIST)/J2DAnalyzer.jar -C $(CLASSES) j2dbench/report

$(CLASSES)/j2dbench/tests/iio/images: $(RESOURCES)/images
	cp -r $< $@
	cd $@ && rm -rf $(SCM_DIRs)

$(CLASSES)/j2dbench/tests/text/textdata: $(RESOURCES)/textdata
	cp -r $< $@
	cd $@ && rm -rf $(SCM_DIRs)

$(CLASSES)/j2dbench/tests/cmm/images: $(RESOURCES)/cmm_images
	cp -r $< $@
	cd $@ && rm -rf $(SCM_DIRs)

$(CLASSES)/j2dbench.manifest:
	echo "Main-Class: j2dbench.J2DBench" > $@

$(CLASSES)/j2danalyzer.manifest:
	echo "Main-Class: j2dbench.report.J2DAnalyzer" > $@

$(DIST):
	mkdir $(DIST)
$(CLASSES):
	mkdir $(CLASSES)

mkdirs: $(DIST) $(CLASSES)

$(CLASSES)/j2dbench/%.class: $(SOURCEPATH)/j2dbench/%.java
	javac -g:none -source 1.7 -target 1.7 -d $(CLASSES) -sourcepath $(SOURCEPATH) $<

clean:
	rm -rf $(CLASSES)
	rm -rf $(DIST)
