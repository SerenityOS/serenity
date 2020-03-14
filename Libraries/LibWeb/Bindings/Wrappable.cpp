#include <LibWeb/Bindings/Wrappable.h>
#include <LibWeb/Bindings/Wrapper.h>

namespace Web {
namespace Bindings {

Wrappable::~Wrappable()
{
}

void Wrappable::set_wrapper(Wrapper& wrapper)
{
    ASSERT(!m_wrapper);
    m_wrapper = wrapper.make_weak_ptr();
}

}
}
