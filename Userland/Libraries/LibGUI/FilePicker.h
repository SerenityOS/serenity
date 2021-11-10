/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/LexicalPath.h>
#include <AK/Optional.h>
#include <LibCore/StandardPaths.h>
#include <LibGUI/Dialog.h>
#include <LibGUI/ImageWidget.h>
#include <LibGUI/Model.h>

namespace GUI {

class FilePicker final
    : public Dialog
    , private ModelClient {
    C_OBJECT(FilePicker);

public:
    enum class Mode {
        Open,
        OpenMultiple,
        OpenFolder,
        Save
    };

    static Optional<String> get_open_filepath(Window* parent_window, const String& window_title = {}, StringView path = Core::StandardPaths::home_directory(), bool folder = false, ScreenPosition screen_position = Dialog::ScreenPosition::CenterWithinParent);
    static Optional<String> get_save_filepath(Window* parent_window, const String& title, const String& extension, StringView path = Core::StandardPaths::home_directory(), ScreenPosition screen_position = Dialog::ScreenPosition::CenterWithinParent);

    virtual ~FilePicker() override;

    String const& selected_file() const { return m_selected_file; }

private:
    void on_file_return();

    void set_path(const String&);

    // ^GUI::ModelClient
    virtual void model_did_update(unsigned) override;

    FilePicker(Window* parent_window, Mode type = Mode::Open, StringView filename = "Untitled", StringView path = Core::StandardPaths::home_directory(), ScreenPosition screen_position = Dialog::ScreenPosition::CenterWithinParent);

    static String ok_button_name(Mode mode)
    {
        switch (mode) {
        case Mode::Open:
        case Mode::OpenMultiple:
        case Mode::OpenFolder:
            return "Open";
        case Mode::Save:
            return "Save";
        default:
            return "OK";
        }
    }

    struct CommonLocationButton {
        String path;
        size_t tray_item_index { 0 };
    };

    RefPtr<MultiView> m_view;
    NonnullRefPtr<FileSystemModel> m_model;
    String m_selected_file;

    RefPtr<GUI::Label> m_error_label;

    RefPtr<TextBox> m_filename_textbox;
    RefPtr<TextBox> m_location_textbox;
    Vector<CommonLocationButton> m_common_location_buttons;
    RefPtr<Menu> m_context_menu;
    Mode m_mode { Mode::Open };
};

}
