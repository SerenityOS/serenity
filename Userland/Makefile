SRCS = $(wildcard *.cpp)
OBJS = ${SRCS:.cpp=.o}
APPS = ${SRCS:.cpp=}

EXTRA_CLEAN = $(APPS)

LIB_DEPS = HTML GUI Gfx Audio Protocol IPC Thread Pthread Core PCIDB Markdown

include ../Makefile.common

all: $(APPS)

list:
	@echo $(APPS)

$(APPS): %: %.o $(STATIC_LIB_DEPS)
	@echo "LINK $@"
	$(QUIET) $(CXX) -o $@ $< $(LDFLAGS)
