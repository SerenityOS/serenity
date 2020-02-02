OBJS = math.o

SHARED ?= 0
ifeq ($(SHARED),1)
    PROGRAM = libm.so
    LDFLAGS = -shared
    SUBPROJECT_CXXFLAGS = -fPIC
	BUILT_MLIB = $(PROGRAM)
else
    LIBRARY = libm.a
	BUILT_MLIB = ${LIBRARY}
endif

install:
	mkdir -p $(SERENITY_BASE_DIR)/Root/usr/include/sys/
	mkdir -p $(SERENITY_BASE_DIR)/Root/usr/lib/
	cp *.h $(SERENITY_BASE_DIR)/Root/usr/include/
	cp $(BUILT_MLIB) $(SERENITY_BASE_DIR)/Root/usr/lib/

include ../../Makefile.common
