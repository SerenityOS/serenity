include ../Makefile.common

SHAREDGRAPHICS_OBJS = \
    ../SharedGraphics/Painter.o \
    ../SharedGraphics/StylePainter.o \
    ../SharedGraphics/Font.o \
    ../SharedGraphics/Rect.o \
    ../SharedGraphics/GraphicsBitmap.o \
    ../SharedGraphics/CharacterBitmap.o \
    ../SharedGraphics/Color.o \
    ../SharedGraphics/PNGLoader.o

LIBGUI_OBJS = \
    GPainter.o \
    GButton.o \
    GCheckBox.o \
    GEventLoop.o \
    GLabel.o \
    GTextBox.o \
    GScrollBar.o \
    GStatusBar.o \
    GWidget.o \
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
    GModel.o \
    GVariant.o \
    GShortcut.o \
    GTextEditor.o \
    GClipboard.o \
    GSortingProxyModel.o \
    GStackWidget.o \
    GScrollableWidget.o \
    GMessageBox.o \
    GInputBox.o \
    GDialog.o \
    GDesktop.o \
    GProgressBar.o \
    GAbstractView.o \
    GItemView.o \
    GIcon.o \
    GFrame.o \
    GTreeView.o \
    GFileSystemModel.o \
    GFilePicker.o \
    GDirectoryModel.o \
    GSplitter.o \
    GSpinBox.o \
    GGroupBox.o \
    GSlider.o \
    GResizeCorner.o \
    GTabWidget.o \
    GRadioButton.o \
    GWindow.o

OBJS = $(SHAREDGRAPHICS_OBJS) $(LIBGUI_OBJS)

LIBRARY = libgui.a
DEFINES += -DUSERLAND

all: $(LIBRARY)

$(LIBRARY): $(OBJS)
	@echo "LIB $@"; $(AR) rcs $@ $(OBJS)

.cpp.o:
	@echo "CXX $<"; $(CXX) $(CXXFLAGS) -o $@ -c $<

-include $(OBJS:%.o=%.d)

clean:
	@echo "CLEAN"; rm -f $(LIBRARY) $(OBJS) *.d

install: $(LIBRARY)
	mkdir -p ../Root/usr/include/LibGUI
	# Copy headers
	rsync -r -a --include '*/' --include '*.h' --exclude '*' . ../Root/usr/include/LibGUI
	# Install the library
	cp $(LIBRARY) ../Root/usr/lib
