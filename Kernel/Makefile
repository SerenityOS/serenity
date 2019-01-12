KERNEL_OBJS = \
       _start.o \
       init.o \
       kmalloc.o \
       StdLib.o \
       i386.o \
       Process.o \
       i8253.o \
       Keyboard.o \
       CMOS.o \
       PIC.o \
       Syscall.o \
       IDEDiskDevice.o \
       MemoryManager.o \
       Console.o \
       IRQHandler.o \
       kprintf.o \
       ProcFileSystem.o \
       RTC.o \
       TTY.o \
       VirtualConsole.o \
       FIFO.o \
       Scheduler.o \
       DoubleBuffer.o \
       ELFImage.o \
       ELFLoader.o \
       KSyms.o \
       PS2MouseDevice.o \
       WindowComposer.o

VFS_OBJS = \
    ../VirtualFileSystem/DiskDevice.o \
    ../VirtualFileSystem/CharacterDevice.o \
    ../VirtualFileSystem/NullDevice.o \
    ../VirtualFileSystem/FullDevice.o \
    ../VirtualFileSystem/ZeroDevice.o \
    ../VirtualFileSystem/RandomDevice.o \
    ../VirtualFileSystem/FileSystem.o \
    ../VirtualFileSystem/DiskBackedFileSystem.o \
    ../VirtualFileSystem/Ext2FileSystem.o \
    ../VirtualFileSystem/VirtualFileSystem.o \
    ../VirtualFileSystem/FileDescriptor.o \
    ../VirtualFileSystem/SyntheticFileSystem.o

WIDGETS_OBJS = \
    ../Widgets/Window.o \
    ../Widgets/Painter.o \
    ../Widgets/WindowManager.o \
    ../Widgets/FrameBuffer.o \
    ../Widgets/GraphicsBitmap.o \
    ../Widgets/Object.o \
    ../Widgets/Rect.o \
    ../Widgets/Widget.o \
    ../Widgets/Font.o \
    ../Widgets/Color.o \
    ../Widgets/CharacterBitmap.o \
    ../Widgets/EventLoop.o \
    ../Widgets/Label.o \
    ../Widgets/Button.o \
    ../Widgets/MsgBox.o \
    ../Widgets/ListBox.o \
    ../Widgets/CheckBox.o \
    ../Widgets/TextBox.o \
    ../Widgets/AbstractScreen.o

AK_OBJS = \
    ../AK/String.o \
    ../AK/StringImpl.o \
    ../AK/StringBuilder.o \
    ../AK/FileSystemPath.o

OBJS = $(KERNEL_OBJS) $(VFS_OBJS) $(AK_OBJS) $(WIDGETS_OBJS)

NASM = nasm
KERNEL = kernel
BOOTLOADER = Boot/boot.bin
IMAGE = .floppy-image
ARCH_FLAGS =
STANDARD_FLAGS = -std=c++17 -nostdinc++ -nostdlib #-nostdinc
KERNEL_FLAGS = -ffreestanding -fno-stack-protector -fno-ident -fno-builtin
WARNING_FLAGS = -Wextra -Wall -Wundef -Wcast-qual -Wwrite-strings
FLAVOR_FLAGS = -mregparm=3 -march=i386 -m32 -fno-exceptions -fno-rtti -fmerge-all-constants -fno-unroll-loops -fno-pie -fno-pic
OPTIMIZATION_FLAGS = -Oz -fno-asynchronous-unwind-tables -fno-omit-frame-pointer
INCLUDE_FLAGS = -I.. -I.
#SUGGEST_FLAGS = -Wsuggest-final-types -Wsuggest-final-methods -Wsuggest-override #-Wsuggest-attribute=noreturn 

DEFINES = -DSERENITY -DKERNEL -DSANITIZE_PTRS

CXXFLAGS = $(WARNING_FLAGS) $(OPTIMIZATION_FLAGS) $(KERNEL_FLAGS) $(FLAVOR_FLAGS) $(ARCH_FLAGS) $(STANDARD_FLAGS) $(SUGGEST_FLAGS) $(INCLUDE_FLAGS) $(DEFINES)
CXX = clang
LD = ld
LDFLAGS = -T linker.ld --strip-debug -melf_i386 --gc-sections --build-id=none -z norelro -z now

all: $(KERNEL) $(IMAGE) kernel.map

kernel.map: kernel
	@echo "MKMAP $@"; sh mkmap.sh

$(KERNEL): $(OBJS)
	@echo "LD $@"; $(LD) $(LDFLAGS) -o $@ -Ttext 0x10000 $(OBJS)

$(IMAGE): $(KERNEL) $(BOOTLOADER)
	@echo "CREATE $@"; cat $(BOOTLOADER) $(KERNEL) > .tmp-floppy-image
	@dd if=/dev/zero bs=2M count=1 >> .tmp-floppy-image 2> /dev/null
	@dd if=.tmp-floppy-image of=.floppy-image bs=1440k count=1 2>/dev/null
	@rm -f .tmp-floppy-image

$(BOOTLOADER): Boot/boot.asm
	@echo "NASM $<"; $(NASM) -f bin -o $@ $<

.cpp.o:
	@echo "CXX $<"; $(CXX) $(CXXFLAGS) -o $@ -c $<

clean:
	@echo "CLEAN"; rm -f $(KERNEL) $(OBJS) $(BOOTLOADER) $(IMAGE)

