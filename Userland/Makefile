SRCS = $(wildcard *.cpp)
OBJS = ${SRCS:.cpp=.o}
APPS = ${SRCS:.cpp=}

EXTRA_CLEAN = $(APPS)

LIB_DEPS = Web GUI Gfx Audio Protocol IPC Thread Pthread PCIDB Markdown JS Core Line X86 Debug

include ../Makefile.common

all: $(APPS)

list:
	@echo $(APPS)

$(APPS): %: %.o $(STATIC_LIB_DEPS)
	@echo "LINK $@"
	$(QUIET) $(CXX) -o $@ $< $(LDFLAGS)
