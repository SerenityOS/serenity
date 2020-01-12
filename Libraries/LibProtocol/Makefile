OBJS = \
    Download.o \
    Client.o

LIBRARY = libprotocol.a

Download.cpp Client.cpp: ../../Servers/ProtocolServer/ProtocolClientEndpoint.h ../../Servers/ProtocolServer/ProtocolServerEndpoint.h
../../Servers/ProtocolServer/ProtocolClientEndpoint.h ../../Servers/ProtocolServer/ProtocolServerEndpoint.h:
	@flock $(dir $(@)) $(MAKE) -C $(dir $(@))

include ../../Makefile.common
