/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullOwnPtrVector.h>
#include <LibGUI/Button.h>
#include <LibGUI/Widget.h>

namespace GUI {

class Toolbar : public Widget {
    C_OBJECT(Toolbar)
public:
    virtual ~Toolbar() override;

    ErrorOr<NonnullRefPtr<GUI::Button>> try_add_action(GUI::Action&);
    ErrorOr<void> try_add_separator();

    GUI::Button& add_action(GUI::Action&);
    void add_separator();

    bool has_frame() const { return m_has_frame; }
    void set_has_frame(bool has_frame) { m_has_frame = has_frame; }

protected:
    explicit Toolbar(Gfx::Orientation = Gfx::Orientation::Horizontal, int button_size = 16);

    virtual void paint_event(PaintEvent&) override;

private:
    struct Item {
        enum class Type {
            Invalid,
            Separator,
            Action
        };
        Type type { Type::Invalid };
        RefPtr<Action> action;
    };
    NonnullOwnPtrVector<Item> m_items;
    const Gfx::Orientation m_orientation;
    int m_button_size { 16 };
    bool m_has_frame { true };
};

}
