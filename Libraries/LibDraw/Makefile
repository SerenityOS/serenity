include ../../Makefile.common

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
DEFINES += -DUSERLAND

all: $(LIBRARY)

$(LIBRARY): $(OBJS)
	@echo "LIB $@"; $(AR) rcs $@ $(OBJS) $(LIBS)

.cpp.o:
	@echo "CXX $<"; $(CXX) $(CXXFLAGS) -o $@ -c $<

-include $(OBJS:%.o=%.d)

clean:
	@echo "CLEAN"; rm -f $(LIBRARY) $(OBJS) *.d
