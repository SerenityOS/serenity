OBJS = \
    AClientConnection.o \
    AWavLoader.o

LIBRARY = libaudio.a

install:
	mkdir -p $(SERENITY_BASE_DIR)/Root/usr/include/LibAudio/
	cp *.h $(SERENITY_BASE_DIR)/Root/usr/include/LibAudio/
	cp $(LIBRARY) $(SERENITY_BASE_DIR)/Root/usr/lib/

include ../../Makefile.common
