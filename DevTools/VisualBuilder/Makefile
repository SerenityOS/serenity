include ../../Makefile.common

OBJS = \
    VBForm.o \
    VBWidget.o \
    VBWidgetRegistry.o \
    VBWidgetPropertyModel.o \
    VBProperty.o \
    VBPropertiesWindow.o \
    main.o

APP = VisualBuilder 

DEFINES += -DUSERLAND

all: $(APP)

$(APP): $(OBJS)
	$(LD) -o $(APP) $(LDFLAGS) $(OBJS) -lgui -ldraw -lipc -lcore -lc

.cpp.o:
	@echo "CXX $<"; $(CXX) $(CXXFLAGS) -o $@ -c $<

-include $(OBJS:%.o=%.d)

clean:
	@echo "CLEAN"; rm -f $(APP) $(OBJS) *.d

