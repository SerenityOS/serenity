OBJS = \
    Thread.o \
    BackgroundAction.o

LIBRARY = libthread.a

install:
	mkdir -p $(SERENITY_BASE_DIR)/Root/usr/include/LibThread/
	cp *.h $(SERENITY_BASE_DIR)/Root/usr/include/LibThread/
	cp $(LIBRARY) $(SERENITY_BASE_DIR)/Root/usr/lib/

include ../../Makefile.common
