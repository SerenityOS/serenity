#include "GObject.h"
#include "GEvent.h"
#include "GEventLoop.h"
#include <AK/Assertions.h>

GObject::GObject(GObject* parent)
    : m_parent(parent)
{
    if (m_parent)
        m_parent->add_child(*this);
}

GObject::~GObject()
{
    stop_timer();
    if (m_parent)
        m_parent->remove_child(*this);
    auto children_to_delete = move(m_children);
    for (auto* child : children_to_delete)
        delete child;
}

void GObject::event(GEvent& event)
{
    switch (event.type()) {
    case GEvent::Timer:
        return timer_event(static_cast<GTimerEvent&>(event));
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

void GObject::add_child(GObject& object)
{
    m_children.append(&object);
}

void GObject::remove_child(GObject& object)
{
    for (ssize_t i = 0; i < m_children.size(); ++i) {
        if (m_children[i] == &object) {
            m_children.remove(i);
            return;
        }
    }
}

void GObject::timer_event(GTimerEvent&)
{
}

void GObject::start_timer(int ms)
{
    if (m_timer_id) {
        dbgprintf("GObject{%p} already has a timer!\n", this);
        ASSERT_NOT_REACHED();
    }

    m_timer_id = GEventLoop::main().register_timer(*this, ms, true);
}

void GObject::stop_timer()
{
    if (!m_timer_id)
        return;
    bool success = GEventLoop::main().unregister_timer(m_timer_id);
    ASSERT(success);
    m_timer_id = 0;
}

void GObject::delete_later()
{
    GEventLoop::main().post_event(this, make<GEvent>(GEvent::DeferredDestroy));
}

