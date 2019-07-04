#include <LibCore/CEvent.h>
#include <LibCore/CObject.h>

CChildEvent::CChildEvent(Type type, CObject& child)
    : CEvent(type)
    , m_child(child.make_weak_ptr())
{
}

CChildEvent::~CChildEvent()
{
}
