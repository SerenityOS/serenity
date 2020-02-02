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

#include <LibDraw/StylePainter.h>
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GLabel.h>
#include <LibGUI/GPainter.h>
#include <LibGUI/GResizeCorner.h>
#include <LibGUI/GStatusBar.h>

GStatusBar::GStatusBar(GWidget* parent)
    : GStatusBar(1, parent)
{
}

GStatusBar::GStatusBar(int label_count, GWidget* parent)
    : GWidget(parent)
{
    set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
    set_preferred_size(0, 20);
    set_layout(make<GHBoxLayout>());
    layout()->set_margins({ 2, 2, 2, 2 });
    layout()->set_spacing(2);

    if (label_count < 1)
        label_count = 1;

    for (auto i = 0; i < label_count; i++)
        m_labels.append(create_label());

    m_corner = GResizeCorner::construct(this);
}

GStatusBar::~GStatusBar()
{
}

NonnullRefPtr<GLabel> GStatusBar::create_label()
{
    auto label = GLabel::construct(this);
    label->set_frame_shadow(FrameShadow::Sunken);
    label->set_frame_shape(FrameShape::Panel);
    label->set_frame_thickness(1);
    label->set_text_alignment(TextAlignment::CenterLeft);
    return label;
}

void GStatusBar::set_text(const StringView& text)
{
    m_labels.first().set_text(text);
}

String GStatusBar::text() const
{
    return m_labels.first().text();
}

void GStatusBar::set_text(int index, const StringView& text)
{
    m_labels.at(index).set_text(text);
}

String GStatusBar::text(int index) const
{
    return m_labels.at(index).text();
}

void GStatusBar::paint_event(GPaintEvent& event)
{
    GPainter painter(*this);
    painter.add_clip_rect(event.rect());
    StylePainter::paint_surface(painter, rect(), palette(), !spans_entire_window_horizontally());
}
