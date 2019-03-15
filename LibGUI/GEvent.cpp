#include <LibGUI/GEvent.h>
#include <LibGUI/GObject.h>

GChildEvent::GChildEvent(Type type, GObject& child)
    : GEvent(type)
    , m_child(child.make_weak_ptr())
{
}

GChildEvent::~GChildEvent()
{
}
