#include <AK/LogStream.h>
#include <AK/String.h>
#include <LibJS/Heap/Heap.h>
#include <LibJS/Interpreter.h>
#include <LibJS/Runtime/ConsoleObject.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/NativeFunction.h>
#include <LibJS/Runtime/Value.h>
#include <stdio.h>

namespace JS {

GlobalObject::GlobalObject()
{
    put_native_function("print", [](Object*, Vector<Value> arguments) -> Value {
        for (auto& argument : arguments)
            printf("%s ", argument.to_string().characters());
        return js_undefined();
    });
    put_native_function("gc", [](Object* this_object, Vector<Value>) -> Value {
        dbg() << "Forced garbage collection requested!";
        this_object->heap().collect_garbage();
        return js_undefined();
    });
}

GlobalObject::~GlobalObject()
{
}

}
