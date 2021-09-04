/*
 * Copyright (c) 2021, xSlendiX <gamingxslendix@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Entry.h"
#include <AK/JsonArray.h>
#include <AK/JsonObject.h>
#include <AK/JsonValue.h>
#include <LibCore/Timer.h>
#include <LibGUI/Label.h>
#include <LibGUI/Widget.h>
#include <LibWeb/OutOfProcessWebView.h>

namespace SereniTrivia {

enum LoadError {
    OK,
    GAME_FINISH,
    DATA_EMPTY,
    UNINTIALIZED_WIDGET,
};

class TriviaWidget final : public GUI::Widget {
    C_OBJECT(TriviaWidget);

public:
    virtual ~TriviaWidget() override;

    int load_data(const String& path);
    LoadError load_current();
    LoadError load_random();
    void check_button(size_t button_id);

private:
    TriviaWidget();
    virtual void timer_event(Core::TimerEvent& event) override;

    void shuffle_vector();

    RefPtr<GUI::Label> m_score_label;
    RefPtr<GUI::Label> m_best_score_label;
    RefPtr<GUI::Label> m_lives_label;
    RefPtr<GUI::Label> m_prompt_label;

    RefPtr<GUI::Widget> m_choice_buttons;

    Vector<Entry> m_entries;
    Vector<Core::Timer> m_timers;

    i32 m_score = 0;
    i32 m_lives = 5;

    u32 m_children_visible = 0;

    const String m_trivia_path = "/res/trivia.json";
};

}
