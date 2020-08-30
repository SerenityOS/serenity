/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
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

#include "PreviewWidget.h"
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/ColorInput.h>
#include <LibGUI/ComboBox.h>
#include <LibGUI/Model.h>
#include <LibGUI/Window.h>

class ColorRoleModel final : public GUI::Model {
public:
    virtual int row_count(const GUI::ModelIndex&) const { return m_color_roles.size(); }
    virtual int column_count(const GUI::ModelIndex&) const { return 1; }
    virtual GUI::Variant data(const GUI::ModelIndex& index, GUI::ModelRole role = GUI::ModelRole::Display) const
    {
        if (role == GUI::ModelRole::Display)
            return Gfx::to_string(m_color_roles[(size_t)index.row()]);
        return {};
    }
    virtual void update() { did_update(); }

    explicit ColorRoleModel(const Vector<Gfx::ColorRole>& color_roles)
        : m_color_roles(color_roles)
    {
    }

    Gfx::ColorRole color_role(const GUI::ModelIndex& index) const
    {
        return m_color_roles[index.row()];
    }

    Gfx::ColorRole color_role(size_t index) const
    {
        return m_color_roles[index];
    }

private:
    const Vector<Gfx::ColorRole>& m_color_roles;
};

int main(int argc, char** argv)
{
    auto app = GUI::Application::construct(argc, argv);

    Gfx::Palette preview_palette = app->palette();

    auto window = GUI::Window::construct();
    auto& main_widget = window->set_main_widget<GUI::Widget>();
    main_widget.set_fill_with_background_color(true);
    main_widget.set_layout<GUI::VerticalBoxLayout>();

    auto& preview_widget = main_widget.add<ThemeEditor::PreviewWidget>(app->palette());
    preview_widget.set_preferred_size(480, 360);
    preview_widget.set_size_policy(GUI::SizePolicy::Fixed, GUI::SizePolicy::Fixed);

    auto& horizontal_container = main_widget.add<GUI::Widget>();
    horizontal_container.set_layout<GUI::HorizontalBoxLayout>();
    horizontal_container.set_preferred_size(480, 20);
    horizontal_container.set_size_policy(GUI::SizePolicy::Fixed, GUI::SizePolicy::Fixed);

    auto& combo_box = horizontal_container.add<GUI::ComboBox>();
    auto& color_input = horizontal_container.add<GUI::ColorInput>();

    Vector<Gfx::ColorRole> color_roles;
#define __ENUMERATE_COLOR_ROLE(role) color_roles.append(Gfx::ColorRole::role);
    ENUMERATE_COLOR_ROLES(__ENUMERATE_COLOR_ROLE)
#undef __ENUMERATE_COLOR_ROLE

    combo_box.set_only_allow_values_from_model(true);
    combo_box.set_model(adopt(*new ColorRoleModel(color_roles)));
    combo_box.on_change = [&](auto&, auto& index) {
        auto role = static_cast<const ColorRoleModel*>(index.model())->color_role(index);
        color_input.set_color(preview_palette.color(role));
    };

    combo_box.set_selected_index((size_t)Gfx::ColorRole::Window - 1);

    color_input.on_change = [&] {
        auto role = static_cast<const ColorRoleModel*>(combo_box.model())->color_role(combo_box.selected_index());
        preview_palette.set_color(role, color_input.color());
        preview_widget.set_preview_palette(preview_palette);
    };

    window->resize(480, 500);
    window->show();
    window->set_title("Theme Editor");
    window->set_icon(Gfx::Bitmap::load_from_file("/res/icons/16x16/app-theme-editor.png"));
    return app->exec();
}
