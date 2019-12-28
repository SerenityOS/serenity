OBJS = \
    Download.o \
    Client.o

LIBRARY = libprotocol.a

Download.cpp: ../../Servers/ProtocolServer/ProtocolClientEndpoint.h
../../Servers/ProtocolServer/ProtocolClientEndpoint.h:
	@flock $(dir $(@)) $(MAKE) -C $(dir $(@))

include ../../Makefile.common
