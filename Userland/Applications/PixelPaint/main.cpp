/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "CreateNewImageDialog.h"
#include "CreateNewLayerDialog.h"
#include "FilterParams.h"
#include "Image.h"
#include "ImageEditor.h"
#include "Layer.h"
#include "LayerListWidget.h"
#include "LayerPropertiesWidget.h"
#include "PaletteWidget.h"
#include "Tool.h"
#include "ToolPropertiesWidget.h"
#include "ToolboxWidget.h"
#include <Applications/PixelPaint/PixelPaintWindowGML.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibGUI/Action.h>
#include <LibGUI/Application.h>
#include <LibGUI/Clipboard.h>
#include <LibGUI/FilePicker.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Menubar.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Statusbar.h>
#include <LibGUI/Toolbar.h>
#include <LibGUI/Window.h>
#include <LibGfx/Bitmap.h>
#include <stdio.h>
#include <unistd.h>

int main(int argc, char** argv)
{
    if (pledge("stdio thread recvfd sendfd rpath unix wpath cpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto app = GUI::Application::construct(argc, argv);

    if (pledge("stdio thread recvfd sendfd rpath wpath cpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    const char* image_file = nullptr;
    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(image_file, "Image file to open", "path", Core::ArgsParser::Required::No);
    args_parser.parse(argc, argv);

    auto app_icon = GUI::Icon::default_icon("app-pixel-paint");

    auto window = GUI::Window::construct();
    window->set_title("Pixel Paint");
    window->resize(800, 480);
    window->set_icon(app_icon.bitmap_for_size(16));

    auto& main_widget = window->set_main_widget<GUI::Widget>();
    main_widget.load_from_gml(pixel_paint_window_gml);

    auto& toolbox = *main_widget.find_descendant_of_type_named<PixelPaint::ToolboxWidget>("toolbox");
    auto& image_editor = *main_widget.find_descendant_of_type_named<PixelPaint::ImageEditor>("image_editor");
    image_editor.set_focus(true);

    auto& palette_widget = *main_widget.find_descendant_of_type_named<PixelPaint::PaletteWidget>("palette_widget");
    palette_widget.set_image_editor(image_editor);

    auto& layer_list_widget = *main_widget.find_descendant_of_type_named<PixelPaint::LayerListWidget>("layer_list_widget");
    layer_list_widget.on_layer_select = [&](auto* layer) {
        image_editor.set_active_layer(layer);
    };

    auto& layer_properties_widget = *main_widget.find_descendant_of_type_named<PixelPaint::LayerPropertiesWidget>("layer_properties_widget");

    auto& tool_properties_widget = *main_widget.find_descendant_of_type_named<PixelPaint::ToolPropertiesWidget>("tool_properties_widget");

    toolbox.on_tool_selection = [&](auto* tool) {
        image_editor.set_active_tool(tool);
        tool_properties_widget.set_active_tool(tool);
    };

    auto new_image_action = GUI::Action::create(
        "&New Image...", { Mod_Ctrl, Key_N }, Gfx::Bitmap::load_from_file("/res/icons/16x16/new.png"), [&](auto&) {
            auto dialog = PixelPaint::CreateNewImageDialog::construct(window);
            if (dialog->exec() == GUI::Dialog::ExecOK) {
                auto image = PixelPaint::Image::create_with_size(dialog->image_size());
                auto bg_layer = PixelPaint::Layer::create_with_size(*image, image->size(), "Background");
                image->add_layer(*bg_layer);
                bg_layer->bitmap().fill(Color::White);

                image_editor.set_image(image);
                layer_list_widget.set_image(image);
                image_editor.set_active_layer(bg_layer);
            }
        },
        window);

    auto open_image_file = [&](auto& path) {
        auto image = PixelPaint::Image::create_from_file(path);
        if (!image) {
            GUI::MessageBox::show_error(window, String::formatted("Invalid image file: {}", path));
            return;
        }
        image_editor.set_image(image);
        layer_list_widget.set_image(image);
    };

    auto open_image_action = GUI::CommonActions::make_open_action([&](auto&) {
        Optional<String> open_path = GUI::FilePicker::get_open_filepath(window);
        if (!open_path.has_value())
            return;
        open_image_file(open_path.value());
    });

    auto save_image_as_action = GUI::CommonActions::make_save_as_action([&](auto&) {
        if (!image_editor.image())
            return;
        Optional<String> save_path = GUI::FilePicker::get_save_filepath(window, "untitled", "pp");
        if (!save_path.has_value())
            return;
        image_editor.image()->save(save_path.value());
    });

    auto menubar = GUI::Menubar::construct();
    auto& file_menu = menubar->add_menu("&File");

    file_menu.add_action(new_image_action);
    file_menu.add_action(open_image_action);
    file_menu.add_action(save_image_as_action);
    auto& export_submenu = file_menu.add_submenu("&Export");
    export_submenu.add_action(
        GUI::Action::create(
            "As &BMP", [&](auto&) {
                if (!image_editor.image())
                    return;
                Optional<String> save_path = GUI::FilePicker::get_save_filepath(window, "untitled", "bmp");
                if (!save_path.has_value())
                    return;
                image_editor.image()->export_bmp(save_path.value());
            },
            window));
    export_submenu.add_action(
        GUI::Action::create(
            "As &PNG", [&](auto&) {
                if (!image_editor.image())
                    return;

                Optional<String> save_path = GUI::FilePicker::get_save_filepath(window, "untitled", "png");

                if (!save_path.has_value())
                    return;

                image_editor.image()->export_png(save_path.value());
            },
            window));

    file_menu.add_separator();
    file_menu.add_action(GUI::CommonActions::make_quit_action([](auto&) {
        GUI::Application::the()->quit();
    }));

    auto& edit_menu = menubar->add_menu("&Edit");
    auto paste_action = GUI::CommonActions::make_paste_action([&](auto&) {
        VERIFY(image_editor.image());
        auto bitmap = GUI::Clipboard::the().bitmap();
        if (!bitmap)
            return;

        auto layer = PixelPaint::Layer::create_with_bitmap(*image_editor.image(), *bitmap, "Pasted layer");
        image_editor.image()->add_layer(layer.release_nonnull());
    });
    GUI::Clipboard::the().on_change = [&](auto& mime_type) {
        paste_action->set_enabled(mime_type == "image/x-serenityos");
    };
    paste_action->set_enabled(GUI::Clipboard::the().mime_type() == "image/x-serenityos");

    edit_menu.add_action(paste_action);

    auto undo_action = GUI::CommonActions::make_undo_action([&](auto&) {
        VERIFY(image_editor.image());
        image_editor.undo();
    });
    edit_menu.add_action(undo_action);

    auto redo_action = GUI::CommonActions::make_redo_action([&](auto&) {
        VERIFY(image_editor.image());
        image_editor.redo();
    });
    edit_menu.add_action(redo_action);

    auto& view_menu = menubar->add_menu("&View");

    auto zoom_in_action = GUI::CommonActions::make_zoom_in_action(
        [&](auto&) {
            image_editor.scale_by(0.1f);
        },
        window);

    auto zoom_out_action = GUI::CommonActions::make_zoom_out_action(
        [&](auto&) {
            image_editor.scale_by(-0.1f);
        },
        window);

    auto reset_zoom_action = GUI::CommonActions::make_reset_zoom_action(
        [&](auto&) {
            image_editor.reset_scale_and_position();
        },
        window);

    view_menu.add_action(zoom_in_action);
    view_menu.add_action(zoom_out_action);
    view_menu.add_action(reset_zoom_action);

    auto& tool_menu = menubar->add_menu("&Tool");
    toolbox.for_each_tool([&](auto& tool) {
        if (tool.action())
            tool_menu.add_action(*tool.action());
        return IterationDecision::Continue;
    });

    auto& layer_menu = menubar->add_menu("&Layer");
    layer_menu.add_action(GUI::Action::create(
        "New &Layer...", { Mod_Ctrl | Mod_Shift, Key_N }, [&](auto&) {
            auto dialog = PixelPaint::CreateNewLayerDialog::construct(image_editor.image()->size(), window);
            if (dialog->exec() == GUI::Dialog::ExecOK) {
                auto layer = PixelPaint::Layer::create_with_size(*image_editor.image(), dialog->layer_size(), dialog->layer_name());
                if (!layer) {
                    GUI::MessageBox::show_error(window, String::formatted("Unable to create layer with size {}", dialog->size().to_string()));
                    return;
                }
                image_editor.image()->add_layer(layer.release_nonnull());
                image_editor.layers_did_change();
            }
        },
        window));

    layer_menu.add_separator();
    layer_menu.add_action(GUI::Action::create(
        "Select &Previous Layer", { 0, Key_PageUp }, [&](auto&) {
            layer_list_widget.move_selection(1);
        },
        window));
    layer_menu.add_action(GUI::Action::create(
        "Select &Next Layer", { 0, Key_PageDown }, [&](auto&) {
            layer_list_widget.move_selection(-1);
        },
        window));
    layer_menu.add_action(GUI::Action::create(
        "Select &Top Layer", { 0, Key_Home }, [&](auto&) {
            layer_list_widget.select_top_layer();
        },
        window));
    layer_menu.add_action(GUI::Action::create(
        "Select &Bottom Layer", { 0, Key_End }, [&](auto&) {
            layer_list_widget.select_bottom_layer();
        },
        window));
    layer_menu.add_separator();
    layer_menu.add_action(GUI::Action::create(
        "Move Active Layer &Up", { Mod_Ctrl, Key_PageUp }, [&](auto&) {
            auto active_layer = image_editor.active_layer();
            if (!active_layer)
                return;
            image_editor.image()->move_layer_up(*active_layer);
        },
        window));
    layer_menu.add_action(GUI::Action::create(
        "Move Active Layer &Down", { Mod_Ctrl, Key_PageDown }, [&](auto&) {
            auto active_layer = image_editor.active_layer();
            if (!active_layer)
                return;
            image_editor.image()->move_layer_down(*active_layer);
        },
        window));
    layer_menu.add_separator();
    layer_menu.add_action(GUI::Action::create(
        "&Remove Active Layer", { Mod_Ctrl, Key_D }, [&](auto&) {
            auto active_layer = image_editor.active_layer();
            if (!active_layer)
                return;
            image_editor.image()->remove_layer(*active_layer);
            image_editor.set_active_layer(nullptr);
        },
        window));

    auto& filter_menu = menubar->add_menu("&Filter");
    auto& spatial_filters_menu = filter_menu.add_submenu("&Spatial");

    auto& edge_detect_submenu = spatial_filters_menu.add_submenu("&Edge Detect");
    edge_detect_submenu.add_action(GUI::Action::create("Laplacian (&Cardinal)", [&](auto&) {
        if (auto* layer = image_editor.active_layer()) {
            Gfx::LaplacianFilter filter;
            if (auto parameters = PixelPaint::FilterParameters<Gfx::LaplacianFilter>::get(false)) {
                filter.apply(layer->bitmap(), layer->rect(), layer->bitmap(), layer->rect(), *parameters);
                image_editor.did_complete_action();
            }
        }
    }));
    edge_detect_submenu.add_action(GUI::Action::create("Laplacian (&Diagonal)", [&](auto&) {
        if (auto* layer = image_editor.active_layer()) {
            Gfx::LaplacianFilter filter;
            if (auto parameters = PixelPaint::FilterParameters<Gfx::LaplacianFilter>::get(true)) {
                filter.apply(layer->bitmap(), layer->rect(), layer->bitmap(), layer->rect(), *parameters);
                image_editor.did_complete_action();
            }
        }
    }));
    auto& blur_submenu = spatial_filters_menu.add_submenu("&Blur and Sharpen");
    blur_submenu.add_action(GUI::Action::create("&Gaussian Blur (3x3)", [&](auto&) {
        if (auto* layer = image_editor.active_layer()) {
            Gfx::SpatialGaussianBlurFilter<3> filter;
            if (auto parameters = PixelPaint::FilterParameters<Gfx::SpatialGaussianBlurFilter<3>>::get()) {
                filter.apply(layer->bitmap(), layer->rect(), layer->bitmap(), layer->rect(), *parameters);
                image_editor.did_complete_action();
            }
        }
    }));
    blur_submenu.add_action(GUI::Action::create("G&aussian Blur (5x5)", [&](auto&) {
        if (auto* layer = image_editor.active_layer()) {
            Gfx::SpatialGaussianBlurFilter<5> filter;
            if (auto parameters = PixelPaint::FilterParameters<Gfx::SpatialGaussianBlurFilter<5>>::get()) {
                filter.apply(layer->bitmap(), layer->rect(), layer->bitmap(), layer->rect(), *parameters);
                image_editor.did_complete_action();
            }
        }
    }));
    blur_submenu.add_action(GUI::Action::create("&Box Blur (3x3)", [&](auto&) {
        if (auto* layer = image_editor.active_layer()) {
            Gfx::BoxBlurFilter<3> filter;
            if (auto parameters = PixelPaint::FilterParameters<Gfx::BoxBlurFilter<3>>::get()) {
                filter.apply(layer->bitmap(), layer->rect(), layer->bitmap(), layer->rect(), *parameters);
                image_editor.did_complete_action();
            }
        }
    }));
    blur_submenu.add_action(GUI::Action::create("B&ox Blur (5x5)", [&](auto&) {
        if (auto* layer = image_editor.active_layer()) {
            Gfx::BoxBlurFilter<5> filter;
            if (auto parameters = PixelPaint::FilterParameters<Gfx::BoxBlurFilter<5>>::get()) {
                filter.apply(layer->bitmap(), layer->rect(), layer->bitmap(), layer->rect(), *parameters);
                image_editor.did_complete_action();
            }
        }
    }));
    blur_submenu.add_action(GUI::Action::create("&Sharpen", [&](auto&) {
        if (auto* layer = image_editor.active_layer()) {
            Gfx::SharpenFilter filter;
            if (auto parameters = PixelPaint::FilterParameters<Gfx::SharpenFilter>::get()) {
                filter.apply(layer->bitmap(), layer->rect(), layer->bitmap(), layer->rect(), *parameters);
                image_editor.did_complete_action();
            }
        }
    }));

    spatial_filters_menu.add_separator();
    spatial_filters_menu.add_action(GUI::Action::create("Generic 5x5 &Convolution", [&](auto&) {
        if (auto* layer = image_editor.active_layer()) {
            Gfx::GenericConvolutionFilter<5> filter;
            if (auto parameters = PixelPaint::FilterParameters<Gfx::GenericConvolutionFilter<5>>::get(window)) {
                filter.apply(layer->bitmap(), layer->rect(), layer->bitmap(), layer->rect(), *parameters);
                image_editor.did_complete_action();
            }
        }
    }));

    auto& help_menu = menubar->add_menu("&Help");
    help_menu.add_action(GUI::CommonActions::make_about_action("Pixel Paint", app_icon, window));

    window->set_menubar(move(menubar));

    auto& toolbar = *main_widget.find_descendant_of_type_named<GUI::Toolbar>("toolbar");
    toolbar.add_action(new_image_action);
    toolbar.add_action(open_image_action);
    toolbar.add_action(save_image_as_action);
    toolbar.add_separator();
    toolbar.add_action(paste_action);
    toolbar.add_action(undo_action);
    toolbar.add_action(redo_action);
    toolbar.add_separator();
    toolbar.add_action(zoom_in_action);
    toolbar.add_action(zoom_out_action);
    toolbar.add_action(reset_zoom_action);

    image_editor.on_active_layer_change = [&](auto* layer) {
        layer_list_widget.set_selected_layer(layer);
        layer_properties_widget.set_layer(layer);
    };

    auto image_file_real_path = Core::File::real_path_for(image_file);
    if (Core::File::exists(image_file_real_path)) {
        open_image_file(image_file_real_path);
    } else {
        auto image = PixelPaint::Image::create_with_size({ 480, 360 });

        auto bg_layer = PixelPaint::Layer::create_with_size(*image, image->size(), "Background");
        image->add_layer(*bg_layer);
        bg_layer->bitmap().fill(Color::White);

        layer_list_widget.set_image(image);

        image_editor.set_image(image);
        image_editor.set_active_layer(bg_layer);
    }

    auto& statusbar = *main_widget.find_descendant_of_type_named<GUI::Statusbar>("statusbar");

    app->on_action_enter = [&statusbar](GUI::Action& action) {
        auto text = action.status_tip();
        if (text.is_empty())
            text = Gfx::parse_ampersand_string(action.text());
        statusbar.set_override_text(move(text));
    };

    app->on_action_leave = [&statusbar](GUI::Action&) {
        statusbar.set_override_text({});
    };

    window->show();
    return app->exec();
}
