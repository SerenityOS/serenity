/*
 * Copyright (c) 2021, xSlendiX <gamingxslendix@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "TriviaWidget.h"
#include <AK/JsonArray.h>
#include <AK/JsonObject.h>
#include <AK/JsonValue.h>
#include <AK/QuickSort.h>
#include <AK/Random.h>
#include <Games/SereniTrivia/TriviaWindowGML.h>
#include <LibConfig/Client.h>
#include <LibCore/File.h>
#include <LibCore/Timer.h>
#include <LibGUI/AbstractButton.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/Label.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Window.h>
#include <LibGfx/FontDatabase.h>

namespace SereniTrivia {

void TriviaWidget::shuffle_vector()
{
    if (m_entries.size() > 1) {
        for (size_t i = 0; i < m_entries.size(); i++) {
            size_t j = get_random_uniform(m_entries.size());
            Entry e = m_entries[j];
            m_entries[j] = m_entries[i];
            m_entries[i] = e;
        }
    }
}

void TriviaWidget::check_button(size_t button_id)
{
    if ((u32)button_id == m_entries[0].answer()) {
        m_score += 100;
    } else {
        m_lives--;
    }

    m_lives_label->set_text(String::formatted("Lives: {}", m_lives));
    m_score_label->set_text(String::formatted("Score: {}", m_score));

    m_entries.take_first();
    LoadError err = load_random();
    if (err == GAME_FINISH) {
        if (m_score > Config::read_i32("SereniTrivia", "Score", "BestScore")) {
            Config::write_i32("SereniTrivia", "Score", "BestScore", m_score);
            m_best_score_label->set_text(String::formatted("Best score: {}", Config::read_i32("SereniTrivia", "Score", "BestScore", 0)));
        }

        int result = false;
        if (m_lives == 0)
            result = GUI::MessageBox::show(window(), String::formatted("Score: {}\nPlay again?", m_score), "You lose!", GUI::MessageBox::Type::None, GUI::MessageBox::InputType::YesNo);
        else
            result = GUI::MessageBox::show(window(), String::formatted("Score: {}\nLives remaining: {}\nPlay again?", m_score, m_lives), "You win!", GUI::MessageBox::Type::None, GUI::MessageBox::InputType::YesNo);

        if (result == GUI::Dialog::ExecResult::ExecYes) {
            m_lives = 5;
            m_score = 0;

            load_data(m_trivia_path);
            load_random();
        } else {
            window()->close();
        }
    }
}

int TriviaWidget::load_data(const String& path)
{
    auto file = Core::File::construct(path);
    if (!file->open(Core::OpenMode::ReadOnly)) {
        warnln("Couldn't open {} for reading: {}", path, file->error_string());
        return 1;
    }

    auto json = AK::JsonValue::from_string(file->read_all());
    if (!json.has_value()) {
        warnln("Couldn't parse {} as JSON", path);
        return 1;
    }

    AK::JsonValue& value = json.value();
    if (value.is_object()) {
        auto& object = value.as_object();
        auto& data = object.get("data");
        if (data.is_array()) {
            auto& data_array = data.as_array();
            for (size_t i = 0; i < data_array.size(); ++i) {
                if (data_array[i].is_object()) {
                    Optional<Entry> new_entry = Entry::try_parse(data_array[i]);
                    if (!new_entry.has_value()) {
                        warnln("Couldn't parse entry #{}!", i);
                        continue;
                    } else {
                        m_entries.append(new_entry.value());
                    }
                }
            }
        } else {
            warnln("Couldn't parse {}: Data is not an array", path);
        }
    } else {
        warnln("Couldn't parse {}: Not an object", path);
        return 1;
    }

    dbgln("Parsed game data from `{}` successfully. Entries: {}", path, m_entries.size());

    return 0;
}

void TriviaWidget::timer_event(Core::TimerEvent&)
{
    size_t children_number = m_choice_buttons->children().size();
    if (m_children_visible == children_number - 1) {
        m_choice_buttons->for_each_child_of_type<GUI::Button>([&](auto& child) {
            child.set_enabled(true);
            child.set_visible(true);
            return IterationDecision::Continue;
        });
        stop_timer();
        return;
    }

    auto& children = m_choice_buttons->children();

    if (is<GUI::Button>(children[m_children_visible])) {
        static_cast<GUI::Button&>(children[m_children_visible]).set_visible(true);
        m_children_visible++;
    }
}

LoadError TriviaWidget::load_current()
{
    if (
        m_prompt_label.is_null() || m_choice_buttons.is_null()) {
        return UNINTIALIZED_WIDGET;
    }

    if (m_entries.size() < 1)
        return GAME_FINISH;

    m_prompt_label->set_text(m_entries[0].prompt());

    m_choice_buttons->remove_all_children();

    for (size_t j = 0; j < m_entries[0].answers().size(); j++) {
        m_choice_buttons->add<GUI::Button>(m_entries[0].answers()[j]).on_click = [&, j](auto) {
            check_button(j);
        };
    }

    m_timers.clear();
    m_children_visible = 0;
    m_choice_buttons->for_each_child_of_type<GUI::Button>([&](auto& child) {
        child.set_visible(false);
        child.set_enabled(false);
        return IterationDecision::Continue;
    });

    start_timer(500);

    return OK;
}

LoadError TriviaWidget::load_random()
{
    if (m_entries.is_empty() || m_lives == 0)
        return GAME_FINISH;

    shuffle_vector();

    return load_current();
}

TriviaWidget::TriviaWidget()
{
    load_from_gml(trivia_window_gml);

    m_score_label = *find_descendant_of_type_named<GUI::Label>("score_label");
    m_best_score_label = *find_descendant_of_type_named<GUI::Label>("best_score_label");
    m_lives_label = *find_descendant_of_type_named<GUI::Label>("lives_label");
    m_prompt_label = *find_descendant_of_type_named<GUI::Label>("prompt_label");
    m_prompt_label->set_font(Gfx::FontDatabase::the().get("Liberation Serif", "Regular", 17));
    m_choice_buttons = *find_descendant_of_type_named<GUI::Widget>("choice_buttons");

    m_best_score_label->set_text(String::formatted("Best score: {}", Config::read_i32("SereniTrivia", "Score", "BestScore", 0)));

    load_data(m_trivia_path);
    load_random();
}

TriviaWidget::~TriviaWidget()
{
}

}
