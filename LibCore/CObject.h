#pragma once

#include <AK/Function.h>
#include <AK/Vector.h>
#include <AK/Weakable.h>

class CEvent;
class CChildEvent;
class CTimerEvent;

class CObject : public Weakable<CObject> {
public:
    CObject(CObject* parent = nullptr);
    virtual ~CObject();

    virtual const char* class_name() const { return "CObject"; }

    virtual void event(CEvent&);

    Vector<CObject*>& children() { return m_children; }

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

    virtual bool is_widget() const { return false; }
    virtual bool is_window() const { return false; }

protected:
    virtual void timer_event(CTimerEvent&);
    virtual void child_event(CChildEvent&);

private:
    CObject* m_parent { nullptr };
    int m_timer_id { 0 };
    Vector<CObject*> m_children;
};
