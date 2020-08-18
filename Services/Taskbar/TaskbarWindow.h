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

#include "WindowList.h"
#include <LibGUI/Widget.h>
#include <LibGUI/Window.h>

class TaskbarWindow final : public GUI::Window {
    C_OBJECT(TaskbarWindow)
public:
    TaskbarWindow();
    virtual ~TaskbarWindow() override;

    int taskbar_height() const { return 28; }

private:
    void create_quick_launch_bar();
    void on_screen_rect_change(const Gfx::IntRect&);
    NonnullRefPtr<GUI::Button> create_button(const WindowIdentifier&);
    void add_window_button(::Window&, const WindowIdentifier&);
    void remove_window_button(::Window&, bool);
    void update_window_button(::Window&, bool);
    ::Window* find_window_owner(::Window&) const;

    virtual void wm_event(GUI::WMEvent&) override;

    RefPtr<Gfx::Bitmap> m_default_icon;
};
