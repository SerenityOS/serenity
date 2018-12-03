#pragma once

class Editor;

class Operation {
public:
    virtual ~Operation();

    virtual bool apply(Editor&) = 0;
    virtual bool unapply(Editor&) = 0;

protected:
    Operation();
};
