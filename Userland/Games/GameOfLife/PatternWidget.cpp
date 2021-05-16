/*
 * Copyright (c) 2021, Ryan Wilson <ryan@rdwilson.xyz>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "PatternWidget.h"
#include "Pattern.h"
#include <AK/StringBuilder.h>
#include <LibGUI/Action.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/Toolbar.h>
#include <LibGUI/Window.h>

class PatternButton final : public GUI::Button {
    C_OBJECT(PatternButton)
public:
    PatternButton(PatternWidget& patternbox, const String& name, const GUI::Shortcut& shortcut, OwnPtr<Pattern> pattern)
        : m_patternbox(patternbox)
        , m_pattern(move(pattern))
    {
        StringBuilder builder;
        builder.append(name);
        builder.append(" (");
        builder.append(shortcut.to_string());
        builder.append(")");
        set_tooltip(builder.to_string());

        m_action = GUI::Action::create_checkable(
            name, shortcut, [this](auto& action) {
                if (action.is_checked())
                    m_patternbox.on_pattern_selection(m_pattern);
                else
                    m_patternbox.on_pattern_selection(nullptr);
            },
            patternbox.window());

        m_pattern->set_action(m_action);
        set_action(*m_action);
        m_patternbox.m_action_group.add_action(*m_action);
    }

    const Pattern& pattern() const { return *m_pattern; }
    Pattern& pattern() { return *m_pattern; }

    virtual bool is_uncheckable() const override { return false; }

    virtual void context_menu_event(GUI::ContextMenuEvent& event) override
    {
        m_action->activate();
        m_pattern->on_pattern_button_contextmenu(event);
    }

private:
    PatternWidget& m_patternbox;
    OwnPtr<Pattern> m_pattern;
    RefPtr<GUI::Action> m_action;
};

PatternWidget::PatternWidget()
{
    set_fill_with_background_color(true);

    set_fixed_width(26);

    set_layout<GUI::VerticalBoxLayout>();
    layout()->set_spacing(0);
    layout()->set_margins({ 2, 2, 2, 2 });

    m_action_group.set_exclusive(true);
    m_action_group.set_unchecking_allowed(true);

    m_toolbar = add<GUI::Toolbar>(Gfx::Orientation::Vertical);
    setup_patterns();
}

PatternWidget::~PatternWidget()
{
}

void PatternWidget::setup_patterns()
{
    auto add_pattern = [&](String name, NonnullOwnPtr<Pattern> pattern) {
        auto action = GUI::Action::create_checkable(move(name),
            [this, pattern = pattern.ptr()](auto& action) {
                if (action.is_checked())
                    on_pattern_selection(pattern);
                else
                    on_pattern_selection(nullptr);
            });
        m_action_group.add_action(action);
        auto& button = m_toolbar->add_action(action);
        button.on_context_menu_request = [action = action.ptr(), tool = pattern.ptr()](auto& event) {
            action->activate();
            tool->on_pattern_button_contextmenu(event);
        };
        pattern->set_action(action);
        m_patterns.append(move(pattern));
    };

    Vector<String> blinker = {
        "OOO"
    };

    Vector<String> toad = {
        ".OOO",
        "OOO."
    };

    Vector<String> glider = {
        ".O.",
        "..O",
        "OOO",
    };

    Vector<String> lightweight_spaceship = {
        ".OO..",
        "OOOO.",
        "OO.OO",
        "..OO."
    };

    Vector<String> middleweight_spaceship = {
        ".OOOOO",
        "O....O",
        ".....O",
        "O...O.",
        "..O..."
    };

    Vector<String> heavyweight_spaceship = {
        "..OO...",
        "O....O.",
        "......O",
        "O.....O",
        ".OOOOOO"
    };

    Vector<String> infinite_1 = { "OOOOOOOO.OOOOO...OOO......OOOOOOO.OOOOO" };

    Vector<String> infinite_2 = {
        "......O.",
        "....O.OO",
        "....O.O.",
        "....O...",
        "..O.....",
        "O.O....."
    };

    Vector<String> infinite_3 = {
        "OOO.O",
        "O....",
        "...OO",
        ".OO.O",
        "O.O.O"
    };

    Vector<String> simkin_glider_gun = {
        "OO.....OO........................",
        "OO.....OO........................",
        ".................................",
        "....OO...........................",
        "....OO...........................",
        ".................................",
        ".................................",
        ".................................",
        ".................................",
        "......................OO.OO......",
        ".....................O.....O.....",
        ".....................O......O..OO",
        ".....................OOO...O...OO",
        "..........................O......",
        ".................................",
        ".................................",
        ".................................",
        "....................OO...........",
        "....................O............",
        ".....................OOO.........",
        ".......................O........."
    };
    Vector<String> gosper_glider_gun = {
        "........................O...........",
        "......................O.O...........",
        "............OO......OO............OO",
        "...........O...O....OO............OO",
        "OO........O.....O...OO..............",
        "OO........O...O.OO....O.O...........",
        "..........O.....O.......O...........",
        "...........O...O....................",
        "............OO......................"
    };

    Vector<String> r_pentomino = {
        ".OO",
        "OO.",
        ".O."
    };

    Vector<String> diehard = {
        "......O.",
        "OO......",
        ".O...OOO"
    };

    Vector<String> acorn = {
        ".O.....",
        "...O...",
        "OO..OOO"
    };

    add_pattern("Blinker", make<Pattern>(blinker));
    add_pattern("Toad", make<Pattern>(toad));
    add_pattern("Glider", make<Pattern>(glider));
    add_pattern("Lightweight Spaceship", make<Pattern>(lightweight_spaceship));
    add_pattern("Middleweight Spaceship", make<Pattern>(middleweight_spaceship));
    add_pattern("Heavyweight Spaceship", make<Pattern>(heavyweight_spaceship));
    add_pattern("Infinite 1", make<Pattern>(infinite_1));
    add_pattern("Infinite 2", make<Pattern>(infinite_2));
    add_pattern("Infinite 3", make<Pattern>(infinite_3));
    add_pattern("R-Pentomino", make<Pattern>(r_pentomino));
    add_pattern("Diehard", make<Pattern>(diehard));
    add_pattern("Acorn", make<Pattern>(acorn));
    add_pattern("Simkin's Glider Gun", make<Pattern>(simkin_glider_gun));
    add_pattern("Gosper's Glider Gun", make<Pattern>(gosper_glider_gun));
}
