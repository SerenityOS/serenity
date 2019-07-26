PROGRAM = FormCompiler

OBJS = \
    main.o \
    ../../AK/String.o \
    ../../AK/StringImpl.o \
    ../../AK/StringBuilder.o \
    ../../AK/StringView.o \
    ../../AK/JsonObject.o \
    ../../AK/JsonValue.o \
    ../../AK/JsonArray.o \
    ../../AK/JsonParser.o \
    ../../AK/LogStream.o \
    ../../Libraries/LibCore/CIODevice.o \
    ../../Libraries/LibCore/CFile.o \
    ../../Libraries/LibCore/CObject.o \
    ../../Libraries/LibCore/CEvent.o \
    ../../Libraries/LibCore/CSocket.o \
    ../../Libraries/LibCore/CLocalSocket.o \
    ../../Libraries/LibCore/CNotifier.o \
    ../../Libraries/LibCore/CEventLoop.o

all: $(PROGRAM)

CXXFLAGS = -std=c++17 -Wall -Wextra

%.o: %.cpp
	$(PRE_CXX) $(CXX) $(CXXFLAGS) -I../ -I../../ -I../../Libraries/ -o $@ -c $<

$(PROGRAM): $(OBJS)
	$(CXX) $(LDFLAGS) -I../ -I../../ -I../../Libraries/ -o $(PROGRAM) $(OBJS)

clean:
	rm -f $(PROGRAM) $(OBJS)
