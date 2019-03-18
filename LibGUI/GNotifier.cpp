#include <LibGUI/GNotifier.h>
#include <LibGUI/GEventLoop.h>

GNotifier::GNotifier(int fd, unsigned event_mask)
    : m_fd(fd)
    , m_event_mask(event_mask)
{
    GEventLoop::register_notifier(Badge<GNotifier>(), *this);
}

GNotifier::~GNotifier()
{
    GEventLoop::unregister_notifier(Badge<GNotifier>(), *this);
}

