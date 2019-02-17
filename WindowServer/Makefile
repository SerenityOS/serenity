SHAREDGRAPHICS_OBJS = \
    ../SharedGraphics/Painter.o \
    ../SharedGraphics/Font.o \
    ../SharedGraphics/Rect.o \
    ../SharedGraphics/GraphicsBitmap.o \
    ../SharedGraphics/CharacterBitmap.o \
    ../SharedGraphics/Color.o

WINDOWSERVER_OBJS = \
    WSMessageReceiver.o \
    WSMessageLoop.o \
    WSWindow.o \
    WSWindowManager.o \
    WSScreen.o \
    WSMenuBar.o \
    WSMenu.o \
    WSMenuItem.o \
    WSClientConnection.o \
    main.o

APP = WindowServer
OBJS = $(SHAREDGRAPHICS_OBJS) $(WINDOWSERVER_OBJS)

ARCH_FLAGS =
STANDARD_FLAGS = -std=c++17 -nostdinc++ -nostdlib -nostdinc
USERLAND_FLAGS = -ffreestanding -fno-stack-protector
WARNING_FLAGS = -Wextra -Wall -Wundef -Wcast-qual -Wwrite-strings -Wimplicit-fallthrough
FLAVOR_FLAGS = -march=i686 -m32 -fno-exceptions -fno-rtti
OPTIMIZATION_FLAGS = -Os
INCLUDE_FLAGS = -I.. -I. -I../LibC

DEFINES = -DSERENITY -DSANITIZE_PTRS -DUSERLAND

CXXFLAGS = -MMD -MP $(WARNING_FLAGS) $(OPTIMIZATION_FLAGS) $(USERLAND_FLAGS) $(FLAVOR_FLAGS) $(ARCH_FLAGS) $(STANDARD_FLAGS) $(INCLUDE_FLAGS) $(DEFINES)
CXX = clang
LD = ld
AR = ar
LDFLAGS = -static --strip-debug -melf_i386 -e _start --gc-sections

all: $(APP)

$(APP): $(OBJS)
	$(LD) -o $(APP) $(LDFLAGS) $(OBJS) ../LibC/LibC.a

.cpp.o:
	@echo "CXX $<"; $(CXX) $(CXXFLAGS) -o $@ -c $<

-include $(OBJS:%.o=%.d)

clean:
	@echo "CLEAN"; rm -f $(APPS) $(OBJS) *.d

