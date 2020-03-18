#include <AK/Function.h>
#include <LibJS/Runtime/Function.h>
#include <LibWeb/Bindings/EventListenerWrapper.h>
#include <LibWeb/Bindings/EventTargetWrapper.h>
#include <LibWeb/DOM/EventListener.h>
#include <LibWeb/DOM/EventTarget.h>

namespace Web {
namespace Bindings {

EventTargetWrapper::EventTargetWrapper(EventTarget& impl)
    : m_impl(impl)
{
    put_native_function("addEventListener", [](Object* this_object, const Vector<JS::Value>& arguments) {
        if (arguments.size() < 2)
            return JS::js_undefined();

        auto event_name = arguments[0].to_string();
        ASSERT(arguments[1].is_object());
        ASSERT(arguments[1].as_object()->is_function());
        auto listener = adopt(*new EventListener(static_cast<JS::Function*>(const_cast<Object*>(arguments[1].as_object()))));
        wrap(this_object->heap(), *listener);
        static_cast<EventTargetWrapper*>(this_object)->impl().add_event_listener(event_name, move(listener));
        return JS::js_undefined();
    });
}

EventTargetWrapper::~EventTargetWrapper()
{
}

}
}
