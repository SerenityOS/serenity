SHAREDGRAPHICS_OBJS = \
    ../../SharedGraphics/Painter.o \
    ../../SharedGraphics/StylePainter.o \
    ../../SharedGraphics/Font.o \
    ../../SharedGraphics/Rect.o \
    ../../SharedGraphics/GraphicsBitmap.o \
    ../../SharedGraphics/CharacterBitmap.o \
    ../../SharedGraphics/DisjointRectSet.o \
    ../../SharedGraphics/Color.o \
    ../../SharedGraphics/PNGLoader.o

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
    WSWindowSwitcher.o \
    WSClipboard.o \
    WSCursor.o \
    main.o

APP = WindowServer
OBJS = $(SHAREDGRAPHICS_OBJS) $(WINDOWSERVER_OBJS)

STANDARD_FLAGS = -std=c++17 -Wno-sized-deallocation
WARNING_FLAGS = -Wextra -Wall -Wundef -Wcast-qual -Wwrite-strings -Wimplicit-fallthrough
FLAVOR_FLAGS = -fno-exceptions -fno-rtti
OPTIMIZATION_FLAGS = -Os
INCLUDE_FLAGS = -I.. -I../.. -I. -I../../LibC
LDFLAGS = -L../../LibC

DEFINES = -DSERENITY -DSANITIZE_PTRS -DUSERLAND

CXXFLAGS = -MMD -MP $(WARNING_FLAGS) $(OPTIMIZATION_FLAGS) $(FLAVOR_FLAGS) $(STANDARD_FLAGS) $(INCLUDE_FLAGS) $(DEFINES)
CXX = i686-pc-serenity-g++
LD = i686-pc-serenity-g++
AR = i686-pc-serenity-ar

all: $(APP)

$(APP): $(OBJS)
	$(LD) -o $(APP) $(LDFLAGS) $(OBJS) -lc

.cpp.o:
	@echo "CXX $<"; $(CXX) $(CXXFLAGS) -o $@ -c $<

-include $(OBJS:%.o=%.d)

clean:
	@echo "CLEAN"; rm -f $(APPS) $(OBJS) *.d

