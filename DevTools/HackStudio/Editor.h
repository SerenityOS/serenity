#pragma once

#include <LibGUI/GTextEditor.h>

class EditorWrapper;

class Editor final : public GTextEditor {
    C_OBJECT(Editor)
public:
    virtual ~Editor() override {}

    Function<void()> on_focus;

    EditorWrapper& wrapper();
    const EditorWrapper& wrapper() const;

private:
    virtual void focusin_event(CEvent&) override;
    virtual void focusout_event(CEvent&) override;
    virtual void paint_event(GPaintEvent&) override;

    Editor(GWidget* parent)
        : GTextEditor(GTextEditor::MultiLine, parent)
    {
    }
};
