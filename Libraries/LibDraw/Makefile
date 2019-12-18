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
    Emoji.o

LIBRARY = libdraw.a

install:
	mkdir -p $(SERENITY_BASE_DIR)/Root/usr/include/LibDraw/
	cp *.h $(SERENITY_BASE_DIR)/Root/usr/include/LibDraw/
	cp $(LIBRARY) $(SERENITY_BASE_DIR)/Root/usr/lib/

include ../../Makefile.common
