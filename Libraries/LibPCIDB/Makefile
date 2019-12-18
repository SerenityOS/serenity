OBJS = \
    Database.o

LIBRARY = libpcidb.a

install:
	mkdir -p $(SERENITY_BASE_DIR)/Root/usr/include/LibPCIDB/
	cp ./*.h $(SERENITY_BASE_DIR)/Root/usr/include/LibPCIDB/
	cp $(LIBRARY) $(SERENITY_BASE_DIR)/Root/usr/lib/

include ../../Makefile.common
