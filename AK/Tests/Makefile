all: TestString TestQueue

CXXFLAGS = -std=c++17 -Wall -Wextra

TestString: TestString.cpp ../String.cpp ../StringImpl.cpp ../StringBuilder.cpp ../StringView.cpp TestHelpers.h
	$(CXX) $(CXXFLAGS) -I../ -I../../ -o $@ TestString.cpp ../String.cpp ../StringImpl.cpp ../StringBuilder.cpp ../StringView.cpp

TestQueue: TestQueue.cpp ../String.cpp ../StringImpl.cpp ../StringBuilder.cpp ../StringView.cpp TestHelpers.h
	$(CXX) $(CXXFLAGS) -I../ -I../../ -o $@ TestQueue.cpp ../String.cpp ../StringImpl.cpp ../StringBuilder.cpp ../StringView.cpp

clean:
	rm -f TestString TestQueue
