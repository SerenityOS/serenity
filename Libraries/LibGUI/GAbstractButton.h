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

#include <LibCore/Timer.h>
#include <LibGfx/TextAlignment.h>
#include <LibGUI/GWidget.h>

namespace GUI {

class Painter;

class AbstractButton : public Widget {
    C_OBJECT_ABSTRACT(GAbstractButton)
public:
    virtual ~AbstractButton() override;

    Function<void(bool)> on_checked;

    void set_text(const StringView&);
    const String& text() const { return m_text; }

    bool is_exclusive() const { return m_exclusive; }
    void set_exclusive(bool b) { m_exclusive = b; }

    bool is_checked() const { return m_checked; }
    void set_checked(bool);

    bool is_checkable() const { return m_checkable; }
    void set_checkable(bool);

    bool is_hovered() const { return m_hovered; }
    bool is_being_pressed() const { return m_being_pressed; }

    virtual void click() = 0;
    virtual bool accepts_focus() const override { return true; }
    virtual bool is_uncheckable() const { return true; }

    int auto_repeat_interval() const { return m_auto_repeat_interval; }
    void set_auto_repeat_interval(int interval) { m_auto_repeat_interval = interval; }

protected:
    explicit AbstractButton(Widget* parent);
    AbstractButton(const StringView&, Widget* parent);

    virtual void mousedown_event(MouseEvent&) override;
    virtual void mousemove_event(MouseEvent&) override;
    virtual void mouseup_event(MouseEvent&) override;
    virtual void keydown_event(KeyEvent&) override;
    virtual void enter_event(Core::Event&) override;
    virtual void leave_event(Core::Event&) override;
    virtual void change_event(Event&) override;

    void paint_text(Painter&, const Gfx::Rect&, const Gfx::Font&, Gfx::TextAlignment);

private:
    virtual bool is_abstract_button() const final { return true; }

    String m_text;
    bool m_checked { false };
    bool m_checkable { false };
    bool m_hovered { false };
    bool m_being_pressed { false };
    bool m_exclusive { false };

    int m_auto_repeat_interval { 0 };
    RefPtr<Core::Timer> m_auto_repeat_timer;
};

}

template<>
inline bool Core::is<GUI::AbstractButton>(const Core::Object& object)
{
    if (!is<GUI::Widget>(object))
        return false;
    return to<GUI::Widget>(object).is_abstract_button();
}
