PROGRAM = FormCompiler

SOURCES = \
    main.cpp \
    ../../AK/String.cpp \
    ../../AK/StringImpl.cpp \
    ../../AK/StringBuilder.cpp \
    ../../AK/StringView.cpp \
    ../../AK/JsonObject.cpp \
    ../../AK/JsonValue.cpp \
    ../../AK/JsonArray.cpp \
    ../../AK/JsonParser.cpp \
    ../../AK/LogStream.cpp \
    ../../Libraries/LibCore/CIODevice.cpp \
    ../../Libraries/LibCore/CFile.cpp \
    ../../Libraries/LibCore/CObject.cpp \
    ../../Libraries/LibCore/CEvent.cpp \
    ../../Libraries/LibCore/CEventLoop.cpp

all: $(PROGRAM)

CXXFLAGS = -std=c++17 -Wall -Wextra

$(PROGRAM): $(SOURCES)
	$(PRE_CXX) $(CXX) $(CXXFLAGS) -I../ -I../../ -I../../Libraries/ -o $@ $(SOURCES)

clean:
	rm -f $(PROGRAM)
