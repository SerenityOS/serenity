#pragma once

#include "HexEditor.h"
#include <AK/FileSystemPath.h>
#include <AK/Function.h>
#include <LibGUI/GApplication.h>
#include <LibGUI/GTextEditor.h>
#include <LibGUI/GWidget.h>
#include <LibGUI/GWindow.h>

class HexEditor;
class GStatusBar;

class HexEditorWidget final : public GWidget {
    C_OBJECT(HexEditorWidget)
public:
    virtual ~HexEditorWidget() override;
    void open_file(const String& path);
    bool request_close();

private:
    HexEditorWidget();
    void set_path(const FileSystemPath& file);
    void update_title();

    RefPtr<HexEditor> m_editor;
    String m_path;
    String m_name;
    String m_extension;
    RefPtr<GAction> m_new_action;
    RefPtr<GAction> m_open_action;
    RefPtr<GAction> m_save_action;
    RefPtr<GAction> m_save_as_action;
    RefPtr<GAction> m_goto_decimal_offset_action;
    RefPtr<GAction> m_goto_hex_offset_action;

    RefPtr<GStatusBar> m_statusbar;

    bool m_document_dirty { false };
};
