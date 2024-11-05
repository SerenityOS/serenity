/*
 * Copyright (c) 2021, Spencer Dixon <spencercdixon@gmail.com>
 * Copyright (c) 2022-2023, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Providers.h"
#include <AK/Array.h>
#include <AK/ByteString.h>
#include <AK/Error.h>
#include <AK/LexicalPath.h>
#include <AK/QuickSort.h>
#include <AK/Try.h>
#include <LibCore/Debounce.h>
#include <LibCore/LockFile.h>
#include <LibCore/System.h>
#include <LibDesktop/Launcher.h>
#include <LibGUI/Action.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Event.h>
#include <LibGUI/Icon.h>
#include <LibGUI/ImageWidget.h>
#include <LibGUI/Label.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Painter.h>
#include <LibGUI/TextBox.h>
#include <LibGfx/Palette.h>
#include <LibMain/Main.h>
#include <LibThreading/Mutex.h>
#include <string.h>
#include <unistd.h>

namespace Assistant {

struct AppState {
    Optional<size_t> selected_index;
    Vector<NonnullRefPtr<Result const>> results;
    size_t visible_result_count { 0 };

    Threading::Mutex lock;
    ByteString last_query;
};

class ResultRow final : public GUI::Button {
    C_OBJECT(ResultRow)
    ResultRow()
    {
        set_greedy_for_hits(true);
        set_fixed_height(36);
        set_text_alignment(Gfx::TextAlignment::CenterLeft);
        set_button_style(Gfx::ButtonStyle::Coolbar);
        set_focus_policy(GUI::FocusPolicy::NoFocus);

        on_context_menu_request = [this](auto& event) {
            if (!m_context_menu) {
                m_context_menu = GUI::Menu::construct();

                if (LexicalPath path { text().to_byte_string() }; path.is_absolute()) {
                    m_context_menu->add_action(GUI::Action::create("&Show in File Manager", MUST(Gfx::Bitmap::load_from_file("/res/icons/16x16/app-file-manager.png"sv)), [=](auto&) {
                        Desktop::Launcher::open(URL::create_with_file_scheme(path.dirname(), path.basename()));
                    }));
                    m_context_menu->add_separator();
                }

                m_context_menu->add_action(GUI::Action::create("&Copy as Text", MUST(Gfx::Bitmap::load_from_file("/res/icons/16x16/edit-copy.png"sv)), [&](auto&) {
                    GUI::Clipboard::the().set_plain_text(text());
                }));
            }
            m_context_menu->popup(event.screen_position());
        };
    }

    RefPtr<GUI::Menu> m_context_menu;
};

template<size_t ProviderCount>
class Database {
public:
    explicit Database(AppState& state, Array<NonnullRefPtr<Provider>, ProviderCount>& providers)
        : m_state(state)
        , m_providers(providers)
    {
    }

    Function<void(Vector<NonnullRefPtr<Result const>>&&)> on_new_results;

    void search(ByteString const& query)
    {
        auto should_display_precached_results = false;
        for (size_t i = 0; i < ProviderCount; ++i) {
            auto& result_array = m_result_cache.ensure(query);
            if (result_array.at(i) == nullptr) {
                m_providers[i]->query(query, [this, query, i](auto results) {
                    {
                        Threading::MutexLocker db_locker(m_mutex);
                        auto& result_array = m_result_cache.ensure(query);
                        if (result_array.at(i) != nullptr)
                            return;
                        result_array[i] = make<Vector<NonnullRefPtr<Result>>>(results);
                    }
                    on_result_cache_updated();
                });
            } else {
                should_display_precached_results = true;
            }
        }
        if (should_display_precached_results)
            on_result_cache_updated();
    }

private:
    void on_result_cache_updated()
    {
        Threading::MutexLocker state_locker(m_state.lock);
        auto new_results = m_result_cache.find(m_state.last_query);
        if (new_results == m_result_cache.end())
            return;

        Vector<NonnullRefPtr<Result const>> all_results;
        for (auto const& results_for_provider : new_results->value) {
            if (results_for_provider == nullptr)
                continue;
            for (auto const& result : *results_for_provider) {
                all_results.append(result);
            }
        }

        dual_pivot_quick_sort(all_results, 0, static_cast<int>(all_results.size() - 1), [](auto& a, auto& b) {
            return a->score() > b->score();
        });

        on_new_results(move(all_results));
    }

    AppState& m_state;

    Array<NonnullRefPtr<Provider>, ProviderCount> m_providers;

    Threading::Mutex m_mutex;
    HashMap<ByteString, Array<OwnPtr<Vector<NonnullRefPtr<Result>>>, ProviderCount>> m_result_cache;
};

}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio recvfd sendfd rpath cpath unix proc exec thread map_fixed"));

    TRY(Core::System::enter_jail_mode_until_exec());

    Core::LockFile lockfile("/tmp/lock/assistant.lock");

    if (!lockfile.is_held()) {
        if (lockfile.error_code()) {
            warnln("Core::LockFile: {}", strerror(lockfile.error_code()));
            return 1;
        }

        // Another assistant is open, so exit silently.
        return 0;
    }

    auto app = TRY(GUI::Application::create(arguments));
    auto window = GUI::Window::construct();
    window->set_minimizable(false);

    Assistant::AppState app_state;
    Array<NonnullRefPtr<Assistant::Provider>, 5> providers = {
        make_ref_counted<Assistant::AppProvider>(),
        make_ref_counted<Assistant::CalculatorProvider>(),
        make_ref_counted<Assistant::TerminalProvider>(),
        make_ref_counted<Assistant::URLProvider>(),
        make_ref_counted<Assistant::FileProvider>()
    };
    Assistant::Database db { app_state, providers };

    auto container = window->set_main_widget<GUI::Frame>();
    container->set_fill_with_background_color(true);
    container->set_frame_style(Gfx::FrameStyle::Window);
    container->set_layout<GUI::VerticalBoxLayout>(8);

    auto& text_box = container->add<GUI::TextBox>();
    auto& results_container = container->add<GUI::Widget>();
    results_container.set_layout<GUI::VerticalBoxLayout>();

    auto mark_selected_item = [&]() {
        for (size_t i = 0; i < app_state.visible_result_count; ++i) {
            auto& row = static_cast<Assistant::ResultRow&>(results_container.child_widgets()[i]);
            row.set_font_weight(i == app_state.selected_index ? 700 : 400);
        }
    };

    text_box.on_change = Core::debounce(5, [&]() {
        {
            Threading::MutexLocker locker(app_state.lock);
            if (app_state.last_query == text_box.text())
                return;

            app_state.last_query = text_box.text();
        }

        db.search(text_box.text());
    });
    text_box.on_return_pressed = [&]() {
        if (!app_state.selected_index.has_value())
            return;
        lockfile.release();
        app_state.results[app_state.selected_index.value()]->activate(window);
        GUI::Application::the()->quit();
    };
    text_box.on_up_pressed = [&]() {
        if (!app_state.visible_result_count)
            return;
        auto new_selected_index = app_state.selected_index.value_or(0);
        if (new_selected_index == 0)
            new_selected_index = app_state.visible_result_count - 1;
        else if (new_selected_index > 0)
            new_selected_index -= 1;

        app_state.selected_index = new_selected_index;
        mark_selected_item();
    };
    text_box.on_down_pressed = [&]() {
        if (!app_state.visible_result_count)
            return;

        auto new_selected_index = app_state.selected_index.value_or(0);
        if ((app_state.visible_result_count - 1) == new_selected_index)
            new_selected_index = 0;
        else if (app_state.visible_result_count > new_selected_index)
            new_selected_index += 1;

        app_state.selected_index = new_selected_index;
        mark_selected_item();
    };
    text_box.on_escape_pressed = []() {
        GUI::Application::the()->quit();
    };
    window->on_active_window_change = [](bool is_active_window) {
        if (!is_active_window)
            GUI::Application::the()->quit();
    };

    auto update_ui_timer = Core::Timer::create_single_shot(10, [&] {
        results_container.remove_all_children();
        results_container.layout()->set_margins(app_state.visible_result_count ? GUI::Margins { 4, 0, 0, 0 } : GUI::Margins { 0 });

        for (size_t i = 0; i < app_state.visible_result_count; ++i) {
            auto& result = app_state.results[i];
            auto& match = results_container.add<Assistant::ResultRow>();
            match.set_icon(result->bitmap());
            match.set_text(String::from_byte_string(result->title()).release_value_but_fixme_should_propagate_errors());
            match.set_tooltip(result->tooltip());
            match.on_click = [&](auto) {
                result->activate(window);
                GUI::Application::the()->quit();
            };
        }

        mark_selected_item();
        Core::deferred_invoke([window] { window->resize(GUI::Desktop::the().rect().width() / 3, {}); });
    });

    db.on_new_results = [&](auto results) {
        if (results.is_empty()) {
            app_state.selected_index = {};
        } else if (app_state.selected_index.has_value()) {
            auto current_selected_result = app_state.results[app_state.selected_index.value()];
            auto new_selected_index = results.find_first_index(current_selected_result).value_or(0);
            if (new_selected_index >= Assistant::MAX_SEARCH_RESULTS) {
                new_selected_index = 0;
            }
            app_state.selected_index = new_selected_index;
        } else {
            app_state.selected_index = 0;
        }
        app_state.results = results;
        app_state.visible_result_count = min(results.size(), Assistant::MAX_SEARCH_RESULTS);

        update_ui_timer->restart();
    };

    window->set_window_type(GUI::WindowType::Popup);
    window->set_forced_shadow(true);
    window->resize(GUI::Desktop::the().rect().width() / 3, {});
    window->center_on_screen();
    window->move_to(window->x(), window->y() - (GUI::Desktop::the().rect().height() * 0.33));
    window->show();

    return app->exec();
}
