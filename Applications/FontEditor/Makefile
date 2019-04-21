include ../../Makefile.common

OBJS = \
    FontEditor.o \
    GlyphMapWidget.o \
    GlyphEditorWidget.o \
    main.o

APP = FontEditor

DEFINES += -DUSERLAND

all: $(APP)

$(APP): $(OBJS)
	$(LD) -o $(APP) $(LDFLAGS) $(OBJS) -lgui -lcore -lc

.cpp.o:
	@echo "CXX $<"; $(CXX) $(CXXFLAGS) -o $@ -c $<

-include $(OBJS:%.o=%.d)

clean:
	@echo "CLEAN"; rm -f $(APPS) $(OBJS) *.d

