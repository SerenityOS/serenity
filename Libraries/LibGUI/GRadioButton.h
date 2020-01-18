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

#include <LibGUI/GAbstractButton.h>

class GRadioButton : public GAbstractButton {
    C_OBJECT(GRadioButton)
public:
    virtual ~GRadioButton() override;

    virtual void click() override;

protected:
    explicit GRadioButton(GWidget* parent);
    explicit GRadioButton(const StringView& text, GWidget* parent);
    virtual void paint_event(GPaintEvent&) override;

private:
    // These don't make sense for a radio button, so hide them.
    using GAbstractButton::auto_repeat_interval;
    using GAbstractButton::set_auto_repeat_interval;

    virtual bool is_radio_button() const final { return true; }

    template<typename Callback>
    void for_each_in_group(Callback);
    static Size circle_size();
};

template<>
inline bool is<GRadioButton>(const CObject& object)
{
    if (!is<GWidget>(object))
        return false;
    return to<GWidget>(object).is_radio_button();
}
