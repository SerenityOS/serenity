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

#pragma once

#include <AK/Function.h>
#include <AK/String.h>
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
    DeferredInvocationEvent(Function<void(Object&)> invokee)
        : Event(Event::Type::DeferredInvoke)
        , m_invokee(move(invokee))
    {
    }

private:
    Function<void(Object&)> m_invokee;
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

    Object* child() { return m_child.ptr(); }
    const Object* child() const { return m_child.ptr(); }

    Object* insertion_before_child() { return m_insertion_before_child.ptr(); }
    const Object* insertion_before_child() const { return m_insertion_before_child.ptr(); }

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
