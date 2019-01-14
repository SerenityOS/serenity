AK_OBJS = \
    ../AK/StringImpl.o \
    ../AK/String.o \
    ../AK/StringBuilder.o \
    ../AK/FileSystemPath.o \
    ../AK/kmalloc.o

WIDGETS_OBJS = \
    ../Widgets/Painter.o \
    ../Widgets/Font.o \
    ../Widgets/GraphicsBitmap.o \
    ../Widgets/CharacterBitmap.o \
    ../Widgets/Color.o

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
       setjmp.o \
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
       gui.o \
       entry.o

OBJS = $(AK_OBJS) $(WIDGETS_OBJS) $(LIBC_OBJS)

LIBRARY = LibC.a
ARCH_FLAGS =
STANDARD_FLAGS = -std=c++17 -nostdinc++ -nostdlib -nostdinc
LIBC_FLAGS = -ffreestanding -fno-stack-protector -fno-ident
WARNING_FLAGS = -Wextra -Wall -Wundef -Wcast-qual -Wwrite-strings
FLAVOR_FLAGS = -fomit-frame-pointer -mregparm=3 -march=i386 -m32 -fno-exceptions -fno-rtti -ffunction-sections -fdata-sections -fmerge-all-constants -fno-unroll-loops -fno-pie -fno-pic
OPTIMIZATION_FLAGS = -Oz -fno-asynchronous-unwind-tables
INCLUDE_FLAGS = -I.. -I.

DEFINES = -DSERENITY -DUSERLAND -DSANITIZE_PTRS

CXXFLAGS = $(WARNING_FLAGS) $(OPTIMIZATION_FLAGS) $(LIBC_FLAGS) $(FLAVOR_FLAGS) $(ARCH_FLAGS) $(STANDARD_FLAGS) $(INCLUDE_FLAGS) $(DEFINES)
CXX = clang
LD = ld
AR = ar
LDFLAGS = -T linker.ld --strip-debug -melf_i386 --gc-sections --build-id=none -z norelro -z now

all: $(LIBRARY)

$(LIBRARY): $(OBJS)
	@echo "LIB $@"; $(AR) rcs $@ $(OBJS)

.cpp.o:
	@echo "CXX $<"; $(CXX) $(CXXFLAGS) -o $@ -c $<

clean:
	@echo "CLEAN"; rm -f $(LIBRARY) $(OBJS)

