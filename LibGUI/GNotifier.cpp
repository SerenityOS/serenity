#include <LibGUI/GNotifier.h>
#include <LibGUI/GEventLoop.h>

GNotifier::GNotifier(int fd, unsigned event_mask)
    : m_fd(fd)
    , m_event_mask(event_mask)
{
    GEventLoop::main().register_notifier(Badge<GNotifier>(), *this);
}

GNotifier::~GNotifier()
{
    GEventLoop::main().unregister_notifier(Badge<GNotifier>(), *this);
}

