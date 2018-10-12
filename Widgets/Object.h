#pragma once

#include <AK/Vector.h>

class Event;
class TimerEvent;

class Object {
public:
    Object(Object* parent = nullptr);
    virtual ~Object();

    virtual const char* className() const { return "Object"; }

    virtual void event(Event&);

    Vector<Object*>& children() { return m_children; }

    Object* parent() { return m_parent; }
    const Object* parent() const { return m_parent; }

    void startTimer(int ms);
    void stopTimer();
    bool hasTimer() const { return m_timerID; }

private:
    virtual void onTimer(TimerEvent&);

    void addChild(Object&);
    void removeChild(Object&);

    Object* m_parent { nullptr };

    int m_timerID { 0 };

    Vector<Object*> m_children;
};
