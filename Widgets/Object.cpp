#include "Object.h"
#include "Event.h"
#include <AK/Assertions.h>

Object::Object(Object* parent)
    : m_parent(parent)
{
}

Object::~Object()
{
}

void Object::event(Event& event)
{
    switch (event.type()) {
    case Event::Invalid:
        ASSERT_NOT_REACHED();
        break;
    default:
        break;
    }
}
