OBJS = \
    Launcher.o

LIBRARY = libdesktop.a

# HACK: LaunchServer builds after LibDesktop so we need to explicitly generate these IPC headers
Launcher.cpp: ../../Servers/LaunchServer/LaunchServerEndpoint.h ../../Servers/LaunchServer/LaunchClientEndpoint.h

../../Servers/LaunchServer/LaunchServerEndpoint.h:
	$(MAKE) -C $(dir $(@)) LaunchServerEndpoint.h

../../Servers/LaunchServer/LaunchClientEndpoint.h:
	$(MAKE) -C $(dir $(@)) LaunchClientEndpoint.h

install:
	mkdir -p $(SERENITY_BASE_DIR)/Root/usr/include/LibDesktop/
	cp ./*.h $(SERENITY_BASE_DIR)/Root/usr/include/LibDesktop/
	cp $(LIBRARY) $(SERENITY_BASE_DIR)/Root/usr/lib/

include ../../Makefile.common
