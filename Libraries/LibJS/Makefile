OBJS = \
    AST.o \
    Cell.o \
    Function.o \
    Heap.o \
    HeapBlock.o \
    Interpreter.o \
    Object.o \
    PrimitiveString.o \
    StringObject.o \
    Value.o

LIBRARY = libjs.a

install:
	mkdir -p $(SERENITY_BASE_DIR)/Root/usr/include/LibJS/
	cp *.h $(SERENITY_BASE_DIR)/Root/usr/include/LibJS/
	cp $(LIBRARY) $(SERENITY_BASE_DIR)/Root/usr/lib/

include ../../Makefile.common
