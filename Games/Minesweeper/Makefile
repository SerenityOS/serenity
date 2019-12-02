include ../../Makefile.common

OBJS = \
    Field.o \
    main.o

APP = Minesweeper

DEFINES += -DUSERLAND

all: $(APP)

$(APP): $(OBJS)
	$(LD) -o $(APP) $(LDFLAGS) $(OBJS) -lgui -ldraw -lipc -lcore -lc

.cpp.o:
	@echo "CXX $<"; $(CXX) $(CXXFLAGS) -o $@ -c $<

-include $(OBJS:%.o=%.d)

clean:
	@echo "CLEAN"; rm -f $(APP) $(OBJS) *.d

