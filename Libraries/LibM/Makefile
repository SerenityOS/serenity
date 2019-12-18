OBJS = math.o

LIBRARY = libm.a

install:
	mkdir -p $(SERENITY_BASE_DIR)/Root/usr/include/sys/
	mkdir -p $(SERENITY_BASE_DIR)/Root/usr/lib/
	cp *.h $(SERENITY_BASE_DIR)/Root/usr/include/
	cp $(LIBRARY) $(SERENITY_BASE_DIR)/Root/usr/lib/

include ../../Makefile.common
