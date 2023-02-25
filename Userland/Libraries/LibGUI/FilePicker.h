/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/LexicalPath.h>
#include <AK/Optional.h>
#include <AK/String.h>
#include <LibCore/StandardPaths.h>
#include <LibGUI/ComboBox.h>
#include <LibGUI/Dialog.h>
#include <LibGUI/FileTypeFilter.h>
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

    static Optional<DeprecatedString> get_open_filepath(Window* parent_window, DeprecatedString const& window_title = {}, StringView path = Core::StandardPaths::home_directory(), bool folder = false, ScreenPosition screen_position = Dialog::ScreenPosition::CenterWithinParent, Optional<Vector<FileTypeFilter>> allowed_file_types = {});
    static Optional<DeprecatedString> get_save_filepath(Window* parent_window, DeprecatedString const& title, DeprecatedString const& extension, StringView path = Core::StandardPaths::home_directory(), ScreenPosition screen_position = Dialog::ScreenPosition::CenterWithinParent);

    virtual ~FilePicker() override;

    DeprecatedString const& selected_file() const { return m_selected_file; }

private:
    void on_file_return();

    void set_path(DeprecatedString const&);

    // ^GUI::ModelClient
    virtual void model_did_update(unsigned) override;

    FilePicker(Window* parent_window, Mode type = Mode::Open, StringView filename = "Untitled"sv, StringView path = Core::StandardPaths::home_directory(), ScreenPosition screen_position = Dialog::ScreenPosition::CenterWithinParent, Optional<Vector<FileTypeFilter>> allowed_file_types = {});

    static String ok_button_name(Mode mode)
    {
        switch (mode) {
        case Mode::Open:
        case Mode::OpenMultiple:
        case Mode::OpenFolder:
            return "Open"_short_string;
        case Mode::Save:
            return "Save"_short_string;
        default:
            return "OK"_short_string;
        }
    }

    struct CommonLocationButton {
        DeprecatedString path;
        size_t tray_item_index { 0 };
    };

    RefPtr<MultiView> m_view;
    NonnullRefPtr<FileSystemModel> m_model;
    DeprecatedString m_selected_file;

    Vector<DeprecatedString> m_allowed_file_types_names;
    Optional<Vector<FileTypeFilter>> m_allowed_file_types;

    RefPtr<GUI::Label> m_error_label;

    RefPtr<TextBox> m_filename_textbox;
    RefPtr<TextBox> m_location_textbox;
    Vector<CommonLocationButton> m_common_location_buttons;
    RefPtr<Menu> m_context_menu;
    Mode m_mode { Mode::Open };
};

}
