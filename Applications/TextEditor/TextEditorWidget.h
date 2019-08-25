#pragma once

#include <AK/FileSystemPath.h>
#include <AK/Function.h>
#include <LibGUI/GApplication.h>
#include <LibGUI/GTextEditor.h>
#include <LibGUI/GWidget.h>
#include <LibGUI/GWindow.h>

class GButton;
class GTextBox;
class GTextEditor;

class TextEditorWidget final : public GWidget {
public:
    TextEditorWidget();
    virtual ~TextEditorWidget() override;
    void open_sesame(const String& path);

private:
    void set_path(const FileSystemPath& file);

    GTextEditor* m_editor { nullptr };
    String m_path;
    String m_name;
    String m_extension;
    RefPtr<GAction> m_new_action;
    RefPtr<GAction> m_open_action;
    RefPtr<GAction> m_save_action;
    RefPtr<GAction> m_save_as_action;
    RefPtr<GAction> m_find_action;
    RefPtr<GAction> m_line_wrapping_setting_action;

    GTextBox* m_find_textbox { nullptr };
    GButton* m_find_prev_button { nullptr };
    GButton* m_find_next_button { nullptr };
    GWidget* m_find_widget { nullptr };
};
