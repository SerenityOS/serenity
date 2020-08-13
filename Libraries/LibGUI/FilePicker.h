/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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
        Save
    };

    enum class Options : unsigned {
        None = 0,
        DisablePreview = (1 << 0)
    };

    static Optional<String> get_open_filepath(Window* parent_window, Options options)
    {
        return get_open_filepath(parent_window, {}, options);
    }
    static Optional<String> get_open_filepath(Window* parent_window, const String& window_title = {}, Options options = Options::None);
    static Optional<String> get_save_filepath(Window* parent_window, const String& title, const String& extension, Options options = Options::None);
    static bool file_exists(const StringView& path);

    virtual ~FilePicker() override;

    LexicalPath selected_file() const { return m_selected_file; }

private:
    bool have_preview() const { return m_preview_container; }
    void set_preview(const LexicalPath&);
    void clear_preview();
    void on_file_return();

    void set_path(const String&);

    // ^GUI::ModelClient
    virtual void model_did_update(unsigned) override;

    FilePicker(Window* parent_window, Mode type = Mode::Open, Options = Options::None, const StringView& file_name = "Untitled", const StringView& path = Core::StandardPaths::home_directory());

    static String ok_button_name(Mode mode)
    {
        switch (mode) {
        case Mode::Open:
        case Mode::OpenMultiple:
            return "Open";
        case Mode::Save:
            return "Save";
        default:
            return "OK";
        }
    }

    RefPtr<MultiView> m_view;
    NonnullRefPtr<FileSystemModel> m_model;
    LexicalPath m_selected_file;

    RefPtr<TextBox> m_filename_textbox;
    RefPtr<TextBox> m_location_textbox;
    RefPtr<Frame> m_preview_container;
    RefPtr<ImageWidget> m_preview_image;
    RefPtr<Label> m_preview_name_label;
    RefPtr<Label> m_preview_geometry_label;
    Mode m_mode { Mode::Open };
};

}
