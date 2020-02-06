OBJS = \
    ClientConnection.o \
    WavWriter.o \
    WavLoader.o

LIBRARY = libaudio.a

ClientConnection.cpp: ../../Servers/AudioServer/AudioClientEndpoint.h
../../Servers/AudioServer/AudioClientEndpoint.h:
	@flock $(dir $(@)) $(MAKE) -C $(dir $(@))

install:
	mkdir -p $(SERENITY_BASE_DIR)/Root/usr/include/LibAudio/
	cp *.h $(SERENITY_BASE_DIR)/Root/usr/include/LibAudio/
	cp $(LIBRARY) $(SERENITY_BASE_DIR)/Root/usr/lib/

include ../../Makefile.common
