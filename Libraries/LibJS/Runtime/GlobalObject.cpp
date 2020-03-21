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
    put("Math", heap().allocate<MathObject>());
}

GlobalObject::~GlobalObject()
{
}

}
