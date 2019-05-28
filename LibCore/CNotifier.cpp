#include <LibCore/CEvent.h>
#include <LibCore/CEventLoop.h>
#include <LibCore/CNotifier.h>

CNotifier::CNotifier(int fd, unsigned event_mask)
    : m_fd(fd)
    , m_event_mask(event_mask)
{
    CEventLoop::register_notifier(Badge<CNotifier>(), *this);
}

CNotifier::~CNotifier()
{
    CEventLoop::unregister_notifier(Badge<CNotifier>(), *this);
}
