PROGRAM = test

AK_OBJS = \
    ../AK/String.o \
    ../AK/StringImpl.o \
    ../AK/SimpleMalloc.o \
    ../AK/kmalloc.o

VFS_OBJS = \
    AbstractScreen.o \
    FrameBufferSDL.o \
    EventLoop.o \
    EventLoopSDL.o \
    Object.o \
    Widget.o \
    RootWidget.o \
    ColorSDL.o \
    Painter.o \
    Label.o \
    Button.o \
    TerminalWidget.o \
    WindowManager.o \
    Font.o \
    Window.o \
    ClockWidget.o \
    test.o

OBJS = $(AK_OBJS) $(VFS_OBJS)

CXXFLAGS = -std=c++17 -O0 -W -Wall -Wextra -Wconversion -I. -I.. -g `sdl2-config --cflags` -DUSE_SDL

LDFLAGS = `sdl2-config --libs`

#test.o: BlockDevice.h FileBackedBlockDevice.h FileSystem.h Ext2FileSystem.h VirtualFileSystem.h FileHandle.h

all:	$(PROGRAM)

.cpp.o:
	$(CXX) $(CXXFLAGS) -o $@ -c $<

clean:
	rm -f $(OBJS) $(PROGRAM)

$(PROGRAM): $(OBJS)
	$(CXX) $(LDFLAGS) -o $@ $(OBJS)

