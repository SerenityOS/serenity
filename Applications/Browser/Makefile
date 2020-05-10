OBJS = \
    BookmarksBarWidget.o \
    DownloadWidget.o \
    InspectorWidget.o \
    Tab.o \
    WindowActions.o \
    main.o

PROGRAM = Browser

LIB_DEPS = Web JS Markdown TextCodec GUI Gfx IPC Protocol Core

main.cpp: ../../Libraries/LibWeb/CSS/PropertyID.h
../../Libraries/LibWeb/CSS/PropertyID.h:
	@flock ../../Libraries/LibWeb $(MAKE) -C ../../Libraries/LibWeb

main.cpp: ../../Services/ProtocolServer/ProtocolClientEndpoint.h
../../Services/ProtocolServer/ProtocolClientEndpoint.h:
	@flock ../../Services/ProtocolServer $(MAKE) -C $(dir $(@))

include ../../Makefile.common
