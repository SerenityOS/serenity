#include <LibCore/CObject.h>
#include <LibCore/CEventLoop.h>
#include <LibCore/CEvent.h>
#include <LibCore/CLock.h>
#include <LibCore/CNotifier.h>
#include <LibC/unistd.h>
#include <LibC/stdio.h>
#include <LibC/fcntl.h>
#include <LibC/string.h>
#include <LibC/time.h>
#include <LibC/sys/select.h>
#include <LibC/sys/socket.h>
#include <LibC/sys/time.h>
#include <LibC/errno.h>
#include <LibC/string.h>
#include <LibC/stdlib.h>
#include <AK/Time.h>

//#define CEVENTLOOP_DEBUG
//#define DEFERRED_INVOKE_DEBUG

static CEventLoop* s_main_event_loop;
static Vector<CEventLoop*>* s_event_loop_stack;
HashMap<int, OwnPtr<CEventLoop::EventLoopTimer>>* CEventLoop::s_timers;
HashTable<CNotifier*>* CEventLoop::s_notifiers;
int CEventLoop::s_next_timer_id = 1;

CEventLoop::CEventLoop()
{
    if (!s_event_loop_stack) {
        s_event_loop_stack = new Vector<CEventLoop*>;
        s_timers = new HashMap<int, OwnPtr<CEventLoop::EventLoopTimer>>;
        s_notifiers = new HashTable<CNotifier*>;
    }

    if (!s_main_event_loop) {
        s_main_event_loop = this;
        s_event_loop_stack->append(this);
    }

#ifdef CEVENTLOOP_DEBUG
    dbgprintf("(%u) CEventLoop constructed :)\n", getpid());
#endif
}

CEventLoop::~CEventLoop()
{
}

CEventLoop& CEventLoop::main()
{
    ASSERT(s_main_event_loop);
    return *s_main_event_loop;
}

CEventLoop& CEventLoop::current()
{
    return *s_event_loop_stack->last();
}

void CEventLoop::quit(int code)
{
    m_exit_requested = true;
    m_exit_code = code;
}

struct CEventLoopPusher {
public:
    CEventLoopPusher(CEventLoop& event_loop) : m_event_loop(event_loop)
    {
        if (&m_event_loop != s_main_event_loop) {
            m_event_loop.take_pending_events_from(CEventLoop::current());
            s_event_loop_stack->append(&event_loop);
        }
    }
    ~CEventLoopPusher()
    {
        if (&m_event_loop != s_main_event_loop) {
            s_event_loop_stack->take_last();
            CEventLoop::current().take_pending_events_from(m_event_loop);
        }
    }
private:
    CEventLoop& m_event_loop;
};

int CEventLoop::exec()
{
    CEventLoopPusher pusher(*this);

    m_running = true;
    for (;;) {
        if (m_exit_requested)
            return m_exit_code;
        pump();
    }
    ASSERT_NOT_REACHED();
}

void CEventLoop::pump(WaitMode mode)
{
    // window server event processing...
    do_processing();

    if (m_queued_events.is_empty()) {
        wait_for_event(mode);
        do_processing();
    }
    decltype(m_queued_events) events;
    {
        LOCKER(m_lock);
        events = move(m_queued_events);
    }

    for (auto& queued_event : events) {
        auto* receiver = queued_event.receiver.ptr();
        auto& event = *queued_event.event;
#ifdef CEVENTLOOP_DEBUG
        dbgprintf("CEventLoop: %s{%p} event %u\n", receiver->class_name(), receiver, (unsigned)event.type());
#endif
        if (!receiver) {
            switch (event.type()) {
            case CEvent::Quit:
                ASSERT_NOT_REACHED();
                return;
            default:
                dbgprintf("Event type %u with no receiver :(\n", event.type());
            }
        } else if (event.type() == CEvent::Type::DeferredInvoke) {
#ifdef DEFERRED_INVOKE_DEBUG
            printf("DeferredInvoke: receiver=%s{%p}\n", receiver->class_name(), receiver);
#endif
            static_cast<CDeferredInvocationEvent&>(event).m_invokee(*receiver);
        } else {
            receiver->event(event);
        }

        if (m_exit_requested) {
            LOCKER(m_lock);
            auto rejigged_event_queue = move(events);
            rejigged_event_queue.append(move(m_queued_events));
            m_queued_events = move(rejigged_event_queue);
        }
    }
}

void CEventLoop::post_event(CObject& receiver, OwnPtr<CEvent>&& event)
{
    LOCKER(m_lock);
#ifdef CEVENTLOOP_DEBUG
    dbgprintf("CEventLoop::post_event: {%u} << receiver=%p, event=%p\n", m_queued_events.size(), &receiver, event.ptr());
#endif
    m_queued_events.append({ receiver.make_weak_ptr(), move(event) });
}

void CEventLoop::wait_for_event(WaitMode mode)
{
    fd_set rfds;
    fd_set wfds;
    FD_ZERO(&rfds);
    FD_ZERO(&wfds);

    int max_fd = 0;
    auto add_fd_to_set = [&max_fd] (int fd, fd_set& set){
        FD_SET(fd, &set);
        if (fd > max_fd)
            max_fd = fd;
    };

    int max_fd_added = -1;
    add_file_descriptors_for_select(rfds, max_fd_added);
    max_fd = max(max_fd, max_fd_added);
    for (auto& notifier : *s_notifiers) {
        if (notifier->event_mask() & CNotifier::Read)
            add_fd_to_set(notifier->fd(), rfds);
        if (notifier->event_mask() & CNotifier::Write)
            add_fd_to_set(notifier->fd(), wfds);
        if (notifier->event_mask() & CNotifier::Exceptional)
            ASSERT_NOT_REACHED();
    }

    bool queued_events_is_empty;
    {
        LOCKER(m_lock);
        queued_events_is_empty = m_queued_events.is_empty();
    }

    timeval now;
    struct timeval timeout = { 0, 0 };
    bool should_wait_forever = false;
    if (mode == WaitMode::WaitForEvents) {
        if (!s_timers->is_empty() && queued_events_is_empty) {
            gettimeofday(&now, nullptr);
            get_next_timer_expiration(timeout);
            AK::timeval_sub(&timeout, &now, &timeout);
        } else {
            should_wait_forever = true;
        }
    } else {
        should_wait_forever = false;
    }

    int rc = select(max_fd + 1, &rfds, &wfds, nullptr, should_wait_forever ? nullptr : &timeout);
    if (rc < 0) {
        ASSERT_NOT_REACHED();
    }

    if (!s_timers->is_empty()) {
        gettimeofday(&now, nullptr);
    }

    for (auto& it : *s_timers) {
        auto& timer = *it.value;
        if (!timer.has_expired(now))
            continue;
#ifdef CEVENTLOOP_DEBUG
        dbgprintf("CEventLoop: Timer %d has expired, sending CTimerEvent to %p\n", timer.timer_id, timer.owner);
#endif
        post_event(*timer.owner, make<CTimerEvent>(timer.timer_id));
        if (timer.should_reload) {
            timer.reload(now);
        } else {
            // FIXME: Support removing expired timers that don't want to reload.
            ASSERT_NOT_REACHED();
        }
    }

    for (auto& notifier : *s_notifiers) {
        if (FD_ISSET(notifier->fd(), &rfds)) {
            if (notifier->on_ready_to_read)
                notifier->on_ready_to_read();
        }
        if (FD_ISSET(notifier->fd(), &wfds)) {
            if (notifier->on_ready_to_write)
                notifier->on_ready_to_write();
        }
    }

    process_file_descriptors_after_select(rfds);
}

bool CEventLoop::EventLoopTimer::has_expired(const timeval& now) const
{
    return now.tv_sec > fire_time.tv_sec || (now.tv_sec == fire_time.tv_sec && now.tv_usec >= fire_time.tv_usec);
}

void CEventLoop::EventLoopTimer::reload(const timeval& now)
{
    fire_time = now;
    fire_time.tv_sec += interval / 1000;
    fire_time.tv_usec += (interval % 1000) * 1000;
}

void CEventLoop::get_next_timer_expiration(timeval& soonest)
{
    ASSERT(!s_timers->is_empty());
    bool has_checked_any = false;
    for (auto& it : *s_timers) {
        auto& fire_time = it.value->fire_time;
        if (!has_checked_any || fire_time.tv_sec < soonest.tv_sec || (fire_time.tv_sec == soonest.tv_sec && fire_time.tv_usec < soonest.tv_usec))
            soonest = fire_time;
        has_checked_any = true;
    }
}

int CEventLoop::register_timer(CObject& object, int milliseconds, bool should_reload)
{
    ASSERT(milliseconds >= 0);
    auto timer = make<EventLoopTimer>();
    timer->owner = object.make_weak_ptr();
    timer->interval = milliseconds;
    timeval now;
    gettimeofday(&now, nullptr);
    timer->reload(now);
    timer->should_reload = should_reload;
    int timer_id = ++s_next_timer_id;  // FIXME: This will eventually wrap around.
    ASSERT(timer_id); // FIXME: Aforementioned wraparound.
    timer->timer_id = timer_id;
    s_timers->set(timer->timer_id, move(timer));
    return timer_id;
}

bool CEventLoop::unregister_timer(int timer_id)
{
    auto it = s_timers->find(timer_id);
    if (it == s_timers->end())
        return false;
    s_timers->remove(it);
    return true;
}

void CEventLoop::register_notifier(Badge<CNotifier>, CNotifier& notifier)
{
    s_notifiers->set(&notifier);
}

void CEventLoop::unregister_notifier(Badge<CNotifier>, CNotifier& notifier)
{
    s_notifiers->remove(&notifier);
}
