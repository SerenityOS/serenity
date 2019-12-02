include ../../Makefile.common

OBJS = \
    RemoteObjectGraphModel.o \
    RemoteObjectPropertyModel.o \
    RemoteProcess.o \
    RemoteObject.o \
    main.o

APP = Inspector

DEFINES += -DUSERLAND

all: $(APP)

$(APP): $(OBJS)
	$(LD) -o $(APP) $(LDFLAGS) $(OBJS) -lgui -ldraw -lipc -lcore -lc

.cpp.o:
	@echo "CXX $<"; $(CXX) $(CXXFLAGS) -o $@ -c $<

-include $(OBJS:%.o=%.d)

clean:
	@echo "CLEAN"; rm -f $(APP) $(OBJS) *.d

