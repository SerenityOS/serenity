include ../../Makefile.common

OBJS = \
    Project.o \
    ProjectFile.o \
    TerminalWrapper.o \
    FindInFilesWidget.o \
    ProcessStateWidget.o \
    FormEditorWidget.o \
    FormWidget.o \
    CppLexer.o \
    Editor.o \
    EditorWrapper.o \
    Locator.o \
    Tool.o \
    CursorTool.o \
    WidgetTool.o \
    main.o

APP = HackStudio

DEFINES += -DUSERLAND

all: $(APP)

$(APP): $(OBJS)
	$(LD) -o $(APP) $(LDFLAGS) $(OBJS) -lvt -lhtml -lmarkdown -lgui -ldraw -lthread -lcore -lc

.cpp.o:
	@echo "CXX $<"; $(CXX) $(CXXFLAGS) -o $@ -c $<

-include $(OBJS:%.o=%.d)

clean:
	@echo "CLEAN"; rm -f $(APP) $(OBJS) *.d

