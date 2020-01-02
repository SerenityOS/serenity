include ../../../Makefile.common

DYNLIBRARY = libDynamicLib.so

EXTRA_CLEAN = *.o *.so

all: $(DYNLIBRARY)

DynamicLib.o: DynamicLib.cpp
	@echo "$(notdir $(CURDIR)): C++ $@"
	$(QUIET) $(CXX) -DDEBUG -fPIC -isystem../../../ -o $@ -c $<

$(DYNLIBRARY): DynamicLib.o
	@echo "$(notdir $(CURDIR)): DYLIB $@"
	$(QUIET) $(CXX) -shared -o $(DYNLIBRARY) $<
