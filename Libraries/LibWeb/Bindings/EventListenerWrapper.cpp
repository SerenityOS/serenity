#include <LibJS/Runtime/Function.h>
#include <LibWeb/Bindings/EventListenerWrapper.h>
#include <LibWeb/DOM/EventListener.h>

namespace Web {
namespace Bindings {

EventListenerWrapper::EventListenerWrapper(EventListener& impl)
    : m_impl(impl)
{
}

EventListenerWrapper::~EventListenerWrapper()
{
}

}
}
