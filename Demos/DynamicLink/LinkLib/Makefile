include ../../../Makefile.common

DYNLIBRARY = libDynamicLib.so

EXTRA_CLEAN = *.o *.so

.PHONY: clean all

all: $(DYNLIBRARY)

DynamicLib.o: DynamicLib.cpp
	$(CXX) -DDEBUG -fPIC -isystem../../../ -o $@ -c $<

# FIXME: Why do I need -nostartfiles and -nofreestanding?
#     GCC isn't smart enough to not link crt0 against this dynamic lib
#     which is clearly wrong. Isn't it? We don't want _start...
$(DYNLIBRARY): DynamicLib.o
	$(CXX) -shared -nostartfiles -ffreestanding -o $(DYNLIBRARY) $<
