/*
 * Copyright (c) 2018-2023, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Function.h>
#include <AK/Types.h>
#include <AK/WeakPtr.h>
#include <LibCore/Forward.h>

namespace Core {

class Event {
public:
    enum Type {
        Invalid = 0,
        Quit,
        Timer,
        NotifierActivation,
        DeferredInvoke,
        ChildAdded,
        ChildRemoved,
        Custom,
    };

    Event() = default;
    explicit Event(unsigned type)
        : m_type(type)
    {
    }
    virtual ~Event() = default;

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
    friend class ThreadEventQueue;

public:
    DeferredInvocationEvent(Function<void()>&& invokee)
        : Event(Event::Type::DeferredInvoke)
        , m_invokee(move(invokee))
    {
    }

private:
    Function<void()> m_invokee;
};

class TimerEvent final : public Event {
public:
    explicit TimerEvent()
        : Event(Event::Timer)
    {
    }

    ~TimerEvent() = default;
};

class NotifierActivationEvent final : public Event {
public:
    explicit NotifierActivationEvent()
        : Event(Event::NotifierActivation)
    {
    }
    ~NotifierActivationEvent() = default;
};

class ChildEvent final : public Event {
public:
    ChildEvent(Type, EventReceiver& child, EventReceiver* insertion_before_child = nullptr);
    ~ChildEvent() = default;

    EventReceiver* child();
    EventReceiver const* child() const;

    EventReceiver* insertion_before_child();
    EventReceiver const* insertion_before_child() const;

private:
    WeakPtr<EventReceiver> m_child;
    WeakPtr<EventReceiver> m_insertion_before_child;
};

class CustomEvent : public Event {
public:
    CustomEvent(int custom_type)
        : Event(Event::Type::Custom)
        , m_custom_type(custom_type)
    {
    }
    ~CustomEvent() = default;

    int custom_type() const { return m_custom_type; }

private:
    int m_custom_type { 0 };
};

}
