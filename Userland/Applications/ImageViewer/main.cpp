/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Mohsan Ali <mohsan0073@gmail.com>
 * Copyright (c) 2023, Caoimhe Byrne <caoimhebyrne06@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "MainWidget.h"
#include "ViewWidget.h"
#include <AK/URL.h>
#include <LibConfig/Client.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/System.h>
#include <LibDesktop/Launcher.h>
#include <LibFileSystemAccessClient/Client.h>
#include <LibGUI/Action.h>
#include <LibGUI/ActionGroup.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Clipboard.h>
#include <LibGUI/Desktop.h>
#include <LibGUI/FilePicker.h>
#include <LibGUI/Label.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Menubar.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Toolbar.h>
#include <LibGUI/ToolbarContainer.h>
#include <LibGUI/Window.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Palette.h>
#include <LibGfx/Rect.h>
#include <LibMain/Main.h>
#include <serenity.h>
#include <string.h>

using namespace ImageViewer;

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio recvfd sendfd rpath wpath cpath unix thread"));

    auto app = TRY(GUI::Application::create(arguments));

    Config::pledge_domains({ "ImageViewer", "WindowManager" });

    app->set_config_domain("ImageViewer"_string);

    TRY(Desktop::Launcher::add_allowed_handler_with_any_url("/bin/ImageViewer"));
    TRY(Desktop::Launcher::add_allowed_handler_with_only_specific_urls("/bin/Help", { URL::create_with_file_scheme("/usr/share/man/man1/Applications/ImageViewer.md") }));
    TRY(Desktop::Launcher::seal_allowlist());

    // FIXME: Use unveil when we solve the issue with ViewWidget::load_files_from_directory, an explanation is given in ViewWidget.cpp
    // TRY(Core::System::unveil("/tmp/session/%sid/portal/filesystemaccess", "rw"));
    // TRY(Core::System::unveil("/tmp/session/%sid/portal/image", "rw"));
    // TRY(Core::System::unveil("/res", "r"));
    // TRY(Core::System::unveil(nullptr, nullptr));

    auto app_icon = GUI::Icon::default_icon("filetype-image"sv);

    StringView path;
    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(path, "The image file to be displayed.", "file", Core::ArgsParser::Required::No);
    args_parser.parse(arguments);

    auto window = TRY(GUI::Window::try_create());
    window->set_double_buffering_enabled(true);
    window->resize(300, 200);
    window->set_icon(app_icon.bitmap_for_size(16));
    window->set_title("Image Viewer");

    auto root_widget = TRY(window->set_main_widget<MainWidget>());

    auto toolbar_container = TRY(root_widget->try_add<GUI::ToolbarContainer>());
    auto main_toolbar = TRY(toolbar_container->try_add<GUI::Toolbar>());

    auto widget = TRY(root_widget->try_add<ViewWidget>());
    widget->on_scale_change = [&](float scale) {
        if (!widget->image()) {
            window->set_title("Image Viewer");
            return;
        }

        window->set_title(DeprecatedString::formatted("{} {} {}% - Image Viewer", widget->path(), widget->image()->size().to_deprecated_string(), (int)(scale * 100)));

        if (!widget->scaled_for_first_image()) {
            widget->set_scaled_for_first_image(true);
            widget->resize_window();
        }
    };
    widget->on_drop = [&](auto& event) {
        if (!event.mime_data().has_urls())
            return;

        auto urls = event.mime_data().urls();

        if (urls.is_empty())
            return;

        window->move_to_front();

        auto path = urls.first().serialize_path();
        auto result = FileSystemAccessClient::Client::the().request_file_read_only_approved(window, path);
        if (result.is_error())
            return;

        auto value = result.release_value();
        widget->open_file(value.filename(), value.stream());

        for (size_t i = 1; i < urls.size(); ++i) {
            Desktop::Launcher::open(URL::create_with_file_scheme(urls[i].serialize_path().characters()), "/bin/ImageViewer");
        }
    };
    widget->on_doubleclick = [&] {
        window->set_fullscreen(!window->is_fullscreen());
        toolbar_container->set_visible(!window->is_fullscreen());
        widget->set_frame_style(window->is_fullscreen() ? Gfx::FrameStyle::NoFrame : Gfx::FrameStyle::SunkenContainer);
    };

    // Actions
    auto open_action = GUI::CommonActions::make_open_action(
        [&](auto&) {
            FileSystemAccessClient::OpenFileOptions options {
                .window_title = "Open Image"sv,
                .allowed_file_types = Vector { GUI::FileTypeFilter::image_files(), GUI::FileTypeFilter::all_files() },
            };
            auto result = FileSystemAccessClient::Client::the().open_file(window, options);
            if (result.is_error())
                return;

            auto value = result.release_value();
            widget->open_file(value.filename(), value.stream());
        });

    auto delete_action = GUI::CommonActions::make_delete_action(
        [&](auto&) {
            auto path = widget->path();
            if (path.is_empty())
                return;

            auto msgbox_result = GUI::MessageBox::show(window,
                DeprecatedString::formatted("Are you sure you want to delete {}?", path),
                "Confirm Deletion"sv,
                GUI::MessageBox::Type::Warning,
                GUI::MessageBox::InputType::OKCancel);

            if (msgbox_result == GUI::MessageBox::ExecResult::Cancel)
                return;

            auto unlinked_or_error = Core::System::unlink(widget->path());
            if (unlinked_or_error.is_error()) {
                GUI::MessageBox::show(window,
                    DeprecatedString::formatted("unlink({}) failed: {}", path, unlinked_or_error.error()),
                    "Delete Failed"sv,
                    GUI::MessageBox::Type::Error);

                return;
            }

            widget->clear();
        });

    auto quit_action = GUI::CommonActions::make_quit_action(
        [&](auto&) {
            app->quit();
        });

    auto rotate_counterclockwise_action = GUI::CommonActions::make_rotate_counterclockwise_action([&](auto&) {
        widget->rotate(Gfx::RotationDirection::CounterClockwise);
    });

    auto rotate_clockwise_action = GUI::CommonActions::make_rotate_clockwise_action([&](auto&) {
        widget->rotate(Gfx::RotationDirection::Clockwise);
    });

    auto vertical_flip_action = GUI::Action::create("Flip &Vertically", { Mod_None, Key_V }, TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/edit-flip-vertical.png"sv)),
        [&](auto&) {
            widget->flip(Gfx::Orientation::Vertical);
        });

    auto horizontal_flip_action = GUI::Action::create("Flip &Horizontally", { Mod_None, Key_H }, TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/edit-flip-horizontal.png"sv)),
        [&](auto&) {
            widget->flip(Gfx::Orientation::Horizontal);
        });

    auto desktop_wallpaper_action = GUI::Action::create("Set as Desktop &Wallpaper", TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/app-display-settings.png"sv)),
        [&](auto&) {
            if (!GUI::Desktop::the().set_wallpaper(widget->image()->bitmap(GUI::Desktop::the().rect().size()).release_value_but_fixme_should_propagate_errors(), widget->path())) {
                GUI::MessageBox::show(window,
                    DeprecatedString::formatted("set_wallpaper({}) failed", widget->path()),
                    "Could not set wallpaper"sv,
                    GUI::MessageBox::Type::Error);
            }
        });

    auto go_first_action = GUI::Action::create("&Go to First", { Mod_None, Key_Home }, TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/go-first.png"sv)),
        [&](auto&) {
            widget->navigate(ViewWidget::Directions::First);
        });

    auto go_back_action = GUI::Action::create("Go to &Previous", { Mod_None, Key_Left }, TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/go-back.png"sv)),
        [&](auto&) {
            widget->navigate(ViewWidget::Directions::Back);
        });

    auto go_forward_action = GUI::Action::create("Go to &Next", { Mod_None, Key_Right }, TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/go-forward.png"sv)),
        [&](auto&) {
            widget->navigate(ViewWidget::Directions::Forward);
        });

    auto go_last_action = GUI::Action::create("Go to &Last", { Mod_None, Key_End }, TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/go-last.png"sv)),
        [&](auto&) {
            widget->navigate(ViewWidget::Directions::Last);
        });

    auto full_screen_action = GUI::CommonActions::make_fullscreen_action(
        [&](auto&) {
            widget->on_doubleclick();
        });

    auto zoom_in_action = GUI::CommonActions::make_zoom_in_action(
        [&](auto&) {
            widget->set_scale(widget->scale() * 1.44f);
        },
        window);

    auto reset_zoom_action = GUI::CommonActions::make_reset_zoom_action(
        [&](auto&) {
            widget->set_scale(1.f);
        },
        window);

    auto fit_image_to_view_action = GUI::Action::create(
        "Fit Image To &View", TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/fit-image-to-view.png"sv)), [&](auto&) {
            widget->fit_content_to_view();
        });

    auto zoom_out_action = GUI::CommonActions::make_zoom_out_action(
        [&](auto&) {
            widget->set_scale(widget->scale() / 1.44f);
        },
        window);

    auto hide_show_toolbar_action = GUI::Action::create_checkable("&Toolbar", { Mod_Ctrl, Key_T },
        [&](auto& action) {
            toolbar_container->set_visible(action.is_checked());
        });
    hide_show_toolbar_action->set_checked(true);

    auto copy_action = GUI::CommonActions::make_copy_action([&](auto&) {
        if (widget->image())
            GUI::Clipboard::the().set_bitmap(*widget->image()->bitmap({}).release_value_but_fixme_should_propagate_errors());
    });

    auto nearest_neighbor_action = GUI::Action::create_checkable("&Nearest Neighbor", [&](auto&) {
        widget->set_scaling_mode(Gfx::Painter::ScalingMode::NearestNeighbor);
    });
    nearest_neighbor_action->set_checked(true);

    auto smooth_pixels_action = GUI::Action::create_checkable("&Smooth Pixels", [&](auto&) {
        widget->set_scaling_mode(Gfx::Painter::ScalingMode::SmoothPixels);
    });

    auto bilinear_action = GUI::Action::create_checkable("&Bilinear", [&](auto&) {
        widget->set_scaling_mode(Gfx::Painter::ScalingMode::BilinearBlend);
    });

    auto box_sampling_action = GUI::Action::create_checkable("B&ox Sampling", [&](auto&) {
        widget->set_scaling_mode(Gfx::Painter::ScalingMode::BoxSampling);
    });

    widget->on_image_change = [&](Image const* image) {
        bool should_enable_image_actions = (image != nullptr);
        bool should_enable_forward_actions = (widget->is_next_available() && should_enable_image_actions);
        bool should_enable_backward_actions = (widget->is_previous_available() && should_enable_image_actions);
        delete_action->set_enabled(should_enable_image_actions);
        rotate_counterclockwise_action->set_enabled(should_enable_image_actions);
        rotate_clockwise_action->set_enabled(should_enable_image_actions);
        vertical_flip_action->set_enabled(should_enable_image_actions);
        horizontal_flip_action->set_enabled(should_enable_image_actions);
        desktop_wallpaper_action->set_enabled(should_enable_image_actions);

        go_first_action->set_enabled(should_enable_backward_actions);
        go_back_action->set_enabled(should_enable_backward_actions);
        go_forward_action->set_enabled(should_enable_forward_actions);
        go_last_action->set_enabled(should_enable_forward_actions);

        zoom_in_action->set_enabled(should_enable_image_actions);
        reset_zoom_action->set_enabled(should_enable_image_actions);
        zoom_out_action->set_enabled(should_enable_image_actions);
        if (!should_enable_image_actions) {
            window->set_title("Image Viewer");
        }
    };

    (void)TRY(main_toolbar->try_add_action(open_action));
    (void)TRY(main_toolbar->try_add_action(delete_action));
    (void)TRY(main_toolbar->try_add_separator());
    (void)TRY(main_toolbar->try_add_action(go_first_action));
    (void)TRY(main_toolbar->try_add_action(go_back_action));
    (void)TRY(main_toolbar->try_add_action(go_forward_action));
    (void)TRY(main_toolbar->try_add_action(go_last_action));
    (void)TRY(main_toolbar->try_add_separator());
    (void)TRY(main_toolbar->try_add_action(zoom_in_action));
    (void)TRY(main_toolbar->try_add_action(reset_zoom_action));
    (void)TRY(main_toolbar->try_add_action(zoom_out_action));

    auto file_menu = TRY(window->try_add_menu("&File"_short_string));
    TRY(file_menu->try_add_action(open_action));
    TRY(file_menu->try_add_action(delete_action));
    TRY(file_menu->try_add_separator());

    TRY(file_menu->add_recent_files_list([&](auto& action) {
        auto path = action.text();
        auto result = FileSystemAccessClient::Client::the().request_file_read_only_approved(window, path);
        if (result.is_error())
            return;

        auto value = result.release_value();
        widget->open_file(value.filename(), value.stream());
    }));

    TRY(file_menu->try_add_action(quit_action));

    auto image_menu = TRY(window->try_add_menu("&Image"_short_string));
    TRY(image_menu->try_add_action(rotate_counterclockwise_action));
    TRY(image_menu->try_add_action(rotate_clockwise_action));
    TRY(image_menu->try_add_action(vertical_flip_action));
    TRY(image_menu->try_add_action(horizontal_flip_action));
    TRY(image_menu->try_add_separator());
    TRY(image_menu->try_add_action(desktop_wallpaper_action));

    auto navigate_menu = TRY(window->try_add_menu("&Navigate"_string));
    TRY(navigate_menu->try_add_action(go_first_action));
    TRY(navigate_menu->try_add_action(go_back_action));
    TRY(navigate_menu->try_add_action(go_forward_action));
    TRY(navigate_menu->try_add_action(go_last_action));

    auto view_menu = TRY(window->try_add_menu("&View"_short_string));
    TRY(view_menu->try_add_action(full_screen_action));
    TRY(view_menu->try_add_separator());
    TRY(view_menu->try_add_action(zoom_in_action));
    TRY(view_menu->try_add_action(reset_zoom_action));
    TRY(view_menu->try_add_action(fit_image_to_view_action));
    TRY(view_menu->try_add_action(zoom_out_action));
    TRY(view_menu->try_add_separator());

    auto scaling_mode_menu = TRY(view_menu->try_add_submenu("&Scaling Mode"_string));
    scaling_mode_menu->set_icon(TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/scale.png"sv)));

    auto scaling_mode_group = make<GUI::ActionGroup>();
    scaling_mode_group->set_exclusive(true);
    scaling_mode_group->add_action(*nearest_neighbor_action);
    scaling_mode_group->add_action(*smooth_pixels_action);
    scaling_mode_group->add_action(*bilinear_action);
    scaling_mode_group->add_action(*box_sampling_action);

    TRY(scaling_mode_menu->try_add_action(nearest_neighbor_action));
    TRY(scaling_mode_menu->try_add_action(smooth_pixels_action));
    TRY(scaling_mode_menu->try_add_action(bilinear_action));
    TRY(scaling_mode_menu->try_add_action(box_sampling_action));

    TRY(view_menu->try_add_separator());
    TRY(view_menu->try_add_action(hide_show_toolbar_action));

    auto help_menu = TRY(window->try_add_menu("&Help"_short_string));
    TRY(help_menu->try_add_action(GUI::CommonActions::make_command_palette_action(window)));
    TRY(help_menu->try_add_action(GUI::CommonActions::make_help_action([](auto&) {
        Desktop::Launcher::open(URL::create_with_file_scheme("/usr/share/man/man1/Applications/ImageViewer.md"), "/bin/Help");
    })));
    TRY(help_menu->try_add_action(GUI::CommonActions::make_about_action("Image Viewer", app_icon, window)));

    window->show();

    // We must do this here and not any earlier, as we need a visible window to call FileSystemAccessClient::Client::request_file_read_only_approved();
    if (path != nullptr) {
        auto result = FileSystemAccessClient::Client::the().request_file_read_only_approved(window, path);
        if (result.is_error())
            return 1;

        auto value = result.release_value();
        widget->open_file(value.filename(), value.stream());
    } else {
        widget->clear();
    }

    return app->exec();
}
