#include <AK/LogStream.h>
#include <AK/String.h>
#include <LibJS/Heap/Heap.h>
#include <LibJS/Interpreter.h>
#include <LibJS/Runtime/ConsoleObject.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/MathObject.h>
#include <LibJS/Runtime/NativeFunction.h>
#include <LibJS/Runtime/Value.h>

namespace JS {

GlobalObject::GlobalObject()
{
    put("console", heap().allocate<ConsoleObject>());
    put_native_function("gc", [](Object* this_object, Vector<Value>) -> Value {
        dbg() << "Forced garbage collection requested!";
        this_object->heap().collect_garbage();
        return js_undefined();
    });
    put_native_function("isNaN", [](Object*, Vector<Value> arguments) -> Value {
        if (arguments.size() < 1)
            return js_undefined();
        return Value(arguments[0].to_number().is_nan());
    });
    put("Math", heap().allocate<MathObject>());
}

GlobalObject::~GlobalObject()
{
}

}
