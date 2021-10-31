/*
 * Copyright (c) 2021, Spencer Dixon <spencercdixon@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Providers.h"
#include <AK/QuickSort.h>
#include <AK/String.h>
#include <LibCore/LockFile.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Event.h>
#include <LibGUI/Icon.h>
#include <LibGUI/ImageWidget.h>
#include <LibGUI/Label.h>
#include <LibGUI/Painter.h>
#include <LibGUI/TextBox.h>
#include <LibGfx/Palette.h>
#include <LibThreading/Mutex.h>
#include <string.h>
#include <unistd.h>

namespace Assistant {

struct AppState {
    Optional<size_t> selected_index;
    NonnullRefPtrVector<Result> results;
    size_t visible_result_count { 0 };

    Threading::Mutex lock;
    String last_query;
};

class ResultRow final : public GUI::Widget {
    C_OBJECT(ResultRow)
public:
    void set_image(RefPtr<Gfx::Bitmap> bitmap)
    {
        m_image->set_bitmap(bitmap);
    }
    void set_title(String text)
    {
        m_title->set_text(move(text));
    }
    void set_subtitle(String text)
    {
        if (text.is_empty()) {
            if (m_subtitle)
                m_subtitle->remove_from_parent();
            m_subtitle = nullptr;
            return;
        }
        if (!m_subtitle) {
            m_subtitle = m_label_container->add<GUI::Label>();
            m_subtitle->set_text_alignment(Gfx::TextAlignment::CenterLeft);
        }
        m_subtitle->set_text(move(text));
    }
    void set_is_highlighted(bool value)
    {
        if (m_is_highlighted == value)
            return;

        m_is_highlighted = value;
        m_title->set_font_weight(value ? 700 : 400);
    }

    Function<void()> on_selected;

private:
    ResultRow()
    {
        auto& layout = set_layout<GUI::HorizontalBoxLayout>();
        layout.set_spacing(12);
        layout.set_margins(4);

        m_image = add<GUI::ImageWidget>();

        m_label_container = add<GUI::Widget>();
        m_label_container->set_layout<GUI::VerticalBoxLayout>();
        m_label_container->set_fixed_height(30);

        m_title = m_label_container->add<GUI::Label>();
        m_title->set_text_alignment(Gfx::TextAlignment::CenterLeft);

        set_shrink_to_fit(true);
        set_fill_with_background_color(true);
        set_greedy_for_hits(true);
    }

    void mousedown_event(GUI::MouseEvent&) override
    {
        set_background_role(ColorRole::MenuBase);
    }

    void mouseup_event(GUI::MouseEvent&) override
    {
        set_background_role(ColorRole::NoRole);
        on_selected();
    }

    void enter_event(Core::Event&) override
    {
        set_background_role(ColorRole::HoverHighlight);
    }

    void leave_event(Core::Event&) override
    {
        set_background_role(ColorRole::NoRole);
    }

    RefPtr<GUI::ImageWidget> m_image;
    RefPtr<GUI::Widget> m_label_container;
    RefPtr<GUI::Label> m_title;
    RefPtr<GUI::Label> m_subtitle;
    bool m_is_highlighted { false };
};

class Database {
public:
    explicit Database(AppState& state)
        : m_state(state)
    {
        m_providers.append(make<AppProvider>());
        m_providers.append(make<CalculatorProvider>());
        m_providers.append(make<FileProvider>());
        m_providers.append(make<TerminalProvider>());
        m_providers.append(make<URLProvider>());
    }

    Function<void(NonnullRefPtrVector<Result>)> on_new_results;

    void search(String const& query)
    {
        for (auto& provider : m_providers) {
            provider.query(query, [=, this](auto results) {
                did_receive_results(query, results);
            });
        }
    }

private:
    void did_receive_results(String const& query, NonnullRefPtrVector<Result> const& results)
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

    NonnullOwnPtrVector<Provider> m_providers;

    Threading::Mutex m_mutex;
    HashMap<String, NonnullRefPtrVector<Result>> m_result_cache;
};

}

static constexpr size_t MAX_SEARCH_RESULTS = 6;

int main(int argc, char** argv)
{
    if (pledge("stdio recvfd sendfd rpath cpath unix proc exec thread", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    Core::LockFile lockfile("/tmp/lock/assistant.lock");

    if (!lockfile.is_held()) {
        if (lockfile.error_code()) {
            warnln("Core::LockFile: {}", strerror(lockfile.error_code()));
            return 1;
        }

        // Another assistant is open, so exit silently.
        return 0;
    }

    auto app = GUI::Application::construct(argc, argv);
    auto window = GUI::Window::construct();
    window->set_minimizable(false);

    Assistant::AppState app_state;
    Assistant::Database db { app_state };

    auto& container = window->set_main_widget<GUI::Frame>();
    container.set_fill_with_background_color(true);
    container.set_frame_shadow(Gfx::FrameShadow::Raised);
    auto& layout = container.set_layout<GUI::VerticalBoxLayout>();
    layout.set_margins({ 8, 8, 0 });

    auto& text_box = container.add<GUI::TextBox>();
    auto& results_container = container.add<GUI::Widget>();
    auto& results_layout = results_container.set_layout<GUI::VerticalBoxLayout>();
    results_layout.set_margins({ 10, 0 });

    auto mark_selected_item = [&]() {
        for (size_t i = 0; i < app_state.visible_result_count; ++i) {
            auto& row = static_cast<Assistant::ResultRow&>(results_container.child_widgets()[i]);
            row.set_is_highlighted(i == app_state.selected_index);
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

        for (size_t i = 0; i < app_state.visible_result_count; ++i) {
            auto& result = app_state.results[i];
            auto& match = results_container.add<Assistant::ResultRow>();
            match.set_image(result.bitmap());
            match.set_title(result.title());
            match.set_subtitle(result.subtitle());
            match.on_selected = [&result]() {
                result.activate();
                GUI::Application::the()->quit();
            };
        }

        mark_selected_item();

        auto window_height = app_state.visible_result_count * 40 + text_box.height() + 28;
        window->resize(GUI::Desktop::the().rect().width() / 3, window_height);
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

    window->set_frameless(true);
    window->set_forced_shadow(true);
    window->resize(GUI::Desktop::the().rect().width() / 3, 46);
    window->center_on_screen();
    window->move_to(window->x(), window->y() - (GUI::Desktop::the().rect().height() * 0.33));
    window->show();

    return app->exec();
}
