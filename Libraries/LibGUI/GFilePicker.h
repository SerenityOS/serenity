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

#include <AK/FileSystemPath.h>
#include <AK/Optional.h>
#include <LibCore/CUserInfo.h>
#include <LibGUI/GDialog.h>
#include <LibGUI/GTableView.h>

class GFileSystemModel;
class GLabel;
class GTextBox;

class GFilePicker final : public GDialog {
    C_OBJECT(GFilePicker)
public:
    enum class Mode {
        Open,
        Save
    };

    static Optional<String> get_open_filepath(const String& window_title = {});
    static Optional<String> get_save_filepath(const String& title, const String& extension);
    static bool file_exists(const StringView& path);

    virtual ~GFilePicker() override;

    FileSystemPath selected_file() const { return m_selected_file; }

private:
    void set_preview(const FileSystemPath&);
    void clear_preview();
    void on_file_return();

    GFilePicker(Mode type = Mode::Open, const StringView& file_name = "Untitled", const StringView& path = String(get_current_user_home_path()), Core::Object* parent = nullptr);

    static String ok_button_name(Mode mode)
    {
        switch (mode) {
        case Mode::Open:
            return "Open";
        case Mode::Save:
            return "Save";
        default:
            return "OK";
        }
    }

    RefPtr<GTableView> m_view;
    NonnullRefPtr<GFileSystemModel> m_model;
    FileSystemPath m_selected_file;

    RefPtr<GTextBox> m_filename_textbox;
    RefPtr<GLabel> m_preview_image_label;
    RefPtr<GLabel> m_preview_name_label;
    RefPtr<GLabel> m_preview_geometry_label;
    Mode m_mode { Mode::Open };
};
