#include <LibJS/Runtime/Function.h>
#include <LibWeb/DOM/EventListener.h>

namespace Web {

JS::Function* EventListener::function()
{
    return m_function.cell();
}

}
