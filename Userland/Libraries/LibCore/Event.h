/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Function.h>
#include <AK/String.h>
#include <AK/Types.h>
#include <AK/WeakPtr.h>
#include <LibCore/DeferredInvocationContext.h>
#include <LibCore/Forward.h>

namespace Core {

class Event {
public:
    enum Type {
        Invalid = 0,
        Quit,
        Timer,
        NotifierRead,
        NotifierWrite,
        DeferredInvoke,
        ChildAdded,
        ChildRemoved,
        Custom,
    };

    Event() { }
    explicit Event(unsigned type)
        : m_type(type)
    {
    }
    virtual ~Event() { }

    unsigned type() const { return m_type; }

    bool is_accepted() const { return m_accepted; }
    void accept() { m_accepted = true; }
    void ignore() { m_accepted = false; }

private:
    unsigned m_type { Type::Invalid };
    bool m_accepted { true };
};

class DeferredInvocationEvent : public Event {
    friend class EventLoop;

public:
    DeferredInvocationEvent(NonnullRefPtr<DeferredInvocationContext> context, Function<void()> invokee)
        : Event(Event::Type::DeferredInvoke)
        , m_context(move(context))
        , m_invokee(move(invokee))
    {
    }

private:
    NonnullRefPtr<DeferredInvocationContext> m_context;
    Function<void()> m_invokee;
};

class TimerEvent final : public Event {
public:
    explicit TimerEvent(int timer_id)
        : Event(Event::Timer)
        , m_timer_id(timer_id)
    {
    }
    ~TimerEvent() { }

    int timer_id() const { return m_timer_id; }

private:
    int m_timer_id;
};

class NotifierReadEvent final : public Event {
public:
    explicit NotifierReadEvent(int fd)
        : Event(Event::NotifierRead)
        , m_fd(fd)
    {
    }
    ~NotifierReadEvent() { }

    int fd() const { return m_fd; }

private:
    int m_fd;
};

class NotifierWriteEvent final : public Event {
public:
    explicit NotifierWriteEvent(int fd)
        : Event(Event::NotifierWrite)
        , m_fd(fd)
    {
    }
    ~NotifierWriteEvent() { }

    int fd() const { return m_fd; }

private:
    int m_fd;
};

class ChildEvent final : public Event {
public:
    ChildEvent(Type, Object& child, Object* insertion_before_child = nullptr);
    ~ChildEvent();

    Object* child();
    const Object* child() const;

    Object* insertion_before_child();
    const Object* insertion_before_child() const;

private:
    WeakPtr<Object> m_child;
    WeakPtr<Object> m_insertion_before_child;
};

class CustomEvent : public Event {
public:
    CustomEvent(int custom_type)
        : Event(Event::Type::Custom)
        , m_custom_type(custom_type)
    {
    }
    ~CustomEvent() { }

    int custom_type() const { return m_custom_type; }

private:
    int m_custom_type { 0 };
};

}
