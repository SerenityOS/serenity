/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <LibGUI/FilteringProxyModel.h>
#include <LibGUI/Widget.h>

namespace HackStudio {

class Locator final : public GUI::Widget {
    C_OBJECT(Locator)
public:
    virtual ~Locator() override = default;

    void open();
    void close();

private:
    void update_suggestions();
    void open_suggestion(const GUI::ModelIndex&);

    Locator(Core::EventReceiver* parent = nullptr);

    RefPtr<GUI::TextBox> m_textbox;
    RefPtr<GUI::Window> m_popup_window;
    RefPtr<GUI::TableView> m_suggestion_view;
    RefPtr<GUI::FilteringProxyModel> m_model;
};

}
