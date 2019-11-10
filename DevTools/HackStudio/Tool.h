#pragma once

#include <AK/Noncopyable.h>

class FormEditorWidget;
class GKeyEvent;
class GMouseEvent;

class Tool {
    AK_MAKE_NONCOPYABLE(Tool)
    AK_MAKE_NONMOVABLE(Tool)
public:
    virtual ~Tool() {}

    virtual void on_mousedown(GMouseEvent&) = 0;
    virtual void on_mouseup(GMouseEvent&) = 0;
    virtual void on_mousemove(GMouseEvent&) = 0;
    virtual void on_keydown(GKeyEvent&) = 0;

    virtual const char* class_name() const = 0;

    virtual void attach() {}
    virtual void detach() {}

protected:
    explicit Tool(FormEditorWidget& editor)
        : m_editor(editor)
    {
    }

    FormEditorWidget& m_editor;
};
