#include <LibCore/CNotifier.h>
#include <LibGUI/GEventLoop.h>

CNotifier::CNotifier(int fd, unsigned event_mask)
    : m_fd(fd)
    , m_event_mask(event_mask)
{
    GEventLoop::register_notifier(Badge<CNotifier>(), *this);
}

CNotifier::~CNotifier()
{
    GEventLoop::unregister_notifier(Badge<CNotifier>(), *this);
}

