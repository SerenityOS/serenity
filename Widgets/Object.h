#pragma once

#include <AK/Vector.h>

class Event;

class Object {
public:
    Object(Object* parent = nullptr);
    virtual ~Object();

    virtual const char* className() const { return "Object"; }

    virtual void event(Event&);

    Vector<Object*>& children() { return m_children; }

private:
    void addChild(Object&);
    void removeChild(Object&);

    Object* m_parent { nullptr };

    Vector<Object*> m_children;
};
