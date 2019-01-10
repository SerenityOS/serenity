#include "Object.h"
#include "Event.h"
#include "EventLoop.h"
#include <AK/Assertions.h>

#ifdef USE_SDL
#include <SDL.h>
#endif

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
    auto childrenToDelete = std::move(m_children);
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

#ifdef USE_SDL
static dword sdlTimerCallback(dword interval, void* param)
{
    EventLoop::main().postEvent(static_cast<Object*>(param), make<TimerEvent>());
    return interval;
}
#endif

void Object::startTimer(int ms)
{
    if (m_timerID) {
        printf("Object{%p} already has a timer!\n", this);
        ASSERT_NOT_REACHED();
    }
#ifdef USE_SDL
    m_timerID = SDL_AddTimer(ms, sdlTimerCallback, this);
#endif
}

void Object::stopTimer()
{
    if (!m_timerID)
        return;
    SDL_RemoveTimer(m_timerID);
    m_timerID = 0;
}

void Object::deleteLater()
{
    EventLoop::main().postEvent(this, make<DeferredDestroyEvent>());
}

