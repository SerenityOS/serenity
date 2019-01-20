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

    void startTimer(int ms);
    void stopTimer();
    bool hasTimer() const { return m_timerID; }

    void addChild(GObject&);
    void removeChild(GObject&);

    void deleteLater();

private:
    virtual void timerEvent(GTimerEvent&);

    GObject* m_parent { nullptr };

    int m_timerID { 0 };

    Vector<GObject*> m_children;
};
