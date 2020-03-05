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

#include <AK/Forward.h>
#include <AK/IntrusiveList.h>
#include <AK/Noncopyable.h>
#include <AK/NonnullRefPtrVector.h>
#include <AK/String.h>
#include <AK/Weakable.h>
#include <LibCore/Forward.h>

namespace Core {

class RPCClient;

enum class TimerShouldFireWhenNotVisible {
    No = 0,
    Yes
};

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

class Object
    : public RefCounted<Object>
    , public Weakable<Object> {
    // NOTE: No C_OBJECT macro for Core::Object itself.

    AK_MAKE_NONCOPYABLE(Object)
    AK_MAKE_NONMOVABLE(Object)
public:
    IntrusiveListNode m_all_objects_list_node;

    virtual ~Object();

    virtual const char* class_name() const = 0;
    virtual void event(Core::Event&);

    const String& name() const { return m_name; }
    void set_name(const StringView& name) { m_name = name; }

    NonnullRefPtrVector<Object>& children() { return m_children; }
    const NonnullRefPtrVector<Object>& children() const { return m_children; }

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

    bool is_ancestor_of(const Object&) const;

    Object* parent() { return m_parent; }
    const Object* parent() const { return m_parent; }

    void start_timer(int ms, TimerShouldFireWhenNotVisible = TimerShouldFireWhenNotVisible::No);
    void stop_timer();
    bool has_timer() const { return m_timer_id; }

    void add_child(Object&);
    void insert_child_before(Object& new_child, Object& before_child);
    void remove_child(Object&);

    void dump_tree(int indent = 0);

    void deferred_invoke(Function<void(Object&)>);

    bool is_widget() const { return m_widget; }
    virtual bool is_action() const { return false; }
    virtual bool is_window() const { return false; }

    virtual void save_to(AK::JsonObject&);

    static IntrusiveList<Object, &Object::m_all_objects_list_node>& all_objects();

    void dispatch_event(Core::Event&, Object* stay_within = nullptr);

    void remove_from_parent()
    {
        if (m_parent)
            m_parent->remove_child(*this);
    }

    template<class T, class... Args>
    inline T& add(Args&&... args)
    {
        auto child = T::construct(forward<Args>(args)...);
        add_child(*child);
        return child;
    }

    virtual bool is_visible_for_timer_purposes() const;

    bool is_being_inspected() const { return m_inspector_count; }

    void increment_inspector_count(Badge<RPCClient>);
    void decrement_inspector_count(Badge<RPCClient>);

protected:
    explicit Object(Object* parent = nullptr, bool is_widget = false);

    virtual void timer_event(TimerEvent&);
    virtual void custom_event(CustomEvent&);

    // NOTE: You may get child events for children that are not yet fully constructed!
    virtual void child_event(ChildEvent&);

    virtual void did_begin_inspection() {}
    virtual void did_end_inspection() {}

private:
    Object* m_parent { nullptr };
    String m_name;
    int m_timer_id { 0 };
    unsigned m_inspector_count { 0 };
    bool m_widget { false };
    NonnullRefPtrVector<Object> m_children;
};

template<typename T>
inline bool is(const Object&) { return false; }

template<typename T>
inline T& to(Object& object)
{
    ASSERT(is<typename RemoveConst<T>::Type>(object));
    return static_cast<T&>(object);
}

template<typename T>
inline const T& to(const Object& object)
{
    ASSERT(is<typename RemoveConst<T>::Type>(object));
    return static_cast<const T&>(object);
}

template<typename T, typename Callback>
inline void Object::for_each_child_of_type(Callback callback)
{
    for_each_child([&](auto& child) {
        if (is<T>(child))
            return callback(to<T>(child));
        return IterationDecision::Continue;
    });
}

const LogStream& operator<<(const LogStream&, const Object&);

}
