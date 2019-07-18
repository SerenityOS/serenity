include ../../Makefile.common

OBJS = \
    CharacterBitmap.o \
    Color.o \
    DisjointRectSet.o \
    Font.o \
    GraphicsBitmap.o \
    Painter.o \
    PNGLoader.o \
    Rect.o \
    StylePainter.o

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

install: $(LIBRARY)
	mkdir -p ../../Root/usr/include/LibGfx
	# Copy headers
	rsync -r -a --include '*/' --include '*.h' --exclude '*' . ../../Root/usr/include/LibGfx
	# Install the library
	cp $(LIBRARY) ../../Root/usr/lib
