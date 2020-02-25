OBJS = \
    Bitmap.o \
    CharacterBitmap.o \
    Color.o \
    DisjointRectSet.o \
    Emoji.o \
    Font.o \
    GIFLoader.o \
    ImageDecoder.o \
    PNGLoader.o \
    Painter.o \
    Palette.o \
    Point.o \
    Rect.o \
    Size.o \
    StylePainter.o \
    SystemTheme.o \
    Triangle.o

LIBRARY = libgfx.a

install:
	mkdir -p $(SERENITY_BASE_DIR)/Root/usr/include/LibGfx/
	cp *.h $(SERENITY_BASE_DIR)/Root/usr/include/LibGfx/
	cp $(LIBRARY) $(SERENITY_BASE_DIR)/Root/usr/lib/

include ../../Makefile.common
