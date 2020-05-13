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

#include "CreateNewLayerDialog.h"
#include "Image.h"
#include "ImageEditor.h"
#include "Layer.h"
#include "PaletteWidget.h"
#include "Tool.h"
#include "ToolboxWidget.h"
#include <LibGUI/AboutDialog.h>
#include <LibGUI/Action.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/FilePicker.h>
#include <LibGUI/Menu.h>
#include <LibGUI/MenuBar.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Model.h>
#include <LibGUI/TableView.h>
#include <LibGUI/Window.h>
#include <LibGfx/Bitmap.h>
#include <stdio.h>

int main(int argc, char** argv)
{
    if (pledge("stdio thread shared_buffer accept rpath unix wpath cpath fattr", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    GUI::Application app(argc, argv);

    if (pledge("stdio thread shared_buffer accept rpath wpath cpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto window = GUI::Window::construct();
    window->set_title("PaintBrush");
    window->set_rect(100, 100, 640, 480);
    window->set_icon(Gfx::Bitmap::load_from_file("/res/icons/16x16/app-paintbrush.png"));

    auto& horizontal_container = window->set_main_widget<GUI::Widget>();
    horizontal_container.set_layout<GUI::HorizontalBoxLayout>();
    horizontal_container.layout()->set_spacing(0);

    auto& toolbox = horizontal_container.add<PaintBrush::ToolboxWidget>();

    auto& vertical_container = horizontal_container.add<GUI::Widget>();
    vertical_container.set_layout<GUI::VerticalBoxLayout>();
    vertical_container.layout()->set_spacing(0);

    auto& image_editor = vertical_container.add<PaintBrush::ImageEditor>();
    image_editor.set_focus(true);

    toolbox.on_tool_selection = [&](auto* tool) {
        image_editor.set_active_tool(tool);
    };

    vertical_container.add<PaintBrush::PaletteWidget>(image_editor);

    auto& right_panel = horizontal_container.add<GUI::Widget>();
    right_panel.set_size_policy(GUI::SizePolicy::Fixed, GUI::SizePolicy::Fill);
    right_panel.set_preferred_size(230, 0);
    right_panel.set_layout<GUI::VerticalBoxLayout>();

    auto& layer_table_view = right_panel.add<GUI::TableView>();
    layer_table_view.set_size_columns_to_fit_content(true);

    window->show();

    auto menubar = GUI::MenuBar::construct();
    auto& app_menu = menubar->add_menu("PaintBrush");

    app_menu.add_action(GUI::CommonActions::make_open_action([&](auto&) {
        Optional<String> open_path = GUI::FilePicker::get_open_filepath();

        if (!open_path.has_value())
            return;

        auto bitmap = Gfx::Bitmap::load_from_file(open_path.value());
        if (!bitmap) {
            GUI::MessageBox::show(String::format("Failed to load '%s'", open_path.value().characters()), "Open failed", GUI::MessageBox::Type::Error, GUI::MessageBox::InputType::OK, window);
            return;
        }
    }));
    app_menu.add_separator();
    app_menu.add_action(GUI::CommonActions::make_quit_action([](auto&) {
        GUI::Application::the().quit(0);
        return;
    }));

    menubar->add_menu("Edit");

    auto& tool_menu = menubar->add_menu("Tool");
    toolbox.for_each_tool([&](auto& tool) {
        if (tool.action())
            tool_menu.add_action(*tool.action());
        return IterationDecision::Continue;
    });

    auto& layer_menu = menubar->add_menu("Layer");
    layer_menu.add_action(GUI::Action::create("Create new layer...", { Mod_Ctrl | Mod_Shift, Key_N }, [&](auto&) {
        auto dialog = PaintBrush::CreateNewLayerDialog::construct(image_editor.image()->size(), window);
        if (dialog->exec() == GUI::Dialog::ExecOK) {
            auto layer = PaintBrush::Layer::create_with_size(dialog->layer_size(), dialog->layer_name());
            if (!layer) {
                GUI::MessageBox::show_error(String::format("Unable to create layer with size %s", dialog->size().to_string().characters()));
                return;
            }
            image_editor.image()->add_layer(layer.release_nonnull());
            image_editor.layers_did_change();
        }
    }, window));

    layer_menu.add_separator();
    layer_menu.add_action(GUI::Action::create("Select previous layer", { 0, Key_PageUp }, [&](auto&) {
        layer_table_view.move_selection(1);
    }, window));
    layer_menu.add_action(GUI::Action::create("Select next layer", { 0, Key_PageDown }, [&](auto&) {
        layer_table_view.move_selection(-1);
    }, window));
    layer_menu.add_action(GUI::Action::create("Select top layer", { 0, Key_Home }, [&](auto&) {
        layer_table_view.selection().set(layer_table_view.model()->index(image_editor.image()->layer_count() - 1));
    }, window));
    layer_menu.add_action(GUI::Action::create("Select bottom layer", { 0, Key_End }, [&](auto&) {
        layer_table_view.selection().set(layer_table_view.model()->index(0));
    }, window));

    auto& help_menu = menubar->add_menu("Help");
    help_menu.add_action(GUI::Action::create("About", [&](auto&) {
        GUI::AboutDialog::show("PaintBrush", Gfx::Bitmap::load_from_file("/res/icons/32x32/app-paintbrush.png"), window);
    }));

    app.set_menubar(move(menubar));

    auto image = PaintBrush::Image::create_with_size({ 640, 480 });

    auto bg_layer = PaintBrush::Layer::create_with_size({ 640, 480 }, "Background");
    image->add_layer(*bg_layer);
    bg_layer->bitmap().fill(Color::Magenta);

    auto fg_layer_1 = PaintBrush::Layer::create_with_size({ 200, 100 }, "Foreground 1");
    image->add_layer(*fg_layer_1);
    fg_layer_1->set_location({ 20, 10 });
    fg_layer_1->bitmap().fill(Color::Green);

    auto fg_layer_2 = PaintBrush::Layer::create_with_size({ 64, 64 }, "Foreground 2");
    image->add_layer(*fg_layer_2);
    fg_layer_2->set_location({ 300, 350 });
    fg_layer_2->bitmap().fill(Color::Yellow);

    layer_table_view.set_model(image->layer_model());
    layer_table_view.on_selection_change = [&] {
        auto index = layer_table_view.selection().first();
        if (index.is_valid())
            image_editor.set_active_layer(const_cast<PaintBrush::Layer*>(&image->layer(index.row())));
        else
            image_editor.set_active_layer(nullptr);
    };

    image_editor.set_image(image);

    return app.exec();
}
