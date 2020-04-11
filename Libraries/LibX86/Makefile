OBJS = \
    Instruction.o

LIBRARY = libx86.a

install:
	mkdir -p $(SERENITY_BASE_DIR)/Root/usr/include/LibX86/
	cp *.h $(SERENITY_BASE_DIR)/Root/usr/include/LibX86/
	cp $(LIBRARY) $(SERENITY_BASE_DIR)/Root/usr/lib/

include ../../Makefile.common
