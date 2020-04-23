OBJS = \
    CharacterMap.o \
    CharacterMapFile.o \

LIBRARY = libkeyboard.a

install:
	mkdir -p $(SERENITY_BASE_DIR)/Root/usr/include/LibKeyboard/
	cp *.h $(SERENITY_BASE_DIR)/Root/usr/include/LibKeyboard/
	cp $(LIBRARY) $(SERENITY_BASE_DIR)/Root/usr/lib/

include ../../Makefile.common
