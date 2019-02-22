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
       guitest2.o \
       sysctl.o \
       pape.o \
       cp.o \
       rmdir.o \
       dmesg.o \
       chmod.o \
       top.o \
       df.o \
       ln.o \
       su.o \
       env.o \
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
       guitest2 \
       sysctl \
       pape \
       cp \
       rmdir \
       dmesg \
       chmod \
       top \
       ln \
       df \
       su \
       env \
       rm

ARCH_FLAGS =
STANDARD_FLAGS = -std=c++17
WARNING_FLAGS = -Wextra -Wall -Wundef -Wcast-qual -Wwrite-strings -Wimplicit-fallthrough
FLAVOR_FLAGS = -fno-exceptions -fno-rtti
OPTIMIZATION_FLAGS = -Os
INCLUDE_FLAGS = -I.. -I. -I../LibC

DEFINES = -DSERENITY -DSANITIZE_PTRS -DUSERLAND

CXXFLAGS = -MMD -MP $(WARNING_FLAGS) $(OPTIMIZATION_FLAGS) $(FLAVOR_FLAGS) $(ARCH_FLAGS) $(STANDARD_FLAGS) $(INCLUDE_FLAGS) $(DEFINES)
CXX = i686-pc-serenity-g++
LD = i686-pc-serenity-ld
AR = i686-pc-serenity-r

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

guitest2: guitest2.o
	$(LD) -o $@ $(LDFLAGS) $< ../LibGUI/LibGUI.a ../LibC/LibC.a

sysctl: sysctl.o
	$(LD) -o $@ $(LDFLAGS) $< ../LibC/LibC.a

pape: pape.o
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

ln: ln.o
	$(LD) -o $@ $(LDFLAGS) $< ../LibC/LibC.a

df: df.o
	$(LD) -o $@ $(LDFLAGS) $< ../LibC/LibC.a

su: su.o
	$(LD) -o $@ $(LDFLAGS) $< ../LibC/LibC.a

env: env.o
	$(LD) -o $@ $(LDFLAGS) $< ../LibC/LibC.a

.cpp.o:
	@echo "CXX $<"; $(CXX) $(CXXFLAGS) -o $@ -c $<

-include $(OBJS:%.o=%.d)

clean:
	@echo "CLEAN"; rm -f $(APPS) $(OBJS) *.d

