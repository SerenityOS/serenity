#pragma once

#include <LibGUI/GWidget.h>

class GLabel;
class Editor;

class EditorWrapper : public GWidget {
    C_OBJECT(EditorWrapper)
public:
    virtual ~EditorWrapper() override;

    Editor& editor() { return *m_editor; }
    const Editor& editor() const { return *m_editor; }

    GLabel& filename_label() { return *m_filename_label; }

    void set_editor_has_focus(Badge<Editor>, bool);

private:
    explicit EditorWrapper(GWidget* parent = nullptr);

    RefPtr<GLabel> m_filename_label;
    RefPtr<GLabel> m_cursor_label;
    RefPtr<Editor> m_editor;
};

template<>
inline bool is<EditorWrapper>(const CObject& object)
{
    return !strcmp(object.class_name(), "EditorWrapper");
}
