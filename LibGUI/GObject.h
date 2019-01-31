#pragma once

#include <AK/Vector.h>
#include <AK/Weakable.h>

class GEvent;
class GTimerEvent;

class GObject : public Weakable<GObject> {
public:
    GObject(GObject* parent = nullptr);
    virtual ~GObject();

    virtual const char* class_name() const { return "GObject"; }

    virtual void event(GEvent&);

    Vector<GObject*>& children() { return m_children; }

    GObject* parent() { return m_parent; }
    const GObject* parent() const { return m_parent; }

    void start_timer(int ms);
    void stop_timer();
    bool has_timer() const { return m_timer_id; }

    void add_child(GObject&);
    void remove_child(GObject&);

    void delete_later();

private:
    virtual void timer_event(GTimerEvent&);

    GObject* m_parent { nullptr };

    int m_timer_id { 0 };

    Vector<GObject*> m_children;
};
