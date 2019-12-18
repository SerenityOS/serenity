OBJS = \
    main.o \
    TextWidget.o \
    background.png.o

PROGRAM = Welcome

LDFLAGS = -lgui -ldraw -lipc -lcore -lc

.SUFFIXES: .png
%.png.o: %.png
	@echo "LINK $<"; $(LINK) --relocatable --format binary --output $@ $<

include ../../Makefile.common
