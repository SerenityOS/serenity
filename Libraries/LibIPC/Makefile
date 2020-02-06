OBJS = \
    Endpoint.o \
    Message.o

LIBRARY = libipc.a

install:
	mkdir -p $(SERENITY_BASE_DIR)/Root/usr/include/LibIPC/
	cp *.h $(SERENITY_BASE_DIR)/Root/usr/include/LibIPC/
	cp $(LIBRARY) $(SERENITY_BASE_DIR)/Root/usr/lib/

include ../../Makefile.common
