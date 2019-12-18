OBJS = \
    main.o

PROGRAM = HelloWorld2

LDFLAGS = -lgui -ldraw -lipc -lcore -lc

main.cpp: UI_HelloWorld2.h

UI_HelloWorld2.h: HelloWorld2.frm
	../../DevTools/FormCompiler/FormCompiler $< > $@

include ../../Makefile.common
