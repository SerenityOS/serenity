#pragma once

#include <AK/AKString.h>
#include <AK/Function.h>
#include <AK/StdLibExtras.h>
#include <AK/Vector.h>
#include <AK/Weakable.h>

class CEvent;
class CChildEvent;
class CCustomEvent;
class CTimerEvent;

#define C_OBJECT(klass) \
public:                 \
    virtual const char* class_name() const override { return #klass; }

class CObject : public Weakable<CObject> {
    // NOTE: No C_OBJECT macro for CObject itself.
public:
    virtual ~CObject();

    virtual const char* class_name() const = 0;
    virtual void event(CEvent&);

    const String& name() const { return m_name; }
    void set_name(const StringView& name) { m_name = name; }

    Vector<CObject*>& children() { return m_children; }
    const Vector<CObject*>& children() const { return m_children; }

    template<typename Callback>
    void for_each_child(Callback callback)
    {
        for (auto* child : m_children) {
            if (callback(*child) == IterationDecision::Break)
                return;
        }
    }

    template<typename T, typename Callback>
    void for_each_child_of_type(Callback callback);

    CObject* parent() { return m_parent; }
    const CObject* parent() const { return m_parent; }

    void start_timer(int ms);
    void stop_timer();
    bool has_timer() const { return m_timer_id; }

    void add_child(CObject&);
    void remove_child(CObject&);

    void delete_later();

    void dump_tree(int indent = 0);

    void deferred_invoke(Function<void(CObject&)>);

    bool is_widget() const { return m_widget; }
    virtual bool is_window() const { return false; }

protected:
    CObject(CObject* parent = nullptr, bool is_widget = false);

    virtual void timer_event(CTimerEvent&);
    virtual void custom_event(CCustomEvent&);

    // NOTE: You may get child events for children that are not yet fully constructed!
    virtual void child_event(CChildEvent&);

private:
    CObject* m_parent { nullptr };
    String m_name;
    int m_timer_id { 0 };
    bool m_widget { false };
    Vector<CObject*> m_children;
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
