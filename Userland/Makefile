SRCS = $(wildcard *.cpp)
OBJS = ${SRCS:.cpp=.o}
APPS = ${SRCS:.cpp=}

EXTRA_CLEAN = $(APPS)

LDFLAGS = -lc -lhtml -lgui -ldraw -laudio -lipc -lthread -lcore -lpcidb -lmarkdown -lpthread -lprotocol -lipc

all: $(OBJS) $(APPS)

list:
	@echo $(APPS)

$(APPS): %: %.o $(OBJS)
	@echo "LINK $@"
	$(QUIET) $(CXX) -o $@ $< $(LDFLAGS)

include ../Makefile.common
