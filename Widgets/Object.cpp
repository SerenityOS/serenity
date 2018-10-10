#include "Object.h"
#include "Event.h"
#include <AK/Assertions.h>

Object::Object(Object* parent)
    : m_parent(parent)
{
    if (m_parent)
        m_parent->addChild(*this);
}

Object::~Object()
{
    if (m_parent)
        m_parent->removeChild(*this);
    for (auto* child : m_children) {
        delete child;
    }
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

void Object::addChild(Object& object)
{
    m_children.append(&object);
}

void Object::removeChild(Object& object)
{
    // Oh geez, Vector needs a remove() huh...
    Vector<Object*> newList;
    for (auto* child : m_children) {
        if (child != &object)
            newList.append(child);
    }
    m_children = std::move(newList);
}
