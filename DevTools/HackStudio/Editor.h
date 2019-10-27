#pragma once

#include <LibGUI/GTextEditor.h>

class Editor final : public GTextEditor {
    C_OBJECT(Editor)
public:
    virtual ~Editor() override {}

    Function<void()> on_focus;

private:
    virtual void focusin_event(CEvent& event) override;

    Editor(GWidget* parent)
        : GTextEditor(GTextEditor::MultiLine, parent)
    {
    }
};
