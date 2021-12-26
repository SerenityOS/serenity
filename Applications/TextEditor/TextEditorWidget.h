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
class GStatusBar;

class TextEditorWidget final : public GWidget {
    C_OBJECT(TextEditorWidget)
public:
    virtual ~TextEditorWidget() override;
    void open_sesame(const String& path);
    bool request_close();

private:
    TextEditorWidget();
    void set_path(const FileSystemPath& file);
    void update_title();

    ObjectPtr<GTextEditor> m_editor;
    String m_path;
    String m_name;
    String m_extension;
    RefPtr<GAction> m_new_action;
    RefPtr<GAction> m_open_action;
    RefPtr<GAction> m_save_action;
    RefPtr<GAction> m_save_as_action;
    RefPtr<GAction> m_find_action;
    RefPtr<GAction> m_line_wrapping_setting_action;
    RefPtr<GAction> m_find_next_action;
    RefPtr<GAction> m_find_previous_action;

    ObjectPtr<GStatusBar> m_statusbar;

    ObjectPtr<GTextBox> m_find_textbox;
    GButton* m_find_previous_button { nullptr };
    GButton* m_find_next_button { nullptr };
    ObjectPtr<GWidget> m_find_widget;

    bool m_document_dirty { false };
};
