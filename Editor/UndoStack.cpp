#include "cuki.h"
#include "UndoStack.h"

void UndoStack::push(OwnPtr<Operation>&& op)
{
    m_stack.push(std::move(op));
}

OwnPtr<Operation> UndoStack::pop()
{
    OwnPtr<Operation> op = std::move(m_stack.top());
    m_stack.pop();
    return op;
}
