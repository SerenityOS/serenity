#

# This makefile is only used to update b.jar and directory a,
# which are checked in to SCCS.

JBIN=../../../../../build/solaris/bin

all: b.jar

b.jar: classes
	$(JBIN)/jar cmf manifest b.jar -C b p/B.class

classes:
	$(JBIN)/javac -sourcepath a:b ?/p/*.java

clean:
	rm -rf b.jar ?/p/*.class
