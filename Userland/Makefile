OBJS = \
       id.o

ARCH_FLAGS =
STANDARD_FLAGS = -std=c++17 -nostdinc++ -nostdlib
USERLAND_FLAGS = -ffreestanding -fno-stack-protector -fno-ident
WARNING_FLAGS = -Wextra -Wall -Wundef -Wcast-qual -Wwrite-strings
FLAVOR_FLAGS = -fomit-frame-pointer -mregparm=3 -march=i386 -m32 -fno-exceptions -fno-rtti -ffunction-sections -fdata-sections -fmerge-all-constants -fno-unroll-loops -falign-functions=1 -falign-jumps=1 -falign-loops=1 -fno-pie -fno-pic
OPTIMIZATION_FLAGS = -Os -fno-asynchronous-unwind-tables
INCLUDE_FLAGS = -I.. -I.

DEFINES = -DSERENITY -DSANITIZE_PTRS

CXXFLAGS = $(WARNING_FLAGS) $(OPTIMIZATION_FLAGS) $(USERLAND_FLAGS) $(FLAVOR_FLAGS) $(ARCH_FLAGS) $(STANDARD_FLAGS) $(INCLUDE_FLAGS) $(DEFINES)
CXX = g++
LD = ld
AR = ar
LDFLAGS = -T linker.ld --strip-debug -melf_i386 --gc-sections --build-id=none -z norelro -z now

all: $(OBJS)

.cpp.o:
	@echo "CXX $<"; $(CXX) $(CXXFLAGS) -o $@ -c $<

clean:
	@echo "CLEAN"; rm -f $(LIBRARY) $(OBJS)

