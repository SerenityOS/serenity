OBJS = \
    AST.o \
    Heap/Heap.o \
    Heap/HeapBlock.o \
    Interpreter.o \
    Lexer.o \
    Parser.o \
    Runtime/Cell.o \
    Runtime/ConsoleObject.o \
    Runtime/Function.o \
    Runtime/GlobalObject.o \
    Runtime/NativeFunction.o \
    Runtime/NativeProperty.o \
    Runtime/Object.o \
    Runtime/ObjectPrototype.o \
    Runtime/PrimitiveString.o \
    Runtime/ScriptFunction.o \
    Runtime/StringObject.o \
    Runtime/StringPrototype.o \
    Runtime/Value.o \
    Token.o

LIBRARY = libjs.a

install:
	mkdir -p $(SERENITY_BASE_DIR)/Root/usr/include/LibJS/
	cp *.h $(SERENITY_BASE_DIR)/Root/usr/include/LibJS/
	cp $(LIBRARY) $(SERENITY_BASE_DIR)/Root/usr/lib/

include ../../Makefile.common
