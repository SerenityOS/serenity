OBJS = \
    main.o \
    InspectorWidget.o

PROGRAM = Browser

LIB_DEPS = GUI HTML Draw IPC Protocol Core

main.cpp: ../../Libraries/LibHTML/CSS/PropertyID.h
../../Libraries/LibHTML/CSS/PropertyID.h:
	@flock ../../Libraries/LibHTML $(MAKE) -C ../../Libraries/LibHTML

main.cpp: ../../Servers/ProtocolServer/ProtocolClientEndpoint.h

../../Servers/ProtocolServer/ProtocolClientEndpoint.h: ../../Servers/ProtocolServer/ProtocolClient.ipc | IPCCOMPILER
	@echo "IPC $<"; $(IPCCOMPILER) $< > $@

include ../../Makefile.common
