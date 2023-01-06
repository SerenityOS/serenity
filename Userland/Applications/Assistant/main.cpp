/*
 * Copyright (c) 2021, Spencer Dixon <spencercdixon@gmail.com>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Providers.h"
#include <AK/DeprecatedString.h>
#include <AK/Error.h>
#include <AK/LexicalPath.h>
#include <AK/QuickSort.h>
#include <AK/Try.h>
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
    NonnullRefPtrVector<Result> results;
    size_t visible_result_count { 0 };

    Threading::Mutex lock;
    DeprecatedString last_query;
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

                if (LexicalPath path { text() }; path.is_absolute()) {
                    m_context_menu->add_action(GUI::Action::create("&Show in File Manager", MUST(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/app-file-manager.png"sv)), [=](auto&) {
                        Desktop::Launcher::open(URL::create_with_file_scheme(path.dirname(), path.basename()));
                    }));
                    m_context_menu->add_separator();
                }

                m_context_menu->add_action(GUI::Action::create("&Copy as Text", MUST(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/edit-copy.png"sv)), [&](auto&) {
                    GUI::Clipboard::the().set_plain_text(text());
                }));
            }
            m_context_menu->popup(event.screen_position());
        };
    }

    RefPtr<GUI::Menu> m_context_menu;
};

class Database {
public:
    explicit Database(AppState& state)
        : m_state(state)
    {
        m_providers.append(make_ref_counted<AppProvider>());
        m_providers.append(make_ref_counted<CalculatorProvider>());
        m_providers.append(make_ref_counted<FileProvider>());
        m_providers.append(make_ref_counted<TerminalProvider>());
        m_providers.append(make_ref_counted<URLProvider>());
    }

    Function<void(NonnullRefPtrVector<Result>)> on_new_results;

    void search(DeprecatedString const& query)
    {
        for (auto& provider : m_providers) {
            provider.query(query, [=, this](auto results) {
                did_receive_results(query, results);
            });
        }
    }

private:
    void did_receive_results(DeprecatedString const& query, NonnullRefPtrVector<Result> const& results)
    {
        {
            Threading::MutexLocker db_locker(m_mutex);
            auto& cache_entry = m_result_cache.ensure(query);

            for (auto& result : results) {
                auto found = cache_entry.find_if([&result](auto& other) {
                    return result.equals(other);
                });

                if (found.is_end())
                    cache_entry.append(result);
            }
        }

        Threading::MutexLocker state_locker(m_state.lock);
        auto new_results = m_result_cache.find(m_state.last_query);
        if (new_results == m_result_cache.end())
            return;

        // NonnullRefPtrVector will provide dual_pivot_quick_sort references rather than pointers,
        // and dual_pivot_quick_sort requires being able to construct the underlying type on the
        // stack. Assistant::Result is pure virtual, thus cannot be constructed on the stack.
        auto& sortable_results = static_cast<Vector<NonnullRefPtr<Result>>&>(new_results->value);

        dual_pivot_quick_sort(sortable_results, 0, static_cast<int>(sortable_results.size() - 1), [](auto& a, auto& b) {
            return a->score() > b->score();
        });

        on_new_results(new_results->value);
    }

    AppState& m_state;

    NonnullRefPtrVector<Provider> m_providers;

    Threading::Mutex m_mutex;
    HashMap<DeprecatedString, NonnullRefPtrVector<Result>> m_result_cache;
};

}

static constexpr size_t MAX_SEARCH_RESULTS = 6;

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio recvfd sendfd rpath cpath unix proc exec thread"));

    Core::LockFile lockfile("/tmp/lock/assistant.lock");

    if (!lockfile.is_held()) {
        if (lockfile.error_code()) {
            warnln("Core::LockFile: {}", strerror(lockfile.error_code()));
            return 1;
        }

        // Another assistant is open, so exit silently.
        return 0;
    }

    auto app = TRY(GUI::Application::try_create(arguments));
    auto window = GUI::Window::construct();
    window->set_minimizable(false);

    Assistant::AppState app_state;
    Assistant::Database db { app_state };

    auto container = TRY(window->set_main_widget<GUI::Frame>());
    container->set_fill_with_background_color(true);
    container->set_frame_shape(Gfx::FrameShape::Window);
    auto& layout = container->set_layout<GUI::VerticalBoxLayout>();
    layout.set_margins({ 8 });

    auto& text_box = container->add<GUI::TextBox>();
    auto& results_container = container->add<GUI::Widget>();
    auto& results_layout = results_container.set_layout<GUI::VerticalBoxLayout>();

    auto mark_selected_item = [&]() {
        for (size_t i = 0; i < app_state.visible_result_count; ++i) {
            auto& row = static_cast<Assistant::ResultRow&>(results_container.child_widgets()[i]);
            row.set_font_weight(i == app_state.selected_index ? 700 : 400);
        }
    };

    text_box.on_change = [&]() {
        {
            Threading::MutexLocker locker(app_state.lock);
            if (app_state.last_query == text_box.text())
                return;

            app_state.last_query = text_box.text();
        }

        db.search(text_box.text());
    };
    text_box.on_return_pressed = [&]() {
        if (!app_state.selected_index.has_value())
            return;
        lockfile.release();
        app_state.results[app_state.selected_index.value()].activate();
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
        results_layout.set_margins(app_state.visible_result_count ? GUI::Margins { 4, 0, 0, 0 } : GUI::Margins { 0 });

        for (size_t i = 0; i < app_state.visible_result_count; ++i) {
            auto& result = app_state.results[i];
            auto& match = results_container.add<Assistant::ResultRow>();
            match.set_icon(result.bitmap());
            match.set_text(move(result.title()));
            match.set_tooltip(move(result.tooltip()));
            match.on_click = [&result](auto) {
                result.activate();
                GUI::Application::the()->quit();
            };
        }

        mark_selected_item();
        Core::deferred_invoke([&] { window->resize(GUI::Desktop::the().rect().width() / 3, {}); });
    });

    db.on_new_results = [&](auto results) {
        if (results.is_empty())
            app_state.selected_index = {};
        else
            app_state.selected_index = 0;
        app_state.results = results;
        app_state.visible_result_count = min(results.size(), MAX_SEARCH_RESULTS);

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
