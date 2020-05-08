OBJS = \
    Download.o \
    Client.o

LIBRARY = libprotocol.a

Download.cpp Client.cpp: ../../Services/ProtocolServer/ProtocolClientEndpoint.h ../../Services/ProtocolServer/ProtocolServerEndpoint.h
../../Services/ProtocolServer/ProtocolClientEndpoint.h ../../Services/ProtocolServer/ProtocolServerEndpoint.h:
	@flock $(dir $(@)) $(MAKE) -C $(dir $(@))

include ../../Makefile.common
