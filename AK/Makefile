PROGRAM = akit-test
OBJS = StringImpl.o String.o MappedFile.o TemporaryFile.o SimpleMalloc.o kmalloc.o FileSystemPath.o StringBuilder.o test.o

CXXFLAGS = -std=c++17 -O0 -W -Wall -ggdb3

all:	$(PROGRAM)

test.o: Vector.h AKString.h StringImpl.h MappedFile.h HashTable.h SinglyLinkedList.h Traits.h HashMap.h TemporaryFile.h Buffer.h FileSystemPath.h StringBuilder.h

.cpp.o:
	$(CXX) $(CXXFLAGS) -o $@ -c $<

clean:
	rm -f $(OBJS) $(PROGRAM)

$(PROGRAM): $(OBJS)
	$(CXX) $(LDFLAGS) -o $@ $(OBJS)

