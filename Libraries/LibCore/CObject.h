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

#include <AK/Badge.h>
#include <AK/Function.h>
#include <AK/IntrusiveList.h>
#include <AK/Noncopyable.h>
#include <AK/NonnullRefPtrVector.h>
#include <AK/StdLibExtras.h>
#include <AK/String.h>
#include <AK/Vector.h>
#include <AK/Weakable.h>

namespace AK {
class JsonObject;
}

enum class TimerShouldFireWhenNotVisible {
    No = 0,
    Yes
};

class CEvent;
class CEventLoop;
class CChildEvent;
class CCustomEvent;
class CTimerEvent;

#define C_OBJECT(klass)                                                \
public:                                                                \
    virtual const char* class_name() const override { return #klass; } \
    template<class... Args>                                            \
    static inline NonnullRefPtr<klass> construct(Args&&... args)       \
    {                                                                  \
        return adopt(*new klass(forward<Args>(args)...));              \
    }

#define C_OBJECT_ABSTRACT(klass) \
public:                          \
    virtual const char* class_name() const override { return #klass; }

class CObject
    : public RefCounted<CObject>
    , public Weakable<CObject> {
    // NOTE: No C_OBJECT macro for CObject itself.

    AK_MAKE_NONCOPYABLE(CObject)
    AK_MAKE_NONMOVABLE(CObject)
public:
    IntrusiveListNode m_all_objects_list_node;

    virtual ~CObject();

    virtual const char* class_name() const = 0;
    virtual void event(CEvent&);

    const String& name() const { return m_name; }
    void set_name(const StringView& name) { m_name = name; }

    NonnullRefPtrVector<CObject>& children() { return m_children; }
    const NonnullRefPtrVector<CObject>& children() const { return m_children; }

    template<typename Callback>
    void for_each_child(Callback callback)
    {
        for (auto& child : m_children) {
            if (callback(child) == IterationDecision::Break)
                return;
        }
    }

    template<typename T, typename Callback>
    void for_each_child_of_type(Callback callback);

    bool is_ancestor_of(const CObject&) const;

    CObject* parent() { return m_parent; }
    const CObject* parent() const { return m_parent; }

    void start_timer(int ms, TimerShouldFireWhenNotVisible = TimerShouldFireWhenNotVisible::No);
    void stop_timer();
    bool has_timer() const { return m_timer_id; }

    void add_child(CObject&);
    void insert_child_before(CObject& new_child, CObject& before_child);
    void remove_child(CObject&);

    void dump_tree(int indent = 0);

    void deferred_invoke(Function<void(CObject&)>);

    bool is_widget() const { return m_widget; }
    virtual bool is_window() const { return false; }

    virtual void save_to(AK::JsonObject&);

    static IntrusiveList<CObject, &CObject::m_all_objects_list_node>& all_objects();

    void dispatch_event(CEvent&, CObject* stay_within = nullptr);

    void remove_from_parent()
    {
        if (m_parent)
            m_parent->remove_child(*this);
    }

    virtual bool is_visible_for_timer_purposes() const;

protected:
    explicit CObject(CObject* parent = nullptr, bool is_widget = false);

    virtual void timer_event(CTimerEvent&);
    virtual void custom_event(CCustomEvent&);

    // NOTE: You may get child events for children that are not yet fully constructed!
    virtual void child_event(CChildEvent&);

private:
    CObject* m_parent { nullptr };
    String m_name;
    int m_timer_id { 0 };
    bool m_widget { false };
    NonnullRefPtrVector<CObject> m_children;
};

template<typename T>
inline bool is(const CObject&) { return false; }

template<>
inline bool is<CObject>(const CObject&) { return true; }

template<typename T>
inline T& to(CObject& object)
{
    ASSERT(is<typename RemoveConst<T>::Type>(object));
    return static_cast<T&>(object);
}

template<typename T>
inline const T& to(const CObject& object)
{
    ASSERT(is<typename RemoveConst<T>::Type>(object));
    return static_cast<const T&>(object);
}

template<typename T, typename Callback>
inline void CObject::for_each_child_of_type(Callback callback)
{
    for_each_child([&](auto& child) {
        if (is<T>(child))
            return callback(to<T>(child));
        return IterationDecision::Continue;
    });
}

inline const LogStream& operator<<(const LogStream& stream, const CObject& object)
{
    return stream << object.class_name() << '{' << &object << '}';
}
