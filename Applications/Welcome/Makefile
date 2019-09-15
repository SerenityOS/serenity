include ../../Makefile.common

OBJS = \
    main.o \
    TextWidget.o \
    background.png.o

APP = Welcome

.SUFFIXES: .png
%.png.o: %.png
	@echo "LINK $<"; $(LINK) --relocatable --format binary --output $@ $<

include ../Makefile.common
