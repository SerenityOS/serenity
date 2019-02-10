SHAREDGRAPHICS_OBJS = \
    ../SharedGraphics/Painter.o \
    ../SharedGraphics/Font.o \
    ../SharedGraphics/Rect.o \
    ../SharedGraphics/GraphicsBitmap.o \
    ../SharedGraphics/CharacterBitmap.o \
    ../SharedGraphics/Color.o

LIBGUI_OBJS = \
    GButton.o \
    GCheckBox.o \
    GEventLoop.o \
    GLabel.o \
    GListBox.o \
    GObject.o \
    GTextBox.o \
    GScrollBar.o \
    GWidget.o \
    GStyle.o \
    GWindow.o

OBJS = $(SHAREDGRAPHICS_OBJS) $(LIBGUI_OBJS)

LIBS = ../LibC/LibC.a

LIBRARY = LibGUI.a
ARCH_FLAGS =
STANDARD_FLAGS = -std=c++17 -nostdinc++ -nostdlib -nostdinc
LIBC_FLAGS = -ffreestanding -fno-stack-protector -fno-ident
WARNING_FLAGS = -Wextra -Wall -Wundef -Wcast-qual -Wwrite-strings
FLAVOR_FLAGS = -fomit-frame-pointer -march=i386 -m32 -fno-exceptions -fno-rtti -ffunction-sections -fdata-sections -fmerge-all-constants -fno-unroll-loops -fno-pie -fno-pic
OPTIMIZATION_FLAGS = -Oz -fno-asynchronous-unwind-tables
INCLUDE_FLAGS = -I../LibC -I.. -I.

DEFINES = -DSERENITY -DUSERLAND -DSANITIZE_PTRS -DLIBGUI

CXXFLAGS = -MMD -MP $(WARNING_FLAGS) $(OPTIMIZATION_FLAGS) $(LIBC_FLAGS) $(FLAVOR_FLAGS) $(ARCH_FLAGS) $(STANDARD_FLAGS) $(INCLUDE_FLAGS) $(DEFINES)
CXX = clang
LD = ld
AR = ar
LDFLAGS = -T linker.ld --strip-debug -melf_i386 --gc-sections

all: $(LIBRARY)

$(LIBRARY): $(OBJS)
	@echo "LIB $@"; $(AR) rcs $@ $(OBJS) $(LIBS)

.cpp.o:
	@echo "CXX $<"; $(CXX) $(CXXFLAGS) -o $@ -c $<

-include $(OBJS:%.o=%.d)

clean:
	@echo "CLEAN"; rm -f $(LIBRARY) $(OBJS) *.d

