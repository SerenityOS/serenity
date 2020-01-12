OBJS = \
    main.o

PROGRAM = HelloWorld2

LIB_DEPS = GUI IPC Draw Core

main.cpp: UI_HelloWorld2.h

UI_HelloWorld2.h: HelloWorld2.frm | FORMCOMPILER
	$(QUIET) $(FORMCOMPILER) $< > $@

include ../../Makefile.common
