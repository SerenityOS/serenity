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

    Object* parent() { return m_parent; }
    const Object* parent() const { return m_parent; }

private:
    void addChild(Object&);
    void removeChild(Object&);

    Object* m_parent { nullptr };

    Vector<Object*> m_children;
};
