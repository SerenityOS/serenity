OBJS = pthread.o

LIBRARY = libpthread.a

POST_LIBRARY_BUILD = $(QUIET) $(MAKE) install

install:
	mkdir -p $(SERENITY_BASE_DIR)/Root/usr/lib/
	cp *.h $(SERENITY_BASE_DIR)/Root/usr/include/
	cp $(LIBRARY) $(SERENITY_BASE_DIR)/Root/usr/lib/

include ../../Makefile.common
