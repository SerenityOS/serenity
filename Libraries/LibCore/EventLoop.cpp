/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/Badge.h>
#include <AK/IDAllocator.h>
#include <AK/JsonObject.h>
#include <AK/JsonValue.h>
#include <AK/Time.h>
#include <LibCore/Event.h>
#include <LibCore/EventLoop.h>
#include <LibCore/LocalServer.h>
#include <LibCore/LocalSocket.h>
#include <LibCore/Notifier.h>
#include <LibCore/Object.h>
#include <LibCore/SyscallUtils.h>
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

namespace Core {

class RPCClient;

struct EventLoopTimer {
    int timer_id { 0 };
    int interval { 0 };
    timeval fire_time { 0, 0 };
    bool should_reload { false };
    TimerShouldFireWhenNotVisible fire_when_not_visible { TimerShouldFireWhenNotVisible::No };
    WeakPtr<Object> owner;

    void reload(const timeval& now);
    bool has_expired(const timeval& now) const;
};

struct EventLoop::Private {
    LibThread::Lock lock;
};

static EventLoop* s_main_event_loop;
static Vector<EventLoop*>* s_event_loop_stack;
static IDAllocator s_id_allocator;
static HashMap<int, NonnullOwnPtr<EventLoopTimer>>* s_timers;
static HashTable<Notifier*>* s_notifiers;
int EventLoop::s_wake_pipe_fds[2];
static RefPtr<LocalServer> s_rpc_server;
HashMap<int, RefPtr<RPCClient>> s_rpc_clients;

class RPCClient : public Object {
    C_OBJECT(RPCClient)
public:
    explicit RPCClient(RefPtr<LocalSocket> socket)
        : m_socket(move(socket))
        , m_client_id(s_id_allocator.allocate())
    {
        s_rpc_clients.set(m_client_id, this);
        add_child(*m_socket);
        m_socket->on_ready_to_read = [this] {
            u32 length;
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
        u32 length = serialized.length();
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
            for (auto& object : Object::all_objects()) {
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
        s_id_allocator.deallocate(m_client_id);
    }

private:
    RefPtr<LocalSocket> m_socket;
    int m_client_id { -1 };
};

EventLoop::EventLoop()
    : m_private(make<Private>())
{
    if (!s_event_loop_stack) {
        s_event_loop_stack = new Vector<EventLoop*>;
        s_timers = new HashMap<int, NonnullOwnPtr<EventLoopTimer>>;
        s_notifiers = new HashTable<Notifier*>;
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
        s_rpc_server = LocalServer::construct();
        s_rpc_server->set_name("Core::EventLoop_RPC_server");
        bool listening = s_rpc_server->listen(rpc_path);
        ASSERT(listening);

        s_rpc_server->on_ready_to_accept = [&] {
            RPCClient::construct(s_rpc_server->accept());
        };
    }

#ifdef CEVENTLOOP_DEBUG
    dbg() << getpid() << " Core::EventLoop constructed :)";
#endif
}

EventLoop::~EventLoop()
{
}

EventLoop& EventLoop::main()
{
    ASSERT(s_main_event_loop);
    return *s_main_event_loop;
}

EventLoop& EventLoop::current()
{
    EventLoop* event_loop = s_event_loop_stack->last();
    ASSERT(event_loop != nullptr);
    return *event_loop;
}

void EventLoop::quit(int code)
{
    dbg() << "Core::EventLoop::quit(" << code << ")";
    m_exit_requested = true;
    m_exit_code = code;
}

void EventLoop::unquit()
{
    dbg() << "Core::EventLoop::unquit()";
    m_exit_requested = false;
    m_exit_code = 0;
}

struct EventLoopPusher {
public:
    EventLoopPusher(EventLoop& event_loop)
        : m_event_loop(event_loop)
    {
        if (&m_event_loop != s_main_event_loop) {
            m_event_loop.take_pending_events_from(EventLoop::current());
            s_event_loop_stack->append(&event_loop);
        }
    }
    ~EventLoopPusher()
    {
        if (&m_event_loop != s_main_event_loop) {
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
    ASSERT_NOT_REACHED();
}

void EventLoop::pump(WaitMode mode)
{
    if (m_queued_events.is_empty())
        wait_for_event(mode);

    decltype(m_queued_events) events;
    {
        LOCKER(m_private->lock);
        events = move(m_queued_events);
    }

    for (size_t i = 0; i < events.size(); ++i) {
        auto& queued_event = events.at(i);
#ifndef __clang__
        ASSERT(queued_event.event);
#endif
        auto* receiver = queued_event.receiver.ptr();
        auto& event = *queued_event.event;
#ifdef CEVENTLOOP_DEBUG
        if (receiver)
            dbg() << "Core::EventLoop: " << *receiver << " event " << (int)event.type();
#endif
        if (!receiver) {
            switch (event.type()) {
            case Event::Quit:
                ASSERT_NOT_REACHED();
                return;
            default:
                dbg() << "Event type " << event.type() << " with no receiver :(";
            }
        } else if (event.type() == Event::Type::DeferredInvoke) {
#ifdef DEFERRED_INVOKE_DEBUG
            printf("DeferredInvoke: receiver=%s{%p}\n", receiver->class_name(), receiver);
#endif
            static_cast<DeferredInvocationEvent&>(event).m_invokee(*receiver);
        } else {
            NonnullRefPtr<Object> protector(*receiver);
            receiver->dispatch_event(event);
        }

        if (m_exit_requested) {
            LOCKER(m_private->lock);
#ifdef CEVENTLOOP_DEBUG
            dbg() << "Core::EventLoop: Exit requested. Rejigging " << (events.size() - i) << " events.";
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

void EventLoop::post_event(Object& receiver, NonnullOwnPtr<Event>&& event)
{
    LOCKER(m_private->lock);
#ifdef CEVENTLOOP_DEBUG
    dbg() << "Core::EventLoop::post_event: {" << m_queued_events.size() << "} << receiver=" << receiver << ", event=" << event;
#endif
    m_queued_events.empend(receiver, move(event));
}

void EventLoop::wait_for_event(WaitMode mode)
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
        if (notifier->event_mask() & Notifier::Read)
            add_fd_to_set(notifier->fd(), rfds);
        if (notifier->event_mask() & Notifier::Write)
            add_fd_to_set(notifier->fd(), wfds);
        if (notifier->event_mask() & Notifier::Exceptional)
            ASSERT_NOT_REACHED();
    }

    bool queued_events_is_empty;
    {
        LOCKER(m_private->lock);
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
            if (timeout.tv_sec < 0) {
                timeout.tv_sec = 0;
                timeout.tv_usec = 0;
            }
        } else {
            should_wait_forever = true;
        }
    } else {
        should_wait_forever = false;
    }

    int marked_fd_count = Core::safe_syscall(select, max_fd + 1, &rfds, &wfds, nullptr, should_wait_forever ? nullptr : &timeout);
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
        dbg() << "Core::EventLoop: Timer " << timer.timer_id << " has expired, sending Core::TimerEvent to " << timer.owner;
#endif
        post_event(*timer.owner, make<TimerEvent>(timer.timer_id));
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
                post_event(*notifier, make<NotifierReadEvent>(notifier->fd()));
        }
        if (FD_ISSET(notifier->fd(), &wfds)) {
            if (notifier->on_ready_to_write)
                post_event(*notifier, make<NotifierWriteEvent>(notifier->fd()));
        }
    }
}

bool EventLoopTimer::has_expired(const timeval& now) const
{
    return now.tv_sec > fire_time.tv_sec || (now.tv_sec == fire_time.tv_sec && now.tv_usec >= fire_time.tv_usec);
}

void EventLoopTimer::reload(const timeval& now)
{
    fire_time = now;
    fire_time.tv_sec += interval / 1000;
    fire_time.tv_usec += (interval % 1000) * 1000;
}

void EventLoop::get_next_timer_expiration(timeval& soonest)
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

int EventLoop::register_timer(Object& object, int milliseconds, bool should_reload, TimerShouldFireWhenNotVisible fire_when_not_visible)
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
    int timer_id = s_id_allocator.allocate();
    timer->timer_id = timer_id;
    s_timers->set(timer_id, move(timer));
    return timer_id;
}

bool EventLoop::unregister_timer(int timer_id)
{
    s_id_allocator.deallocate(timer_id);
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
    char ch = '!';
    int nwritten = write(s_wake_pipe_fds[1], &ch, 1);
    if (nwritten < 0) {
        perror("EventLoop::wake: write");
        ASSERT_NOT_REACHED();
    }
}

EventLoop::QueuedEvent::QueuedEvent(Object& receiver, NonnullOwnPtr<Event> event)
    : receiver(receiver.make_weak_ptr())
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
