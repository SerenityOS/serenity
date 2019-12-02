include ../../Makefile.common

OBJS = \
    main.o

APP = HelloWorld2

DEFINES += -DUSERLAND

all: $(APP)

main.cpp: UI_HelloWorld2.h

UI_HelloWorld2.h: HelloWorld2.frm
	../../DevTools/FormCompiler/FormCompiler $< > $@

$(APP): $(OBJS)
	$(LD) -o $(APP) $(LDFLAGS) $(OBJS) -lgui -ldraw -lipc -lcore -lc

.cpp.o:
	@echo "CXX $<"; $(CXX) $(CXXFLAGS) -o $@ -c $<

-include $(OBJS:%.o=%.d)

clean:
	@echo "CLEAN"; rm -f $(APP) $(OBJS) *.d

