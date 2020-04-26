OBJS = \
    Launcher.o

LIBRARY = libdesktop.a

# HACK: LaunchServer builds after LibDesktop so we need to explicitly generate these IPC headers
Launcher.cpp: ../../Services/LaunchServer/LaunchServerEndpoint.h ../../Services/LaunchServer/LaunchClientEndpoint.h

../../Services/LaunchServer/LaunchServerEndpoint.h:
	$(MAKE) -C $(dir $(@)) LaunchServerEndpoint.h

../../Services/LaunchServer/LaunchClientEndpoint.h:
	$(MAKE) -C $(dir $(@)) LaunchClientEndpoint.h

install:
	mkdir -p $(SERENITY_BASE_DIR)/Root/usr/include/LibDesktop/
	cp ./*.h $(SERENITY_BASE_DIR)/Root/usr/include/LibDesktop/
	cp $(LIBRARY) $(SERENITY_BASE_DIR)/Root/usr/lib/

include ../../Makefile.common
