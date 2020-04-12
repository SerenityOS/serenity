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

#include <LibGUI/AbstractStackView.h>
#include <LibGUI/Layout.h>
#include <LibGUI/ScrollBar.h>

namespace GUI {

AbstractStackView::AbstractStackView()
    : ScrollableWidget()
{
    set_greedy_for_hits(true);
    set_scrollbars_enabled(true);
}

AbstractStackView::~AbstractStackView() {}

void AbstractStackView::redirect_event(MouseEvent& event)
{
    if (auto* child = child_at(event.position())) {
        auto accepter = child->hit_test(event.position() - child->relative_position());
        accepter.widget->event(event);
    }
}

void AbstractStackView::mousewheel_event(MouseEvent& event)
{
    if (!is_scrollbars_enabled()) {
        event.ignore();
        return;
    }

    horizontal_scrollbar().set_value(horizontal_scrollbar().value() + event.wheel_delta() * 20);
}
void AbstractStackView::mousedown_event(MouseEvent& event) { redirect_event(event); }
void AbstractStackView::mouseup_event(MouseEvent& event) { redirect_event(event); }
void AbstractStackView::mousemove_event(MouseEvent& event)
{
    auto* child = child_at(event.position());
    if (child != hovered_child) {
        if (hovered_child) {
            hovered_child->event(*make<Event>(Event::Leave));
        }
        if (child) {
            child->event(*make<Event>(Event::Enter));
        }
        hovered_child = child;
    }
}

void AbstractStackView::enter_event(Core::Event&) {}
void AbstractStackView::leave_event(Core::Event&)
{
    if (hovered_child) {
        hovered_child->event(*make<Event>(Event::Leave));
    }
    hovered_child = nullptr;
}

}