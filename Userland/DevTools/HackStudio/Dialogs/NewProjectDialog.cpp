/*
 * Copyright (c) 2021, Nick Vella <nick@nxk.io>
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

#include "NewProjectDialog.h"
#include "ProjectTemplatesModel.h"
#include <DevTools/HackStudio/Dialogs/NewProjectDialogGML.h>
#include <DevTools/HackStudio/ProjectTemplate.h>

#include <AK/LexicalPath.h>
#include <AK/String.h>
#include <LibCore/File.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/FilePicker.h>
#include <LibGUI/IconView.h>
#include <LibGUI/Label.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/RadioButton.h>
#include <LibGUI/TextBox.h>
#include <LibGUI/Widget.h>
#include <LibGfx/Font.h>
#include <LibGfx/FontDatabase.h>
#include <LibRegex/Regex.h>

namespace HackStudio {

static const Regex<PosixExtended> s_project_name_validity_regex("^([A-Za-z0-9_-])*$");

int NewProjectDialog::show(GUI::Window* parent_window)
{
    auto dialog = NewProjectDialog::construct(parent_window);

    if (parent_window)
        dialog->set_icon(parent_window->icon());

    auto result = dialog->exec();

    return result;
}

NewProjectDialog::NewProjectDialog(GUI::Window* parent)
    : Dialog(parent)
    , m_model(ProjectTemplatesModel::create())
{
    resize(500, 385);
    center_on_screen();
    set_resizable(false);
    set_modal(true);
    set_title("New project");

    auto& main_widget = set_main_widget<GUI::Widget>();
    main_widget.load_from_gml(new_project_dialog_gml);

    m_icon_view_container = *main_widget.find_descendant_of_type_named<GUI::Widget>("icon_view_container");
    m_icon_view = m_icon_view_container->add<GUI::IconView>();
    m_icon_view->set_always_wrap_item_labels(true);
    m_icon_view->set_model(m_model);
    m_icon_view->set_model_column(ProjectTemplatesModel::Column::Name);
    m_icon_view->on_selection_change = [&]() {
        update_dialog();
    };
    m_icon_view->on_activation = [&]() {
        if (m_input_valid)
            do_create_project();
    };

    m_description_label = *main_widget.find_descendant_of_type_named<GUI::Label>("description_label");
    m_name_input = *main_widget.find_descendant_of_type_named<GUI::TextBox>("name_input");
    m_name_input->on_change = [&]() {
        update_dialog();
    };
    m_name_input->on_return_pressed = [&]() {
        if (m_input_valid)
            do_create_project();
    };
    m_create_in_input = *main_widget.find_descendant_of_type_named<GUI::TextBox>("create_in_input");
    m_create_in_input->on_change = [&]() {
        update_dialog();
    };
    m_create_in_input->on_return_pressed = [&]() {
        if (m_input_valid)
            do_create_project();
    };
    m_full_path_label = *main_widget.find_descendant_of_type_named<GUI::Label>("full_path_label");

    m_ok_button = *main_widget.find_descendant_of_type_named<GUI::Button>("ok_button");
    m_ok_button->on_click = [this](auto) {
        do_create_project();
    };

    m_cancel_button = *main_widget.find_descendant_of_type_named<GUI::Button>("cancel_button");
    m_cancel_button->on_click = [this](auto) {
        done(ExecResult::ExecCancel);
    };

    m_browse_button = *find_descendant_of_type_named<GUI::Button>("browse_button");
    m_browse_button->on_click = [this](auto) {
        Optional<String> path = GUI::FilePicker::get_open_filepath(this);
        if (path.has_value())
            m_create_in_input->set_text(path.value().view());
    };
}

NewProjectDialog::~NewProjectDialog()
{
}

RefPtr<ProjectTemplate> NewProjectDialog::selected_template()
{
    if (m_icon_view->selection().is_empty()) {
        return {};
    }

    auto project_template = m_model->template_for_index(m_icon_view->selection().first());
    ASSERT(!project_template.is_null());

    return project_template;
}

void NewProjectDialog::update_dialog()
{
    auto project_template = selected_template();
    m_input_valid = true;

    if (project_template) {
        m_description_label->set_text(project_template->description());
    } else {
        m_description_label->set_text("Select a project template to continue.");
        m_input_valid = false;
    }

    auto maybe_project_path = get_project_full_path();

    if (maybe_project_path.has_value()) {
        m_full_path_label->set_text(maybe_project_path.value());
    } else {
        m_full_path_label->set_text("Invalid name or creation directory.");
        m_input_valid = false;
    }

    m_ok_button->set_enabled(m_input_valid);
}

Optional<String> NewProjectDialog::get_available_project_name()
{
    auto create_in = m_create_in_input->text();
    auto chosen_name = m_name_input->text();

    // Ensure project name isn't empty or entirely whitespace
    if (chosen_name.is_empty() || chosen_name.is_whitespace())
        return {};

    // Validate project name with validity regex
    if (!s_project_name_validity_regex.has_match(chosen_name))
        return {};

    if (!Core::File::exists(create_in) || !Core::File::is_directory(create_in))
        return {};

    // Check for up-to 999 variations of the project name, in case it's already taken
    for (int i = 0; i < 1000; i++) {
        auto candidate = (i == 0)
            ? chosen_name
            : String::formatted("{}-{}", chosen_name, i);

        if (!Core::File::exists(String::formatted("{}/{}", create_in, candidate)))
            return candidate;
    }

    return {};
}

Optional<String> NewProjectDialog::get_project_full_path()
{
    // Do not permit forward-slashes in project names
    if (m_name_input->text().contains("/"))
        return {};

    auto create_in = m_create_in_input->text();
    auto maybe_project_name = get_available_project_name();

    if (!maybe_project_name.has_value()) {
        return {};
    }

    auto project_name = maybe_project_name.value();
    auto full_path = LexicalPath(String::formatted("{}/{}", create_in, project_name));

    // Do not permit otherwise invalid paths.
    if (!full_path.is_valid())
        return {};

    return full_path.string();
}

void NewProjectDialog::do_create_project()
{
    auto project_template = selected_template();
    if (!project_template) {
        GUI::MessageBox::show_error(this, "Could not create project: no template selected.");
        return;
    }

    auto maybe_project_name = get_available_project_name();
    auto maybe_project_full_path = get_project_full_path();
    if (!maybe_project_name.has_value() || !maybe_project_full_path.has_value()) {
        GUI::MessageBox::show_error(this, "Could not create project: invalid project name or path.");
        return;
    }

    auto creation_result = project_template->create_project(maybe_project_name.value(), maybe_project_full_path.value());
    if (!creation_result.is_error()) {
        // Succesfully created, attempt to open the new project
        m_created_project_path = maybe_project_full_path.value();
        done(ExecResult::ExecOK);
    } else {
        GUI::MessageBox::show_error(this, String::formatted("Could not create project: {}", creation_result.error()));
    }
}

}
