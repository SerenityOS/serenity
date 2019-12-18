OBJS = \
    FontEditor.o \
    GlyphMapWidget.o \
    GlyphEditorWidget.o \
    main.o

PROGRAM = FontEditor

LDFLAGS = -lgui -ldraw -lipc -lcore -lc

FontEditor.cpp: UI_FontEditorBottom.h

UI_FontEditorBottom.h: FontEditorBottom.frm
	../../DevTools/FormCompiler/FormCompiler $< > $@

EXTRA_CLEAN = UI_FontEditorBottom.h

include ../../Makefile.common
