#pragma once

#include <AK/Function.h>
#include <AK/Vector.h>
#include <AK/Weakable.h>

class CEvent;
class CChildEvent;
class CTimerEvent;

class CObject : public Weakable<CObject> {
public:
    CObject(CObject* parent = nullptr, bool is_widget = false);
    virtual ~CObject();

    virtual const char* class_name() const { return "CObject"; }

    virtual void event(CEvent&);

    Vector<CObject*>& children() { return m_children; }
    const Vector<CObject*>& children() const { return m_children; }

    template<typename Callback>
    void for_each_child(Callback callback)
    {
        for (auto* child : m_children) {
            if (callback(*child) == IterationDecision::Abort)
                return;
        }
    }

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
    virtual void timer_event(CTimerEvent&);
    virtual void child_event(CChildEvent&);

private:
    CObject* m_parent { nullptr };
    int m_timer_id { 0 };
    bool m_widget { false };
    Vector<CObject*> m_children;
};

template<typename T> inline bool is(const CObject&) { return false; }
template<> inline bool is<CObject>(const CObject&) { return true; }

template<typename T>
inline T& to(CObject& object)
{
    ASSERT(is<T>(object));
    return static_cast<T&>(object);
}

template<typename T>
inline const T& to(const CObject& object)
{
    ASSERT(is<T>(object));
    return static_cast<const T&>(object);
}
