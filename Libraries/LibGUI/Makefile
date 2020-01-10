OBJS = \
    GPainter.o \
    GButton.o \
    GCheckBox.o \
    GWindowServerConnection.o \
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
    GActionGroup.o \
    GFontDatabase.o \
    GToolBar.o \
    GTableView.o \
    GModel.o \
    GVariant.o \
    GShortcut.o \
    GTextEditor.o \
    GTextDocument.o \
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
    GAbstractColumnView.o \
    GItemView.o \
    GIcon.o \
    GFrame.o \
    GTreeView.o \
    GFileSystemModel.o \
    GFilePicker.o \
    GColumnsView.o \
    GSplitter.o \
    GSpinBox.o \
    GGroupBox.o \
    GSlider.o \
    GResizeCorner.o \
    GTabWidget.o \
    GRadioButton.o \
    GAbstractButton.o \
    GListView.o \
    GComboBox.o \
    GJsonArrayModel.o \
    GAboutDialog.o \
    GModelSelection.o \
    GLazyWidget.o \
    GCommand.o \
    GUndoStack.o \
    GDragOperation.o \
    GWindow.o

LIBRARY = libgui.a

GWindowServerConnection.cpp: ../../Servers/WindowServer/WindowServerEndpoint.h

../../Servers/WindowServer/WindowServerEndpoint.h:
	@flock $(dir $(@)) $(MAKE) -C $(dir $(@))

install:
	mkdir -p $(SERENITY_BASE_DIR)/Root/usr/include/LibGUI/
	cp ./*.h $(SERENITY_BASE_DIR)/Root/usr/include/LibGUI/
	cp $(LIBRARY) $(SERENITY_BASE_DIR)/Root/usr/lib/

include ../../Makefile.common
