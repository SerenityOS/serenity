/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, kleines Filmröllchen <malu.bertsch@gmail.com>
 * Copyright (c) 2022, the SerenityOS developers.
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
#    include <LibCore/Account.h>

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

    void reload(Time const& now);
    bool has_expired(Time const& now) const;
};

struct EventLoop::Private {
    Threading::Mutex lock;
};

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
            auto maybe_bytes_read = m_socket->read({ (u8*)&length, sizeof(length) });
            if (maybe_bytes_read.is_error()) {
                dbgln("InspectorServerConnection: Failed to read message length from inspector server connection: {}", maybe_bytes_read.error());
                shutdown();
                return;
            }

            auto bytes_read = maybe_bytes_read.release_value();
            if (bytes_read.is_empty()) {
                dbgln_if(EVENTLOOP_DEBUG, "RPC client disconnected");
                shutdown();
                return;
            }

            VERIFY(bytes_read.size() == sizeof(length));

            auto request_buffer = ByteBuffer::create_uninitialized(length).release_value();
            maybe_bytes_read = m_socket->read(request_buffer.bytes());
            if (maybe_bytes_read.is_error()) {
                dbgln("InspectorServerConnection: Failed to read message content from inspector server connection: {}", maybe_bytes_read.error());
                shutdown();
                return;
            }

            bytes_read = maybe_bytes_read.release_value();

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
    void send_response(JsonObject const& response)
    {
        auto serialized = response.to_string();
        auto bytes_to_send = serialized.bytes();
        u32 length = bytes_to_send.size();
        // FIXME: Propagate errors
        auto sent = MUST(m_socket->write({ (u8 const*)&length, sizeof(length) }));
        VERIFY(sent == sizeof(length));
        while (!bytes_to_send.is_empty()) {
            size_t bytes_sent = MUST(m_socket->write(bytes_to_send));
            bytes_to_send = bytes_to_send.slice(bytes_sent);
        }
    }

    void handle_request(JsonObject const& request)
    {
        auto type = request.get("type"sv).as_string_or({});

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
            auto address = request.get("address"sv).to_number<FlatPtr>();
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
            auto address = request.get("address"sv).to_number<FlatPtr>();
            for (auto& object : Object::all_objects()) {
                if ((FlatPtr)&object == address) {
                    bool success = object.set_property(request.get("name"sv).to_string(), request.get("value"sv));
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
    : m_wake_pipe_fds(&s_wake_pipe_fds)
    , m_private(make<Private>())
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

    if (s_event_loop_stack->is_empty()) {
        s_pid = getpid();
        s_event_loop_stack->append(*this);

#ifdef __serenity__
        if (getuid() != 0) {
            if (getenv("MAKE_INSPECTABLE") == "1"sv)
                make_inspectable = Core::EventLoop::MakeInspectable::Yes;

            if (make_inspectable == MakeInspectable::Yes
                && !s_inspector_server_connection.with_locked([](auto inspector_server_connection) { return inspector_server_connection; })) {
                if (!connect_to_inspector_server())
                    dbgln("Core::EventLoop: Failed to connect to InspectorServer");
            }
        }
#endif
    }

    initialize_wake_pipes();

    dbgln_if(EVENTLOOP_DEBUG, "{} Core::EventLoop constructed :)", getpid());
}

EventLoop::~EventLoop()
{
    if (!s_event_loop_stack->is_empty() && &s_event_loop_stack->last() == this)
        s_event_loop_stack->take_last();
}

bool connect_to_inspector_server()
{
#ifdef __serenity__
    auto inspector_server_path = Account::parse_path_with_uid("/tmp/user/%uid/portal/inspectables"sv);
    auto maybe_socket = Stream::LocalSocket::connect(inspector_server_path);
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

#define VERIFY_EVENT_LOOP_INITIALIZED()                                              \
    do {                                                                             \
        if (!s_event_loop_stack) {                                                   \
            warnln("EventLoop static API was called without prior EventLoop init!"); \
            VERIFY_NOT_REACHED();                                                    \
        }                                                                            \
    } while (0)

EventLoop& EventLoop::current()
{
    VERIFY_EVENT_LOOP_INITIALIZED();
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
        if (EventLoop::has_been_instantiated()) {
            m_event_loop.take_pending_events_from(EventLoop::current());
            s_event_loop_stack->append(event_loop);
        }
    }
    ~EventLoopPusher()
    {
        if (EventLoop::has_been_instantiated()) {
            s_event_loop_stack->take_last();
            EventLoop::current().take_pending_events_from(m_event_loop);
        }
    }

private:
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

void EventLoop::post_event(Object& receiver, NonnullOwnPtr<Event>&& event, ShouldWake should_wake)
{
    Threading::MutexLocker lock(m_private->lock);
    dbgln_if(EVENTLOOP_DEBUG, "Core::EventLoop::post_event: ({}) << receiver={}, event={}", m_queued_events.size(), receiver, event);
    m_queued_events.empend(receiver, move(event));
    if (should_wake == ShouldWake::Yes)
        wake();
}

void EventLoop::wake_once(Object& receiver, int custom_event_type)
{
    Threading::MutexLocker lock(m_private->lock);
    dbgln_if(EVENTLOOP_DEBUG, "Core::EventLoop::wake_once: event type {}", custom_event_type);
    auto identical_events = m_queued_events.find_if([&](auto& queued_event) {
        if (queued_event.receiver.is_null())
            return false;
        auto const& event = queued_event.event;
        auto is_receiver_identical = queued_event.receiver.ptr() == &receiver;
        auto event_id_matches = event->type() == Event::Type::Custom && static_cast<CustomEvent const*>(event.ptr())->custom_type() == custom_event_type;
        return is_receiver_identical && event_id_matches;
    });
    // Event is not in the queue yet, so we want to wake.
    if (identical_events.is_end())
        post_event(receiver, make<CustomEvent>(custom_event_type), ShouldWake::Yes);
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
    VERIFY_EVENT_LOOP_INITIALIZED();
    switch (event) {
    case ForkEvent::Child:
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

bool EventLoopTimer::has_expired(Time const& now) const
{
    return now > fire_time;
}

void EventLoopTimer::reload(Time const& now)
{
    fire_time = now + interval;
}

Optional<Time> EventLoop::get_next_timer_expiration()
{
    auto now = Time::now_monotonic_coarse();
    Optional<Time> soonest {};
    for (auto& it : *s_timers) {
        auto& fire_time = it.value->fire_time;
        auto owner = it.value->owner.strong_ref();
        if (it.value->fire_when_not_visible == TimerShouldFireWhenNotVisible::No
            && owner && !owner->is_visible_for_timer_purposes()) {
            continue;
        }
        // OPTIMIZATION: If we have a timer that needs to fire right away, we can stop looking here.
        // FIXME: This whole operation could be O(1) with a better data structure.
        if (fire_time < now)
            return now;
        if (!soonest.has_value() || fire_time < soonest.value())
            soonest = fire_time;
    }
    return soonest;
}

int EventLoop::register_timer(Object& object, int milliseconds, bool should_reload, TimerShouldFireWhenNotVisible fire_when_not_visible)
{
    VERIFY_EVENT_LOOP_INITIALIZED();
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
    VERIFY_EVENT_LOOP_INITIALIZED();
    s_id_allocator.with_locked([&](auto& allocator) { allocator->deallocate(timer_id); });
    auto it = s_timers->find(timer_id);
    if (it == s_timers->end())
        return false;
    s_timers->remove(it);
    return true;
}

void EventLoop::register_notifier(Badge<Notifier>, Notifier& notifier)
{
    VERIFY_EVENT_LOOP_INITIALIZED();
    s_notifiers->set(&notifier);
}

void EventLoop::unregister_notifier(Badge<Notifier>, Notifier& notifier)
{
    VERIFY_EVENT_LOOP_INITIALIZED();
    s_notifiers->remove(&notifier);
}

void EventLoop::wake_current()
{
    EventLoop::current().wake();
}

void EventLoop::wake()
{
    dbgln_if(EVENTLOOP_DEBUG, "Core::EventLoop::wake()");
    int wake_event = 0;
    int nwritten = write((*m_wake_pipe_fds)[1], &wake_event, sizeof(wake_event));
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

}
