PROGRAM = run

AK_OBJS = \
    ../AK/String.o \
    ../AK/StringImpl.o \
    ../AK/MappedFile.o \
    ../AK/TemporaryFile.o \
    ../AK/kmalloc.o

CSILLA_OBJS = \
    ExecSpace.o \
    ELFImage.o \
    ELFLoader.o \
    test.o

TEST_OBJ = \
    _test.o

TEST_OBJ_CXXFLAGS = -Os -fno-exceptions -fno-rtti -fno-asynchronous-unwind-tables

OBJS = $(AK_OBJS) $(CSILLA_OBJS)

CXXFLAGS = -std=c++17 -Os -W -Wall -I. -I.. -ggdb3

all:	$(PROGRAM) $(TEST_OBJ)

_test.o: _test.cpp
	$(CXX) $(TEST_OBJ_CXXFLAGS) -c $<

.cpp.o:
	$(CXX) $(CXXFLAGS) -o $@ -c $<

clean:
	rm -f $(OBJS) $(TEST_OBJ) $(PROGRAM)

$(PROGRAM): $(OBJS)
	$(CXX) $(LDFLAGS) -o $@ $(OBJS)

