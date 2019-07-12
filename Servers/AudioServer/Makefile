include ../../Makefile.common

AUDIOSERVER_OBJS = \
    main.o

APP = AudioServer
OBJS = $(AUDIOSERVER_OBJS)

DEFINES += -DUSERLAND

all: $(APP)

$(APP): $(OBJS)
	$(LD) -o $(APP) $(LDFLAGS) $(OBJS) -lc -lcore

.cpp.o:
	@echo "CXX $<"; $(CXX) $(CXXFLAGS) -o $@ -c $<

-include $(OBJS:%.o=%.d)

clean:
	@echo "CLEAN"; rm -f $(APP) $(OBJS) *.d

