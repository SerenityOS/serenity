#include <LibCore/CEvent.h>
#include <LibCore/CObject.h>

CChildEvent::CChildEvent(Type type, CObject& child, CObject* insertion_before_child)
    : CEvent(type)
    , m_child(child.make_weak_ptr())
    , m_insertion_before_child(insertion_before_child ? insertion_before_child->make_weak_ptr() : nullptr)
{
}

CChildEvent::~CChildEvent()
{
}
