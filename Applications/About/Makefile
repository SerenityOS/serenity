OBJS = \
    main.o

PROGRAM = About

LIB_DEPS = GUI Draw IPC Core

DEFINES += -DGIT_COMMIT=\"`git rev-parse --short HEAD`\" -DGIT_BRANCH=\"`git rev-parse --abbrev-ref HEAD`\" -DGIT_CHANGES=\"`git diff-index --quiet HEAD -- && echo "tracked"|| echo "untracked"`\"

include ../../Makefile.common
