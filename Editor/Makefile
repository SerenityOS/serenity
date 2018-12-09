BINARY = cuki

OBJS = \
	Document.o \
	Line.o \
	FileReader.o \
	Editor.o \
	main.o \
    InsertOperation.o \
    Operation.o \
    UndoStack.o

CXX = g++
CXXFLAGS = -O2 -std=c++1z -W -Wall -g -I../AK
LIBS = -lncurses

$(BINARY): $(OBJS)
	$(CXX) $(LDFLAGS) -o $@ $(OBJS) $(LIBS)

all: $(BINARY)

clean:
	rm -f $(BINARY) $(OBJS)

%.o: %.cpp
	$(CXX) $(CXXFLAGS) -c $< -o $@
