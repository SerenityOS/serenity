#include "InsertOperation.h"
#include "Editor.h"

InsertOperation::InsertOperation(const std::string& text)
    : m_text(text)
{
}

InsertOperation::InsertOperation(char ch)
    : m_text(&ch, 1)
{
}

InsertOperation::~InsertOperation()
{
}

bool InsertOperation::apply(Editor& editor)
{
    return editor.insert_text_at_cursor(m_text);
}

bool InsertOperation::unapply(Editor& editor)
{
    return editor.remove_text_at_cursor(m_text);
}
