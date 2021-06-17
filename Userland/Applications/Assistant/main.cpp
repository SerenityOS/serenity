/*
 * Copyright (c) 2021, Spencer Dixon <spencercdixon@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Providers.h"
#include <AK/QuickSort.h>
#include <AK/String.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Event.h>
#include <LibGUI/Icon.h>
#include <LibGUI/ImageWidget.h>
#include <LibGUI/Label.h>
#include <LibGUI/Painter.h>
#include <LibGUI/TextBox.h>
#include <LibGfx/Palette.h>
#include <LibThreading/Lock.h>

namespace Assistant {

struct AppState {
    size_t selected_index { 0 };
    Vector<NonnullRefPtr<Result>> results;
    size_t visible_result_count { 0 };

    Threading::Lock lock;
    String last_query;
};

class ResultRow final : public GUI::Widget {
    C_OBJECT(ResultRow)
public:
    ResultRow()
    {
        auto& layout = set_layout<GUI::HorizontalBoxLayout>();
        layout.set_spacing(12);
        layout.set_margins({ 4, 4, 4, 4 });

        m_image = add<GUI::ImageWidget>();

        m_label_container = add<GUI::Widget>();
        m_label_container->set_layout<GUI::VerticalBoxLayout>();
        m_label_container->set_fixed_height(30);

        m_title = m_label_container->add<GUI::Label>();
        m_title->set_text_alignment(Gfx::TextAlignment::CenterLeft);

        set_shrink_to_fit(true);
        set_fill_with_background_color(true);
        set_global_cursor_tracking(true);
        set_greedy_for_hits(true);
    }

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
    }

    Function<void(Vector<NonnullRefPtr<Result>>)> on_new_results;

    void search(String const& query)
    {

        m_app_provider.query(query, [=, this](auto results) {
            recv_results(query, results);
        });

        m_calculator_provider.query(query, [=, this](auto results) {
            recv_results(query, results);
        });
    }

private:
    void recv_results(String const& query, Vector<NonnullRefPtr<Result>> const& results)
    {
        {
            Threading::Locker db_locker(m_lock);
            auto it = m_result_cache.find(query);
            if (it == m_result_cache.end()) {
                m_result_cache.set(query, {});
            }
            it = m_result_cache.find(query);

            for (auto& result : results) {
                auto found = it->value.find_if([result](auto& other) {
                    return result->equals(other);
                });

                if (found.is_end())
                    it->value.append(result);
            }
        }

        Threading::Locker state_locker(m_state.lock);
        auto new_results = m_result_cache.find(m_state.last_query);
        if (new_results == m_result_cache.end())
            return;

        dual_pivot_quick_sort(new_results->value, 0, static_cast<int>(new_results->value.size() - 1), [](auto& a, auto& b) {
            return a->score() > b->score();
        });

        on_new_results(new_results->value);
    }

    AppState& m_state;

    AppProvider m_app_provider;
    CalculatorProvider m_calculator_provider;

    Threading::Lock m_lock;
    HashMap<String, Vector<NonnullRefPtr<Result>>> m_result_cache;
};

}

static constexpr size_t MAX_SEARCH_RESULTS = 6;

int main(int argc, char** argv)
{
    if (pledge("stdio recvfd sendfd rpath unix proc exec", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto app = GUI::Application::construct(argc, argv);
    auto window = GUI::Window::construct();

    Assistant::AppState app_state;
    Assistant::Database db { app_state };

    auto& container = window->set_main_widget<GUI::Widget>();
    container.set_fill_with_background_color(true);
    auto& layout = container.set_layout<GUI::VerticalBoxLayout>();
    layout.set_margins({ 8, 8, 8, 0 });

    auto& text_box = container.add<GUI::TextBox>();
    auto& results_container = container.add<GUI::Widget>();
    auto& results_layout = results_container.set_layout<GUI::VerticalBoxLayout>();
    results_layout.set_margins({ 0, 10, 0, 10 });

    auto mark_selected_item = [&]() {
        for (size_t i = 0; i < app_state.visible_result_count; ++i) {
            auto& row = dynamic_cast<Assistant::ResultRow&>(results_container.child_widgets()[i]);
            row.set_is_highlighted(i == app_state.selected_index);
        }
    };

    text_box.on_change = [&]() {
        {
            Threading::Locker locker(app_state.lock);
            if (app_state.last_query == text_box.text())
                return;

            app_state.last_query = text_box.text();
        }

        db.search(text_box.text());
    };
    text_box.on_return_pressed = [&]() {
        app_state.results[app_state.selected_index]->activate();
        exit(0);
    };
    text_box.on_up_pressed = [&]() {
        if (app_state.selected_index == 0)
            app_state.selected_index = app_state.visible_result_count - 1;
        else if (app_state.selected_index > 0)
            app_state.selected_index -= 1;

        mark_selected_item();
    };
    text_box.on_down_pressed = [&]() {
        if ((app_state.visible_result_count - 1) == app_state.selected_index)
            app_state.selected_index = 0;
        else if (app_state.visible_result_count > app_state.selected_index)
            app_state.selected_index += 1;

        mark_selected_item();
    };
    text_box.on_escape_pressed = []() {
        exit(0);
    };

    db.on_new_results = [&](auto results) {
        app_state.selected_index = 0;
        app_state.results = results;
        app_state.visible_result_count = min(results.size(), MAX_SEARCH_RESULTS);
        results_container.remove_all_children();

        for (size_t i = 0; i < app_state.visible_result_count; ++i) {
            auto result = app_state.results[i];
            auto& match = results_container.add<Assistant::ResultRow>();
            match.set_image(result->bitmap());
            match.set_title(result->title());
            match.on_selected = [result_copy = result]() {
                result_copy->activate();
                exit(0);
            };

            if (result->kind() == Assistant::Result::Kind::Calculator) {
                match.set_subtitle("'Enter' will copy to clipboard");
            }
        }

        mark_selected_item();

        auto window_height = app_state.visible_result_count * 40 + text_box.height() + 28;
        window->resize(GUI::Desktop::the().rect().width() / 3, window_height);
    };

    window->set_frameless(true);
    window->resize(GUI::Desktop::the().rect().width() / 3, 46);
    window->center_on_screen();
    window->move_to(window->x(), window->y() - (GUI::Desktop::the().rect().height() * 0.33));
    window->show();

    return app->exec();
}
