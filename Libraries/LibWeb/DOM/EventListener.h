#pragma once

#include <AK/RefCounted.h>
#include <LibJS/Forward.h>
#include <LibWeb/Bindings/Wrappable.h>

namespace Web {

class EventListener
    : public RefCounted<EventListener>
    , public Bindings::Wrappable {
public:
    using WrapperType = Bindings::EventListenerWrapper;

    explicit EventListener(JS::Function* function)
        : m_function(function)
    {
    }

    JS::Function* function() { return m_function; }

private:
    JS::Function* m_function { nullptr };
};

}
