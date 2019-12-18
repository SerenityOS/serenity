PROGRAMS = \
	TestBinarySearch \
	TestAtomic \
	TestString \
	TestQueue \
	TestVector \
	TestHashMap \
	TestJSON \
	TestWeakPtr \
	TestNonnullRefPtr \
	TestRefPtr \
	TestFixedArray \
	TestFileSystemPath \
	TestURL \
	TestStringView \
	TestUtf8 \
	TestCircularQueue

CXXFLAGS = -std=c++17 -Wall -Wextra -ggdb3 -O2 -I../ -I../../

SHARED_TEST_OBJS = \
	../String.o \
	../StringImpl.o \
	../StringBuilder.o \
	../StringView.o \
	../LogStream.o \
	../JsonValue.o \
	../JsonParser.o \
    ../FileSystemPath.o \
    ../URL.o \
    ../Utf8View.o \

%.o: %.cpp ../*.h
	@echo "HOST_CXX $<"; $(PRE_CXX) $(CXX) $(CXXFLAGS) -o $@ -c $<

define execute-command
$(1)

endef

all: $(PROGRAMS)
	$(foreach x,$(PROGRAMS),$(call execute-command,./$(x)))

TestBinarySearch: TestBinarySearch.o $(SHARED_TEST_OBJS)
	$(PRE_CXX) $(CXX) $(CXXFLAGS) -o $@ TestBinarySearch.o $(SHARED_TEST_OBJS)

TestAtomic: TestAtomic.o $(SHARED_TEST_OBJS)
	$(PRE_CXX) $(CXX) $(CXXFLAGS) -o $@ TestAtomic.o $(SHARED_TEST_OBJS)

TestString: TestString.o $(SHARED_TEST_OBJS)
	$(PRE_CXX) $(CXX) $(CXXFLAGS) -o $@ TestString.o $(SHARED_TEST_OBJS)

TestQueue: TestQueue.o $(SHARED_TEST_OBJS)
	$(PRE_CXX) $(CXX) $(CXXFLAGS) -o $@ TestQueue.o $(SHARED_TEST_OBJS)

TestVector: TestVector.o $(SHARED_TEST_OBJS)
	$(PRE_CXX) $(CXX) $(CXXFLAGS) -o $@ TestVector.o $(SHARED_TEST_OBJS)

TestFixedArray: TestFixedArray.o $(SHARED_TEST_OBJS)
	$(PRE_CXX) $(CXX) $(CXXFLAGS) -o $@ TestFixedArray.o $(SHARED_TEST_OBJS)

TestHashMap: TestHashMap.o $(SHARED_TEST_OBJS)
	$(PRE_CXX) $(CXX) $(CXXFLAGS) -o $@ TestHashMap.o $(SHARED_TEST_OBJS)

TestJSON: TestJSON.o $(SHARED_TEST_OBJS)
	$(PRE_CXX) $(CXX) $(CXXFLAGS) -o $@ TestJSON.o $(SHARED_TEST_OBJS)

TestWeakPtr: TestWeakPtr.o $(SHARED_TEST_OBJS) 
	$(PRE_CXX) $(CXX) $(CXXFLAGS) -o $@ TestWeakPtr.o $(SHARED_TEST_OBJS)

TestNonnullRefPtr: TestNonnullRefPtr.o $(SHARED_TEST_OBJS)
	$(PRE_CXX) $(CXX) $(CXXFLAGS) -o $@ TestNonnullRefPtr.o $(SHARED_TEST_OBJS)

TestRefPtr: TestRefPtr.o $(SHARED_TEST_OBJS)
	$(PRE_CXX) $(CXX) $(CXXFLAGS) -o $@ TestRefPtr.o $(SHARED_TEST_OBJS)

TestOptional: TestOptional.o $(SHARED_TEST_OBJS)
	$(PRE_CXX) $(CXX) $(CXXFLAGS) -o $@ TestOptional.o $(SHARED_TEST_OBJS)

TestFileSystemPath: TestFileSystemPath.o $(SHARED_TEST_OBJS)
	$(PRE_CXX) $(CXX) $(CXXFLAGS) -o $@ TestFileSystemPath.o $(SHARED_TEST_OBJS)

TestURL: TestURL.o $(SHARED_TEST_OBJS)
	$(PRE_CXX) $(CXX) $(CXXFLAGS) -o $@ TestURL.o $(SHARED_TEST_OBJS)

TestStringView: TestStringView.o $(SHARED_TEST_OBJS)
	$(PRE_CXX) $(CXX) $(CXXFLAGS) -o $@ TestStringView.o $(SHARED_TEST_OBJS)

TestUtf8: TestUtf8.o $(SHARED_TEST_OBJS)
	$(PRE_CXX) $(CXX) $(CXXFLAGS) -o $@ TestUtf8.o $(SHARED_TEST_OBJS)

TestCircularQueue: TestCircularQueue.o $(SHARED_TEST_OBJS)
	$(PRE_CXX) $(CXX) $(CXXFLAGS) -o $@ TestCircularQueue.o $(SHARED_TEST_OBJS)


clean:
	rm -f $(SHARED_TEST_OBJS)
	rm -f $(PROGRAMS)
