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

namespace FileSystemAccessServer {

class ConnectionFromClient;

}

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

    static Optional<ByteString> get_open_filepath(Window* parent_window, ByteString const& window_title = {}, StringView path = Core::StandardPaths::home_directory(), bool folder = false, ScreenPosition screen_position = Dialog::ScreenPosition::CenterWithinParent, Optional<Vector<FileTypeFilter>> allowed_file_types = {});
    static Optional<ByteString> get_save_filepath(Window* parent_window, ByteString const& title, ByteString const& extension, StringView path = Core::StandardPaths::home_directory(), ScreenPosition screen_position = Dialog::ScreenPosition::CenterWithinParent);

    static ErrorOr<Optional<String>> get_filepath(Badge<FileSystemAccessServer::ConnectionFromClient>, i32 window_server_client_id, i32 parent_window_id, Mode, StringView window_title, StringView file_basename = {}, StringView path = Core::StandardPaths::home_directory(), Optional<Vector<FileTypeFilter>> = {});

    virtual ~FilePicker() override;

    Optional<ByteString> const& selected_file() const { return m_selected_file; }

private:
    void on_file_return();

    void set_path(ByteString const&);

    // ^GUI::ModelClient
    virtual void model_did_update(unsigned) override;

    FilePicker(Window* parent_window, Mode type = Mode::Open, StringView filename = "Untitled"sv, StringView path = Core::StandardPaths::home_directory(), ScreenPosition screen_position = Dialog::ScreenPosition::CenterWithinParent, Optional<Vector<FileTypeFilter>> allowed_file_types = {});

    static String ok_button_name(Mode mode)
    {
        switch (mode) {
        case Mode::Open:
        case Mode::OpenMultiple:
        case Mode::OpenFolder:
            return "Open"_string;
        case Mode::Save:
            return "Save"_string;
        default:
            return "OK"_string;
        }
    }

    struct CommonLocationButton {
        ByteString path;
        size_t tray_item_index { 0 };
    };

    RefPtr<MultiView> m_view;
    NonnullRefPtr<FileSystemModel> m_model;
    Optional<ByteString> m_selected_file;

    Vector<ByteString> m_allowed_file_types_names;
    Optional<Vector<FileTypeFilter>> m_allowed_file_types;

    RefPtr<GUI::Label> m_error_label;

    RefPtr<TextBox> m_filename_textbox;
    RefPtr<TextBox> m_location_textbox;
    Vector<CommonLocationButton> m_common_location_buttons;
    RefPtr<Menu> m_context_menu;
    Mode m_mode { Mode::Open };
};

}
