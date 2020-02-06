OBJS = \
    CharacterBitmap.o \
    Color.o \
    DisjointRectSet.o \
    Font.o \
    GraphicsBitmap.o \
    Painter.o \
    PNGLoader.o \
    GIFLoader.o \
    ImageDecoder.o \
    Rect.o \
    StylePainter.o \
    SystemTheme.o \
    Palette.o \
    Emoji.o

LIBRARY = libgfx.a

install:
	mkdir -p $(SERENITY_BASE_DIR)/Root/usr/include/LibGfx/
	cp *.h $(SERENITY_BASE_DIR)/Root/usr/include/LibGfx/
	cp $(LIBRARY) $(SERENITY_BASE_DIR)/Root/usr/lib/

include ../../Makefile.common
