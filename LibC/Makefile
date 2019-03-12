AK_OBJS = \
    ../AK/StringImpl.o \
    ../AK/String.o \
    ../AK/StringBuilder.o \
    ../AK/FileSystemPath.o \
    ../AK/StdLibExtras.o \
    ../AK/kmalloc.o

LIBC_OBJS = \
       SharedBuffer.o \
       stdio.o \
       unistd.o \
       string.o \
       strings.o \
       mman.o \
       dirent.o \
       stdlib.o \
       time.o \
       utsname.o \
       assert.o \
       signal.o \
       getopt.o \
       scanf.o \
       pwd.o \
       grp.o \
       times.o \
       termcap.o \
       stat.o \
       mntent.o \
       ctype.o \
       fcntl.o \
       termios.o \
       ulimit.o \
       qsort.o \
       ioctl.o \
       utime.o \
       sys/select.o \
       sys/socket.o \
       sys/wait.o \
       poll.o \
       locale.o \
       arpa/inet.o \
       crt0.o

ASM_OBJS = setjmp.no

CPP_OBJS = $(AK_OBJS) $(WIDGETS_OBJS) $(LIBC_OBJS)

LIBRARY = libc.a
STANDARD_FLAGS = -std=c++17
WARNING_FLAGS = -Wextra -Wall -Wundef -Wcast-qual -Wwrite-strings -Wimplicit-fallthrough
FLAVOR_FLAGS = -fno-exceptions -fno-rtti
OPTIMIZATION_FLAGS = -Os
INCLUDE_FLAGS = -I.. -I.

DEFINES = -DSERENITY -DUSERLAND -DSANITIZE_PTRS

CXXFLAGS = -MMD -MP $(WARNING_FLAGS) $(OPTIMIZATION_FLAGS) $(FLAVOR_FLAGS) $(STANDARD_FLAGS) $(INCLUDE_FLAGS) $(DEFINES)
CXX = i686-pc-serenity-g++
LD = i686-pc-serenity-ld
AR = i686-pc-serenity-ar

all: $(LIBRARY)

$(LIBRARY): $(CPP_OBJS) $(ASM_OBJS)
	@echo "LIB $@"; $(AR) rcs $@ $(CPP_OBJS) $(ASM_OBJS)

.cpp.o:
	@echo "CXX $<"; $(CXX) $(CXXFLAGS) -o $@ -c $<

%.no: %.asm
	@echo "NASM $@"; nasm -f elf -o $@ $<

-include $(OBJS:%.o=%.d)

clean:
	@echo "CLEAN"; rm -f $(LIBRARY) $(CPP_OBJS) $(ASM_OBJS) *.d

