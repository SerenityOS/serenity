/*
 * Copyright (c) 2021-2023, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Frame.h>
#include <LibGfx/Bitmap.h>

namespace GUI {

class Tray : public GUI::Frame {
    C_OBJECT(Tray);

public:
    virtual ~Tray() override = default;

    size_t add_item(ByteString text, RefPtr<Gfx::Bitmap const>, ByteString custom_data);

    void set_item_checked(size_t index, bool);

    Function<void(ByteString const&)> on_item_activation;

protected:
    virtual void paint_event(GUI::PaintEvent&) override;
    virtual void mousemove_event(GUI::MouseEvent&) override;
    virtual void mousedown_event(GUI::MouseEvent&) override;
    virtual void mouseup_event(GUI::MouseEvent&) override;
    virtual void leave_event(Core::Event&) override;
    virtual void focusin_event(GUI::FocusEvent&) override;
    virtual void focusout_event(GUI::FocusEvent&) override;
    virtual void keydown_event(GUI::KeyEvent&) override;

private:
    Tray();

    struct Item {
        ByteString text;
        RefPtr<Gfx::Bitmap const> bitmap;
        ByteString custom_data;
        size_t index { 0 };
        Gfx::IntRect rect(Tray const&) const;
    };

    Item* item_at(Gfx::IntPoint);

    Vector<Item> m_items;

    Optional<size_t> m_pressed_item_index;
    Optional<size_t> m_hovered_item_index;
    Optional<size_t> m_checked_item_index;
};

}
