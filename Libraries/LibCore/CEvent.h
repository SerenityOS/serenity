#pragma once

#include <AK/AKString.h>
#include <AK/Function.h>
#include <AK/Types.h>
#include <AK/WeakPtr.h>

class CObject;

class CEvent {
public:
    enum Type {
        Invalid = 0,
        Quit,
        Timer,
        NotifierRead,
        NotifierWrite,
        DeferredDestroy,
        DeferredInvoke,
        ChildAdded,
        ChildRemoved,
        Custom,
    };

    CEvent() {}
    explicit CEvent(unsigned type)
        : m_type(type)
    {
    }
    virtual ~CEvent() {}

    unsigned type() const { return m_type; }

private:
    unsigned m_type { Type::Invalid };
};

class CDeferredInvocationEvent : public CEvent {
    friend class CEventLoop;

public:
    CDeferredInvocationEvent(Function<void(CObject&)> invokee)
        : CEvent(CEvent::Type::DeferredInvoke)
        , m_invokee(move(invokee))
    {
    }

private:
    Function<void(CObject&)> m_invokee;
};

class CTimerEvent final : public CEvent {
public:
    explicit CTimerEvent(int timer_id)
        : CEvent(CEvent::Timer)
        , m_timer_id(timer_id)
    {
    }
    ~CTimerEvent() {}

    int timer_id() const { return m_timer_id; }

private:
    int m_timer_id;
};

class CNotifierReadEvent final : public CEvent {
public:
    explicit CNotifierReadEvent(int fd)
        : CEvent(CEvent::NotifierRead)
        , m_fd(fd)
    {
    }
    ~CNotifierReadEvent() {}

    int fd() const { return m_fd; }

private:
    int m_fd;
};

class CNotifierWriteEvent final : public CEvent {
public:
    explicit CNotifierWriteEvent(int fd)
        : CEvent(CEvent::NotifierWrite)
        , m_fd(fd)
    {
    }
    ~CNotifierWriteEvent() {}

    int fd() const { return m_fd; }

private:
    int m_fd;
};

class CChildEvent final : public CEvent {
public:
    CChildEvent(Type, CObject& child);
    ~CChildEvent();

    CObject* child() { return m_child.ptr(); }
    const CObject* child() const { return m_child.ptr(); }

private:
    WeakPtr<CObject> m_child;
};

class CCustomEvent : public CEvent {
public:
    CCustomEvent(int custom_type, void* data = nullptr)
        : CEvent(CEvent::Type::Custom)
        , m_custom_type(custom_type)
        , m_data(data)
    {
    }
    ~CCustomEvent() {}

    int custom_type() const { return m_custom_type; }
    void* data() { return m_data; }
    const void* data() const { return m_data; }

private:
    int m_custom_type { 0 };
    void* m_data { nullptr };
};
