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
#include <AK/Function.h>
#include <LibDraw/PNGLoader.h>
#include <LibGUI/GAction.h>
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GButton.h>
#include <LibGUI/GFilePicker.h>
#include <LibGUI/GFileSystemModel.h>
#include <LibGUI/GInputBox.h>
#include <LibGUI/GLabel.h>
#include <LibGUI/GMessageBox.h>
#include <LibGUI/GSortingProxyModel.h>
#include <LibGUI/GTextBox.h>
#include <LibGUI/GToolBar.h>

Optional<String> GFilePicker::get_open_filepath(const String& window_title)
{
    auto picker = GFilePicker::construct(Mode::Open);

    if (!window_title.is_null())
        picker->set_title(window_title);

    if (picker->exec() == GDialog::ExecOK) {
        String file_path = picker->selected_file().string();

        if (file_path.is_null())
            return {};

        return file_path;
    }
    return {};
}

Optional<String> GFilePicker::get_save_filepath(const String& title, const String& extension)
{
    auto picker = GFilePicker::construct(Mode::Save, String::format("%s.%s", title.characters(), extension.characters()));

    if (picker->exec() == GDialog::ExecOK) {
        String file_path = picker->selected_file().string();

        if (file_path.is_null())
            return {};

        return file_path;
    }
    return {};
}

GFilePicker::GFilePicker(Mode mode, const StringView& file_name, const StringView& path, CObject* parent)
    : GDialog(parent)
    , m_model(GFileSystemModel::create())
    , m_mode(mode)
{
    set_title(m_mode == Mode::Open ? "Open File" : "Save File");
    set_rect(200, 200, 700, 400);
    auto horizontal_container = GWidget::construct();
    set_main_widget(horizontal_container);
    horizontal_container->set_layout(make<GHBoxLayout>());
    horizontal_container->layout()->set_margins({ 4, 4, 4, 4 });
    horizontal_container->set_fill_with_background_color(true);

    auto vertical_container = GWidget::construct(horizontal_container.ptr());
    vertical_container->set_layout(make<GVBoxLayout>());
    vertical_container->layout()->set_spacing(4);

    auto upper_container = GWidget::construct(vertical_container.ptr());
    upper_container->set_layout(make<GHBoxLayout>());
    upper_container->layout()->set_spacing(4);
    upper_container->set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
    upper_container->set_preferred_size(0, 26);

    auto toolbar = GToolBar::construct(upper_container);
    toolbar->set_size_policy(SizePolicy::Fixed, SizePolicy::Fill);
    toolbar->set_preferred_size(85, 0);
    toolbar->set_has_frame(false);

    auto location_textbox = GTextBox::construct(upper_container);
    location_textbox->set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
    location_textbox->set_preferred_size(0, 20);

    m_view = GTableView::construct(vertical_container);
    m_view->set_model(GSortingProxyModel::create(*m_model));
    m_view->set_column_hidden(GFileSystemModel::Column::Owner, true);
    m_view->set_column_hidden(GFileSystemModel::Column::Group, true);
    m_view->set_column_hidden(GFileSystemModel::Column::Permissions, true);
    m_view->set_column_hidden(GFileSystemModel::Column::Inode, true);
    m_view->set_column_hidden(GFileSystemModel::Column::SymlinkTarget, true);
    m_model->set_root_path(path);

    location_textbox->on_return_pressed = [&] {
        m_model->set_root_path(location_textbox->text());
        clear_preview();
    };

    auto open_parent_directory_action = GAction::create("Open parent directory", { Mod_Alt, Key_Up }, GraphicsBitmap::load_from_file("/res/icons/16x16/open-parent-directory.png"), [this](const GAction&) {
        m_model->set_root_path(String::format("%s/..", m_model->root_path().characters()));
        clear_preview();
    });
    toolbar->add_action(*open_parent_directory_action);

    auto go_home_action = GCommonActions::make_go_home_action([this](auto&) {
        m_model->set_root_path(get_current_user_home_path());
    });
    toolbar->add_action(go_home_action);
    toolbar->add_separator();

    auto mkdir_action = GAction::create("New directory...", GraphicsBitmap::load_from_file("/res/icons/16x16/mkdir.png"), [this](const GAction&) {
        auto input_box = GInputBox::construct("Enter name:", "New directory", this);
        if (input_box->exec() == GInputBox::ExecOK && !input_box->text_value().is_empty()) {
            auto new_dir_path = FileSystemPath(String::format("%s/%s",
                                                   m_model->root_path().characters(),
                                                   input_box->text_value().characters()))
                                    .string();
            int rc = mkdir(new_dir_path.characters(), 0777);
            if (rc < 0) {
                GMessageBox::show(String::format("mkdir(\"%s\") failed: %s", new_dir_path.characters(), strerror(errno)), "Error", GMessageBox::Type::Error, GMessageBox::InputType::OK, this);
            } else {
                m_model->update();
            }
        }
    });
    toolbar->add_action(*mkdir_action);

    auto lower_container = GWidget::construct(vertical_container.ptr());
    lower_container->set_layout(make<GVBoxLayout>());
    lower_container->layout()->set_spacing(4);
    lower_container->set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
    lower_container->set_preferred_size(0, 60);

    auto filename_container = GWidget::construct(lower_container.ptr());
    filename_container->set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
    filename_container->set_preferred_size(0, 20);
    filename_container->set_layout(make<GHBoxLayout>());
    auto filename_label = GLabel::construct("File name:", filename_container);
    filename_label->set_text_alignment(TextAlignment::CenterLeft);
    filename_label->set_size_policy(SizePolicy::Fixed, SizePolicy::Fill);
    filename_label->set_preferred_size(60, 0);
    m_filename_textbox = GTextBox::construct(filename_container.ptr());
    if (m_mode == Mode::Save) {
        m_filename_textbox->set_text(file_name);
        m_filename_textbox->set_focus(true);
        m_filename_textbox->select_all();
    }
    m_filename_textbox->on_return_pressed = [&] {
        on_file_return();
    };

    m_view->on_selection = [this](auto& index) {
        auto& filter_model = (GSortingProxyModel&)*m_view->model();
        auto local_index = filter_model.map_to_target(index);
        const GFileSystemModel::Node& node = m_model->node(local_index);
        FileSystemPath path { node.full_path(m_model) };

        clear_preview();

        if (!node.is_directory())
            m_filename_textbox->set_text(node.name);
        set_preview(path);
    };

    auto button_container = GWidget::construct(lower_container.ptr());
    button_container->set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
    button_container->set_preferred_size(0, 20);
    button_container->set_layout(make<GHBoxLayout>());
    button_container->layout()->set_spacing(4);
    button_container->layout()->add_spacer();

    auto cancel_button = GButton::construct(button_container);
    cancel_button->set_size_policy(SizePolicy::Fixed, SizePolicy::Fill);
    cancel_button->set_preferred_size(80, 0);
    cancel_button->set_text("Cancel");
    cancel_button->on_click = [this](auto&) {
        done(ExecCancel);
    };

    auto ok_button = GButton::construct(button_container);
    ok_button->set_size_policy(SizePolicy::Fixed, SizePolicy::Fill);
    ok_button->set_preferred_size(80, 0);
    ok_button->set_text(ok_button_name(m_mode));
    ok_button->on_click = [this](auto&) {
        on_file_return();
    };

    m_view->on_activation = [this](auto& index) {
        auto& filter_model = (GSortingProxyModel&)*m_view->model();
        auto local_index = filter_model.map_to_target(index);
        const GFileSystemModel::Node& node = m_model->node(local_index);
        auto path = node.full_path(m_model);

        if (node.is_directory()) {
            m_model->set_root_path(path);
            // NOTE: 'node' is invalid from here on
        } else {
            on_file_return();
        }
    };

    auto preview_container = GFrame::construct(horizontal_container);
    preview_container->set_size_policy(SizePolicy::Fixed, SizePolicy::Fill);
    preview_container->set_preferred_size(180, 0);
    preview_container->set_frame_shape(FrameShape::Container);
    preview_container->set_frame_shadow(FrameShadow::Sunken);
    preview_container->set_frame_thickness(2);
    preview_container->set_layout(make<GVBoxLayout>());
    preview_container->layout()->set_margins({ 8, 8, 8, 8 });

    m_preview_image_label = GLabel::construct(preview_container);
    m_preview_image_label->set_should_stretch_icon(true);
    m_preview_image_label->set_size_policy(SizePolicy::Fixed, SizePolicy::Fixed);
    m_preview_image_label->set_preferred_size(160, 160);

    m_preview_name_label = GLabel::construct(preview_container);
    m_preview_name_label->set_font(Font::default_bold_font());
    m_preview_name_label->set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
    m_preview_name_label->set_preferred_size(0, m_preview_name_label->font().glyph_height());

    m_preview_geometry_label = GLabel::construct(preview_container);
    m_preview_geometry_label->set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
    m_preview_geometry_label->set_preferred_size(0, m_preview_name_label->font().glyph_height());
}

GFilePicker::~GFilePicker()
{
}

void GFilePicker::set_preview(const FileSystemPath& path)
{
    if (path.has_extension(".png")) {
        auto bitmap = load_png(path.string());
        if (!bitmap) {
            clear_preview();
            return;
        }
        bool should_stretch = bitmap->width() > m_preview_image_label->width() || bitmap->height() > m_preview_image_label->height();
        m_preview_name_label->set_text(path.basename());
        m_preview_geometry_label->set_text(bitmap->size().to_string());
        m_preview_image_label->set_should_stretch_icon(should_stretch);
        m_preview_image_label->set_icon(move(bitmap));
    }
}

void GFilePicker::clear_preview()
{
    m_preview_image_label->set_icon(nullptr);
    m_preview_name_label->set_text(String::empty());
    m_preview_geometry_label->set_text(String::empty());
}

void GFilePicker::on_file_return()
{
    FileSystemPath path(String::format("%s/%s", m_model->root_path().characters(), m_filename_textbox->text().characters()));

    if (GFilePicker::file_exists(path.string()) && m_mode == Mode::Save) {
        auto result = GMessageBox::show("File already exists, overwrite?", "Existing File", GMessageBox::Type::Warning, GMessageBox::InputType::OKCancel);
        if (result == GMessageBox::ExecCancel)
            return;
    }

    m_selected_file = path;
    done(ExecOK);
}

bool GFilePicker::file_exists(const StringView& path)
{
    struct stat st;
    int rc = stat(String(path).characters(), &st);
    if (rc < 0) {
        if (errno == ENOENT)
            return false;
    }
    if (rc == 0) {
        return true;
    }
    return false;
}
