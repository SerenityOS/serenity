/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Frame.h>
#include <LibGUI/WordWrapMode.h>
#include <LibGfx/TextAlignment.h>

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

    bool should_stretch_icon() const { return m_should_stretch_icon; }
    void set_should_stretch_icon(bool b) { m_should_stretch_icon = b; }

    bool is_autosize() const { return m_autosize; }
    void set_autosize(bool);

    bool is_word_wrap() const { return m_word_wrap; }
    void set_word_wrap(bool);

    GUI::WordWrapMode word_wrap_mode() const { return m_word_wrap_mode; }
    void set_word_wrap_mode(GUI::WordWrapMode mode) { m_word_wrap_mode = mode; }

    Gfx::IntRect text_rect(size_t line = 0) const;

protected:
    explicit Label(String text = {});

    virtual void paint_event(PaintEvent&) override;
    virtual void did_change_text() { }

private:
    void size_to_fit();
    void wrap_text();
    size_t next_substring_size(String*, size_t);

    String m_text;
    RefPtr<Gfx::Bitmap> m_icon;
    Gfx::TextAlignment m_text_alignment { Gfx::TextAlignment::Center };
    bool m_should_stretch_icon { false };
    bool m_autosize { false };
    bool m_word_wrap { false };
    GUI::WordWrapMode m_word_wrap_mode { GUI::WordWrapMode::Word };
    Vector<String> m_lines;
};

}
