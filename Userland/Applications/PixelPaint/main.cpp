/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Mustafa Quraish <mustafa@cs.toronto.edu>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "CreateNewImageDialog.h"
#include "CreateNewLayerDialog.h"
#include "EditGuideDialog.h"
#include "FilterParams.h"
#include "Guide.h"
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
#include <LibFileSystemAccessClient/Client.h>
#include <LibGUI/Action.h>
#include <LibGUI/Application.h>
#include <LibGUI/Clipboard.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Menubar.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Statusbar.h>
#include <LibGUI/TabWidget.h>
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

    const char* image_file = nullptr;
    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(image_file, "Image file to open", "path", Core::ArgsParser::Required::No);
    args_parser.parse(argc, argv);

    String file_to_edit_full_path;

    if (image_file) {
        file_to_edit_full_path = Core::File::absolute_path(image_file);
        VERIFY(!file_to_edit_full_path.is_empty());
        if (Core::File::exists(file_to_edit_full_path)) {
            dbgln("unveil for: {}", file_to_edit_full_path);
            if (unveil(file_to_edit_full_path.characters(), "r") < 0) {
                perror("unveil");
                return 1;
            }
        }
    }

    if (unveil("/res", "r") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil("/tmp/portal/clipboard", "rw") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil("/tmp/portal/filesystemaccess", "rw") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil("/tmp/portal/image", "rw") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil(nullptr, nullptr) < 0) {
        perror("unveil");
        return 1;
    }

    auto app_icon = GUI::Icon::default_icon("app-pixel-paint");

    auto window = GUI::Window::construct();
    window->set_title("Pixel Paint");
    window->resize(800, 510);
    window->set_icon(app_icon.bitmap_for_size(16));

    auto& main_widget = window->set_main_widget<GUI::Widget>();
    main_widget.load_from_gml(pixel_paint_window_gml);

    auto& toolbox = *main_widget.find_descendant_of_type_named<PixelPaint::ToolboxWidget>("toolbox");
    auto& tab_widget = *main_widget.find_descendant_of_type_named<GUI::TabWidget>("tab_widget");
    tab_widget.set_container_margins({ 4, 5, 5, 4 });
    tab_widget.set_close_button_enabled(true);

    auto& palette_widget = *main_widget.find_descendant_of_type_named<PixelPaint::PaletteWidget>("palette_widget");

    auto current_image_editor = [&]() -> PixelPaint::ImageEditor* {
        if (!tab_widget.active_widget())
            return nullptr;
        return verify_cast<PixelPaint::ImageEditor>(tab_widget.active_widget());
    };

    Function<PixelPaint::ImageEditor&(NonnullRefPtr<PixelPaint::Image>)> create_new_editor;

    auto& layer_list_widget = *main_widget.find_descendant_of_type_named<PixelPaint::LayerListWidget>("layer_list_widget");
    layer_list_widget.on_layer_select = [&](auto* layer) {
        if (auto* editor = current_image_editor())
            editor->set_active_layer(layer);
    };

    auto& layer_properties_widget = *main_widget.find_descendant_of_type_named<PixelPaint::LayerPropertiesWidget>("layer_properties_widget");

    auto& tool_properties_widget = *main_widget.find_descendant_of_type_named<PixelPaint::ToolPropertiesWidget>("tool_properties_widget");

    toolbox.on_tool_selection = [&](auto* tool) {
        if (auto* editor = current_image_editor())
            editor->set_active_tool(tool);
        tool_properties_widget.set_active_tool(tool);
    };

    auto new_image_action = GUI::Action::create(
        "&New Image...", { Mod_Ctrl, Key_N }, Gfx::Bitmap::try_load_from_file("/res/icons/16x16/new.png"), [&](auto&) {
            auto dialog = PixelPaint::CreateNewImageDialog::construct(window);
            if (dialog->exec() == GUI::Dialog::ExecOK) {
                auto image = PixelPaint::Image::try_create_with_size(dialog->image_size());
                auto bg_layer = PixelPaint::Layer::try_create_with_size(*image, image->size(), "Background");
                VERIFY(bg_layer);
                image->add_layer(*bg_layer);
                bg_layer->bitmap().fill(Color::White);
                auto image_title = dialog->image_name().trim_whitespace();
                image->set_title(image_title.is_empty() ? "Untitled" : image_title);

                create_new_editor(*image);
                layer_list_widget.set_image(image);
                layer_list_widget.set_selected_layer(bg_layer);
            }
        },
        window);

    auto open_image_file = [&](auto& path) {
        auto image_or_error = PixelPaint::Image::try_create_from_path(path);
        if (image_or_error.is_error()) {
            GUI::MessageBox::show_error(window, String::formatted("Unable to open file: {}", path));
            return;
        }
        auto& image = *image_or_error.value();
        create_new_editor(image);
        layer_list_widget.set_image(&image);
    };

    auto open_image_fd = [&](int fd, auto& path) {
        auto image_or_error = PixelPaint::Image::try_create_from_fd_and_close(fd, path);
        if (image_or_error.is_error()) {
            GUI::MessageBox::show_error(window, String::formatted("Unable to open file: {}, {}", path, image_or_error.error()));
            return;
        }
        auto& image = *image_or_error.value();
        create_new_editor(image);
        layer_list_widget.set_image(&image);
    };

    auto open_image_action = GUI::CommonActions::make_open_action([&](auto&) {
        auto result = FileSystemAccessClient::Client::the().open_file(window->window_id());
        if (result.error != 0)
            return;

        open_image_fd(*result.fd, *result.chosen_file);
    });

    auto save_image_as_action = GUI::CommonActions::make_save_as_action([&](auto&) {
        auto* editor = current_image_editor();
        if (!editor)
            return;
        auto save_result = FileSystemAccessClient::Client::the().save_file(window->window_id(), "untitled", "pp");
        if (save_result.error != 0)
            return;
        auto result = editor->save_project_to_fd_and_close(*save_result.fd);
        if (result.is_error()) {
            GUI::MessageBox::show_error(window, String::formatted("Could not save {}: {}", *save_result.chosen_file, result.error()));
            return;
        }
        editor->image().set_path(*save_result.chosen_file);
    });

    auto& file_menu = window->add_menu("&File");

    file_menu.add_action(new_image_action);
    file_menu.add_action(open_image_action);
    file_menu.add_action(save_image_as_action);
    auto& export_submenu = file_menu.add_submenu("&Export");
    export_submenu.add_action(
        GUI::Action::create(
            "As &BMP", [&](auto&) {
                auto* editor = current_image_editor();
                if (!editor)
                    return;
                auto save_result = FileSystemAccessClient::Client::the().save_file(window->window_id(), "untitled", "bmp");
                if (save_result.error != 0)
                    return;
                auto preserve_alpha_channel = GUI::MessageBox::show(window, "Do you wish to preserve transparency?", "Preserve transparency?", GUI::MessageBox::Type::Question, GUI::MessageBox::InputType::YesNo);
                auto result = editor->image().export_bmp_to_fd_and_close(*save_result.fd, preserve_alpha_channel == GUI::MessageBox::ExecYes);
                if (result.is_error())
                    GUI::MessageBox::show_error(window, String::formatted("Export to BMP failed: {}", result.error()));
            },
            window));
    export_submenu.add_action(
        GUI::Action::create(
            "As &PNG", [&](auto&) {
                auto* editor = current_image_editor();
                auto save_result = FileSystemAccessClient::Client::the().save_file(window->window_id(), "untitled", "png");
                if (save_result.error != 0)
                    return;
                auto preserve_alpha_channel = GUI::MessageBox::show(window, "Do you wish to preserve transparency?", "Preserve transparency?", GUI::MessageBox::Type::Question, GUI::MessageBox::InputType::YesNo);
                auto result = editor->image().export_png_to_fd_and_close(*save_result.fd, preserve_alpha_channel == GUI::MessageBox::ExecYes);
                if (result.is_error())
                    GUI::MessageBox::show_error(window, String::formatted("Export to PNG failed: {}", result.error()));
            },
            window));

    file_menu.add_separator();
    file_menu.add_action(GUI::CommonActions::make_quit_action([](auto&) {
        GUI::Application::the()->quit();
    }));

    auto& edit_menu = window->add_menu("&Edit");

    auto copy_action = GUI::CommonActions::make_copy_action([&](auto&) {
        auto* editor = current_image_editor();
        if (!editor)
            return;
        if (!editor->active_layer()) {
            dbgln("Cannot copy with no active layer selected");
            return;
        }
        auto bitmap = editor->active_layer()->try_copy_bitmap(editor->selection());
        if (!bitmap) {
            dbgln("try_copy_bitmap() from Layer failed");
            return;
        }
        GUI::Clipboard::the().set_bitmap(*bitmap);
    });

    auto paste_action = GUI::CommonActions::make_paste_action([&](auto&) {
        auto* editor = current_image_editor();
        if (!editor)
            return;
        auto bitmap = GUI::Clipboard::the().bitmap();
        if (!bitmap)
            return;

        auto layer = PixelPaint::Layer::try_create_with_bitmap(editor->image(), *bitmap, "Pasted layer");
        VERIFY(layer);
        editor->image().add_layer(*layer);
        editor->set_active_layer(layer);
        editor->selection().clear();
    });
    GUI::Clipboard::the().on_change = [&](auto& mime_type) {
        paste_action->set_enabled(mime_type == "image/x-serenityos");
    };
    paste_action->set_enabled(GUI::Clipboard::the().mime_type() == "image/x-serenityos");

    edit_menu.add_action(copy_action);
    edit_menu.add_action(paste_action);

    auto undo_action = GUI::CommonActions::make_undo_action([&](auto&) {
        if (auto* editor = current_image_editor())
            editor->undo();
    });
    edit_menu.add_action(undo_action);

    auto redo_action = GUI::CommonActions::make_redo_action([&](auto&) {
        if (auto* editor = current_image_editor())
            editor->redo();
    });
    edit_menu.add_action(redo_action);

    edit_menu.add_separator();
    edit_menu.add_action(GUI::CommonActions::make_select_all_action([&](auto&) {
        auto* editor = current_image_editor();
        if (!editor->active_layer())
            return;
        editor->selection().merge(editor->active_layer()->relative_rect(), PixelPaint::Selection::MergeMode::Set);
    }));
    edit_menu.add_action(GUI::Action::create(
        "Clear &Selection", { Mod_Ctrl | Mod_Shift, Key_A }, [&](auto&) {
            if (auto* editor = current_image_editor())
                editor->selection().clear();
        },
        window));

    edit_menu.add_separator();
    edit_menu.add_action(GUI::Action::create(
        "&Swap Colors", { Mod_None, Key_X }, [&](auto&) {
            auto* editor = current_image_editor();
            if (!editor)
                return;
            auto old_primary_color = editor->primary_color();
            editor->set_primary_color(editor->secondary_color());
            editor->set_secondary_color(old_primary_color);
        },
        window));
    edit_menu.add_action(GUI::Action::create(
        "&Default Colors", { Mod_None, Key_D }, [&](auto&) {
            if (auto* editor = current_image_editor()) {
                editor->set_primary_color(Color::Black);
                editor->set_secondary_color(Color::White);
            }
        },
        window));
    edit_menu.add_action(GUI::Action::create(
        "&Load Color Palette", [&](auto&) {
            auto open_result = FileSystemAccessClient::Client::the().open_file(window->window_id(), "Load Color Palette");
            if (open_result.error != 0)
                return;

            auto result = PixelPaint::PaletteWidget::load_palette_fd_and_close(*open_result.fd);
            if (result.is_error()) {
                GUI::MessageBox::show_error(window, String::formatted("Loading color palette failed: {}", result.error()));
                return;
            }

            palette_widget.display_color_list(result.value());
        },
        window));
    edit_menu.add_action(GUI::Action::create(
        "Sa&ve Color Palette", [&](auto&) {
            auto save_result = FileSystemAccessClient::Client::the().save_file(window->window_id(), "untitled", "palette");
            if (save_result.error != 0)
                return;

            auto result = PixelPaint::PaletteWidget::save_palette_fd_and_close(palette_widget.colors(), *save_result.fd);
            if (result.is_error())
                GUI::MessageBox::show_error(window, String::formatted("Writing color palette failed: {}", result.error()));
        },
        window));

    auto& view_menu = window->add_menu("&View");

    auto zoom_in_action = GUI::CommonActions::make_zoom_in_action(
        [&](auto&) {
            if (auto* editor = current_image_editor())
                editor->scale_by(0.1f);
        },
        window);

    auto zoom_out_action = GUI::CommonActions::make_zoom_out_action(
        [&](auto&) {
            if (auto* editor = current_image_editor())
                editor->scale_by(-0.1f);
        },
        window);

    auto reset_zoom_action = GUI::CommonActions::make_reset_zoom_action(
        [&](auto&) {
            if (auto* editor = current_image_editor())
                editor->reset_scale_and_position();
        },
        window);

    auto add_guide_action = GUI::Action::create(
        "Add Guide", [&](auto&) {
            auto dialog = PixelPaint::EditGuideDialog::construct(window);
            if (dialog->exec() != GUI::Dialog::ExecOK)
                return;
            if (auto* editor = current_image_editor()) {
                auto offset = dialog->offset_as_pixel(*editor);
                if (!offset.has_value())
                    return;
                editor->add_guide(PixelPaint::Guide::construct(dialog->orientation(), offset.value()));
            }
        },
        window);

    auto show_guides_action = GUI::Action::create_checkable(
        "Show Guides", [&](auto& action) {
            if (auto* editor = current_image_editor()) {
                editor->set_guide_visibility(action.is_checked());
            }
        },
        window);
    show_guides_action->set_checked(true);

    view_menu.add_action(zoom_in_action);
    view_menu.add_action(zoom_out_action);
    view_menu.add_action(reset_zoom_action);
    view_menu.add_separator();
    view_menu.add_action(add_guide_action);
    view_menu.add_action(show_guides_action);

    auto& tool_menu = window->add_menu("&Tool");
    toolbox.for_each_tool([&](auto& tool) {
        if (tool.action())
            tool_menu.add_action(*tool.action());
        return IterationDecision::Continue;
    });

    auto& image_menu = window->add_menu("&Image");
    image_menu.add_action(GUI::Action::create(
        "Flip &Vertically", [&](auto&) {
            auto* editor = current_image_editor();
            if (!editor)
                return;
            editor->image().flip(Gfx::Orientation::Vertical);
        },
        window));
    image_menu.add_action(GUI::Action::create(
        "Flip &Horizontally", [&](auto&) {
            auto* editor = current_image_editor();
            if (!editor)
                return;
            editor->image().flip(Gfx::Orientation::Horizontal);
        },
        window));
    image_menu.add_action(GUI::Action::create(
        "Rotate &Left", [&](auto&) {
            auto* editor = current_image_editor();
            if (!editor)
                return;
            editor->image().rotate(Gfx::RotationDirection::CounterClockwise);
        },
        window));
    image_menu.add_action(GUI::Action::create(
        "Rotate &Right", [&](auto&) {
            auto* editor = current_image_editor();
            if (!editor)
                return;
            editor->image().rotate(Gfx::RotationDirection::Clockwise);
        },
        window));

    auto& layer_menu = window->add_menu("&Layer");
    layer_menu.add_action(GUI::Action::create(
        "New &Layer...", { Mod_Ctrl | Mod_Shift, Key_N }, [&](auto&) {
            auto* editor = current_image_editor();
            if (!editor)
                return;
            auto dialog = PixelPaint::CreateNewLayerDialog::construct(editor->image().size(), window);
            if (dialog->exec() == GUI::Dialog::ExecOK) {
                auto layer = PixelPaint::Layer::try_create_with_size(editor->image(), dialog->layer_size(), dialog->layer_name());
                if (!layer) {
                    GUI::MessageBox::show_error(window, String::formatted("Unable to create layer with size {}", dialog->size().to_string()));
                    return;
                }
                editor->image().add_layer(layer.release_nonnull());
                editor->layers_did_change();
                layer_list_widget.select_top_layer();
            }
        },
        window));

    layer_menu.add_separator();
    layer_menu.add_action(GUI::Action::create(
        "Select &Previous Layer", { 0, Key_PageUp }, [&](auto&) {
            layer_list_widget.cycle_through_selection(1);
        },
        window));
    layer_menu.add_action(GUI::Action::create(
        "Select &Next Layer", { 0, Key_PageDown }, [&](auto&) {
            layer_list_widget.cycle_through_selection(-1);
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
    layer_menu.add_action(GUI::CommonActions::make_move_to_front_action(
        [&](auto&) {
            auto* editor = current_image_editor();
            if (!editor)
                return;
            auto active_layer = editor->active_layer();
            if (!active_layer)
                return;
            editor->image().move_layer_to_front(*active_layer);
            editor->layers_did_change();
        },
        window));
    layer_menu.add_action(GUI::CommonActions::make_move_to_back_action(
        [&](auto&) {
            auto* editor = current_image_editor();
            if (!editor)
                return;
            auto active_layer = editor->active_layer();
            if (!active_layer)
                return;
            editor->image().move_layer_to_back(*active_layer);
            editor->layers_did_change();
        },
        window));
    layer_menu.add_separator();
    layer_menu.add_action(GUI::Action::create(
        "Move Active Layer &Up", { Mod_Ctrl, Key_PageUp }, [&](auto&) {
            auto* editor = current_image_editor();
            if (!editor)
                return;
            auto active_layer = editor->active_layer();
            if (!active_layer)
                return;
            editor->image().move_layer_up(*active_layer);
        },
        window));
    layer_menu.add_action(GUI::Action::create(
        "Move Active Layer &Down", { Mod_Ctrl, Key_PageDown }, [&](auto&) {
            auto* editor = current_image_editor();
            if (!editor)
                return;
            auto active_layer = editor->active_layer();
            if (!active_layer)
                return;
            editor->image().move_layer_down(*active_layer);
        },
        window));
    layer_menu.add_separator();
    layer_menu.add_action(GUI::Action::create(
        "&Remove Active Layer", { Mod_Ctrl, Key_D }, Gfx::Bitmap::try_load_from_file("/res/icons/16x16/delete.png"), [&](auto&) {
            auto* editor = current_image_editor();
            if (!editor)
                return;
            auto active_layer = editor->active_layer();
            if (!active_layer)
                return;

            auto active_layer_index = editor->image().index_of(*active_layer);
            editor->image().remove_layer(*active_layer);

            if (editor->image().layer_count()) {
                auto& next_active_layer = editor->image().layer(active_layer_index > 0 ? active_layer_index - 1 : 0);
                editor->set_active_layer(&next_active_layer);
            } else {
                editor->set_active_layer(nullptr);
            }
        },
        window));

    layer_list_widget.on_context_menu_request = [&](auto& event) {
        layer_menu.popup(event.screen_position());
    };
    layer_menu.add_separator();
    layer_menu.add_action(GUI::Action::create(
        "&Flatten Image", { Mod_Ctrl, Key_F }, [&](auto&) {
            auto* editor = current_image_editor();
            if (!editor)
                return;
            editor->image().flatten_all_layers();
            editor->did_complete_action();
        },
        window));

    layer_menu.add_action(GUI::Action::create(
        "&Merge Visible", { Mod_Ctrl, Key_M }, [&](auto&) {
            auto* editor = current_image_editor();
            if (!editor)
                return;
            editor->image().merge_visible_layers();
            editor->did_complete_action();
        },
        window));

    layer_menu.add_action(GUI::Action::create(
        "M&erge Active Layer Down", { Mod_Ctrl, Key_E }, [&](auto&) {
            auto* editor = current_image_editor();
            if (!editor)
                return;
            auto active_layer = editor->active_layer();
            if (!active_layer)
                return;
            editor->image().merge_active_layer_down(*active_layer);
            editor->did_complete_action();
        },
        window));

    auto& filter_menu = window->add_menu("&Filter");
    auto& spatial_filters_menu = filter_menu.add_submenu("&Spatial");

    auto& edge_detect_submenu = spatial_filters_menu.add_submenu("&Edge Detect");
    edge_detect_submenu.add_action(GUI::Action::create("Laplacian (&Cardinal)", [&](auto&) {
        auto* editor = current_image_editor();
        if (!editor)
            return;
        if (auto* layer = editor->active_layer()) {
            Gfx::LaplacianFilter filter;
            if (auto parameters = PixelPaint::FilterParameters<Gfx::LaplacianFilter>::get(false)) {
                filter.apply(layer->bitmap(), layer->rect(), layer->bitmap(), layer->rect(), *parameters);
                editor->did_complete_action();
            }
        }
    }));
    edge_detect_submenu.add_action(GUI::Action::create("Laplacian (&Diagonal)", [&](auto&) {
        auto* editor = current_image_editor();
        if (!editor)
            return;
        if (auto* layer = editor->active_layer()) {
            Gfx::LaplacianFilter filter;
            if (auto parameters = PixelPaint::FilterParameters<Gfx::LaplacianFilter>::get(true)) {
                filter.apply(layer->bitmap(), layer->rect(), layer->bitmap(), layer->rect(), *parameters);
                editor->did_complete_action();
            }
        }
    }));
    auto& blur_submenu = spatial_filters_menu.add_submenu("&Blur and Sharpen");
    blur_submenu.add_action(GUI::Action::create("&Gaussian Blur (3x3)", [&](auto&) {
        auto* editor = current_image_editor();
        if (!editor)
            return;
        if (auto* layer = editor->active_layer()) {
            Gfx::SpatialGaussianBlurFilter<3> filter;
            if (auto parameters = PixelPaint::FilterParameters<Gfx::SpatialGaussianBlurFilter<3>>::get()) {
                filter.apply(layer->bitmap(), layer->rect(), layer->bitmap(), layer->rect(), *parameters);
                editor->did_complete_action();
            }
        }
    }));
    blur_submenu.add_action(GUI::Action::create("G&aussian Blur (5x5)", [&](auto&) {
        auto* editor = current_image_editor();
        if (!editor)
            return;
        if (auto* layer = editor->active_layer()) {
            Gfx::SpatialGaussianBlurFilter<5> filter;
            if (auto parameters = PixelPaint::FilterParameters<Gfx::SpatialGaussianBlurFilter<5>>::get()) {
                filter.apply(layer->bitmap(), layer->rect(), layer->bitmap(), layer->rect(), *parameters);
                editor->did_complete_action();
            }
        }
    }));
    blur_submenu.add_action(GUI::Action::create("&Box Blur (3x3)", [&](auto&) {
        auto* editor = current_image_editor();
        if (!editor)
            return;
        if (auto* layer = editor->active_layer()) {
            Gfx::BoxBlurFilter<3> filter;
            if (auto parameters = PixelPaint::FilterParameters<Gfx::BoxBlurFilter<3>>::get()) {
                filter.apply(layer->bitmap(), layer->rect(), layer->bitmap(), layer->rect(), *parameters);
                editor->did_complete_action();
            }
        }
    }));
    blur_submenu.add_action(GUI::Action::create("B&ox Blur (5x5)", [&](auto&) {
        auto* editor = current_image_editor();
        if (!editor)
            return;
        if (auto* layer = editor->active_layer()) {
            Gfx::BoxBlurFilter<5> filter;
            if (auto parameters = PixelPaint::FilterParameters<Gfx::BoxBlurFilter<5>>::get()) {
                filter.apply(layer->bitmap(), layer->rect(), layer->bitmap(), layer->rect(), *parameters);
                editor->did_complete_action();
            }
        }
    }));
    blur_submenu.add_action(GUI::Action::create("&Sharpen", [&](auto&) {
        auto* editor = current_image_editor();
        if (!editor)
            return;
        if (auto* layer = editor->active_layer()) {
            Gfx::SharpenFilter filter;
            if (auto parameters = PixelPaint::FilterParameters<Gfx::SharpenFilter>::get()) {
                filter.apply(layer->bitmap(), layer->rect(), layer->bitmap(), layer->rect(), *parameters);
                editor->did_complete_action();
            }
        }
    }));

    spatial_filters_menu.add_separator();
    spatial_filters_menu.add_action(GUI::Action::create("Generic 5x5 &Convolution", [&](auto&) {
        auto* editor = current_image_editor();
        if (!editor)
            return;
        if (auto* layer = editor->active_layer()) {
            Gfx::GenericConvolutionFilter<5> filter;
            if (auto parameters = PixelPaint::FilterParameters<Gfx::GenericConvolutionFilter<5>>::get(window)) {
                filter.apply(layer->bitmap(), layer->rect(), layer->bitmap(), layer->rect(), *parameters);
                editor->did_complete_action();
            }
        }
    }));

    auto& color_filters_menu = filter_menu.add_submenu("&Color");
    color_filters_menu.add_action(GUI::Action::create("Grayscale", [&](auto&) {
        auto* editor = current_image_editor();
        if (!editor)
            return;
        if (auto* layer = editor->active_layer()) {
            Gfx::GrayscaleFilter filter;
            filter.apply(layer->bitmap(), layer->rect(), layer->bitmap(), layer->rect());
            editor->did_complete_action();
        }
    }));

    auto& help_menu = window->add_menu("&Help");
    help_menu.add_action(GUI::CommonActions::make_about_action("Pixel Paint", app_icon, window));

    auto& toolbar = *main_widget.find_descendant_of_type_named<GUI::Toolbar>("toolbar");
    toolbar.add_action(new_image_action);
    toolbar.add_action(open_image_action);
    toolbar.add_action(save_image_as_action);
    toolbar.add_separator();
    toolbar.add_action(copy_action);
    toolbar.add_action(paste_action);
    toolbar.add_action(undo_action);
    toolbar.add_action(redo_action);
    toolbar.add_separator();
    toolbar.add_action(zoom_in_action);
    toolbar.add_action(zoom_out_action);
    toolbar.add_action(reset_zoom_action);

    create_new_editor = [&](NonnullRefPtr<PixelPaint::Image> image) -> PixelPaint::ImageEditor& {
        auto& image_editor = tab_widget.add_tab<PixelPaint::ImageEditor>("Untitled", image);

        image_editor.on_active_layer_change = [&](auto* layer) {
            if (current_image_editor() != &image_editor)
                return;
            layer_list_widget.set_selected_layer(layer);
            layer_properties_widget.set_layer(layer);
        };

        image_editor.on_image_title_change = [&](auto const& title) {
            tab_widget.set_tab_title(image_editor, title);
        };

        auto& statusbar = *main_widget.find_descendant_of_type_named<GUI::Statusbar>("statusbar");
        image_editor.on_image_mouse_position_change = [&](auto const& mouse_position) {
            auto const& image_size = current_image_editor()->image().size();
            auto image_rectangle = Gfx::IntRect { 0, 0, image_size.width(), image_size.height() };
            if (image_rectangle.contains(mouse_position)) {
                statusbar.set_override_text(mouse_position.to_string());
            } else {
                statusbar.set_override_text({});
            }
        };

        image_editor.on_leave = [&]() {
            statusbar.set_override_text({});
        };

        image_editor.on_set_guide_visibility = [&](bool show_guides) {
            show_guides_action->set_checked(show_guides);
        };

        // NOTE: We invoke the above hook directly here to make sure the tab title is set up.
        image_editor.on_image_title_change(image->title());

        if (image->layer_count())
            image_editor.set_active_layer(&image->layer(0));

        tab_widget.set_active_widget(&image_editor);
        image_editor.set_focus(true);
        return image_editor;
    };

    tab_widget.on_tab_close_click = [&](auto& widget) {
        auto& image_editor = verify_cast<PixelPaint::ImageEditor>(widget);

        if (tab_widget.children().size() == 1) {
            layer_list_widget.set_image(nullptr);
            layer_properties_widget.set_layer(nullptr);
        }

        tab_widget.deferred_invoke([&] {
            tab_widget.remove_tab(image_editor);
        });
    };

    tab_widget.on_change = [&](auto& widget) {
        auto& image_editor = verify_cast<PixelPaint::ImageEditor>(widget);
        palette_widget.set_image_editor(image_editor);
        layer_list_widget.set_image(&image_editor.image());
        layer_properties_widget.set_layer(image_editor.active_layer());
        // FIXME: This is badly factored. It transfers tools from the previously active editor to the new one.
        toolbox.template for_each_tool([&](auto& tool) {
            if (tool.editor()) {
                tool.editor()->set_active_tool(nullptr);
                image_editor.set_active_tool(&tool);
            }
        });
        show_guides_action->set_checked(image_editor.guide_visibility());
    };

    if (Core::File::exists(file_to_edit_full_path)) {
        open_image_file(file_to_edit_full_path);
    } else {
        auto image = PixelPaint::Image::try_create_with_size({ 480, 360 });

        auto bg_layer = PixelPaint::Layer::try_create_with_size(*image, image->size(), "Background");
        VERIFY(bg_layer);
        image->add_layer(*bg_layer);
        bg_layer->bitmap().fill(Color::White);

        layer_list_widget.set_image(image);

        auto& editor = create_new_editor(*image);
        editor.set_active_layer(bg_layer);
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
