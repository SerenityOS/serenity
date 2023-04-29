/*
 * Copyright (c) 2018-2023, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Frame.h>
#include <LibGfx/TextAlignment.h>
#include <LibGfx/TextWrapping.h>

namespace GUI {

class Label : public Frame {
    C_OBJECT(Label);

public:
    virtual ~Label() override = default;

    String const& text() const { return m_text; }
    void set_text(String);

    Gfx::TextAlignment text_alignment() const { return m_text_alignment; }
    void set_text_alignment(Gfx::TextAlignment text_alignment) { m_text_alignment = text_alignment; }

    Gfx::TextWrapping text_wrapping() const { return m_text_wrapping; }
    void set_text_wrapping(Gfx::TextWrapping text_wrapping) { m_text_wrapping = text_wrapping; }

    bool is_autosize() const { return m_autosize; }
    void set_autosize(bool, size_t padding = 0);

    virtual Optional<UISize> calculated_min_size() const override;
    virtual Optional<UISize> calculated_preferred_size() const override;
    int text_calculated_preferred_height() const;
    int text_calculated_preferred_width() const;

    Gfx::IntRect text_rect() const;

protected:
    explicit Label(String text = {});

    virtual void paint_event(PaintEvent&) override;
    virtual void did_change_font() override;
    virtual void did_change_text() { }

private:
    void size_to_fit();

    String m_text;
    Gfx::TextAlignment m_text_alignment { Gfx::TextAlignment::Center };
    Gfx::TextWrapping m_text_wrapping { Gfx::TextWrapping::Wrap };
    bool m_autosize { false };
    size_t m_autosize_padding { 0 };
};

}
