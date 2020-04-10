OBJS = \
    AST.o \
    Heap/Handle.o \
    Heap/Heap.o \
    Heap/HeapBlock.o \
    Interpreter.o \
    Lexer.o \
    Parser.o \
    Runtime/Array.o \
    Runtime/ArrayConstructor.o \
    Runtime/ArrayPrototype.o \
    Runtime/BooleanConstructor.o \
    Runtime/BooleanObject.o \
    Runtime/BooleanPrototype.o \
    Runtime/Cell.o \
    Runtime/ConsoleObject.o \
    Runtime/Date.o \
    Runtime/DateConstructor.o \
    Runtime/DatePrototype.o \
    Runtime/Error.o \
    Runtime/ErrorConstructor.o \
    Runtime/ErrorPrototype.o \
    Runtime/Exception.o \
    Runtime/Function.o \
    Runtime/FunctionConstructor.o \
    Runtime/FunctionPrototype.o \
    Runtime/GlobalObject.o \
    Runtime/MathObject.o \
    Runtime/NativeFunction.o \
    Runtime/NativeProperty.o \
    Runtime/NumberConstructor.o \
    Runtime/NumberObject.o \
    Runtime/NumberPrototype.o \
    Runtime/Object.o \
    Runtime/ObjectConstructor.o \
    Runtime/ObjectPrototype.o \
    Runtime/PrimitiveString.o \
    Runtime/ScriptFunction.o \
    Runtime/Shape.o \
    Runtime/StringConstructor.o \
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
