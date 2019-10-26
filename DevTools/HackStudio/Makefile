include ../../Makefile.common

OBJS = \
    Project.o \
    TextDocument.o \
    TerminalWrapper.o \
    FindInFilesWidget.o \
    ProcessStateWidget.o \
    CppLexer.o \
    main.o

APP = HackStudio

DEFINES += -DUSERLAND

all: $(APP)

$(APP): $(OBJS)
	$(LD) -o $(APP) $(LDFLAGS) $(OBJS) -lvt -lgui -ldraw -lthread -lcore -lc

.cpp.o:
	@echo "CXX $<"; $(CXX) $(CXXFLAGS) -o $@ -c $<

-include $(OBJS:%.o=%.d)

clean:
	@echo "CLEAN"; rm -f $(APP) $(OBJS) *.d

