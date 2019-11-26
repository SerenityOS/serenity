include ../../Makefile.common

SYSTEMSERVER_OBJS = \
    Service.o \
    main.o

APP = SystemServer
OBJS = $(SYSTEMSERVER_OBJS)

DEFINES += -DUSERLAND

all: $(APP)

$(APP): $(OBJS)
	$(LD) -o $(APP) $(LDFLAGS) $(OBJS) -lc -lcore

.cpp.o:
	@echo "CXX $<"; $(CXX) $(CXXFLAGS) -o $@ -c $<

-include $(OBJS:%.o=%.d)

clean:
	@echo "CLEAN"; rm -f $(APP) $(OBJS) *.d

