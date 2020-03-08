OBJS = \
    main.o

PROGRAM = About

LIB_DEPS = GUI Gfx IPC Core

DEFINES += -DGIT_COMMIT=\"`git rev-parse --short HEAD`\" -DGIT_BRANCH=\"`git rev-parse --abbrev-ref HEAD`\"

include ../../Makefile.common
