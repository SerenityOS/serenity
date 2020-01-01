include ../../../Makefile.common

DYNLIBRARY = libDynamicLib.so

EXTRA_CLEAN = *.o *.so

.PHONY: clean all

all: $(DYNLIBRARY)

DynamicLib.o: DynamicLib.cpp
	$(CXX) -DDEBUG -fPIC -isystem../../../ -o $@ -c $<

$(DYNLIBRARY): DynamicLib.o
	$(CXX) -shared -o $(DYNLIBRARY) $<
