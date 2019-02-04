OBJS = \
       id.o \
       sh.o \
       ps.o \
       ls.o \
       sleep.o \
       date.o \
       true.o \
       false.o \
       hostname.o \
       cat.o \
       uname.o \
       clear.o \
       tst.o \
       mm.o \
       kill.o \
       fgrep.o \
       tty.o \
       mkdir.o \
       touch.o \
       more.o \
       guitest.o \
       guitest2.o \
       sysctl.o \
       cp.o \
       rmdir.o \
       dmesg.o \
       chmod.o \
       top.o \
       rm.o

APPS = \
       id \
       sh \
       ps \
       ls \
       sleep \
       date \
       true \
       false \
       hostname \
       cat \
       uname \
       clear \
       tst \
       mm \
       kill \
       fgrep \
       tty \
       mkdir \
       touch \
       sync \
       more \
       guitest \
       guitest2 \
       sysctl \
       cp \
       rmdir \
       dmesg \
       chmod \
       top \
       rm

ARCH_FLAGS =
STANDARD_FLAGS = -std=c++17 -nostdinc++ -nostdlib -nostdinc
USERLAND_FLAGS = -ffreestanding -fno-stack-protector -fno-ident
WARNING_FLAGS = -Wextra -Wall -Wundef -Wcast-qual -Wwrite-strings
FLAVOR_FLAGS = -march=i386 -mregparm=3 -m32 -fno-exceptions -fno-rtti -fmerge-all-constants -fno-unroll-loops -fno-pie -fno-pic
OPTIMIZATION_FLAGS = -Oz -fno-asynchronous-unwind-tables
INCLUDE_FLAGS = -I.. -I. -I../LibC

DEFINES = -DSERENITY -DSANITIZE_PTRS -DUSERLAND

CXXFLAGS = -MMD -MP $(WARNING_FLAGS) $(OPTIMIZATION_FLAGS) $(USERLAND_FLAGS) $(FLAVOR_FLAGS) $(ARCH_FLAGS) $(STANDARD_FLAGS) $(INCLUDE_FLAGS) $(DEFINES)
CXX = clang
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

fgrep: fgrep.o
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

clear: clear.o
	$(LD) -o $@ $(LDFLAGS) $< ../LibC/LibC.a

tst: tst.o
	$(LD) -o $@ $(LDFLAGS) $< ../LibC/LibC.a

mm: mm.o
	$(LD) -o $@ $(LDFLAGS) $< ../LibC/LibC.a

dmesg: dmesg.o
	$(LD) -o $@ $(LDFLAGS) $< ../LibC/LibC.a

kill: kill.o
	$(LD) -o $@ $(LDFLAGS) $< ../LibC/LibC.a

tty: tty.o
	$(LD) -o $@ $(LDFLAGS) $< ../LibC/LibC.a

mkdir: mkdir.o
	$(LD) -o $@ $(LDFLAGS) $< ../LibC/LibC.a

touch: touch.o
	$(LD) -o $@ $(LDFLAGS) $< ../LibC/LibC.a

sync: sync.o
	$(LD) -o $@ $(LDFLAGS) $< ../LibC/LibC.a

more: more.o
	$(LD) -o $@ $(LDFLAGS) $< ../LibC/LibC.a

guitest: guitest.o
	$(LD) -o $@ $(LDFLAGS) $< ../LibC/LibC.a

guitest2: guitest2.o
	$(LD) -o $@ $(LDFLAGS) $< ../LibGUI/LibGUI.a ../LibC/LibC.a

sysctl: sysctl.o
	$(LD) -o $@ $(LDFLAGS) $< ../LibC/LibC.a

cp: cp.o
	$(LD) -o $@ $(LDFLAGS) $< ../LibC/LibC.a

rm: rm.o
	$(LD) -o $@ $(LDFLAGS) $< ../LibC/LibC.a

rmdir: rmdir.o
	$(LD) -o $@ $(LDFLAGS) $< ../LibC/LibC.a

chmod: chmod.o
	$(LD) -o $@ $(LDFLAGS) $< ../LibC/LibC.a

top: top.o
	$(LD) -o $@ $(LDFLAGS) $< ../LibC/LibC.a

.cpp.o:
	@echo "CXX $<"; $(CXX) $(CXXFLAGS) -o $@ -c $<

-include $(OBJS:%.o=%.d)

clean:
	@echo "CLEAN"; rm -f $(APPS) $(OBJS) *.d

