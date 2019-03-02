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
    GNotifier.o \
    GTextBox.o \
    GScrollBar.o \
    GStatusBar.o \
    GWidget.o \
    GStyle.o \
    GLayout.o \
    GBoxLayout.o \
    GMenuBar.o \
    GMenu.o \
    GMenuItem.o \
    GApplication.o \
    GAction.o \
    GFontDatabase.o \
    GToolBar.o \
    GTableView.o \
    GTableModel.o \
    GVariant.o \
    GShortcut.o \
    GWindow.o

OBJS = $(SHAREDGRAPHICS_OBJS) $(LIBGUI_OBJS)

LIBS = -lc

LIBRARY = libgui.a
STANDARD_FLAGS = -std=c++17
WARNING_FLAGS = -Wextra -Wall -Wundef -Wcast-qual -Wwrite-strings -Wimplicit-fallthrough
FLAVOR_FLAGS = -fno-exceptions -fno-rtti
OPTIMIZATION_FLAGS = -Os
INCLUDE_FLAGS = -I../LibC -I.. -I.

DEFINES = -DSERENITY -DUSERLAND -DSANITIZE_PTRS -DLIBGUI

CXXFLAGS = -MMD -MP $(WARNING_FLAGS) $(OPTIMIZATION_FLAGS) $(FLAVOR_FLAGS) $(STANDARD_FLAGS) $(INCLUDE_FLAGS) $(DEFINES)
CXX = i686-pc-serenity-g++
LD = i686-pc-serenity-ld
AR = i686-pc-serenity-ar

all: $(LIBRARY)

$(LIBRARY): $(OBJS)
	@echo "LIB $@"; $(AR) rcs $@ $(OBJS) $(LIBS)

.cpp.o:
	@echo "CXX $<"; $(CXX) $(CXXFLAGS) -o $@ -c $<

-include $(OBJS:%.o=%.d)

clean:
	@echo "CLEAN"; rm -f $(LIBRARY) $(OBJS) *.d

