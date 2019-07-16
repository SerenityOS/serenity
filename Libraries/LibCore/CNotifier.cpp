#include <LibCore/CEvent.h>
#include <LibCore/CEventLoop.h>
#include <LibCore/CNotifier.h>

CNotifier::CNotifier(int fd, unsigned event_mask)
    : m_fd(fd)
    , m_event_mask(event_mask)
{
    set_enabled(true);
}

CNotifier::~CNotifier()
{
    set_enabled(false);
}

void CNotifier::set_enabled(bool enabled)
{
    if (enabled)
        CEventLoop::register_notifier({}, *this);
    else
        CEventLoop::unregister_notifier({}, *this);
}
