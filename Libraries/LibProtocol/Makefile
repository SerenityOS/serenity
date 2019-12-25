OBJS = \
    Download.o \
    Client.o

LIBRARY = libprotocol.a

Download.cpp: ../../Servers/ProtocolServer/ProtocolClientEndpoint.h
../../Servers/ProtocolServer/ProtocolClientEndpoint.h:
	@$(MAKE) -C $(dir $(@))

include ../../Makefile.common
