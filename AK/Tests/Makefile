SHARED_TEST_SOURCES = \
	../String.cpp \
	../StringImpl.cpp \
	../StringBuilder.cpp \
	../StringView.cpp \
	../LogStream.cpp \
	../JsonValue.cpp \
	../JsonParser.cpp \
    ../FileSystemPath.cpp \
    ../URL.cpp \
    ../Utf8View.cpp

SRCS = $(wildcard *.cpp)
SHARED_TEST_OBJS = ${SHARED_TEST_SOURCES:.cpp=.host.o}
OBJS = ${SRCS:.cpp=.host.o} $(SHARED_TEST_OBJS)
APPS = ${SRCS:.cpp=}

EXTRA_CLEAN = $(APPS) *.o

HOST_CXX = 1

include ../../Makefile.common

CXXFLAGS = -std=c++17 -Wall -Wextra -ggdb3 -O2 -I../ -I../../

APPS_RUN = $(addsuffix .run,$(APPS))
test: $(APPS) $(APPS_RUN)
$(APPS_RUN): %.run:
	./$*

$(APPS): %: %$(OBJ_SUFFIX).o $(SHARED_TEST_OBJS)
	@echo "LINK $@"
	$(QUIET) $(CXX) -o $@ $< $(SHARED_TEST_OBJS) $(LDFLAGS)

all: | $(APPS) $(APPS_RUN)
