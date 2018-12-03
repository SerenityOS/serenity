#pragma once

#include <stack>
#include "Operation.h"
#include "OwnPtr.h"

class UndoStack {
public:
    UndoStack() { }

    void push(OwnPtr<Operation>&&);
    OwnPtr<Operation> pop();

    bool is_empty() const { return m_stack.empty(); }

private:
    std::stack<OwnPtr<Operation>> m_stack;
};
