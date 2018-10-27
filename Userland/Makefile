OBJS = \
       id.o \
       sh.o \
       ps.o \
       ls.o \
       pwd.o \
       sleep.o \
       date.o \
       true.o \
       false.o \
       hostname.o \
       cat.o \
       uname.o

APPS = \
       id \
       sh \
       ps \
       ls \
       pwd \
       sleep \
       date \
       true \
       false \
       hostname \
       cat \
       uname

ARCH_FLAGS =
STANDARD_FLAGS = -std=c++17 -nostdinc++ -nostdlib
USERLAND_FLAGS = -ffreestanding -fno-stack-protector -fno-ident
WARNING_FLAGS = -Wextra -Wall -Wundef -Wcast-qual -Wwrite-strings
FLAVOR_FLAGS = -march=i386 -mregparm=3 -m32 -fno-exceptions -fno-rtti -fmerge-all-constants -fno-unroll-loops -fno-pie -fno-pic
OPTIMIZATION_FLAGS = -Os -fno-asynchronous-unwind-tables
INCLUDE_FLAGS = -I.. -I.

DEFINES = -DSERENITY -DSANITIZE_PTRS -DUSERLAND

CXXFLAGS = $(WARNING_FLAGS) $(OPTIMIZATION_FLAGS) $(USERLAND_FLAGS) $(FLAVOR_FLAGS) $(ARCH_FLAGS) $(STANDARD_FLAGS) $(INCLUDE_FLAGS) $(DEFINES)
CXX = g++-8
LD = ld
AR = ar
LDFLAGS = -static --strip-debug -melf_i386 --build-id=none -z norelro -z now -e _start --gc-sections

all: $(OBJS) $(APPS)

id: id.o
	$(LD) -o $@ $(LDFLAGS) $< ../LibC/LibC.a

sh: sh.o
	$(LD) -o $@ $(LDFLAGS) $< ../LibC/LibC.a

ps: ps.o
	$(LD) -o $@ $(LDFLAGS) $< ../LibC/LibC.a

ls: ls.o
	$(LD) -o $@ $(LDFLAGS) $< ../LibC/LibC.a

pwd: pwd.o
	$(LD) -o $@ $(LDFLAGS) $< ../LibC/LibC.a

sleep: sleep.o
	$(LD) -o $@ $(LDFLAGS) $< ../LibC/LibC.a

date: date.o
	$(LD) -o $@ $(LDFLAGS) $< ../LibC/LibC.a

true: true.o
	$(LD) -o $@ $(LDFLAGS) $< ../LibC/LibC.a

false: false.o
	$(LD) -o $@ $(LDFLAGS) $< ../LibC/LibC.a

hostname: hostname.o
	$(LD) -o $@ $(LDFLAGS) $< ../LibC/LibC.a

cat: cat.o
	$(LD) -o $@ $(LDFLAGS) $< ../LibC/LibC.a

uname: uname.o
	$(LD) -o $@ $(LDFLAGS) $< ../LibC/LibC.a

.cpp.o:
	@echo "CXX $<"; $(CXX) $(CXXFLAGS) -o $@ -c $<

clean:
	@echo "CLEAN"; rm -f $(LIBRARY) $(OBJS)

