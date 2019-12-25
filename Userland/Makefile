SRCS = $(wildcard *.cpp)
OBJS = ${SRCS:.cpp=.o}
APPS = ${SRCS:.cpp=}

EXTRA_CLEAN = $(APPS)

LIB_DEPS = HTML GUI Draw Audio Protocol IPC Thread Pthread Core PCIDB Markdown

all: $(OBJS) $(APPS)

list:
	@echo $(APPS)

$(APPS): %: %.o $(OBJS)
	@echo "LINK $@"
	$(QUIET) $(CXX) -o $@ $< $(LDFLAGS)

include ../Makefile.common
