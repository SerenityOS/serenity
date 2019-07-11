include ../../Makefile.common

OBJS = \
    FontEditor.o \
    GlyphMapWidget.o \
    GlyphEditorWidget.o \
    main.o

APP = FontEditor

include ../Makefile.common

FontEditor.cpp: UI_FontEditorBottom.h

UI_FontEditorBottom.h: FontEditorBottom.frm
	../../DevTools/FormCompiler/FormCompiler $< > $@

