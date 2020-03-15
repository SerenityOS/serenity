OBJS = \
    AST.o \
    Cell.o \
    Function.o \
    GlobalObject.o \
    Heap.o \
    HeapBlock.o \
    Interpreter.o \
    Lexer.o \
    NativeFunction.o \
    NativeProperty.o \
    Object.o \
    ObjectPrototype.o \
    Parser.o \
    PrimitiveString.o \
    ScriptFunction.o \
    StringObject.o \
    StringPrototype.o \
    Token.o \
    Value.o

LIBRARY = libjs.a

install:
	mkdir -p $(SERENITY_BASE_DIR)/Root/usr/include/LibJS/
	cp *.h $(SERENITY_BASE_DIR)/Root/usr/include/LibJS/
	cp $(LIBRARY) $(SERENITY_BASE_DIR)/Root/usr/lib/

include ../../Makefile.common
