OBJS = \
    main.o \
    TextWidget.o \
    background.png.o

PROGRAM = Welcome

LIB_DEPS = GUI Draw IPC Core

LDFLAGS = -static

.SUFFIXES: .png
%.png.o: %.png
	@echo "LINK $<"; $(LINK) --relocatable --format binary --output $@ $<

include ../../Makefile.common
