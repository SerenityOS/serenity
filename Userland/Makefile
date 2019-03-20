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
       chown.o \
       top.o \
       df.o \
       ln.o \
       su.o \
       env.o \
       stat.o \
       ping.o \
       uc.o \
       tc.o \
       host.o \
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
       chown \
       top \
       ln \
       df \
       su \
       env \
       stat \
       ping \
       uc \
       tc \
       host \
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
LDFLAGS = -L ../LibC

all: $(OBJS) $(APPS)

id: id.o
	$(LD) -o $@ $(LDFLAGS) $< -lc

sh: sh.o
	$(LD) -o $@ $(LDFLAGS) $< -lc

ps: ps.o
	$(LD) -o $@ $(LDFLAGS) $< -lc

ls: ls.o
	$(LD) -o $@ $(LDFLAGS) $< -lc

fgrep: fgrep.o
	$(LD) -o $@ $(LDFLAGS) $< -lc

sleep: sleep.o
	$(LD) -o $@ $(LDFLAGS) $< -lc

date: date.o
	$(LD) -o $@ $(LDFLAGS) $< -lc

true: true.o
	$(LD) -o $@ $(LDFLAGS) $< -lc

false: false.o
	$(LD) -o $@ $(LDFLAGS) $< -lc

hostname: hostname.o
	$(LD) -o $@ $(LDFLAGS) $< -lc

cat: cat.o
	$(LD) -o $@ $(LDFLAGS) $< -lc

uname: uname.o
	$(LD) -o $@ $(LDFLAGS) $< -lc

clear: clear.o
	$(LD) -o $@ $(LDFLAGS) $< -lc

tst: tst.o
	$(LD) -o $@ $(LDFLAGS) $< -lc

mm: mm.o
	$(LD) -o $@ $(LDFLAGS) $< -lc

dmesg: dmesg.o
	$(LD) -o $@ $(LDFLAGS) $< -lc

kill: kill.o
	$(LD) -o $@ $(LDFLAGS) $< -lc

tty: tty.o
	$(LD) -o $@ $(LDFLAGS) $< -lc

mkdir: mkdir.o
	$(LD) -o $@ $(LDFLAGS) $< -lc

touch: touch.o
	$(LD) -o $@ $(LDFLAGS) $< -lc

sync: sync.o
	$(LD) -o $@ $(LDFLAGS) $< -lc

more: more.o
	$(LD) -o $@ $(LDFLAGS) $< -lc

guitest2: guitest2.o
	$(LD) -o $@ $(LDFLAGS) -L../LibGUI $< -lgui -lc

sysctl: sysctl.o
	$(LD) -o $@ $(LDFLAGS) $< -lc

pape: pape.o
	$(LD) -o $@ $(LDFLAGS) $< -lc

cp: cp.o
	$(LD) -o $@ $(LDFLAGS) $< -lc

rm: rm.o
	$(LD) -o $@ $(LDFLAGS) $< -lc

rmdir: rmdir.o
	$(LD) -o $@ $(LDFLAGS) $< -lc

chmod: chmod.o
	$(LD) -o $@ $(LDFLAGS) $< -lc

chown: chown.o
	$(LD) -o $@ $(LDFLAGS) $< -lc

top: top.o
	$(LD) -o $@ $(LDFLAGS) $< -lc

ln: ln.o
	$(LD) -o $@ $(LDFLAGS) $< -lc

df: df.o
	$(LD) -o $@ $(LDFLAGS) $< -lc

su: su.o
	$(LD) -o $@ $(LDFLAGS) $< -lc

env: env.o
	$(LD) -o $@ $(LDFLAGS) $< -lc

stat: stat.o
	$(LD) -o $@ $(LDFLAGS) $< -lc

ping: ping.o
	$(LD) -o $@ $(LDFLAGS) $< -lc

uc: uc.o
	$(LD) -o $@ $(LDFLAGS) $< -lc

tc: tc.o
	$(LD) -o $@ $(LDFLAGS) $< -lc

host: host.o
	$(LD) -o $@ $(LDFLAGS) $< -lc

.cpp.o:
	@echo "CXX $<"; $(CXX) $(CXXFLAGS) -o $@ -c $<

-include $(OBJS:%.o=%.d)

clean:
	@echo "CLEAN"; rm -f $(APPS) $(OBJS) *.d

