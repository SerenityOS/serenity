OBJS = \
    FontEditor.o \
    GlyphMapWidget.o \
    GlyphEditorWidget.o \
    main.o

PROGRAM = FontEditor

LIB_DEPS = GUI Draw Core IPC

FontEditor.cpp: UI_FontEditorBottom.h

UI_FontEditorBottom.h: FontEditorBottom.frm FORMCOMPILER
	$(QUIET) $(FORMCOMPILER) $< > $@

EXTRA_CLEAN = UI_FontEditorBottom.h

include ../../Makefile.common
