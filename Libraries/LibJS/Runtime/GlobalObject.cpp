#include <AK/LogStream.h>
#include <AK/String.h>
#include <LibJS/Heap/Heap.h>
#include <LibJS/Interpreter.h>
#include <LibJS/Runtime/ConsoleObject.h>
#include <LibJS/Runtime/DateConstructor.h>
#include <LibJS/Runtime/ErrorConstructor.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/MathObject.h>
#include <LibJS/Runtime/NativeFunction.h>
#include <LibJS/Runtime/ObjectConstructor.h>
#include <LibJS/Runtime/Value.h>

namespace JS {

GlobalObject::GlobalObject()
{
    put_native_function("gc", gc);
    put_native_function("isNaN", is_nan, 1);

    // FIXME: These are read-only in ES5
    put("NaN", js_nan());
    put("Infinity", js_infinity());
    put("undefined", js_undefined());

    put("console", heap().allocate<ConsoleObject>());
    put("Date", heap().allocate<DateConstructor>());
    put("Error", heap().allocate<ErrorConstructor>());
    put("Math", heap().allocate<MathObject>());
    put("Object", heap().allocate<ObjectConstructor>());
}

GlobalObject::~GlobalObject()
{
}

Value GlobalObject::gc(Interpreter& interpreter)
{
    dbg() << "Forced garbage collection requested!";
    interpreter.heap().collect_garbage();
    return js_undefined();
}

Value GlobalObject::is_nan(Interpreter& interpreter)
{
    if (interpreter.argument_count() < 1)
        return js_undefined();
    return Value(interpreter.argument(0).to_number().is_nan());
}

}
