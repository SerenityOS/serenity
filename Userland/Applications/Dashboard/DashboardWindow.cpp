/*
 * Copyright (c) 2022, Filiph Sandstrom <filiph.sandstrom@filfatstudios.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "DashboardWindow.h"
#include <AK/Debug.h>
#include <AK/JsonObject.h>
#include <AK/QuickSort.h>
#include <LibCore/File.h>
#include <LibCore/StandardPaths.h>
#include <LibDesktop/AppFile.h>
#include <LibDesktop/Launcher.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Desktop.h>
#include <LibGUI/GridLayout.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Label.h>
#include <LibGUI/Painter.h>
#include <LibGUI/ScrollableContainerWidget.h>
#include <LibGUI/Tile.h>
#include <LibGUI/Window.h>
#include <LibGfx/Font/FontDatabase.h>
#include <LibGfx/Palette.h>
#include <serenity.h>
#include <spawn.h>
#include <stdio.h>

class DashboardWidget final : public GUI::Widget {
    C_OBJECT(DashboardWidget);

public:
    virtual ~DashboardWidget() override = default;

    virtual void paint_event(GUI::PaintEvent& event) override
    {
        GUI::Painter painter(*this);
        painter.add_clip_rect(event.rect());
        painter.fill_rect(rect(), palette().desktop_background());
    }

private:
    DashboardWidget() = default;
};

struct AppMetadata {
    String executable;
    String name;
    String category;
    GUI::Icon icon;
    bool run_in_terminal;
};
Vector<AppMetadata> apps;

int const ITEM_SIZE = 114;
DashboardWindow::DashboardWindow(bool desktop_mode)
    : m_desktop_mode(desktop_mode)
{
    set_title("Dashboard");
    set_minimum_size(378, 400);
    resize(378, 480);

    if (m_desktop_mode) {
        set_frameless(true);
        set_forced_shadow(true);
        move_to(0, GUI::Desktop::the().rect().height() - height() - 28);

        on_active_window_change = [](bool is_active_window) {
            if (!is_active_window)
                GUI::Application::the()->quit();
        };
    }

    auto& main_widget = set_main_widget<DashboardWidget>();
    main_widget.set_layout<GUI::VerticalBoxLayout>();
    main_widget.layout()->set_margins({ 0, 0, 0, 0 });

    m_container = GUI::Widget::construct();
    m_container->set_layout<GUI::GridLayout>();
    m_container->layout()->set_margins({ 4, 4, 4, 4 });
    static_cast<GUI::GridLayout*>(m_container->layout())->set_item_size(ITEM_SIZE);
    static_cast<GUI::GridLayout*>(m_container->layout())->set_columns(3);

    auto& scroll_container = main_widget.add<GUI::ScrollableContainerWidget>();
    scroll_container.set_should_hide_unnecessary_scrollbars(true);
    scroll_container.set_widget(m_container);

    apps.append({ "/bin/Settings", "Settings", "Utilities", GUI::Icon::default_icon("settings"sv), false });
    Desktop::AppFile::for_each([&](auto af) {
        if (access(af->executable().characters(), X_OK) == 0 && af->name() != "Dashboard" && !af->name().contains("Settings"sv)) {
            apps.append({ af->executable(), af->name(), af->category(), af->icon(), af->run_in_terminal() });
        }
    });
    quick_sort(apps, [](auto& a, auto& b) { return a.name < b.name; });

    auto data_path = String::formatted("{}/.dashboard", Core::StandardPaths::home_directory());
    for (size_t n = 0; n < apps.size(); n++) {
        auto app = apps.at(n);

        auto& tile = m_container->add<GUI::Tile>();
        tile.set_text(app.name);
        tile.set_icon(app.icon.bitmap_for_size(ITEM_SIZE));
        tile.set_fixed_size(ITEM_SIZE, ITEM_SIZE);

        if (m_desktop_mode) {
            tile.set_animation_start(n * 30);
        }

        tile.on_click = [app](auto) {
            char const* argv[4] { nullptr, nullptr, nullptr, nullptr };
            if (app.run_in_terminal) {
                argv[0] = "/bin/Terminal";
                argv[1] = "-e";
                argv[2] = app.executable.characters();
            } else {
                argv[0] = app.executable.characters();
            }

            posix_spawn_file_actions_t spawn_actions;
            posix_spawn_file_actions_init(&spawn_actions);
            auto home_directory = Core::StandardPaths::home_directory();
            posix_spawn_file_actions_addchdir(&spawn_actions, home_directory.characters());

            pid_t child_pid;
            if ((errno = posix_spawn(&child_pid, argv[0], &spawn_actions, nullptr, const_cast<char**>(argv), environ))) {
                perror("posix_spawn");
            } else {
                if (disown(child_pid) < 0)
                    perror("disown");
            }
            posix_spawn_file_actions_destroy(&spawn_actions);
        };

        auto tile_data_path = String::formatted("{}/{}.json", data_path, app.executable.split('/').last());
        if (Core::File::exists(tile_data_path)) {
            auto tile_data_file = Core::File::open(tile_data_path, Core::OpenMode::ReadOnly).release_value_but_fixme_should_propagate_errors();
            auto tile_data = JsonValue::from_string(tile_data_file->read_all()).release_value_but_fixme_should_propagate_errors().as_object();

            auto animation = tile_data.get("animation"sv).as_string().to_lowercase();
            if (animation == "none") {
                tile.set_animation(GUI::Tile::TileAnimation::None);
            } else if (animation == "slide") {
                tile.set_animation(GUI::Tile::TileAnimation::Slide);
            } else {
                VERIFY_NOT_REACHED();
            }

            auto branding = tile_data.get("branding"sv).as_string().to_lowercase();
            if (branding == "none") {
                tile.set_branding(GUI::Tile::TileBranding::None);
            } else if (branding == "label") {
                tile.set_branding(GUI::Tile::TileBranding::Label);
            } else {
                VERIFY_NOT_REACHED();
            }

            auto contents = tile_data.get("content"sv).as_array();
            if (contents.size() > 0)
                tile.set_contents({});

            contents.for_each([&](auto& value) {
                auto kind = GUI::Tile::TileContent::ContentKind::Branding;
                auto alignment = GUI::Tile::TileContent::ContentAlignment::Bottom;
                auto content = String::empty();

                if (value.as_object().has("kind"sv)) {
                    auto kind_value = value.as_object().get("kind"sv).as_string().to_lowercase();
                    if (kind_value == "branding") {
                        kind = GUI::Tile::TileContent::ContentKind::Branding;
                    } else if (kind_value == "normal") {
                        kind = GUI::Tile::TileContent::ContentKind::Normal;
                    } else if (kind_value == "date") {
                        kind = GUI::Tile::TileContent::ContentKind::Date;
                    } else {
                        VERIFY_NOT_REACHED();
                    }
                }

                if (value.as_object().has("alignment"sv)) {
                    auto alignment_value = value.as_object().get("alignment"sv).as_string().to_lowercase();
                    if (alignment_value == "center") {
                        alignment = GUI::Tile::TileContent::ContentAlignment::Center;
                    } else if (alignment_value == "bottom") {
                        alignment = GUI::Tile::TileContent::ContentAlignment::Bottom;
                    } else {
                        VERIFY_NOT_REACHED();
                    }
                }

                if (value.as_object().has("content"sv)) {
                    content = value.as_object().get("content"sv).as_string();
                }

                tile.append_contents({ kind,
                    alignment,
                    content });
            });
        }
    }
}

void DashboardWindow::event(Core::Event& event)
{
    if (event.type() == GUI::Event::Resize) {
        auto& resize_event = static_cast<GUI::ResizeEvent&>(event);
        auto width = resize_event.size().width() - 12;

        if (width > 480) {
            static_cast<GUI::GridLayout*>(m_container->layout())->set_columns(width / (ITEM_SIZE + 8));
        } else {
            static_cast<GUI::GridLayout*>(m_container->layout())->set_columns(3);
        }
    }

    Window::event(event);
}
