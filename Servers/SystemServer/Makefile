OBJS = \
    Service.o \
    main.o

PROGRAM = SystemServer

LIB_DEPS = Core

LDFLAGS = -static

install:
	mkdir -p ../../Root/usr/include/SystemServer/
	cp *.h ../../Root/usr/include/SystemServer/

include ../../Makefile.common
