/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Button.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Widget.h>

namespace GUI {

class Toolbar : public Widget {
    C_OBJECT(Toolbar)
public:
    virtual ~Toolbar() override = default;

    GUI::Button& add_action(GUI::Action&);
    void add_separator();

    bool is_collapsible() const { return m_collapsible; }
    void set_collapsible(bool b) { m_collapsible = b; }
    bool is_grouped() const { return m_grouped; }
    void set_grouped(bool b) { m_grouped = b; }

    virtual Optional<UISize> calculated_preferred_size() const override;
    virtual Optional<UISize> calculated_min_size() const override;

protected:
    explicit Toolbar(Gfx::Orientation = Gfx::Orientation::Horizontal, int button_size = 24);

    virtual void paint_event(PaintEvent&) override;
    virtual void resize_event(GUI::ResizeEvent&) override;

    ErrorOr<void> update_overflow_menu();
    ErrorOr<void> create_overflow_objects();

private:
    struct Item {
        enum class Type {
            Invalid,
            Separator,
            Action
        };
        Type type { Type::Invalid };
        RefPtr<Action> action;
        RefPtr<Widget> widget;
    };
    Vector<NonnullOwnPtr<Item>> m_items;
    RefPtr<Menu> m_overflow_menu;
    RefPtr<Action> m_overflow_action;
    RefPtr<Button> m_overflow_button;
    Gfx::Orientation const m_orientation;
    int m_button_size { 24 };
    bool m_collapsible { false };
    bool m_grouped { false };
};

}
