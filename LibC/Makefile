AK_OBJS = \
    ../AK/StringImpl.o \
    ../AK/String.o \
    ../AK/StringBuilder.o \
    ../AK/FileSystemPath.o \
    ../AK/StdLibExtras.o \
    ../AK/kmalloc.o

SHAREDGRAPHICS_OBJS = \
    ../SharedGraphics/Painter.o \
    ../SharedGraphics/Font.o \
    ../SharedGraphics/Rect.o \
    ../SharedGraphics/GraphicsBitmap.o \
    ../SharedGraphics/CharacterBitmap.o \
    ../SharedGraphics/Color.o

LIBC_OBJS = \
       stdio.o \
       unistd.o \
       string.o \
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
       math.o \
       utime.o \
       sys/select.o \
       sys/socket.o \
       poll.o \
       locale.o \
       entry.o

ASM_OBJS = setjmp.no

CPP_OBJS = $(AK_OBJS) $(WIDGETS_OBJS) $(LIBC_OBJS) $(SHAREDGRAPHICS_OBJS)

LIBRARY = LibC.a
ARCH_FLAGS =
STANDARD_FLAGS = -std=c++17 -nostdinc++ -nostdlib -nostdinc
LIBC_FLAGS = -ffreestanding -fno-stack-protector -fno-builtin
WARNING_FLAGS = -Wextra -Wall -Wundef -Wcast-qual -Wwrite-strings -Wimplicit-fallthrough
FLAVOR_FLAGS = -march=i686 -m32 -fno-exceptions -fno-rtti
OPTIMIZATION_FLAGS = -Os
INCLUDE_FLAGS = -I.. -I.

DEFINES = -DSERENITY -DUSERLAND -DSANITIZE_PTRS

CXXFLAGS = -MMD -MP $(WARNING_FLAGS) $(OPTIMIZATION_FLAGS) $(LIBC_FLAGS) $(FLAVOR_FLAGS) $(ARCH_FLAGS) $(STANDARD_FLAGS) $(INCLUDE_FLAGS) $(DEFINES)
CXX = clang
LD = ld
AR = ar
LDFLAGS = -T linker.ld --strip-debug -melf_i386 --gc-sections

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

