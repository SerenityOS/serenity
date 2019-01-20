#include "GObject.h"
#include "GEvent.h"
#include "GEventLoop.h"
#include <AK/Assertions.h>

GObject::GObject(GObject* parent)
    : m_parent(parent)
{
    if (m_parent)
        m_parent->addChild(*this);
}

GObject::~GObject()
{
    if (m_parent)
        m_parent->removeChild(*this);
    auto childrenToDelete = move(m_children);
    for (auto* child : childrenToDelete)
        delete child;
}

void GObject::event(GEvent& event)
{
    switch (event.type()) {
    case GEvent::Timer:
        return timerEvent(static_cast<GTimerEvent&>(event));
    case GEvent::DeferredDestroy:
        delete this;
        break;
    case GEvent::Invalid:
        ASSERT_NOT_REACHED();
        break;
    default:
        break;
    }
}

void GObject::addChild(GObject& object)
{
    m_children.append(&object);
}

void GObject::removeChild(GObject& object)
{
    for (unsigned i = 0; i < m_children.size(); ++i) {
        if (m_children[i] == &object) {
            m_children.remove(i);
            return;
        }
    }
}

void GObject::timerEvent(GTimerEvent&)
{
}

void GObject::startTimer(int ms)
{
    if (m_timerID) {
        dbgprintf("GObject{%p} already has a timer!\n", this);
        ASSERT_NOT_REACHED();
    }
}

void GObject::stopTimer()
{
    if (!m_timerID)
        return;
    m_timerID = 0;
}

void GObject::deleteLater()
{
    GEventLoop::main().postEvent(this, make<GEvent>(GEvent::DeferredDestroy));
}

