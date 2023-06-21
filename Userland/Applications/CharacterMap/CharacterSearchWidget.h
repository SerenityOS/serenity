/*
 * Copyright (c) 2022-2023, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "CharacterMapWidget.h"
#include <LibGUI/Button.h>
#include <LibGUI/TableView.h>
#include <LibGUI/TextBox.h>

class CharacterSearchWidget final : public GUI::Widget {
    C_OBJECT(CharacterSearchWidget);

public:
    virtual ~CharacterSearchWidget() override = default;

    Function<void(u32)> on_character_selected;

private:
    CharacterSearchWidget();

    void search();

    RefPtr<GUI::TextBox> m_search_input;
    RefPtr<GUI::TableView> m_results_table;
};
