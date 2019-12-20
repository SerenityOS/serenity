OBJS = \
    main.o

PROGRAM = About

LDFLAGS = -lgui -ldraw -lipc -lcore -lc

DEFINES += -DGIT_COMMIT=\"`git rev-parse --short HEAD`\" -DGIT_BRANCH=\"`git rev-parse --abbrev-ref HEAD`\" -DGIT_CHANGES=\"`git diff-index --quiet HEAD -- && echo "tracked"|| echo "untracked"`\"

include ../../Makefile.common
