SRCS = $(wildcard *.cpp)
OBJS = ${SRCS:.cpp=.o}
APPS = ${SRCS:.cpp=}

EXTRA_CLEAN = $(APPS)

LIB_DEPS = Web GUI Gfx Audio Protocol IPC Thread Pthread Core PCIDB Markdown JS

include ../Makefile.common

all: $(APPS)

list:
	@echo $(APPS)

$(APPS): %: %.o $(STATIC_LIB_DEPS)
	@echo "LINK $@"
	$(QUIET) $(CXX) -o $@ $< $(LDFLAGS)
