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

CXXFLAGS = -Os -std=c++1z -W -Wall -g -I../AK
LDFLAGS = -lncurses

$(BINARY): $(OBJS)
	$(CXX) $(LDFLAGS) -o $@ $(OBJS)

all: $(BINARY)

clean:
	rm -f $(BINARY) $(OBJS)

%.o: %.cpp
	$(CXX) $(CXXFLAGS) -c $< -o $@
