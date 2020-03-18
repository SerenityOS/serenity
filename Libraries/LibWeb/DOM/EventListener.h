#pragma once

#include <AK/RefCounted.h>
#include <LibJS/Heap/Handle.h>
#include <LibWeb/Bindings/Wrappable.h>

namespace Web {

class EventListener
    : public RefCounted<EventListener>
    , public Bindings::Wrappable {
public:
    using WrapperType = Bindings::EventListenerWrapper;

    explicit EventListener(JS::Handle<JS::Function> function)
        : m_function(move(function))
    {
    }

    JS::Function* function();

private:
    JS::Handle<JS::Function> m_function;
};

}
