include ../../Makefile.common

OBJS = \
    ProcessModel.o \
    ProcessTableView.o \
    MemoryStatsWidget.o \
    GraphWidget.o \
    main.o

APP = ProcessManager

DEFINES += -DUSERLAND

all: $(APP)

$(APP): $(OBJS)
	$(LD) -o $(APP) $(LDFLAGS) $(OBJS) -lgui -lcore -lc

.cpp.o:
	@echo "CXX $<"; $(CXX) $(CXXFLAGS) -o $@ -c $<

-include $(OBJS:%.o=%.d)

clean:
	@echo "CLEAN"; rm -f $(APP) $(OBJS) *.d

