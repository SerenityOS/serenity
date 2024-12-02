/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021-2022, Mustafa Quraish <mustafa@serenityos.org>
 * Copyright (c) 2021-2022, Tobias Christiansen <tobyase@serenityos.org>
 * Copyright (c) 2022, Timothy Slater <tslater2006@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "MainWidget.h"
#include "CreateNewImageDialog.h"
#include "CreateNewLayerDialog.h"
#include "EditGuideDialog.h"
#include "FilterGallery.h"
#include "FilterParams.h"
#include "ImageMasking.h"
#include "LevelsDialog.h"
#include "ResizeImageDialog.h"
#include <AK/String.h>
#include <Applications/PixelPaint/PixelPaintWindowGML.h>
#include <LibConfig/Client.h>
#include <LibCore/Debounce.h>
#include <LibCore/MimeData.h>
#include <LibDesktop/Launcher.h>
#include <LibGUI/Application.h>
#include <LibGUI/Clipboard.h>
#include <LibGUI/Icon.h>
#include <LibGUI/ItemListModel.h>
#include <LibGUI/Menubar.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Toolbar.h>
#include <LibGUI/Window.h>
#include <LibGfx/Rect.h>

namespace PixelPaint {

IconBag g_icon_bag;

MainWidget::MainWidget()
    : Widget()
{
    load_from_gml(pixel_paint_window_gml).release_value_but_fixme_should_propagate_errors();

    m_toolbox = find_descendant_of_type_named<PixelPaint::ToolboxWidget>("toolbox");
    m_statusbar = *find_descendant_of_type_named<GUI::Statusbar>("statusbar");

    m_tab_widget = find_descendant_of_type_named<GUI::TabWidget>("tab_widget");

    m_palette_widget = *find_descendant_of_type_named<PixelPaint::PaletteWidget>("palette_widget");

    m_histogram_widget = *find_descendant_of_type_named<PixelPaint::HistogramWidget>("histogram_widget");
    m_vectorscope_widget = *find_descendant_of_type_named<PixelPaint::VectorscopeWidget>("vectorscope_widget");
    m_layer_list_widget = *find_descendant_of_type_named<PixelPaint::LayerListWidget>("layer_list_widget");
    m_layer_list_widget->on_layer_select = [&](auto* layer) {
        auto* editor = current_image_editor();
        VERIFY(editor);
        editor->set_active_layer(layer);
    };

    m_layer_properties_widget = *find_descendant_of_type_named<PixelPaint::LayerPropertiesWidget>("layer_properties_widget");
    m_tool_properties_widget = *find_descendant_of_type_named<PixelPaint::ToolPropertiesWidget>("tool_properties_widget");

    m_toolbox->on_tool_selection = [&](auto* tool) {
        auto* editor = current_image_editor();
        // Ignore tool selection changes if we don't have an editor yet, e.g. if PixelPaint is started with a path argument.
        if (editor) {
            editor->set_active_tool(tool);
            m_tool_properties_widget->set_active_tool(tool);
        }
    };

    m_tab_widget->on_middle_click = [&](auto& widget) {
        m_tab_widget->on_tab_close_click(widget);
    };

    m_tab_widget->on_tab_close_click = [&](auto& widget) {
        auto& image_editor = verify_cast<PixelPaint::ImageEditor>(widget);
        if (image_editor.request_close()) {
            m_tab_widget->deferred_invoke([&] {
                m_tab_widget->remove_tab(image_editor);
                if (m_tab_widget->children().size() == 0) {
                    m_histogram_widget->set_image(nullptr);
                    m_vectorscope_widget->set_image(nullptr);
                    m_layer_list_widget->set_image(nullptr);
                    m_layer_properties_widget->set_layer(nullptr);
                    m_palette_widget->set_image_editor(nullptr);
                    m_tool_properties_widget->set_enabled(false);
                    set_actions_enabled(false);
                    set_mask_actions_for_layer(nullptr);
                }
                update_window_modified();
            });
        }
    };

    m_tab_widget->on_change = [&](auto& widget) {
        auto& image_editor = verify_cast<PixelPaint::ImageEditor>(widget);
        m_palette_widget->set_image_editor(&image_editor);
        m_histogram_widget->set_image(&image_editor.image());
        m_vectorscope_widget->set_image(&image_editor.image());
        m_layer_list_widget->set_image(&image_editor.image());
        m_layer_properties_widget->set_layer(image_editor.active_layer());
        update_window_modified();
        if (auto* active_tool = m_toolbox->active_tool())
            image_editor.set_active_tool(active_tool);
        m_show_guides_action->set_checked(image_editor.guide_visibility());
        m_show_rulers_action->set_checked(image_editor.ruler_visibility());
        image_editor.on_scale_change(image_editor.scale());
        image_editor.undo_stack().on_state_change = [this] {
            image_editor_did_update_undo_stack();
        };
        // Ensure that our undo/redo actions are in sync with the current editor.
        image_editor_did_update_undo_stack();
        set_mask_actions_for_layer(image_editor.active_layer());
    };
}

void MainWidget::image_editor_did_update_undo_stack()
{
    auto* image_editor = current_image_editor();
    if (!image_editor) {
        m_undo_action->set_enabled(false);
        m_redo_action->set_enabled(false);
        return;
    }
    image_editor->update_modified();

    auto make_action_text = [](auto prefix, auto suffix) {
        StringBuilder builder;
        builder.append(prefix);
        if (suffix.has_value()) {
            builder.append(' ');
            builder.append(suffix.value());
        }
        return builder.to_byte_string();
    };

    auto& undo_stack = image_editor->undo_stack();
    m_undo_action->set_enabled(undo_stack.can_undo());
    m_redo_action->set_enabled(undo_stack.can_redo());

    m_undo_action->set_text(make_action_text("&Undo"sv, undo_stack.undo_action_text()));
    m_redo_action->set_text(make_action_text("&Redo"sv, undo_stack.redo_action_text()));
}

// Note: Update these together! v
static Vector<ByteString> const s_suggested_zoom_levels { "25%", "50%", "100%", "200%", "300%", "400%", "800%", "1600%", "Fit to width", "Fit to height", "Fit entire image" };
static constexpr int s_zoom_level_fit_width = 8;
static constexpr int s_zoom_level_fit_height = 9;
static constexpr int s_zoom_level_fit_image = 10;
// Note: Update these together! ^

ErrorOr<void> MainWidget::initialize_menubar(GUI::Window& window)
{
    auto file_menu = window.add_menu("&File"_string);

    m_new_image_action = GUI::Action::create(
        "&New Image...", { Mod_Ctrl, Key_N }, g_icon_bag.filetype_pixelpaint, [&](auto&) {
            auto dialog = PixelPaint::CreateNewImageDialog::construct(&window);
            if (dialog->exec() == GUI::Dialog::ExecResult::OK) {
                auto image_result = PixelPaint::Image::create_with_size(dialog->image_size());
                if (image_result.is_error()) {
                    GUI::MessageBox::show_error(&window, MUST(String::formatted("Failed to create image with size {}, error: {}", dialog->image_size(), image_result.release_error())));
                    return;
                }
                auto image = image_result.release_value();
                auto bg_layer_result = PixelPaint::Layer::create_with_size(*image, image->size(), "Background");
                if (bg_layer_result.is_error()) {
                    GUI::MessageBox::show_error(&window, MUST(String::formatted("Failed to create layer with size {}, error: {}", image->size(), bg_layer_result.release_error())));
                    return;
                }
                auto bg_layer = bg_layer_result.release_value();
                image->add_layer(*bg_layer);
                auto background_color = dialog->background_color();
                if (background_color != Gfx::Color::Transparent)
                    bg_layer->content_bitmap().fill(background_color);

                auto& editor = create_new_editor(*image);
                auto image_title = dialog->image_name().trim_whitespace();
                editor.set_title((image_title.is_empty() ? "Untitled"_string : String::from_byte_string(image_title)).release_value_but_fixme_should_propagate_errors());
                editor.set_unmodified();

                m_histogram_widget->set_image(image);
                m_vectorscope_widget->set_image(image);
                m_layer_list_widget->set_image(image);
                m_layer_list_widget->set_selected_layer(bg_layer);
            }
        });

    m_new_image_from_clipboard_action = GUI::Action::create(
        "&New Image from Clipboard", { Mod_Ctrl | Mod_Shift, Key_V }, g_icon_bag.new_clipboard, [&](auto&) {
            auto result = create_image_from_clipboard();
            if (result.is_error()) {
                GUI::MessageBox::show_error(&window, MUST(String::formatted("Failed to create image from clipboard: {}", result.release_error())));
            }
        });

    m_open_image_action = GUI::CommonActions::make_open_action([&](auto&) {
        auto image_files = GUI::FileTypeFilter::image_files();
        image_files.extensions->append("pp");
        auto response = FileSystemAccessClient::Client::the().open_file(&window, { .allowed_file_types = Vector { image_files, GUI::FileTypeFilter::all_files() } });
        if (response.is_error())
            return;
        open_image(response.release_value());
    });

    m_save_image_as_action = GUI::CommonActions::make_save_as_action([&](auto&) {
        auto* editor = current_image_editor();
        VERIFY(editor);
        editor->save_project_as();
    });

    m_save_image_action = GUI::CommonActions::make_save_action([&](auto&) {
        auto* editor = current_image_editor();
        VERIFY(editor);
        editor->save_project();
    });

    file_menu->add_action(*m_new_image_action);
    file_menu->add_action(*m_new_image_from_clipboard_action);
    file_menu->add_action(*m_open_image_action);
    file_menu->add_action(*m_save_image_action);
    file_menu->add_action(*m_save_image_as_action);

    m_export_submenu = file_menu->add_submenu("&Export"_string);

    m_export_submenu->add_action(
        GUI::Action::create(
            "As &BMP...", [&](auto&) {
                auto* editor = current_image_editor();
                VERIFY(editor);
                auto response = FileSystemAccessClient::Client::the().save_file(&window, editor->title().to_byte_string(), "bmp");
                if (response.is_error())
                    return;
                auto preserve_alpha_channel = GUI::MessageBox::show(&window, "Do you wish to preserve transparency?"sv, "Preserve transparency?"sv, GUI::MessageBox::Type::Question, GUI::MessageBox::InputType::YesNo);
                auto result = editor->image().export_bmp_to_file(response.value().release_stream(), preserve_alpha_channel == GUI::MessageBox::ExecResult::Yes);
                if (result.is_error())
                    GUI::MessageBox::show_error(&window, MUST(String::formatted("Export to BMP failed: {}", result.release_error())));
            }));

    m_export_submenu->add_action(
        GUI::Action::create(
            "As &PNG...", [&](auto&) {
                auto* editor = current_image_editor();
                VERIFY(editor);
                // TODO: fix bmp on line below?
                auto response = FileSystemAccessClient::Client::the().save_file(&window, editor->title().to_byte_string(), "png");
                if (response.is_error())
                    return;
                auto preserve_alpha_channel = GUI::MessageBox::show(&window, "Do you wish to preserve transparency?"sv, "Preserve transparency?"sv, GUI::MessageBox::Type::Question, GUI::MessageBox::InputType::YesNo);
                auto result = editor->image().export_png_to_file(response.value().release_stream(), preserve_alpha_channel == GUI::MessageBox::ExecResult::Yes);
                if (result.is_error())
                    GUI::MessageBox::show_error(&window, MUST(String::formatted("Export to PNG failed: {}", result.release_error())));
            }));

    m_export_submenu->add_action(
        GUI::Action::create(
            "As &QOI...", [&](auto&) {
                auto* editor = current_image_editor();
                VERIFY(editor);
                auto response = FileSystemAccessClient::Client::the().save_file(&window, editor->title().to_byte_string(), "qoi");
                if (response.is_error())
                    return;
                auto result = editor->image().export_qoi_to_file(response.value().release_stream());
                if (result.is_error())
                    GUI::MessageBox::show_error(&window, MUST(String::formatted("Export to QOI failed: {}", result.release_error())));
            }));

    m_export_submenu->set_icon(g_icon_bag.file_export);

    file_menu->add_separator();

    file_menu->add_recent_files_list([&](auto& action) {
        auto path = action.text();
        auto response = FileSystemAccessClient::Client::the().request_file_read_only_approved(&window, path);
        if (response.is_error())
            return;
        open_image(response.release_value());
    });

    m_close_image_action = GUI::Action::create("&Close Image", { Mod_Ctrl, Key_W }, g_icon_bag.close_image, [&](auto&) {
        auto* active_widget = m_tab_widget->active_widget();
        VERIFY(active_widget);
        m_tab_widget->on_tab_close_click(*active_widget);
    });

    file_menu->add_action(*m_close_image_action);

    file_menu->add_action(GUI::CommonActions::make_quit_action(
        [this](auto&) {
            if (request_close())
                GUI::Application::the()->quit();
        },
        GUI::CommonActions::QuitAltShortcut::None));

    m_edit_menu = window.add_menu("&Edit"_string);

    m_cut_action = GUI::CommonActions::make_cut_action([&](auto&) {
        auto* editor = current_image_editor();
        VERIFY(editor);

        if (!editor->active_layer()) {
            dbgln("Cannot cut with no active layer selected");
            return;
        }
        auto bitmap = editor->active_layer()->copy_bitmap(editor->image().selection());
        if (!bitmap) {
            dbgln("copy_bitmap() from Layer failed");
            return;
        }
        GUI::Clipboard::the().set_bitmap(*bitmap);
        editor->active_layer()->erase_selection(editor->image().selection());
    });

    m_copy_action = GUI::CommonActions::make_copy_action([&](auto&) {
        auto* editor = current_image_editor();
        VERIFY(editor);

        if (!editor->active_layer()) {
            dbgln("Cannot copy with no active layer selected");
            return;
        }
        auto bitmap = editor->active_layer()->copy_bitmap(editor->image().selection());
        if (!bitmap) {
            dbgln("copy_bitmap() from Layer failed");
            return;
        }
        auto layer_rect = editor->active_layer()->relative_rect();
        HashMap<ByteString, ByteString> layer_metadata;
        layer_metadata.set("pixelpaint-layer-x", ByteString::number(layer_rect.x()));
        layer_metadata.set("pixelpaint-layer-y", ByteString::number(layer_rect.y()));

        GUI::Clipboard::the().set_bitmap(*bitmap, layer_metadata);
    });

    m_copy_merged_action = GUI::Action::create(
        "Copy &Merged", { Mod_Ctrl | Mod_Shift, Key_C }, g_icon_bag.edit_copy, [&](auto&) {
            auto* editor = current_image_editor();
            VERIFY(editor);

            auto bitmap = editor->image().copy_bitmap(editor->image().selection());
            if (!bitmap) {
                dbgln("copy_bitmap() from Image failed");
                return;
            }
            GUI::Clipboard::the().set_bitmap(*bitmap);
        });

    m_paste_action = GUI::CommonActions::make_paste_action([&](auto&) {
        auto* editor = current_image_editor();
        if (!editor) {
            auto result = create_image_from_clipboard();
            if (result.is_error()) {
                GUI::MessageBox::show_error(&window, MUST(String::formatted("Failed to create image from clipboard: {}", result.release_error())));
            }
            return;
        }

        auto data_and_type = GUI::Clipboard::the().fetch_data_and_type();
        auto bitmap = data_and_type.as_bitmap();
        if (!bitmap)
            return;

        auto layer_result = PixelPaint::Layer::create_with_bitmap(editor->image(), *bitmap, "Pasted layer");
        if (layer_result.is_error()) {
            GUI::MessageBox::show_error(&window, MUST(String::formatted("Could not create bitmap when pasting: {}", layer_result.release_error())));
            return;
        }
        auto layer = layer_result.release_value();

        auto layer_x_position = data_and_type.metadata.get("pixelpaint-layer-x");
        auto layer_y_position = data_and_type.metadata.get("pixelpaint-layer-y");
        if (layer_x_position.has_value() && layer_y_position.has_value()) {
            auto x = layer_x_position.value().to_number<int>();
            auto y = layer_y_position.value().to_number<int>();
            if (x.has_value() && x.value()) {
                auto pasted_layer_location = Gfx::IntPoint { x.value(), y.value() };

                auto pasted_layer_frame_rect = editor->content_to_frame_rect({ pasted_layer_location, layer->size() }).to_type<int>();
                // If the pasted layer is entirely outside the canvas bounds, default to the top left.
                if (!editor->content_rect().intersects(pasted_layer_frame_rect))
                    pasted_layer_location = {};

                layer->set_location(pasted_layer_location);
                // Ensure the pasted layer is visible to the user.
                if (!editor->frame_inner_rect().intersects(pasted_layer_frame_rect))
                    editor->fit_content_to_view();
            }
        }

        editor->image().add_layer(*layer);
        editor->set_active_layer(layer);
        editor->image().selection().clear();
    });
    GUI::Clipboard::the().on_change = [&](auto& mime_type) {
        m_paste_action->set_enabled(mime_type == "image/x-serenityos");
    };
    m_paste_action->set_enabled(GUI::Clipboard::the().fetch_mime_type() == "image/x-serenityos");

    m_undo_action = GUI::CommonActions::make_undo_action([&](auto&) {
        if (auto* editor = current_image_editor())
            editor->undo();
    });

    m_redo_action = GUI::CommonActions::make_redo_action([&](auto&) {
        auto* editor = current_image_editor();
        VERIFY(editor);
        editor->redo();
    });

    m_edit_menu->add_action(*m_undo_action);
    m_edit_menu->add_action(*m_redo_action);
    m_edit_menu->add_separator();
    m_edit_menu->add_action(*m_cut_action);
    m_edit_menu->add_action(*m_copy_action);
    m_edit_menu->add_action(*m_copy_merged_action);
    m_edit_menu->add_action(*m_paste_action);
    m_edit_menu->add_separator();

    m_edit_menu->add_action(GUI::CommonActions::make_select_all_action([&](auto&) {
        auto* editor = current_image_editor();
        VERIFY(editor);
        if (!editor->active_layer())
            return;
        auto layer_rect = editor->active_layer()->relative_rect();
        editor->image().selection().merge(layer_rect.intersected(editor->image().rect()), PixelPaint::Selection::MergeMode::Set);
        editor->did_complete_action("Select All"sv);
    }));
    m_edit_menu->add_action(GUI::Action::create(
        "Clear &Selection", g_icon_bag.clear_selection, [&](auto&) {
            auto* editor = current_image_editor();
            VERIFY(editor);
            editor->image().selection().clear();
            editor->did_complete_action("Clear Selection"sv);
        }));
    m_edit_menu->add_action(GUI::Action::create(
        "&Invert Selection", g_icon_bag.invert_selection, [&](auto&) {
            auto* editor = current_image_editor();
            VERIFY(editor);
            editor->image().selection().invert();
            editor->did_complete_action("Invert Selection"sv);
        }));

    m_edit_menu->add_separator();
    m_edit_menu->add_action(GUI::Action::create(
        "S&wap Colors", { Mod_None, Key_X }, g_icon_bag.swap_colors, [&](auto&) {
            auto* editor = current_image_editor();
            VERIFY(editor);
            auto old_primary_color = editor->primary_color();
            editor->set_primary_color(editor->secondary_color());
            editor->set_secondary_color(old_primary_color);
        }));
    m_edit_menu->add_action(GUI::Action::create(
        "&Default Colors", { Mod_None, Key_D }, g_icon_bag.default_colors, [&](auto&) {
            auto* editor = current_image_editor();
            VERIFY(editor);
            editor->set_primary_color(Color::Black);
            editor->set_secondary_color(Color::White);
        }));
    m_edit_menu->add_action(GUI::Action::create(
        "&Load Color Palette...", g_icon_bag.load_color_palette, [&](auto&) {
            FileSystemAccessClient::OpenFileOptions options {
                .window_title = "Load Color Palette"sv,
                .allowed_file_types = Vector {
                    { "Palette Files", { { "palette" } } },
                    GUI::FileTypeFilter::all_files(),
                },
            };
            auto response = FileSystemAccessClient::Client::the().open_file(&window, options);
            if (response.is_error())
                return;

            auto result = PixelPaint::PaletteWidget::load_palette_file(response.release_value().release_stream());
            if (result.is_error()) {
                GUI::MessageBox::show_error(&window, MUST(String::formatted("Loading color palette failed: {}", result.release_error())));
                return;
            }

            m_palette_widget->display_color_list(result.value());
        }));
    m_edit_menu->add_action(GUI::Action::create(
        "Sa&ve Color Palette...", g_icon_bag.save_color_palette, [&](auto&) {
            auto response = FileSystemAccessClient::Client::the().save_file(&window, "untitled", "palette");
            if (response.is_error())
                return;

            auto result = PixelPaint::PaletteWidget::save_palette_file(m_palette_widget->colors(), response.release_value().release_stream());
            if (result.is_error())
                GUI::MessageBox::show_error(&window, MUST(String::formatted("Writing color palette failed: {}", result.release_error())));
        }));

    m_view_menu = window.add_menu("&View"_string);

    m_zoom_in_action = GUI::CommonActions::make_zoom_in_action(
        [&](auto&) {
            auto* editor = current_image_editor();
            VERIFY(editor);
            editor->scale_by(0.1f);
        });

    m_zoom_out_action = GUI::CommonActions::make_zoom_out_action(
        [&](auto&) {
            auto* editor = current_image_editor();
            VERIFY(editor);
            editor->scale_by(-0.1f);
        });

    m_reset_zoom_action = GUI::CommonActions::make_reset_zoom_action(
        [&](auto&) {
            if (auto* editor = current_image_editor())
                editor->reset_view();
        });

    m_add_guide_action = GUI::Action::create(
        "&Add Guide...", g_icon_bag.add_guide, [&](auto&) {
            auto dialog = PixelPaint::EditGuideDialog::construct(&window);
            if (dialog->exec() != GUI::Dialog::ExecResult::OK)
                return;
            auto* editor = current_image_editor();
            VERIFY(editor);
            auto offset = dialog->offset_as_pixel(*editor);
            if (!offset.has_value())
                return;
            editor->add_guide(PixelPaint::Guide::construct(dialog->orientation(), offset.value()));
        });

    // Save this so other methods can use it
    m_show_guides_action = GUI::Action::create_checkable(
        "Show &Guides", [&](auto& action) {
            Config::write_bool("PixelPaint"sv, "Guides"sv, "Show"sv, action.is_checked());
            auto* editor = current_image_editor();
            VERIFY(editor);
            editor->set_guide_visibility(action.is_checked());
        });
    m_show_guides_action->set_checked(Config::read_bool("PixelPaint"sv, "Guides"sv, "Show"sv, true));

    m_view_menu->add_action(*m_zoom_in_action);
    m_view_menu->add_action(*m_zoom_out_action);
    m_view_menu->add_action(*m_reset_zoom_action);
    m_view_menu->add_action(GUI::Action::create(
        "Fit Image To &View", g_icon_bag.fit_image_to_view, [&](auto&) {
            auto* editor = current_image_editor();
            VERIFY(editor);
            editor->fit_image_to_view();
        }));
    m_view_menu->add_separator();
    m_view_menu->add_action(*m_add_guide_action);
    m_view_menu->add_action(*m_show_guides_action);

    m_view_menu->add_action(GUI::Action::create(
        "&Clear Guides", g_icon_bag.clear_guides, [&](auto&) {
            auto* editor = current_image_editor();
            VERIFY(editor);
            editor->clear_guides();
        }));
    m_view_menu->add_separator();

    auto show_pixel_grid_action = GUI::Action::create_checkable(
        "Show &Pixel Grid", [&](auto& action) {
            Config::write_bool("PixelPaint"sv, "PixelGrid"sv, "Show"sv, action.is_checked());
            auto* editor = current_image_editor();
            VERIFY(editor);
            editor->set_pixel_grid_visibility(action.is_checked());
        });
    show_pixel_grid_action->set_checked(Config::read_bool("PixelPaint"sv, "PixelGrid"sv, "Show"sv, true));
    m_view_menu->add_action(*show_pixel_grid_action);

    m_show_rulers_action = GUI::Action::create_checkable(
        "Show R&ulers", { Mod_Ctrl, Key_R }, [&](auto& action) {
            Config::write_bool("PixelPaint"sv, "Rulers"sv, "Show"sv, action.is_checked());
            auto* editor = current_image_editor();
            VERIFY(editor);
            editor->set_ruler_visibility(action.is_checked());
        });
    m_show_rulers_action->set_checked(Config::read_bool("PixelPaint"sv, "Rulers"sv, "Show"sv, true));
    m_view_menu->add_action(*m_show_rulers_action);

    m_show_active_layer_boundary_action = GUI::Action::create_checkable(
        "Show Active Layer &Boundary", [&](auto& action) {
            Config::write_bool("PixelPaint"sv, "ImageEditor"sv, "ShowActiveLayerBoundary"sv, action.is_checked());
            auto* editor = current_image_editor();
            VERIFY(editor);
            editor->set_show_active_layer_boundary(action.is_checked());
        });
    m_show_active_layer_boundary_action->set_checked(Config::read_bool("PixelPaint"sv, "ImageEditor"sv, "ShowActiveLayerBoundary"sv, true));
    m_view_menu->add_action(*m_show_active_layer_boundary_action);

    m_view_menu->add_separator();

    auto histogram_action = GUI::Action::create_checkable("&Histogram", [&](auto& action) {
        m_histogram_widget->set_scope_visibility(action.is_checked());
    });
    histogram_action->set_checked(m_histogram_widget->read_visibility_from_configuration());
    m_histogram_widget->set_scope_visibility(histogram_action->is_checked());

    auto vectorscope_action = GUI::Action::create_checkable("&Vectorscope", [&](auto& action) {
        m_vectorscope_widget->set_scope_visibility(action.is_checked());
    });
    vectorscope_action->set_checked(m_vectorscope_widget->read_visibility_from_configuration());
    m_vectorscope_widget->set_scope_visibility(vectorscope_action->is_checked());

    auto scopes_menu = m_view_menu->add_submenu("&Scopes"_string);
    scopes_menu->add_action(histogram_action);
    scopes_menu->add_action(vectorscope_action);

    m_view_menu->add_separator();
    m_view_menu->add_action(GUI::CommonActions::make_fullscreen_action([&](auto&) {
        window.set_fullscreen(!window.is_fullscreen());
    }));

    m_tool_menu = window.add_menu("&Tool"_string);
    m_toolbox->for_each_tool([&](auto& tool) {
        if (tool.action())
            m_tool_menu->add_action(*tool.action());
        return IterationDecision::Continue;
    });

    m_image_menu = window.add_menu("&Image"_string);
    m_image_menu->add_action(GUI::Action::create(
        "Flip Image &Vertically", g_icon_bag.edit_flip_vertical, [&](auto&) {
            auto* editor = current_image_editor();
            VERIFY(editor);
            auto image_flip_or_error = editor->image().flip(Gfx::Orientation::Vertical);
            if (image_flip_or_error.is_error()) {
                GUI::MessageBox::show_error(&window, MUST(String::formatted("Failed to flip image: {}", image_flip_or_error.release_error())));
                return;
            }
            editor->did_complete_action("Flip Image Vertically"sv);
        }));
    m_image_menu->add_action(GUI::Action::create(
        "Flip Image &Horizontally", g_icon_bag.edit_flip_horizontal, [&](auto&) {
            auto* editor = current_image_editor();
            VERIFY(editor);
            auto image_flip_or_error = editor->image().flip(Gfx::Orientation::Horizontal);
            if (image_flip_or_error.is_error()) {
                GUI::MessageBox::show_error(&window, MUST(String::formatted("Failed to flip image: {}", image_flip_or_error.release_error())));
                return;
            }
            editor->did_complete_action("Flip Image Horizontally"sv);
        }));
    m_image_menu->add_separator();

    m_image_menu->add_action(GUI::Action::create("Rotate Image &Counterclockwise", { Mod_Ctrl | Mod_Shift, Key_LessThan }, TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/edit-rotate-ccw.png"sv)),
        [&](auto&) {
            auto* editor = current_image_editor();
            VERIFY(editor);
            auto image_rotate_or_error = editor->image().rotate(Gfx::RotationDirection::CounterClockwise);
            if (image_rotate_or_error.is_error()) {
                GUI::MessageBox::show_error(&window, MUST(String::formatted("Failed to rotate image: {}", image_rotate_or_error.release_error())));
                return;
            }
            editor->did_complete_action("Rotate Image Counterclockwise"sv);
        }));

    m_image_menu->add_action(GUI::Action::create("Rotate Image Clock&wise", { Mod_Ctrl | Mod_Shift, Key_GreaterThan }, TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/edit-rotate-cw.png"sv)),
        [&](auto&) {
            auto* editor = current_image_editor();
            VERIFY(editor);
            auto image_rotate_or_error = editor->image().rotate(Gfx::RotationDirection::Clockwise);
            if (image_rotate_or_error.is_error()) {
                GUI::MessageBox::show_error(&window, MUST(String::formatted("Failed to rotate image: {}", image_rotate_or_error.release_error())));
                return;
            }
            editor->did_complete_action("Rotate Image Clockwise"sv);
        }));
    m_image_menu->add_separator();
    m_image_menu->add_action(GUI::Action::create(
        "&Resize Image...", { Mod_Ctrl | Mod_Shift, Key_R }, g_icon_bag.resize_image, [&](auto&) {
            auto* editor = current_image_editor();
            VERIFY(editor);
            auto dialog = PixelPaint::ResizeImageDialog::construct(editor->image().size(), &window);
            if (dialog->exec() == GUI::Dialog::ExecResult::OK) {
                auto image_resize_or_error = editor->image().resize(dialog->desired_size(), dialog->scaling_mode());
                if (image_resize_or_error.is_error()) {
                    GUI::MessageBox::show_error(&window, MUST(String::formatted("Failed to resize image: {}", image_resize_or_error.release_error())));
                    return;
                }
                // FIXME: We should ensure the selection is within the bounds of the image here.
                editor->did_complete_action("Resize Image"sv);
            }
        }));
    m_crop_image_to_selection_action = GUI::Action::create(
        "&Crop Image to Selection", g_icon_bag.crop, [&](auto&) {
            auto* editor = current_image_editor();
            VERIFY(editor);
            if (editor->image().selection().is_empty())
                return;
            auto crop_rect = editor->image().rect().intersected(editor->image().selection().bounding_rect());
            // FIXME: It is only possible to hit this condition, as transforming the image (crop, rotate etc.), does not update the selection.
            //        We should ensure that image transformations also transform the selection, so that its relative size and position are maintained.
            if (crop_rect.is_empty())
                return;
            auto image_crop_or_error = editor->image().crop(crop_rect);
            if (image_crop_or_error.is_error()) {
                GUI::MessageBox::show_error(&window, MUST(String::formatted("Failed to crop image: {}", image_crop_or_error.release_error())));
                return;
            }
            editor->image().selection().clear();
            editor->did_complete_action("Crop Image to Selection"sv);
        });
    m_image_menu->add_action(*m_crop_image_to_selection_action);

    m_image_menu->add_action(GUI::Action::create(
        "&Crop Image to Content", g_icon_bag.crop, [&](auto&) {
            auto* editor = current_image_editor();
            VERIFY(editor);

            auto content_bounding_rect = editor->image().nonempty_content_bounding_rect();
            if (!content_bounding_rect.has_value())
                return;

            auto image_crop_or_error = editor->image().crop(content_bounding_rect.value());
            if (image_crop_or_error.is_error()) {
                GUI::MessageBox::show_error(&window, MUST(String::formatted("Failed to crop image: {}", image_crop_or_error.release_error())));
                return;
            }
            editor->did_complete_action("Crop Image to Content"sv);
        }));

    m_layer_menu = window.add_menu("&Layer"_string);

    m_layer_menu->on_visibility_change = [this](bool visible) {
        if (!visible)
            return;

        auto* editor = current_image_editor();
        bool image_has_selection = editor && editor->active_layer() && !editor->active_layer()->image().selection().is_empty();

        m_layer_via_copy->set_enabled(image_has_selection);
        m_layer_via_cut->set_enabled(image_has_selection);
    };

    m_layer_menu->add_action(GUI::Action::create(
        "New &Layer...", { Mod_Ctrl | Mod_Shift, Key_N }, g_icon_bag.new_layer, [&](auto&) {
            auto* editor = current_image_editor();
            VERIFY(editor);
            auto dialog = PixelPaint::CreateNewLayerDialog::construct(editor->image().size(), &window);
            if (dialog->exec() == GUI::Dialog::ExecResult::OK) {
                auto layer_or_error = PixelPaint::Layer::create_with_size(editor->image(), dialog->layer_size(), dialog->layer_name());
                if (layer_or_error.is_error()) {
                    GUI::MessageBox::show_error(&window, MUST(String::formatted("Unable to create layer with size {}", dialog->size())));
                    return;
                }
                editor->image().add_layer(layer_or_error.release_value());
                editor->layers_did_change();
                editor->did_complete_action("New Layer"sv);
                m_layer_list_widget->select_top_layer();
            }
        }));

    m_layer_menu->add_action(GUI::Action::create(
        "Duplicate Layer", { Mod_Ctrl | Mod_Shift, Key_D }, g_icon_bag.new_layer, [&](auto&) {
            auto* editor = current_image_editor();
            VERIFY(editor);
            auto* active_layer = editor->active_layer();
            if (!active_layer)
                return;

            auto layer_index = editor->image().index_of(*active_layer);
            auto new_layer_name = editor->generate_unique_layer_name(active_layer->name());
            auto duplicated_layer_or_error = active_layer->duplicate(new_layer_name);
            if (duplicated_layer_or_error.is_error()) {
                GUI::MessageBox::show_error(&window, MUST(String::formatted("Unable to create duplicate layer: {}", duplicated_layer_or_error.release_error())));
                return;
            }
            auto duplicated_layer = duplicated_layer_or_error.release_value();
            editor->image().insert_layer(duplicated_layer, layer_index + 1);
            editor->image().select_layer(duplicated_layer);
            editor->layers_did_change();
            editor->did_complete_action("Duplicate Layer"sv);
        }));

    m_layer_via_copy = GUI::Action::create(
        "Layer via Copy", { Mod_Ctrl | Mod_Shift, Key_C }, g_icon_bag.new_layer, [&](auto&) {
            auto add_layer_success = current_image_editor()->add_new_layer_from_selection();
            if (add_layer_success.is_error()) {
                GUI::MessageBox::show_error(&window, MUST(String::formatted("{}", add_layer_success.release_error())));
                return;
            }
            current_image_editor()->did_complete_action("New Layer via Copy"sv);
            m_layer_list_widget->select_top_layer();
        });
    m_layer_menu->add_action(*m_layer_via_copy);

    m_layer_via_cut = GUI::Action::create(
        "Layer via Cut", { Mod_Ctrl | Mod_Shift, Key_X }, g_icon_bag.new_layer, [&](auto&) {
            auto add_layer_success = current_image_editor()->add_new_layer_from_selection();
            if (add_layer_success.is_error()) {
                GUI::MessageBox::show_error(&window, MUST(String::formatted("{}", add_layer_success.release_error())));
                return;
            }
            current_image_editor()->active_layer()->erase_selection(current_image_editor()->image().selection());
            current_image_editor()->did_complete_action("New Layer via Cut"sv);
            m_layer_list_widget->select_top_layer();
        });
    m_layer_menu->add_action(*m_layer_via_cut);

    m_layer_menu->add_separator();

    auto create_layer_mask_callback = [&](auto const& action_name, Function<void(Layer*)> mask_function) {
        return [&, mask_function = move(mask_function)](GUI::Action&) {
            auto* editor = current_image_editor();
            VERIFY(editor);
            auto* active_layer = editor->active_layer();
            if (!active_layer)
                return;

            mask_function(active_layer);

            editor->did_complete_action(action_name);
            editor->update();
            m_layer_list_widget->repaint();
            set_mask_actions_for_layer(active_layer);
        };
    };

    auto mask_submenu = m_layer_menu->add_submenu("&Masks"_string);

    m_add_mask_action = GUI::Action::create(
        "Add M&ask", { Mod_Ctrl | Mod_Shift, Key_M }, g_icon_bag.add_mask, create_layer_mask_callback("Add Mask", [&](Layer* active_layer) {
            VERIFY(!active_layer->is_masked());
            if (auto maybe_error = active_layer->create_mask(Layer::MaskType::BasicMask); maybe_error.is_error())
                GUI::MessageBox::show_error(&window, MUST(String::formatted("Failed to create layer mask: {}", maybe_error.release_error())));
        }));
    mask_submenu->add_action(*m_add_mask_action);

    m_add_editing_mask_action = GUI::Action::create(
        "Add E&diting Mask", { Mod_Ctrl | Mod_Alt, Key_E }, g_icon_bag.add_mask, create_layer_mask_callback("Add Editing Mask", [&](Layer* active_layer) {
            VERIFY(!active_layer->is_masked());
            if (auto maybe_error = active_layer->create_mask(Layer::MaskType::EditingMask); maybe_error.is_error())
                GUI::MessageBox::show_error(&window, MUST(String::formatted("Failed to create layer mask: {}", maybe_error.release_error())));
        }));
    mask_submenu->add_action(*m_add_editing_mask_action);

    m_delete_mask_action = GUI::Action::create(
        "Delete Mask", create_layer_mask_callback("Delete Mask", [&](Layer* active_layer) {
            VERIFY(active_layer->is_masked());
            active_layer->delete_mask();
        }));
    mask_submenu->add_action(*m_delete_mask_action);

    m_apply_mask_action = GUI::Action::create(
        "Apply Mask", create_layer_mask_callback("Apply Mask", [&](Layer* active_layer) {
            VERIFY(active_layer->is_masked());
            active_layer->apply_mask();
        }));
    mask_submenu->add_action(*m_apply_mask_action);

    m_invert_mask_action = GUI::Action::create(
        "Invert Mask", create_layer_mask_callback("Invert Mask", [&](Layer* active_layer) {
            VERIFY(active_layer->is_masked());
            active_layer->invert_mask();
        }));
    mask_submenu->add_action(*m_invert_mask_action);

    m_clear_mask_action = GUI::Action::create(
        "Clear Mask", create_layer_mask_callback("Clear Mask", [&](Layer* active_layer) {
            VERIFY(active_layer->is_masked());
            active_layer->clear_mask();
        }));
    mask_submenu->add_action(*m_clear_mask_action);

    m_toggle_mask_visibility_action = GUI::Action::create_checkable(
        "Show Mask", [&](auto&) {
            auto* editor = current_image_editor();
            VERIFY(editor);
            if (!editor->active_layer())
                return;

            VERIFY(editor->active_layer()->is_masked());
            editor->active_layer()->set_mask_visibility(m_toggle_mask_visibility_action->is_checked());
            editor->update();
        });

    mask_submenu->add_action(*m_toggle_mask_visibility_action);

    m_open_luminosity_masking_action = GUI::Action::create(
        "Luminosity Masking", [&](auto&) {
            auto* editor = current_image_editor();
            VERIFY(editor);
            if (!editor->active_layer())
                return;
            VERIFY(editor->active_layer()->mask_type() == Layer::MaskType::EditingMask);

            PixelPaint::ImageMasking::construct(&window, editor, ImageMasking::MaskingType::Luminosity)->exec();
            m_layer_list_widget->repaint();
        });

    mask_submenu->add_action(*m_open_luminosity_masking_action);

    m_open_color_masking_action = GUI::Action::create(
        "Color Masking", [&](auto&) {
            auto* editor = current_image_editor();
            VERIFY(editor);
            if (!editor->active_layer())
                return;
            VERIFY(editor->active_layer()->mask_type() == Layer::MaskType::EditingMask);

            PixelPaint::ImageMasking::construct(&window, editor, ImageMasking::MaskingType::Color)->exec();
            m_layer_list_widget->repaint();
        });

    mask_submenu->add_action(*m_open_color_masking_action);

    m_layer_menu->add_separator();

    m_layer_menu->add_action(GUI::Action::create(
        "Select &Previous Layer", { 0, Key_PageUp }, g_icon_bag.previous_layer, [&](auto&) {
            m_layer_list_widget->cycle_through_selection(1);
        }));
    m_layer_menu->add_action(GUI::Action::create(
        "Select &Next Layer", { 0, Key_PageDown }, g_icon_bag.next_layer, [&](auto&) {
            m_layer_list_widget->cycle_through_selection(-1);
        }));
    m_layer_menu->add_action(GUI::Action::create(
        "Select &Top Layer", { 0, Key_Home }, g_icon_bag.top_layer, [&](auto&) {
            m_layer_list_widget->select_top_layer();
        }));
    m_layer_menu->add_action(GUI::Action::create(
        "Select B&ottom Layer", { 0, Key_End }, g_icon_bag.bottom_layer, [&](auto&) {
            m_layer_list_widget->select_bottom_layer();
        }));
    m_layer_menu->add_separator();
    m_layer_menu->add_action(GUI::CommonActions::make_move_to_front_action(
        [&](auto&) {
            auto* editor = current_image_editor();
            VERIFY(editor);
            auto active_layer = editor->active_layer();
            if (!active_layer)
                return;
            editor->image().move_layer_to_front(*active_layer);
            editor->layers_did_change();
        }));
    m_layer_menu->add_action(GUI::CommonActions::make_move_to_back_action(
        [&](auto&) {
            auto* editor = current_image_editor();
            VERIFY(editor);
            auto active_layer = editor->active_layer();
            if (!active_layer)
                return;
            editor->image().move_layer_to_back(*active_layer);
            editor->layers_did_change();
        }));
    m_layer_menu->add_separator();
    m_layer_menu->add_action(GUI::Action::create(
        "Move Active Layer &Up", { Mod_Ctrl, Key_PageUp }, g_icon_bag.active_layer_up, [&](auto&) {
            auto* editor = current_image_editor();
            VERIFY(editor);
            auto active_layer = editor->active_layer();
            if (!active_layer)
                return;
            editor->image().move_layer_up(*active_layer);
        }));
    m_layer_menu->add_action(GUI::Action::create(
        "Move Active Layer &Down", { Mod_Ctrl, Key_PageDown }, g_icon_bag.active_layer_down, [&](auto&) {
            auto* editor = current_image_editor();
            VERIFY(editor);
            auto active_layer = editor->active_layer();
            if (!active_layer)
                return;
            editor->image().move_layer_down(*active_layer);
        }));
    m_layer_menu->add_separator();
    m_layer_menu->add_action(GUI::Action::create(
        "&Remove Active Layer", { Mod_Ctrl, Key_D }, g_icon_bag.delete_layer, [&](auto&) {
            auto* editor = current_image_editor();
            VERIFY(editor);
            auto active_layer = editor->active_layer();
            if (!active_layer)
                return;

            auto active_layer_index = editor->image().index_of(*active_layer);
            editor->image().remove_layer(*active_layer);

            if (editor->image().layer_count()) {
                auto& next_active_layer = editor->image().layer(active_layer_index > 0 ? active_layer_index - 1 : 0);
                editor->set_active_layer(&next_active_layer);
            } else {
                auto layer_result = PixelPaint::Layer::create_with_size(editor->image(), editor->image().size(), "Background");
                if (layer_result.is_error()) {
                    GUI::MessageBox::show_error(&window, MUST(String::formatted("Failed to create layer with size {}, error: {}", editor->image().size(), layer_result.release_error())));
                    return;
                }
                auto layer = layer_result.release_value();
                editor->image().add_layer(move(layer));
                editor->layers_did_change();
                m_layer_list_widget->select_top_layer();
            }
        }));

    m_layer_list_widget->on_context_menu_request = [&](auto& event) {
        m_layer_menu->popup(event.screen_position());
    };
    m_layer_menu->add_separator();
    m_layer_menu->add_action(GUI::Action::create(
        "Fl&atten Image", { Mod_Ctrl, Key_F }, g_icon_bag.flatten_image, [&](auto&) {
            auto* editor = current_image_editor();
            VERIFY(editor);
            if (auto maybe_error = editor->image().flatten_all_layers(); maybe_error.is_error()) {
                GUI::MessageBox::show_error(&window, MUST(String::formatted("Failed to flatten all layers: {}", maybe_error.release_error())));
                return;
            }
            editor->did_complete_action("Flatten Image"sv);
        }));

    m_layer_menu->add_action(GUI::Action::create(
        "&Merge Visible", { Mod_Ctrl, Key_M }, g_icon_bag.merge_visible, [&](auto&) {
            auto* editor = current_image_editor();
            VERIFY(editor);
            if (auto maybe_error = editor->image().merge_visible_layers(); maybe_error.is_error()) {
                GUI::MessageBox::show_error(&window, MUST(String::formatted("Failed to merge visible layers: {}", maybe_error.release_error())));
                return;
            }
            editor->did_complete_action("Merge Visible"sv);
        }));

    m_layer_menu->add_action(GUI::Action::create(
        "Merge &Active Layer Up", g_icon_bag.merge_active_layer_up, [&](auto&) {
            auto* editor = current_image_editor();
            VERIFY(editor);
            auto active_layer = editor->active_layer();
            if (!active_layer)
                return;

            if (auto maybe_error = editor->image().merge_active_layer_up(*active_layer); maybe_error.is_error()) {
                GUI::MessageBox::show_error(&window, MUST(String::formatted("Failed to merge active layer up: {}", maybe_error.release_error())));
                return;
            }
            editor->did_complete_action("Merge Active Layer Up"sv);
        }));

    m_layer_menu->add_action(GUI::Action::create(
        "M&erge Active Layer Down", { Mod_Ctrl, Key_E }, g_icon_bag.merge_active_layer_down, [&](auto&) {
            auto* editor = current_image_editor();
            VERIFY(editor);
            auto active_layer = editor->active_layer();
            if (!active_layer)
                return;

            if (auto maybe_error = editor->image().merge_active_layer_down(*active_layer); maybe_error.is_error()) {
                GUI::MessageBox::show_error(&window, MUST(String::formatted("Failed to merge active layer down: {}", maybe_error.release_error())));
                return;
            }
            editor->did_complete_action("Merge Active Layer Down"sv);
        }));

    m_layer_menu->add_separator();
    m_layer_menu->add_action(GUI::Action::create(
        "Flip Layer &Vertically", g_icon_bag.edit_flip_vertical, [&](auto&) {
            auto* editor = current_image_editor();
            VERIFY(editor);
            auto active_layer = editor->active_layer();
            if (!active_layer)
                return;
            auto layer_flip_or_error = active_layer->flip(Gfx::Orientation::Vertical);
            if (layer_flip_or_error.is_error()) {
                GUI::MessageBox::show_error(&window, MUST(String::formatted("Failed to flip layer: {}", layer_flip_or_error.release_error())));
                return;
            }
            editor->did_complete_action("Flip Layer Vertically"sv);
        }));
    m_layer_menu->add_action(GUI::Action::create(
        "Flip Layer &Horizontally", g_icon_bag.edit_flip_horizontal, [&](auto&) {
            auto* editor = current_image_editor();
            VERIFY(editor);
            auto active_layer = editor->active_layer();
            if (!active_layer)
                return;
            auto layer_flip_or_error = active_layer->flip(Gfx::Orientation::Horizontal);
            if (layer_flip_or_error.is_error()) {
                GUI::MessageBox::show_error(&window, MUST(String::formatted("Failed to flip layer: {}", layer_flip_or_error.release_error())));
                return;
            }
            editor->did_complete_action("Flip Layer Horizontally"sv);
        }));
    m_layer_menu->add_separator();

    m_layer_menu->add_action(GUI::Action::create("Rotate Layer &Counterclockwise", TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/edit-rotate-ccw.png"sv)),
        [&](auto&) {
            auto* editor = current_image_editor();
            VERIFY(editor);
            auto active_layer = editor->active_layer();
            if (!active_layer)
                return;
            auto layer_rotate_or_error = active_layer->rotate(Gfx::RotationDirection::CounterClockwise);
            if (layer_rotate_or_error.is_error()) {
                GUI::MessageBox::show_error(&window, MUST(String::formatted("Failed to rotate layer: {}", layer_rotate_or_error.release_error())));
                return;
            }
            editor->did_complete_action("Rotate Layer Counterclockwise"sv);
        }));

    m_layer_menu->add_action(GUI::Action::create("Rotate Layer Clock&wise", TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/edit-rotate-cw.png"sv)),
        [&](auto&) {
            auto* editor = current_image_editor();
            VERIFY(editor);
            auto active_layer = editor->active_layer();
            if (!active_layer)
                return;
            auto layer_rotate_or_error = active_layer->rotate(Gfx::RotationDirection::Clockwise);
            if (layer_rotate_or_error.is_error()) {
                GUI::MessageBox::show_error(&window, MUST(String::formatted("Failed to rotate layer: {}", layer_rotate_or_error.release_error())));
                return;
            }
            editor->did_complete_action("Rotate Layer Clockwise"sv);
        }));

    m_layer_menu->add_separator();
    m_crop_layer_to_selection_action = GUI::Action::create(
        "&Crop Layer to Selection", g_icon_bag.crop, [&](auto&) {
            auto* editor = current_image_editor();
            VERIFY(editor);
            auto active_layer = editor->active_layer();
            if (!active_layer || editor->image().selection().is_empty())
                return;
            auto intersection = editor->image().rect().intersected(editor->image().selection().bounding_rect());
            auto crop_rect = intersection.translated(-active_layer->location());
            auto layer_crop_or_error = active_layer->crop(crop_rect);
            if (layer_crop_or_error.is_error()) {
                GUI::MessageBox::show_error(&window, MUST(String::formatted("Failed to crop layer: {}", layer_crop_or_error.release_error())));
                return;
            }
            active_layer->set_location(intersection.location());
            editor->image().selection().clear();
            editor->did_complete_action("Crop Layer to Selection"sv);
        });
    m_layer_menu->add_action(*m_crop_layer_to_selection_action);
    m_layer_menu->add_action(GUI::Action::create(
        "&Crop Layer to Content", g_icon_bag.crop, [&](auto&) {
            auto* editor = current_image_editor();
            VERIFY(editor);
            auto active_layer = editor->active_layer();
            if (!active_layer)
                return;
            auto content_bounding_rect = active_layer->nonempty_content_bounding_rect();
            if (!content_bounding_rect.has_value())
                return;
            auto layer_crop_or_error = active_layer->crop(content_bounding_rect.value());
            if (layer_crop_or_error.is_error()) {
                GUI::MessageBox::show_error(&window, MUST(String::formatted("Failed to crop layer: {}", layer_crop_or_error.release_error())));
                return;
            }
            active_layer->set_location(content_bounding_rect->location());
            editor->did_complete_action("Crop Layer to Content"sv);
        }));

    m_filter_menu = window.add_menu("&Filter"_string);

    m_filter_menu->add_action(GUI::Action::create("Filter &Gallery", g_icon_bag.filter, [&](auto&) {
        auto* editor = current_image_editor();
        VERIFY(editor);
        auto dialog = PixelPaint::FilterGallery::construct(&window, editor);
        if (dialog->exec() != GUI::Dialog::ExecResult::OK)
            return;
    }));

    m_filter_menu->add_separator();
    m_filter_menu->add_action(GUI::Action::create("Generic 5x5 &Convolution", g_icon_bag.generic_5x5_convolution, [&](auto&) {
        auto* editor = current_image_editor();
        VERIFY(editor);
        if (auto* layer = editor->active_layer()) {
            Gfx::GenericConvolutionFilter<5> filter;
            if (auto parameters = PixelPaint::FilterParameters<Gfx::GenericConvolutionFilter<5>>::get(&window)) {
                filter.apply(layer->content_bitmap(), layer->rect(), layer->content_bitmap(), layer->rect(), *parameters);
                layer->did_modify_bitmap(layer->rect());
                editor->did_complete_action("Generic 5x5 Convolution"sv);
            }
        }
    }));

    auto help_menu = window.add_menu("&Help"_string);
    help_menu->add_action(GUI::CommonActions::make_command_palette_action(&window));
    help_menu->add_action(GUI::CommonActions::make_help_action([](auto&) {
        Desktop::Launcher::open(URL::create_with_file_scheme("/usr/share/man/man1/Applications/PixelPaint.md"), "/bin/Help");
    }));
    help_menu->add_action(GUI::CommonActions::make_about_action("Pixel Paint"_string, GUI::Icon::default_icon("app-pixel-paint"sv), &window));

    m_levels_dialog_action = GUI::Action::create(
        "Change &Levels...", { Mod_Ctrl, Key_L }, g_icon_bag.levels, [&](auto&) {
            auto* editor = current_image_editor();
            VERIFY(editor);
            auto dialog = PixelPaint::LevelsDialog::construct(&window, editor);
            if (dialog->exec() != GUI::Dialog::ExecResult::OK)
                dialog->revert_possible_changes();
        });

    auto& toolbar = *find_descendant_of_type_named<GUI::Toolbar>("toolbar");
    toolbar.add_action(*m_new_image_action);
    toolbar.add_action(*m_open_image_action);
    toolbar.add_action(*m_save_image_action);
    toolbar.add_separator();
    toolbar.add_action(*m_cut_action);
    toolbar.add_action(*m_copy_action);
    toolbar.add_action(*m_paste_action);
    toolbar.add_action(*m_undo_action);
    toolbar.add_action(*m_redo_action);
    toolbar.add_separator();
    toolbar.add_action(*m_zoom_in_action);
    toolbar.add_action(*m_zoom_out_action);
    toolbar.add_action(*m_reset_zoom_action);

    m_zoom_combobox = toolbar.add<GUI::ComboBox>();
    m_zoom_combobox->set_max_width(75);
    m_zoom_combobox->set_model(*GUI::ItemListModel<ByteString>::create(s_suggested_zoom_levels));
    m_zoom_combobox->on_change = [this](ByteString const& value, GUI::ModelIndex const& index) {
        auto* editor = current_image_editor();
        VERIFY(editor);

        if (index.is_valid()) {
            switch (index.row()) {
            case s_zoom_level_fit_width:
                editor->fit_image_to_view(ImageEditor::FitType::Width);
                return;
            case s_zoom_level_fit_height:
                editor->fit_image_to_view(ImageEditor::FitType::Height);
                return;
            case s_zoom_level_fit_image:
                editor->fit_image_to_view(ImageEditor::FitType::Both);
                return;
            }
        }

        auto zoom_level_optional = value.view().trim("%"sv, TrimMode::Right).to_number<int>();
        if (!zoom_level_optional.has_value()) {
            // Indicate that a parse-error occurred by resetting the text to the current state.
            editor->on_scale_change(editor->scale());
            return;
        }

        editor->set_scale(zoom_level_optional.value() * 1.0f / 100);
        // If the selected zoom level got clamped, or a "fit to …" level was selected,
        // there is a chance that the new scale is identical to the old scale.
        // In these cases, we need to manually reset the text:
        editor->on_scale_change(editor->scale());
    };
    m_zoom_combobox->on_return_pressed = [this]() {
        m_zoom_combobox->on_change(m_zoom_combobox->text(), GUI::ModelIndex());
    };

    toolbar.add_separator();
    toolbar.add_action(*m_levels_dialog_action);

    return {};
}

void MainWidget::set_actions_enabled(bool enabled)
{
    m_save_image_action->set_enabled(enabled);
    m_save_image_as_action->set_enabled(enabled);
    m_close_image_action->set_enabled(enabled);

    m_export_submenu->set_children_actions_enabled(enabled);

    m_edit_menu->set_children_actions_enabled(enabled);
    m_paste_action->set_enabled(true);

    m_view_menu->set_children_actions_enabled(enabled);
    m_layer_menu->set_children_actions_enabled(enabled);
    m_image_menu->set_children_actions_enabled(enabled);
    m_tool_menu->set_children_actions_enabled(enabled);
    m_filter_menu->set_children_actions_enabled(enabled);

    m_zoom_combobox->set_enabled(enabled);

    m_levels_dialog_action->set_enabled(enabled);
}

void MainWidget::set_mask_actions_for_layer(Layer* layer)
{
    if (!layer) {
        m_add_mask_action->set_visible(true);
        m_add_editing_mask_action->set_visible(true);
        m_delete_mask_action->set_visible(false);
        m_apply_mask_action->set_visible(false);
        m_add_mask_action->set_enabled(false);
        m_add_editing_mask_action->set_enabled(false);
        return;
    }

    m_add_mask_action->set_enabled(true);
    m_add_editing_mask_action->set_enabled(true);

    auto masked = layer->is_masked();
    m_add_mask_action->set_visible(!masked);
    m_add_editing_mask_action->set_visible(!masked);
    m_invert_mask_action->set_visible(masked);
    m_clear_mask_action->set_visible(masked);
    m_delete_mask_action->set_visible(masked);
    m_apply_mask_action->set_visible(layer->mask_type() == Layer::MaskType::BasicMask);
    m_toggle_mask_visibility_action->set_visible(layer->mask_type() == Layer::MaskType::EditingMask);
    m_toggle_mask_visibility_action->set_checked(layer->mask_visibility());
    m_open_luminosity_masking_action->set_visible(layer->mask_type() == Layer::MaskType::EditingMask);
    m_open_color_masking_action->set_visible(layer->mask_type() == Layer::MaskType::EditingMask);
}

void MainWidget::open_image(FileSystemAccessClient::File file)
{
    auto try_load = m_loader.load_from_file(file.filename(), file.release_stream());
    if (try_load.is_error()) {
        GUI::MessageBox::show_error(window(), MUST(String::formatted("Unable to open file: {}, {}", file.filename(), try_load.release_error())));
        return;
    }

    auto& image = *m_loader.release_image();
    auto& editor = create_new_editor(image);
    editor.set_loaded_from_image(m_loader.is_raw_image());
    editor.set_path(file.filename());
    editor.set_unmodified();
    m_layer_list_widget->set_image(&image);
    m_toolbox->ensure_tool_selection();
    GUI::Application::the()->set_most_recently_open_file(file.filename());
}

ErrorOr<void> MainWidget::create_default_image()
{
    auto image = TRY(Image::create_with_size({ 510, 356 }));

    auto bg_layer = TRY(Layer::create_with_size(*image, image->size(), "Background"));
    image->add_layer(*bg_layer);
    bg_layer->content_bitmap().fill(Color::Transparent);

    m_layer_list_widget->set_image(image);

    auto& editor = create_new_editor(*image);
    editor.set_title("Untitled"_string);
    editor.set_active_layer(bg_layer);
    editor.set_unmodified();

    return {};
}

ErrorOr<void> MainWidget::create_image_from_clipboard()
{
    auto bitmap = GUI::Clipboard::the().fetch_data_and_type().as_bitmap();
    if (!bitmap) {
        return Error::from_string_view("There is no image in a clipboard to paste."sv);
    }

    auto image = TRY(PixelPaint::Image::create_with_size(bitmap->size()));
    auto layer = TRY(PixelPaint::Layer::create_with_bitmap(image, *bitmap, "Pasted layer"));
    image->add_layer(*layer);

    auto& editor = create_new_editor(*image);
    editor.set_title("Untitled"_string);

    m_layer_list_widget->set_image(image);
    m_layer_list_widget->set_selected_layer(layer);
    set_mask_actions_for_layer(layer);
    return {};
}

void MainWidget::refresh_crop_to_selection_menu_actions()
{
    auto enabled = current_image_editor() && !current_image_editor()->image().selection().is_empty();
    m_crop_image_to_selection_action->set_enabled(enabled);
    m_crop_layer_to_selection_action->set_enabled(enabled);
}

bool MainWidget::request_close()
{
    while (!m_tab_widget->children().is_empty()) {
        auto* editor = current_image_editor();
        VERIFY(editor);
        if (!editor->request_close())
            return false;
        m_tab_widget->remove_tab(*m_tab_widget->active_widget());
    }
    return true;
}

ImageEditor* MainWidget::current_image_editor()
{
    if (!m_tab_widget->active_widget())
        return nullptr;
    return verify_cast<PixelPaint::ImageEditor>(m_tab_widget->active_widget());
}

ImageEditor& MainWidget::create_new_editor(NonnullRefPtr<Image> image)
{
    auto& image_editor = m_tab_widget->add_tab<PixelPaint::ImageEditor>("Untitled"_string, image);

    image_editor.on_active_layer_change = [&](auto* layer) {
        if (current_image_editor() != &image_editor)
            return;
        m_layer_list_widget->set_selected_layer(layer);
        m_layer_properties_widget->set_layer(layer);
        set_mask_actions_for_layer(layer);
    };

    image_editor.on_title_change = [&](auto const& title) {
        m_tab_widget->set_tab_title(image_editor, title);
    };

    image_editor.on_modified_change = Core::debounce(100, [&](auto const modified) {
        m_tab_widget->set_tab_modified(image_editor, modified);
        update_window_modified();
        m_histogram_widget->image_changed();
        m_vectorscope_widget->image_changed();
    });

    image_editor.on_image_mouse_position_change = [&](auto const& mouse_position) {
        auto const& image_size = current_image_editor()->image().size();
        auto image_rectangle = Gfx::IntRect { 0, 0, image_size.width(), image_size.height() };
        if (image_rectangle.contains(mouse_position)) {
            update_status_bar(current_image_editor()->appended_status_info());
            m_histogram_widget->set_color_at_mouseposition(current_image_editor()->image().color_at(mouse_position));
            m_vectorscope_widget->set_color_at_mouseposition(current_image_editor()->image().color_at(mouse_position));
        } else {
            m_statusbar->set_override_text({});
            m_histogram_widget->set_color_at_mouseposition(Color::Transparent);
            m_vectorscope_widget->set_color_at_mouseposition(Color::Transparent);
        }
        m_last_image_editor_mouse_position = mouse_position;
    };

    image_editor.on_appended_status_info_change = [&](auto const& appended_status_info) {
        update_status_bar(appended_status_info);
    };

    image_editor.on_leave = [&]() {
        m_statusbar->set_override_text({});
        m_histogram_widget->set_color_at_mouseposition(Color::Transparent);
        m_vectorscope_widget->set_color_at_mouseposition(Color::Transparent);
    };

    image_editor.on_set_guide_visibility = [&](bool show_guides) {
        m_show_guides_action->set_checked(show_guides);
    };

    image_editor.on_set_ruler_visibility = [&](bool show_rulers) {
        m_show_rulers_action->set_checked(show_rulers);
    };

    image_editor.on_scale_change = Core::debounce(100, [this](float scale) {
        m_zoom_combobox->set_text(ByteString::formatted("{}%", roundf(scale * 100)), GUI::AllowCallback::No);
        current_image_editor()->update_tool_cursor();
    });

    image_editor.on_primary_color_change = [&](Color color) {
        m_palette_widget->set_primary_color(color);
        if (image_editor.active_tool())
            image_editor.active_tool()->on_primary_color_change(color);
        if (image_editor.active_layer()->mask_visibility())
            image_editor.update();
    };
    image_editor.on_secondary_color_change = [&](Color color) {
        m_palette_widget->set_secondary_color(color);
        if (image_editor.active_tool())
            image_editor.active_tool()->on_secondary_color_change(color);
    };
    image_editor.on_file_saved = [](ByteString const& filename) {
        GUI::Application::the()->set_most_recently_open_file(filename);
    };

    if (image->layer_count())
        image_editor.set_active_layer(&image->layer(0));

    if (!m_loader.is_raw_image()) {
        m_loader.json_metadata().for_each([&](JsonValue const& value) {
            if (!value.is_object())
                return;
            auto& json_object = value.as_object();
            auto orientation_value = json_object.get_byte_string("orientation"sv);
            if (!orientation_value.has_value())
                return;

            auto offset_value = json_object.get("offset"sv);
            if (!offset_value.has_value() || !offset_value->is_number())
                return;

            auto orientation_string = orientation_value.value();
            PixelPaint::Guide::Orientation orientation;
            if (orientation_string == "horizontal"sv)
                orientation = PixelPaint::Guide::Orientation::Horizontal;
            else if (orientation_string == "vertical"sv)
                orientation = PixelPaint::Guide::Orientation::Vertical;
            else
                return;

            image_editor.add_guide(PixelPaint::Guide::construct(orientation, offset_value->get_float_with_precision_loss().value_or(0)));
        });
    }

    m_tab_widget->set_active_widget(&image_editor);
    image_editor.set_focus(true);
    image_editor.fit_image_to_view();
    m_tool_properties_widget->set_enabled(true);
    set_actions_enabled(true);

    return image_editor;
}

void MainWidget::drag_enter_event(GUI::DragEvent& event)
{
    if (event.mime_data().has_urls())
        event.accept();
}

void MainWidget::drop_event(GUI::DropEvent& event)
{
    if (!event.mime_data().has_urls())
        return;

    event.accept();

    if (event.mime_data().urls().is_empty())
        return;

    for (auto& url : event.mime_data().urls()) {
        if (url.scheme() != "file")
            continue;

        auto response = FileSystemAccessClient::Client::the().request_file(window(), URL::percent_decode(url.serialize_path()), Core::File::OpenMode::Read);
        if (response.is_error())
            return;
        open_image(response.release_value());
    }
}

void MainWidget::update_window_modified()
{
    refresh_crop_to_selection_menu_actions();
    window()->set_modified(m_tab_widget->is_any_tab_modified());
}
void MainWidget::update_status_bar(ByteString appended_text)
{
    StringBuilder builder = StringBuilder();
    builder.append(m_last_image_editor_mouse_position.to_byte_string());
    if (!appended_text.is_empty()) {
        builder.append(" "sv);
        builder.append(appended_text);
    }
    m_statusbar->set_override_text(builder.to_string().release_value_but_fixme_should_propagate_errors());
}

}
