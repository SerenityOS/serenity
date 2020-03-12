#include <AK/LogStream.h>
#include <AK/String.h>
#include <LibJS/GlobalObject.h>
#include <LibJS/Heap.h>
#include <LibJS/Interpreter.h>
#include <LibJS/NativeFunction.h>
#include <LibJS/Value.h>
#include <stdio.h>

namespace JS {

GlobalObject::GlobalObject(Heap& heap)
{
    put("print", heap.allocate<NativeFunction>([](Interpreter&, Vector<Argument> arguments) -> Value {
        for (auto& argument : arguments)
            printf("%s ", argument.value.to_string().characters());
        return js_undefined();
    }));
    put("gc", heap.allocate<NativeFunction>([](Interpreter& interpreter, Vector<Argument>) -> Value {
        dbg() << "Forced garbage collection requested!";
        interpreter.heap().collect_garbage();
        return js_undefined();
    }));
}

GlobalObject::~GlobalObject()
{
}

}
