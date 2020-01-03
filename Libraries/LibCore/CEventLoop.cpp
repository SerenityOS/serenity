#include <AK/JsonArray.h>
#include <AK/JsonObject.h>
#include <AK/JsonValue.h>
#include <AK/Time.h>
#include <LibCore/CEvent.h>
#include <LibCore/CEventLoop.h>
#include <LibCore/CLocalSocket.h>
#include <LibCore/CNotifier.h>
#include <LibCore/CObject.h>
#include <LibCore/CSyscallUtils.h>
#include <LibThread/Lock.h>
#include <errno.h>
#include <fcntl.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/select.h>
#include <sys/socket.h>
#include <sys/time.h>
#include <time.h>
#include <unistd.h>

//#define CEVENTLOOP_DEBUG
//#define DEFERRED_INVOKE_DEBUG

class RPCClient;

static CEventLoop* s_main_event_loop;
static Vector<CEventLoop*>* s_event_loop_stack;
HashMap<int, NonnullOwnPtr<CEventLoop::EventLoopTimer>>* CEventLoop::s_timers;
HashTable<CNotifier*>* CEventLoop::s_notifiers;
int CEventLoop::s_next_timer_id = 1;
int CEventLoop::s_wake_pipe_fds[2];
RefPtr<CLocalServer> CEventLoop::s_rpc_server;
HashMap<int, RefPtr<RPCClient>> s_rpc_clients;
// FIXME: It's not great if this wraps around.
static int s_next_client_id = 0;

class RPCClient : public CObject {
    C_OBJECT(RPCClient)
public:
    explicit RPCClient(RefPtr<CLocalSocket> socket)
        : m_socket(move(socket))
        , m_client_id(s_next_client_id++)
    {
        s_rpc_clients.set(m_client_id, this);
        add_child(*m_socket);
        m_socket->on_ready_to_read = [this] {
            i32 length;
            int nread = m_socket->read((u8*)&length, sizeof(length));
            if (nread == 0) {
                dbg() << "RPC client disconnected";
                shutdown();
                return;
            }
            ASSERT(nread == sizeof(length));
            auto request = m_socket->read(length);

            auto request_json = JsonValue::from_string(request);
            if (!request_json.is_object()) {
                dbg() << "RPC client sent invalid request";
                shutdown();
                return;
            }

            handle_request(request_json.as_object());
        };
    }
    virtual ~RPCClient() override
    {
    }

    void send_response(const JsonObject& response)
    {
        auto serialized = response.to_string();
        i32 length = serialized.length();
        m_socket->write((const u8*)&length, sizeof(length));
        m_socket->write(serialized);
    }

    void handle_request(const JsonObject& request)
    {
        auto type = request.get("type").as_string_or({});

        if (type.is_null()) {
            dbg() << "RPC client sent request without type field";
            return;
        }

        if (type == "Identify") {
            JsonObject response;
            response.set("type", type);
            response.set("pid", getpid());
#ifdef __serenity__
            char buffer[1024];
            if (get_process_name(buffer, sizeof(buffer)) >= 0) {
                response.set("process_name", buffer);
            } else {
                response.set("process_name", JsonValue());
            }
#endif
            send_response(response);
            return;
        }

        if (type == "GetAllObjects") {
            JsonObject response;
            response.set("type", type);
            JsonArray objects;
            for (auto& object : CObject::all_objects()) {
                JsonObject json_object;
                object.save_to(json_object);
                objects.append(move(json_object));
            }
            response.set("objects", move(objects));
            send_response(response);
            return;
        }

        if (type == "Disconnect") {
            shutdown();
            return;
        }
    }

    void shutdown()
    {
        s_rpc_clients.remove(m_client_id);
    }

private:
    RefPtr<CLocalSocket> m_socket;
    int m_client_id { -1 };
};

CEventLoop::CEventLoop()
{
    if (!s_event_loop_stack) {
        s_event_loop_stack = new Vector<CEventLoop*>;
        s_timers = new HashMap<int, NonnullOwnPtr<CEventLoop::EventLoopTimer>>;
        s_notifiers = new HashTable<CNotifier*>;
    }

    if (!s_main_event_loop) {
        s_main_event_loop = this;
#if defined(SOCK_NONBLOCK)
        int rc = pipe2(s_wake_pipe_fds, O_CLOEXEC);
#else
        int rc = pipe(s_wake_pipe_fds);
        fcntl(s_wake_pipe_fds[0], F_SETFD, FD_CLOEXEC);
        fcntl(s_wake_pipe_fds[1], F_SETFD, FD_CLOEXEC);

#endif
        ASSERT(rc == 0);
        s_event_loop_stack->append(this);

        auto rpc_path = String::format("/tmp/rpc.%d", getpid());
        rc = unlink(rpc_path.characters());
        if (rc < 0 && errno != ENOENT) {
            perror("unlink");
            ASSERT_NOT_REACHED();
        }
        s_rpc_server = CLocalServer::construct();
        s_rpc_server->set_name("CEventLoop_RPC_server");
        bool listening = s_rpc_server->listen(rpc_path);
        ASSERT(listening);

        s_rpc_server->on_ready_to_accept = [&] {
            RPCClient::construct(s_rpc_server->accept());
        };
    }

#ifdef CEVENTLOOP_DEBUG
    dbg() << getpid() << " CEventLoop constructed :)";
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
    CEventLoop* event_loop = s_event_loop_stack->last();
    ASSERT(event_loop != nullptr);
    return *event_loop;
}

void CEventLoop::quit(int code)
{
    dbg() << "CEventLoop::quit(" << code << ")";
    m_exit_requested = true;
    m_exit_code = code;
}

void CEventLoop::unquit()
{
    dbg() << "CEventLoop::unquit()";
    m_exit_requested = false;
    m_exit_code = 0;
}

struct CEventLoopPusher {
public:
    CEventLoopPusher(CEventLoop& event_loop)
        : m_event_loop(event_loop)
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
    for (;;) {
        if (m_exit_requested)
            return m_exit_code;
        pump();
    }
    ASSERT_NOT_REACHED();
}

void CEventLoop::pump(WaitMode mode)
{
    if (m_queued_events.is_empty())
        wait_for_event(mode);

    decltype(m_queued_events) events;
    {
        LOCKER(m_lock);
        events = move(m_queued_events);
    }

    for (int i = 0; i < events.size(); ++i) {
        auto& queued_event = events.at(i);
#ifndef __clang__
        ASSERT(queued_event.event);
#endif
        auto* receiver = queued_event.receiver.ptr();
        auto& event = *queued_event.event;
#ifdef CEVENTLOOP_DEBUG
        if (receiver)
            dbg() << "CEventLoop: " << *receiver << " event " << (int)event.type();
#endif
        if (!receiver) {
            switch (event.type()) {
            case CEvent::Quit:
                ASSERT_NOT_REACHED();
                return;
            default:
                dbg() << "Event type " << event.type() << " with no receiver :(";
            }
        } else if (event.type() == CEvent::Type::DeferredInvoke) {
#ifdef DEFERRED_INVOKE_DEBUG
            printf("DeferredInvoke: receiver=%s{%p}\n", receiver->class_name(), receiver);
#endif
            static_cast<CDeferredInvocationEvent&>(event).m_invokee(*receiver);
        } else {
            NonnullRefPtr<CObject> protector(*receiver);
            receiver->dispatch_event(event);
        }

        if (m_exit_requested) {
            LOCKER(m_lock);
#ifdef CEVENTLOOP_DEBUG
            dbg() << "CEventLoop: Exit requested. Rejigging " << (events.size() - i) << " events.";
#endif
            decltype(m_queued_events) new_event_queue;
            new_event_queue.ensure_capacity(m_queued_events.size() + events.size());
            for (; i < events.size(); ++i)
                new_event_queue.unchecked_append(move(events[i]));
            new_event_queue.append(move(m_queued_events));
            m_queued_events = move(new_event_queue);
            return;
        }
    }
}

void CEventLoop::post_event(CObject& receiver, NonnullOwnPtr<CEvent>&& event)
{
    LOCKER(m_lock);
#ifdef CEVENTLOOP_DEBUG
    dbg() << "CEventLoop::post_event: {" << m_queued_events.size() << "} << receiver=" << receiver << ", event=" << event;
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
    auto add_fd_to_set = [&max_fd](int fd, fd_set& set) {
        FD_SET(fd, &set);
        if (fd > max_fd)
            max_fd = fd;
    };

    int max_fd_added = -1;
    add_fd_to_set(s_wake_pipe_fds[0], rfds);
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
            timeval_sub(timeout, now, timeout);
        } else {
            should_wait_forever = true;
        }
    } else {
        should_wait_forever = false;
    }

    int marked_fd_count = CSyscallUtils::safe_syscall(select, max_fd + 1, &rfds, &wfds, nullptr, should_wait_forever ? nullptr : &timeout);
    if (FD_ISSET(s_wake_pipe_fds[0], &rfds)) {
        char buffer[32];
        auto nread = read(s_wake_pipe_fds[0], buffer, sizeof(buffer));
        if (nread < 0) {
            perror("read from wake pipe");
            ASSERT_NOT_REACHED();
        }
        ASSERT(nread > 0);
    }

    if (!s_timers->is_empty()) {
        gettimeofday(&now, nullptr);
    }

    for (auto& it : *s_timers) {
        auto& timer = *it.value;
        if (!timer.has_expired(now))
            continue;
        if (it.value->fire_when_not_visible == TimerShouldFireWhenNotVisible::No
            && it.value->owner
            && !it.value->owner->is_visible_for_timer_purposes()) {
            continue;
        }
#ifdef CEVENTLOOP_DEBUG
        dbg() << "CEventLoop: Timer " << timer.timer_id << " has expired, sending CTimerEvent to " << timer.owner;
#endif
        post_event(*timer.owner, make<CTimerEvent>(timer.timer_id));
        if (timer.should_reload) {
            timer.reload(now);
        } else {
            // FIXME: Support removing expired timers that don't want to reload.
            ASSERT_NOT_REACHED();
        }
    }

    if (!marked_fd_count)
        return;

    for (auto& notifier : *s_notifiers) {
        if (FD_ISSET(notifier->fd(), &rfds)) {
            if (notifier->on_ready_to_read)
                post_event(*notifier, make<CNotifierReadEvent>(notifier->fd()));
        }
        if (FD_ISSET(notifier->fd(), &wfds)) {
            if (notifier->on_ready_to_write)
                post_event(*notifier, make<CNotifierWriteEvent>(notifier->fd()));
        }
    }
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
        if (it.value->fire_when_not_visible == TimerShouldFireWhenNotVisible::No
            && it.value->owner
            && !it.value->owner->is_visible_for_timer_purposes()) {
            continue;
        }
        if (!has_checked_any || fire_time.tv_sec < soonest.tv_sec || (fire_time.tv_sec == soonest.tv_sec && fire_time.tv_usec < soonest.tv_usec))
            soonest = fire_time;
        has_checked_any = true;
    }
}

int CEventLoop::register_timer(CObject& object, int milliseconds, bool should_reload, TimerShouldFireWhenNotVisible fire_when_not_visible)
{
    ASSERT(milliseconds >= 0);
    auto timer = make<EventLoopTimer>();
    timer->owner = object.make_weak_ptr();
    timer->interval = milliseconds;
    timeval now;
    gettimeofday(&now, nullptr);
    timer->reload(now);
    timer->should_reload = should_reload;
    timer->fire_when_not_visible = fire_when_not_visible;
    int timer_id = ++s_next_timer_id; // FIXME: This will eventually wrap around.
    ASSERT(timer_id);                 // FIXME: Aforementioned wraparound.
    timer->timer_id = timer_id;
    s_timers->set(timer_id, move(timer));
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

void CEventLoop::wake()
{
    char ch = '!';
    int nwritten = write(s_wake_pipe_fds[1], &ch, 1);
    if (nwritten < 0) {
        perror("CEventLoop::wake: write");
        ASSERT_NOT_REACHED();
    }
}
