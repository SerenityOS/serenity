#include "Object.h"
#include "Event.h"
#include "EventLoop.h"
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
    auto childrenToDelete = move(m_children);
    for (auto* child : childrenToDelete)
        delete child;
}

void Object::event(Event& event)
{
    switch (event.type()) {
    case Event::Timer:
        return timerEvent(static_cast<TimerEvent&>(event));
    case Event::DeferredDestroy:
        delete this;
        break;
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
    for (unsigned i = 0; i < m_children.size(); ++i) {
        if (m_children[i] == &object) {
            m_children.remove(i);
            return;
        }
    }
}

void Object::timerEvent(TimerEvent&)
{
}

void Object::startTimer(int ms)
{
    if (m_timerID) {
        dbgprintf("Object{%p} already has a timer!\n", this);
        ASSERT_NOT_REACHED();
    }
}

void Object::stopTimer()
{
    if (!m_timerID)
        return;
    m_timerID = 0;
}

void Object::deleteLater()
{
    EventLoop::main().postEvent(this, make<DeferredDestroyEvent>());
}

