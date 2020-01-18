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

#include <AK/Function.h>
#include <AK/String.h>
#include <LibDraw/GraphicsBitmap.h>
#include <LibDraw/StylePainter.h>
#include <LibDraw/TextAlignment.h>
#include <LibGUI/GAbstractButton.h>

class GAction;

class GButton : public GAbstractButton {
    C_OBJECT(GButton)
public:
    virtual ~GButton() override;

    void set_icon(RefPtr<GraphicsBitmap>&&);
    const GraphicsBitmap* icon() const { return m_icon.ptr(); }
    GraphicsBitmap* icon() { return m_icon.ptr(); }

    void set_text_alignment(TextAlignment text_alignment) { m_text_alignment = text_alignment; }
    TextAlignment text_alignment() const { return m_text_alignment; }

    Function<void(GButton&)> on_click;

    void set_button_style(ButtonStyle style) { m_button_style = style; }
    ButtonStyle button_style() const { return m_button_style; }

    virtual void click() override;

    void set_action(GAction&);

    virtual bool accepts_focus() const override { return m_focusable; }
    virtual bool is_uncheckable() const override;

    void set_focusable(bool b) { m_focusable = b; }

protected:
    GButton(const StringView& text, GWidget* parent);
    explicit GButton(GWidget* parent);
    virtual void paint_event(GPaintEvent&) override;

private:
    RefPtr<GraphicsBitmap> m_icon;
    ButtonStyle m_button_style { ButtonStyle::Normal };
    TextAlignment m_text_alignment { TextAlignment::Center };
    WeakPtr<GAction> m_action;
    bool m_focusable { true };
};
