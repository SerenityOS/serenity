/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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
    virtual ~Label() override;

    String text() const { return m_text; }
    void set_text(String);

    void set_icon(const Gfx::Bitmap*);
    const Gfx::Bitmap* icon() const { return m_icon.ptr(); }
    Gfx::Bitmap* icon() { return m_icon.ptr(); }

    Gfx::TextAlignment text_alignment() const { return m_text_alignment; }
    void set_text_alignment(Gfx::TextAlignment text_alignment) { m_text_alignment = text_alignment; }

    Gfx::TextWrapping text_wrapping() const { return m_text_wrapping; }
    void set_text_wrapping(Gfx::TextWrapping text_wrapping) { m_text_wrapping = text_wrapping; }

    bool should_stretch_icon() const { return m_should_stretch_icon; }
    void set_should_stretch_icon(bool b) { m_should_stretch_icon = b; }

    bool is_autosize() const { return m_autosize; }
    void set_autosize(bool);

    int preferred_height() const;

    Gfx::IntRect text_rect() const;

protected:
    explicit Label(String text = {});

    virtual void paint_event(PaintEvent&) override;
    virtual void did_change_text() { }

private:
    void size_to_fit();

    String m_text;
    RefPtr<Gfx::Bitmap> m_icon;
    Gfx::TextAlignment m_text_alignment { Gfx::TextAlignment::Center };
    Gfx::TextWrapping m_text_wrapping { Gfx::TextWrapping::Wrap };
    bool m_should_stretch_icon { false };
    bool m_autosize { false };
};

}
