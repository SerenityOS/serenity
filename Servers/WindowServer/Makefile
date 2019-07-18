include ../../Makefile.common

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
    WSEventLoop.o \
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
    WSWindowFrame.o \
    WSButton.o \
    WSCPUMonitor.o \
    WSCompositor.o \
    WSMenuManager.o \
    main.o

APP = WindowServer
OBJS = $(SHAREDGRAPHICS_OBJS) $(WINDOWSERVER_OBJS)

DEFINES += -DUSERLAND

all: $(APP)

$(APP): $(OBJS)
	$(LD) -o $(APP) $(LDFLAGS) $(OBJS) -lc -lcore

.cpp.o:
	@echo "CXX $<"; $(CXX) $(CXXFLAGS) -o $@ -c $<

-include $(OBJS:%.o=%.d)

clean:
	@echo "CLEAN"; rm -f $(APP) $(OBJS) *.d

