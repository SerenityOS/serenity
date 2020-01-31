OBJS = \
    main.o \
    TextWidget.o \
    background.png.o

PROGRAM = Welcome

LIB_DEPS = GUI Draw IPC Core

.SUFFIXES: .png
%.png.o: %.png
	@echo "LINK $<"
	$(QUIET) $(LINK) --relocatable --format binary -m elf_$(ARCH) --output $@ $<

include ../../Makefile.common
