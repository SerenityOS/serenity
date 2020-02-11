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

#include <LibGUI/Frame.h>

namespace GUI {

class Splitter : public Frame {
    C_OBJECT(Splitter)
public:
    virtual ~Splitter() override;

protected:
    Splitter(Orientation, Widget* parent);

    virtual void mousedown_event(MouseEvent&) override;
    virtual void mousemove_event(MouseEvent&) override;
    virtual void mouseup_event(MouseEvent&) override;
    virtual void enter_event(Core::Event&) override;
    virtual void leave_event(Core::Event&) override;

private:
    void get_resize_candidates_at(const Gfx::Point&, Widget*&, Widget*&);

    Orientation m_orientation;
    bool m_resizing { false };
    Gfx::Point m_resize_origin;
    WeakPtr<Widget> m_first_resizee;
    WeakPtr<Widget> m_second_resizee;
    Gfx::Size m_first_resizee_start_size;
    Gfx::Size m_second_resizee_start_size;
};

class VerticalSplitter final : public Splitter {
    C_OBJECT(VerticalSplitter)
public:
    virtual ~VerticalSplitter() override {}

private:
    explicit VerticalSplitter(Widget* parent)
        : Splitter(Orientation::Vertical, parent)
    {
    }
};

class HorizontalSplitter final : public Splitter {
    C_OBJECT(HorizontalSplitter)
public:
    virtual ~HorizontalSplitter() override {}

private:
    explicit HorizontalSplitter(Widget* parent)
        : Splitter(Orientation::Horizontal, parent)
    {
    }
};

}
