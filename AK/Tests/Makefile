PROGRAMS = TestString TestQueue TestVector TestHashMap TestJSON

all: $(PROGRAMS)

CXXFLAGS = -std=c++17 -Wall -Wextra

TestString: TestString.cpp ../String.cpp ../StringImpl.cpp ../StringBuilder.cpp ../StringView.cpp ../TestSuite.h ../LogStream.cpp
	$(CXX) $(CXXFLAGS) -I../ -I../../ -o $@ TestString.cpp ../String.cpp ../StringImpl.cpp ../StringBuilder.cpp ../StringView.cpp ../LogStream.cpp

TestQueue: TestQueue.cpp ../String.cpp ../StringImpl.cpp ../StringBuilder.cpp ../StringView.cpp ../TestSuite.h ../LogStream.cpp
	$(CXX) $(CXXFLAGS) -I../ -I../../ -o $@ TestQueue.cpp ../String.cpp ../StringImpl.cpp ../StringBuilder.cpp ../StringView.cpp ../LogStream.cpp

TestVector: TestVector.cpp ../String.cpp ../StringImpl.cpp ../StringBuilder.cpp ../StringView.cpp ../TestSuite.h ../LogStream.cpp
	$(CXX) $(CXXFLAGS) -I../ -I../../ -o $@ TestVector.cpp ../String.cpp ../StringImpl.cpp ../StringBuilder.cpp ../StringView.cpp ../LogStream.cpp

TestHashMap: TestHashMap.cpp ../String.cpp ../StringImpl.cpp ../StringBuilder.cpp ../StringView.cpp ../TestSuite.h ../LogStream.cpp
	$(CXX) $(CXXFLAGS) -I../ -I../../ -o $@ TestHashMap.cpp ../String.cpp ../StringImpl.cpp ../StringBuilder.cpp ../StringView.cpp ../LogStream.cpp

TestJSON: TestJSON.cpp ../String.cpp ../StringImpl.cpp ../StringBuilder.cpp ../StringView.cpp ../TestSuite.h ../LogStream.cpp ../JsonObject.cpp ../JsonValue.cpp ../JsonArray.cpp ../JsonParser.cpp
	$(CXX) $(CXXFLAGS) -I../ -I../../ -o $@ TestJSON.cpp ../String.cpp ../StringImpl.cpp ../StringBuilder.cpp ../StringView.cpp ../LogStream.cpp ../JsonObject.cpp ../JsonValue.cpp ../JsonArray.cpp ../JsonParser.cpp

clean:
	rm -f $(PROGRAMS)
