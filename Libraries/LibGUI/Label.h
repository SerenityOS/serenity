/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <LibGfx/TextAlignment.h>
#include <LibGUI/Frame.h>

namespace GUI {

class Label : public Frame {
    C_OBJECT(Label)
public:
    virtual ~Label() override;

    String text() const { return m_text; }
    void set_text(const StringView&);

    void set_icon(Gfx::Bitmap*);
    const Gfx::Bitmap* icon() const { return m_icon.ptr(); }
    Gfx::Bitmap* icon() { return m_icon.ptr(); }

    Gfx::TextAlignment text_alignment() const { return m_text_alignment; }
    void set_text_alignment(Gfx::TextAlignment text_alignment) { m_text_alignment = text_alignment; }

    bool should_stretch_icon() const { return m_should_stretch_icon; }
    void set_should_stretch_icon(bool b) { m_should_stretch_icon = b; }

    void size_to_fit();

protected:
    explicit Label(Widget* parent = nullptr);
    Label(const StringView& text, Widget* parent = nullptr);

    virtual void paint_event(PaintEvent&) override;

private:
    String m_text;
    RefPtr<Gfx::Bitmap> m_icon;
    Gfx::TextAlignment m_text_alignment { Gfx::TextAlignment::Center };
    bool m_should_stretch_icon { false };
};

}
