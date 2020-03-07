OBJS = \
    main.o \
    InspectorWidget.o

PROGRAM = Browser

LIB_DEPS = GUI Web Gfx IPC Protocol Core

main.cpp: ../../Libraries/LibWeb/CSS/PropertyID.h
../../Libraries/LibWeb/CSS/PropertyID.h:
	@flock ../../Libraries/LibWeb $(MAKE) -C ../../Libraries/LibWeb

main.cpp: ../../Servers/ProtocolServer/ProtocolClientEndpoint.h
../../Servers/ProtocolServer/ProtocolClientEndpoint.h:
	@flock ../../Servers/ProtocolServer $(MAKE) -C $(dir $(@))

include ../../Makefile.common
