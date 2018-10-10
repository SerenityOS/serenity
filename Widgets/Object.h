#pragma once

class Event;

class Object {
public:
    Object(Object* parent = nullptr);
    virtual ~Object();

    virtual void event(Event&);

private:
    Object* m_parent { nullptr };
};
