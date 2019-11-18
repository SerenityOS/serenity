PROGRAM = Generate_CSS_PropertyID_h

OBJS = \
    Generate_CSS_PropertyID_h.o \
    $(SERENITY_ROOT)/AK/String.o \
    $(SERENITY_ROOT)/AK/StringImpl.o \
    $(SERENITY_ROOT)/AK/StringBuilder.o \
    $(SERENITY_ROOT)/AK/StringView.o \
    $(SERENITY_ROOT)/AK/JsonValue.o \
    $(SERENITY_ROOT)/AK/JsonParser.o \
    $(SERENITY_ROOT)/AK/LogStream.o \
    $(SERENITY_ROOT)/Libraries/LibCore/CIODevice.o \
    $(SERENITY_ROOT)/Libraries/LibCore/CFile.o \
    $(SERENITY_ROOT)/Libraries/LibCore/CObject.o \
    $(SERENITY_ROOT)/Libraries/LibCore/CEvent.o \
    $(SERENITY_ROOT)/Libraries/LibCore/CSocket.o \
    $(SERENITY_ROOT)/Libraries/LibCore/CLocalSocket.o \
    $(SERENITY_ROOT)/Libraries/LibCore/CNotifier.o \
    $(SERENITY_ROOT)/Libraries/LibCore/CLocalServer.o \
    $(SERENITY_ROOT)/Libraries/LibCore/CEventLoop.o

all: $(PROGRAM)

CXXFLAGS = -std=c++17 -Wall -Wextra

%.o: %.cpp
	$(PRE_CXX) $(CXX) $(CXXFLAGS) -I../ -I$(SERENITY_ROOT)/ -I$(SERENITY_ROOT)/Libraries/ -o $@ -c $<

$(PROGRAM): $(OBJS)
	$(CXX) $(LDFLAGS) -I../ -I$(SERENITY_ROOT)/ -I$(SERENITY_ROOT)/Libraries/ -o $(PROGRAM) $(OBJS)

clean:
	rm -f $(PROGRAM) $(OBJS)
