#include <LibCore/CEvent.h>
#include <LibGUI/GObject.h>

CChildEvent::CChildEvent(Type type, GObject& child)
    : CEvent(type)
    , m_child(child.make_weak_ptr())
{
}

CChildEvent::~CChildEvent()
{
}
