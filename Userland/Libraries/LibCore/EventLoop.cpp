/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, kleines Filmröllchen <malu.bertsch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <AK/Badge.h>
#include <AK/Debug.h>
#include <AK/Format.h>
#include <AK/IDAllocator.h>
#include <AK/JsonObject.h>
#include <AK/JsonValue.h>
#include <AK/NeverDestroyed.h>
#include <AK/Singleton.h>
#include <AK/TemporaryChange.h>
#include <AK/Time.h>
#include <LibCore/Event.h>
#include <LibCore/EventLoop.h>
#include <LibCore/LocalServer.h>
#include <LibCore/Notifier.h>
#include <LibCore/Object.h>
#include <LibThreading/Mutex.h>
#include <LibThreading/MutexProtected.h>
#include <errno.h>
#include <fcntl.h>
#include <signal.h>
#include <stdio.h>
#include <string.h>
#include <sys/select.h>
#include <sys/socket.h>
#include <sys/time.h>
#include <time.h>
#include <unistd.h>

#ifdef __serenity__
extern bool s_global_initializers_ran;
#endif

namespace Core {

class InspectorServerConnection;

[[maybe_unused]] static bool connect_to_inspector_server();

struct EventLoopTimer {
    int timer_id { 0 };
    Time interval;
    Time fire_time;
    bool should_reload { false };
    TimerShouldFireWhenNotVisible fire_when_not_visible { TimerShouldFireWhenNotVisible::No };
    WeakPtr<Object> owner;

    void reload(const Time& now);
    bool has_expired(const Time& now) const;
};

struct EventLoop::Private {
    Threading::Mutex lock;
};

// The main event loop is global to the program, so it may be accessed from multiple threads.
Threading::MutexProtected<EventLoop*> s_main_event_loop;
static Threading::MutexProtected<NeverDestroyed<IDAllocator>> s_id_allocator;
static Threading::MutexProtected<RefPtr<InspectorServerConnection>> s_inspector_server_connection;

// Each thread has its own event loop stack, its own timers, notifiers and a wake pipe.
static thread_local Vector<EventLoop&>* s_event_loop_stack;
static thread_local HashMap<int, NonnullOwnPtr<EventLoopTimer>>* s_timers;
static thread_local HashTable<Notifier*>* s_notifiers;
thread_local int EventLoop::s_wake_pipe_fds[2];
thread_local bool EventLoop::s_wake_pipe_initialized { false };

void EventLoop::initialize_wake_pipes()
{
    if (!s_wake_pipe_initialized) {
#if defined(SOCK_NONBLOCK)
        int rc = pipe2(s_wake_pipe_fds, O_CLOEXEC);
#else
        int rc = pipe(s_wake_pipe_fds);
        fcntl(s_wake_pipe_fds[0], F_SETFD, FD_CLOEXEC);
        fcntl(s_wake_pipe_fds[1], F_SETFD, FD_CLOEXEC);

#endif
        VERIFY(rc == 0);
        s_wake_pipe_initialized = true;
    }
}

bool EventLoop::has_been_instantiated()
{
    return s_event_loop_stack != nullptr && !s_event_loop_stack->is_empty();
}

class SignalHandlers : public RefCounted<SignalHandlers> {
    AK_MAKE_NONCOPYABLE(SignalHandlers);
    AK_MAKE_NONMOVABLE(SignalHandlers);

public:
    SignalHandlers(int signo, void (*handle_signal)(int));
    ~SignalHandlers();

    void dispatch();
    int add(Function<void(int)>&& handler);
    bool remove(int handler_id);

    bool is_empty() const
    {
        if (m_calling_handlers) {
            for (auto& handler : m_handlers_pending) {
                if (handler.value)
                    return false; // an add is pending
            }
        }
        return m_handlers.is_empty();
    }

    bool have(int handler_id) const
    {
        if (m_calling_handlers) {
            auto it = m_handlers_pending.find(handler_id);
            if (it != m_handlers_pending.end()) {
                if (!it->value)
                    return false; // a deletion is pending
            }
        }
        return m_handlers.contains(handler_id);
    }

    int m_signo;
    void (*m_original_handler)(int); // TODO: can't use sighandler_t?
    HashMap<int, Function<void(int)>> m_handlers;
    HashMap<int, Function<void(int)>> m_handlers_pending;
    bool m_calling_handlers { false };
};

struct SignalHandlersInfo {
    HashMap<int, NonnullRefPtr<SignalHandlers>> signal_handlers;
    int next_signal_id { 0 };
};

static Singleton<SignalHandlersInfo> s_signals;
template<bool create_if_null = true>
inline SignalHandlersInfo* signals_info()
{
    return s_signals.ptr();
}

pid_t EventLoop::s_pid;

class InspectorServerConnection : public Object {
    C_OBJECT(InspectorServerConnection)
private:
    explicit InspectorServerConnection(NonnullOwnPtr<Stream::LocalSocket> socket)
        : m_socket(move(socket))
        , m_client_id(s_id_allocator.with_locked([](auto& allocator) {
            return allocator->allocate();
        }))
    {
#ifdef __serenity__
        m_socket->on_ready_to_read = [this] {
            u32 length;
            auto maybe_nread = m_socket->read({ (u8*)&length, sizeof(length) });
            if (maybe_nread.is_error()) {
                dbgln("InspectorServerConnection: Failed to read message length from inspector server connection: {}", maybe_nread.error());
                shutdown();
                return;
            }

            auto nread = maybe_nread.release_value();
            if (nread == 0) {
                dbgln_if(EVENTLOOP_DEBUG, "RPC client disconnected");
                shutdown();
                return;
            }

            VERIFY(nread == sizeof(length));

            auto request_buffer = ByteBuffer::create_uninitialized(length).release_value();
            maybe_nread = m_socket->read(request_buffer.bytes());
            if (maybe_nread.is_error()) {
                dbgln("InspectorServerConnection: Failed to read message content from inspector server connection: {}", maybe_nread.error());
                shutdown();
                return;
            }

            nread = maybe_nread.release_value();

            auto request_json = JsonValue::from_string(request_buffer);
            if (request_json.is_error() || !request_json.value().is_object()) {
                dbgln("RPC client sent invalid request");
                shutdown();
                return;
            }

            handle_request(request_json.value().as_object());
        };
#else
        warnln("RPC Client constructed outside serenity, this is very likely a bug!");
#endif
    }
    virtual ~InspectorServerConnection() override
    {
        if (auto inspected_object = m_inspected_object.strong_ref())
            inspected_object->decrement_inspector_count({});
    }

public:
    void send_response(const JsonObject& response)
    {
        auto serialized = response.to_string();
        u32 length = serialized.length();
        // FIXME: Propagate errors
        MUST(m_socket->write({ (const u8*)&length, sizeof(length) }));
        MUST(m_socket->write(serialized.bytes()));
    }

    void handle_request(const JsonObject& request)
    {
        auto type = request.get("type").as_string_or({});

        if (type.is_null()) {
            dbgln("RPC client sent request without type field");
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
            for (auto& object : Object::all_objects()) {
                JsonObject json_object;
                object.save_to(json_object);
                objects.append(move(json_object));
            }
            response.set("objects", move(objects));
            send_response(response);
            return;
        }

        if (type == "SetInspectedObject") {
            auto address = request.get("address").to_number<FlatPtr>();
            for (auto& object : Object::all_objects()) {
                if ((FlatPtr)&object == address) {
                    if (auto inspected_object = m_inspected_object.strong_ref())
                        inspected_object->decrement_inspector_count({});
                    m_inspected_object = object;
                    object.increment_inspector_count({});
                    break;
                }
            }
            return;
        }

        if (type == "SetProperty") {
            auto address = request.get("address").to_number<FlatPtr>();
            for (auto& object : Object::all_objects()) {
                if ((FlatPtr)&object == address) {
                    bool success = object.set_property(request.get("name").to_string(), request.get("value"));
                    JsonObject response;
                    response.set("type", "SetProperty");
                    response.set("success", success);
                    send_response(response);
                    break;
                }
            }
            return;
        }

        if (type == "Disconnect") {
            shutdown();
            return;
        }
    }

    void shutdown()
    {
        s_id_allocator.with_locked([this](auto& allocator) { allocator->deallocate(m_client_id); });
    }

private:
    NonnullOwnPtr<Stream::LocalSocket> m_socket;
    WeakPtr<Object> m_inspected_object;
    int m_client_id { -1 };
};

EventLoop::EventLoop([[maybe_unused]] MakeInspectable make_inspectable)
    : m_private(make<Private>())
{
#ifdef __serenity__
    if (!s_global_initializers_ran) {
        // NOTE: Trying to have an event loop as a global variable will lead to initialization-order fiascos,
        //       as the event loop constructor accesses and/or sets other global variables.
        //       Therefore, we crash the program before ASAN catches us.
        //       If you came here because of the assertion failure, please redesign your program to not have global event loops.
        //       The common practice is to initialize the main event loop in the main function, and if necessary,
        //       pass event loop references around or access them with EventLoop::with_main_locked() and EventLoop::current().
        VERIFY_NOT_REACHED();
    }
#endif

    if (!s_event_loop_stack) {
        s_event_loop_stack = new Vector<EventLoop&>;
        s_timers = new HashMap<int, NonnullOwnPtr<EventLoopTimer>>;
        s_notifiers = new HashTable<Notifier*>;
    }
    s_main_event_loop.with_locked([&, this](auto*& main_event_loop) {
        if (main_event_loop == nullptr) {
            main_event_loop = this;
            s_pid = getpid();
            s_event_loop_stack->append(*this);

#ifdef __serenity__
            if (getuid() != 0
                && make_inspectable == MakeInspectable::Yes
                // FIXME: Deadlock potential; though the main loop and inspector server connection are rarely used in conjunction
                && s_inspector_server_connection.with_locked([](auto inspector_server_connection) { return inspector_server_connection; })) {
                if (!connect_to_inspector_server())
                    dbgln("Core::EventLoop: Failed to connect to InspectorServer");
            }
#endif
        }
    });

    initialize_wake_pipes();

    dbgln_if(EVENTLOOP_DEBUG, "{} Core::EventLoop constructed :)", getpid());
}

EventLoop::~EventLoop()
{
    // NOTE: Pop the main event loop off of the stack when destroyed.
    s_main_event_loop.with_locked([this](auto*& main_event_loop) {
        if (this == main_event_loop) {
            s_event_loop_stack->take_last();
            main_event_loop = nullptr;
        }
    });
}

bool connect_to_inspector_server()
{
#ifdef __serenity__
    auto maybe_socket = Core::Stream::LocalSocket::connect("/tmp/portal/inspectables");
    if (maybe_socket.is_error()) {
        dbgln("connect_to_inspector_server: Failed to connect: {}", maybe_socket.error());
        return false;
    }
    s_inspector_server_connection.with_locked([&](auto& inspector_server_connection) {
        inspector_server_connection = InspectorServerConnection::construct(maybe_socket.release_value());
    });
    return true;
#else
    VERIFY_NOT_REACHED();
#endif
}

EventLoop& EventLoop::current()
{
    return s_event_loop_stack->last();
}

void EventLoop::quit(int code)
{
    dbgln_if(EVENTLOOP_DEBUG, "Core::EventLoop::quit({})", code);
    m_exit_requested = true;
    m_exit_code = code;
}

void EventLoop::unquit()
{
    dbgln_if(EVENTLOOP_DEBUG, "Core::EventLoop::unquit()");
    m_exit_requested = false;
    m_exit_code = 0;
}

struct EventLoopPusher {
public:
    EventLoopPusher(EventLoop& event_loop)
        : m_event_loop(event_loop)
    {
        if (!is_main_event_loop()) {
            m_event_loop.take_pending_events_from(EventLoop::current());
            s_event_loop_stack->append(event_loop);
        }
    }
    ~EventLoopPusher()
    {
        if (!is_main_event_loop()) {
            s_event_loop_stack->take_last();
            EventLoop::current().take_pending_events_from(m_event_loop);
        }
    }

private:
    bool is_main_event_loop()
    {
        return s_main_event_loop.with_locked([this](auto* main_event_loop) { return &m_event_loop == main_event_loop; });
    }

    EventLoop& m_event_loop;
};

int EventLoop::exec()
{
    EventLoopPusher pusher(*this);
    for (;;) {
        if (m_exit_requested)
            return m_exit_code;
        pump();
    }
    VERIFY_NOT_REACHED();
}

void EventLoop::spin_until(Function<bool()> goal_condition)
{
    EventLoopPusher pusher(*this);
    while (!goal_condition())
        pump();
}

size_t EventLoop::pump(WaitMode mode)
{
    wait_for_event(mode);

    decltype(m_queued_events) events;
    {
        Threading::MutexLocker locker(m_private->lock);
        events = move(m_queued_events);
    }

    size_t processed_events = 0;
    for (size_t i = 0; i < events.size(); ++i) {
        auto& queued_event = events.at(i);
        auto receiver = queued_event.receiver.strong_ref();
        auto& event = *queued_event.event;
        if (receiver)
            dbgln_if(EVENTLOOP_DEBUG, "Core::EventLoop: {} event {}", *receiver, event.type());

        if (!receiver) {
            switch (event.type()) {
            case Event::Quit:
                VERIFY_NOT_REACHED();
            default:
                dbgln_if(EVENTLOOP_DEBUG, "Event type {} with no receiver :(", event.type());
                break;
            }
        } else if (event.type() == Event::Type::DeferredInvoke) {
            dbgln_if(DEFERRED_INVOKE_DEBUG, "DeferredInvoke: receiver = {}", *receiver);
            static_cast<DeferredInvocationEvent&>(event).m_invokee();
        } else {
            NonnullRefPtr<Object> protector(*receiver);
            receiver->dispatch_event(event);
        }
        ++processed_events;

        if (m_exit_requested) {
            Threading::MutexLocker locker(m_private->lock);
            dbgln_if(EVENTLOOP_DEBUG, "Core::EventLoop: Exit requested. Rejigging {} events.", events.size() - i);
            decltype(m_queued_events) new_event_queue;
            new_event_queue.ensure_capacity(m_queued_events.size() + events.size());
            for (++i; i < events.size(); ++i)
                new_event_queue.unchecked_append(move(events[i]));
            new_event_queue.extend(move(m_queued_events));
            m_queued_events = move(new_event_queue);
            break;
        }
    }

    return processed_events;
}

void EventLoop::post_event(Object& receiver, NonnullOwnPtr<Event>&& event)
{
    Threading::MutexLocker lock(m_private->lock);
    dbgln_if(EVENTLOOP_DEBUG, "Core::EventLoop::post_event: ({}) << receiver={}, event={}", m_queued_events.size(), receiver, event);
    m_queued_events.empend(receiver, move(event));
}

SignalHandlers::SignalHandlers(int signo, void (*handle_signal)(int))
    : m_signo(signo)
    , m_original_handler(signal(signo, handle_signal))
{
    dbgln_if(EVENTLOOP_DEBUG, "Core::EventLoop: Registered handler for signal {}", m_signo);
}

SignalHandlers::~SignalHandlers()
{
    dbgln_if(EVENTLOOP_DEBUG, "Core::EventLoop: Unregistering handler for signal {}", m_signo);
    signal(m_signo, m_original_handler);
}

void SignalHandlers::dispatch()
{
    TemporaryChange change(m_calling_handlers, true);
    for (auto& handler : m_handlers)
        handler.value(m_signo);
    if (!m_handlers_pending.is_empty()) {
        // Apply pending adds/removes
        for (auto& handler : m_handlers_pending) {
            if (handler.value) {
                auto result = m_handlers.set(handler.key, move(handler.value));
                VERIFY(result == AK::HashSetResult::InsertedNewEntry);
            } else {
                m_handlers.remove(handler.key);
            }
        }
        m_handlers_pending.clear();
    }
}

int SignalHandlers::add(Function<void(int)>&& handler)
{
    int id = ++signals_info()->next_signal_id; // TODO: worry about wrapping and duplicates?
    if (m_calling_handlers)
        m_handlers_pending.set(id, move(handler));
    else
        m_handlers.set(id, move(handler));
    return id;
}

bool SignalHandlers::remove(int handler_id)
{
    VERIFY(handler_id != 0);
    if (m_calling_handlers) {
        auto it = m_handlers.find(handler_id);
        if (it != m_handlers.end()) {
            // Mark pending remove
            m_handlers_pending.set(handler_id, {});
            return true;
        }
        it = m_handlers_pending.find(handler_id);
        if (it != m_handlers_pending.end()) {
            if (!it->value)
                return false; // already was marked as deleted
            it->value = nullptr;
            return true;
        }
        return false;
    }
    return m_handlers.remove(handler_id);
}

void EventLoop::dispatch_signal(int signo)
{
    auto& info = *signals_info();
    auto handlers = info.signal_handlers.find(signo);
    if (handlers != info.signal_handlers.end()) {
        // Make sure we bump the ref count while dispatching the handlers!
        // This allows a handler to unregister/register while the handlers
        // are being called!
        auto handler = handlers->value;
        dbgln_if(EVENTLOOP_DEBUG, "Core::EventLoop: dispatching signal {}", signo);
        handler->dispatch();
    }
}

void EventLoop::handle_signal(int signo)
{
    VERIFY(signo != 0);
    // We MUST check if the current pid still matches, because there
    // is a window between fork() and exec() where a signal delivered
    // to our fork could be inadvertently routed to the parent process!
    if (getpid() == s_pid) {
        int nwritten = write(s_wake_pipe_fds[1], &signo, sizeof(signo));
        if (nwritten < 0) {
            perror("EventLoop::register_signal: write");
            VERIFY_NOT_REACHED();
        }
    } else {
        // We're a fork who received a signal, reset s_pid
        s_pid = 0;
    }
}

int EventLoop::register_signal(int signo, Function<void(int)> handler)
{
    VERIFY(signo != 0);
    auto& info = *signals_info();
    auto handlers = info.signal_handlers.find(signo);
    if (handlers == info.signal_handlers.end()) {
        auto signal_handlers = adopt_ref(*new SignalHandlers(signo, EventLoop::handle_signal));
        auto handler_id = signal_handlers->add(move(handler));
        info.signal_handlers.set(signo, move(signal_handlers));
        return handler_id;
    } else {
        return handlers->value->add(move(handler));
    }
}

void EventLoop::unregister_signal(int handler_id)
{
    VERIFY(handler_id != 0);
    int remove_signo = 0;
    auto& info = *signals_info();
    for (auto& h : info.signal_handlers) {
        auto& handlers = *h.value;
        if (handlers.remove(handler_id)) {
            if (handlers.is_empty())
                remove_signo = handlers.m_signo;
            break;
        }
    }
    if (remove_signo != 0)
        info.signal_handlers.remove(remove_signo);
}

void EventLoop::notify_forked(ForkEvent event)
{
    switch (event) {
    case ForkEvent::Child:
        s_main_event_loop.with_locked([]([[maybe_unused]] auto*& main_event_loop) { main_event_loop = nullptr; });
        s_event_loop_stack->clear();
        s_timers->clear();
        s_notifiers->clear();
        s_wake_pipe_initialized = false;
        initialize_wake_pipes();
        if (auto* info = signals_info<false>()) {
            info->signal_handlers.clear();
            info->next_signal_id = 0;
        }
        s_pid = 0;
#ifdef __serenity__
        s_main_event_loop.with_locked([]([[maybe_unused]] auto*& main_event_loop) { main_event_loop = nullptr; });
#endif
        return;
    }

    VERIFY_NOT_REACHED();
}

void EventLoop::wait_for_event(WaitMode mode)
{
    fd_set rfds;
    fd_set wfds;
retry:
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
        if (notifier->event_mask() & Notifier::Read)
            add_fd_to_set(notifier->fd(), rfds);
        if (notifier->event_mask() & Notifier::Write)
            add_fd_to_set(notifier->fd(), wfds);
        if (notifier->event_mask() & Notifier::Exceptional)
            VERIFY_NOT_REACHED();
    }

    bool queued_events_is_empty;
    {
        Threading::MutexLocker locker(m_private->lock);
        queued_events_is_empty = m_queued_events.is_empty();
    }

    Time now;
    struct timeval timeout = { 0, 0 };
    bool should_wait_forever = false;
    if (mode == WaitMode::WaitForEvents && queued_events_is_empty) {
        auto next_timer_expiration = get_next_timer_expiration();
        if (next_timer_expiration.has_value()) {
            now = Time::now_monotonic_coarse();
            auto computed_timeout = next_timer_expiration.value() - now;
            if (computed_timeout.is_negative())
                computed_timeout = Time::zero();
            timeout = computed_timeout.to_timeval();
        } else {
            should_wait_forever = true;
        }
    }

try_select_again:
    int marked_fd_count = select(max_fd + 1, &rfds, &wfds, nullptr, should_wait_forever ? nullptr : &timeout);
    if (marked_fd_count < 0) {
        int saved_errno = errno;
        if (saved_errno == EINTR) {
            if (m_exit_requested)
                return;
            goto try_select_again;
        }
        dbgln("Core::EventLoop::wait_for_event: {} ({}: {})", marked_fd_count, saved_errno, strerror(saved_errno));
        VERIFY_NOT_REACHED();
    }
    if (FD_ISSET(s_wake_pipe_fds[0], &rfds)) {
        int wake_events[8];
        ssize_t nread;
        // We might receive another signal while read()ing here. The signal will go to the handle_signal properly,
        // but we get interrupted. Therefore, just retry while we were interrupted.
        do {
            errno = 0;
            nread = read(s_wake_pipe_fds[0], wake_events, sizeof(wake_events));
            if (nread == 0)
                break;
        } while (nread < 0 && errno == EINTR);
        if (nread < 0) {
            perror("Core::EventLoop::wait_for_event: read from wake pipe");
            VERIFY_NOT_REACHED();
        }
        VERIFY(nread > 0);
        bool wake_requested = false;
        int event_count = nread / sizeof(wake_events[0]);
        for (int i = 0; i < event_count; i++) {
            if (wake_events[i] != 0)
                dispatch_signal(wake_events[i]);
            else
                wake_requested = true;
        }

        if (!wake_requested && nread == sizeof(wake_events))
            goto retry;
    }

    if (!s_timers->is_empty()) {
        now = Time::now_monotonic_coarse();
    }

    for (auto& it : *s_timers) {
        auto& timer = *it.value;
        if (!timer.has_expired(now))
            continue;
        auto owner = timer.owner.strong_ref();
        if (timer.fire_when_not_visible == TimerShouldFireWhenNotVisible::No
            && owner && !owner->is_visible_for_timer_purposes()) {
            continue;
        }

        dbgln_if(EVENTLOOP_DEBUG, "Core::EventLoop: Timer {} has expired, sending Core::TimerEvent to {}", timer.timer_id, *owner);

        if (owner)
            post_event(*owner, make<TimerEvent>(timer.timer_id));
        if (timer.should_reload) {
            timer.reload(now);
        } else {
            // FIXME: Support removing expired timers that don't want to reload.
            VERIFY_NOT_REACHED();
        }
    }

    if (!marked_fd_count)
        return;

    for (auto& notifier : *s_notifiers) {
        if (FD_ISSET(notifier->fd(), &rfds)) {
            if (notifier->event_mask() & Notifier::Event::Read)
                post_event(*notifier, make<NotifierReadEvent>(notifier->fd()));
        }
        if (FD_ISSET(notifier->fd(), &wfds)) {
            if (notifier->event_mask() & Notifier::Event::Write)
                post_event(*notifier, make<NotifierWriteEvent>(notifier->fd()));
        }
    }
}

bool EventLoopTimer::has_expired(const Time& now) const
{
    return now > fire_time;
}

void EventLoopTimer::reload(const Time& now)
{
    fire_time = now + interval;
}

Optional<Time> EventLoop::get_next_timer_expiration()
{
    Optional<Time> soonest {};
    for (auto& it : *s_timers) {
        auto& fire_time = it.value->fire_time;
        auto owner = it.value->owner.strong_ref();
        if (it.value->fire_when_not_visible == TimerShouldFireWhenNotVisible::No
            && owner && !owner->is_visible_for_timer_purposes()) {
            continue;
        }
        if (!soonest.has_value() || fire_time < soonest.value())
            soonest = fire_time;
    }
    return soonest;
}

int EventLoop::register_timer(Object& object, int milliseconds, bool should_reload, TimerShouldFireWhenNotVisible fire_when_not_visible)
{
    VERIFY(milliseconds >= 0);
    auto timer = make<EventLoopTimer>();
    timer->owner = object;
    timer->interval = Time::from_milliseconds(milliseconds);
    timer->reload(Time::now_monotonic_coarse());
    timer->should_reload = should_reload;
    timer->fire_when_not_visible = fire_when_not_visible;
    int timer_id = s_id_allocator.with_locked([](auto& allocator) { return allocator->allocate(); });
    timer->timer_id = timer_id;
    s_timers->set(timer_id, move(timer));
    return timer_id;
}

bool EventLoop::unregister_timer(int timer_id)
{
    s_id_allocator.with_locked([&](auto& allocator) { allocator->deallocate(timer_id); });
    auto it = s_timers->find(timer_id);
    if (it == s_timers->end())
        return false;
    s_timers->remove(it);
    return true;
}

void EventLoop::register_notifier(Badge<Notifier>, Notifier& notifier)
{
    s_notifiers->set(&notifier);
}

void EventLoop::unregister_notifier(Badge<Notifier>, Notifier& notifier)
{
    s_notifiers->remove(&notifier);
}

void EventLoop::wake()
{
    int wake_event = 0;
    int nwritten = write(s_wake_pipe_fds[1], &wake_event, sizeof(wake_event));
    if (nwritten < 0) {
        perror("EventLoop::wake: write");
        VERIFY_NOT_REACHED();
    }
}

EventLoop::QueuedEvent::QueuedEvent(Object& receiver, NonnullOwnPtr<Event> event)
    : receiver(receiver)
    , event(move(event))
{
}

EventLoop::QueuedEvent::QueuedEvent(QueuedEvent&& other)
    : receiver(other.receiver)
    , event(move(other.event))
{
}

EventLoop::QueuedEvent::~QueuedEvent()
{
}

}
