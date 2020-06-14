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

#include <LibCore/Timer.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/ImageDecoder.h>
#include <LibWeb/CSS/StyleResolver.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/Event.h>
#include <LibWeb/DOM/HTMLImageElement.h>
#include <LibWeb/Layout/LayoutImage.h>
#include <LibWeb/Loader/ResourceLoader.h>

namespace Web {

HTMLImageElement::HTMLImageElement(Document& document, const FlyString& tag_name)
    : HTMLElement(document, tag_name)
    , m_timer(Core::Timer::construct())
{
    m_image_loader.on_load = [this] {
        if (image_decoder() && image_decoder()->is_animated() && image_decoder()->frame_count() > 1) {
            const auto& first_frame = image_decoder()->frame(0);
            m_timer->set_interval(first_frame.duration);
            m_timer->on_timeout = [this] { animate(); };
            m_timer->start();
        }
        this->document().update_layout();
        dispatch_event(Event::create("load"));
    };

    m_image_loader.on_fail = [this] {
        dbg() << "HTMLImageElement: Resource did fail: " << this->src();
        this->document().update_layout();
        dispatch_event(Event::create("error"));
    };
}

HTMLImageElement::~HTMLImageElement()
{
}

void HTMLImageElement::parse_attribute(const FlyString& name, const String& value)
{
    HTMLElement::parse_attribute(name, value);

    if (name == HTML::AttributeNames::src)
        m_image_loader.load(document().complete_url(value));
}

void HTMLImageElement::animate()
{
    if (!layout_node())
        return;

    auto* decoder = image_decoder();
    ASSERT(decoder);

    m_current_frame_index = (m_current_frame_index + 1) % decoder->frame_count();
    const auto& current_frame = decoder->frame(m_current_frame_index);

    if (current_frame.duration != m_timer->interval()) {
        m_timer->restart(current_frame.duration);
    }

    if (m_current_frame_index == decoder->frame_count() - 1) {
        ++m_loops_completed;
        if (m_loops_completed > 0 && m_loops_completed == decoder->loop_count()) {
            m_timer->stop();
        }
    }

    layout_node()->set_needs_display();
}

RefPtr<LayoutNode> HTMLImageElement::create_layout_node(const StyleProperties* parent_style) const
{
    auto style = document().style_resolver().resolve_style(*this, parent_style);
    auto display = style->string_or_fallback(CSS::PropertyID::Display, "inline");
    if (display == "none")
        return nullptr;
    return adopt(*new LayoutImage(*this, move(style), m_image_loader));
}

const Gfx::ImageDecoder* HTMLImageElement::image_decoder() const
{
    return m_image_loader.image_decoder();
}

const Gfx::Bitmap* HTMLImageElement::bitmap() const
{
    auto* decoder = image_decoder();
    if (!decoder)
        return nullptr;
    if (decoder->is_animated())
        return decoder->frame(m_current_frame_index).image;
    return decoder->bitmap();
}

void HTMLImageElement::set_visible_in_viewport(Badge<LayoutDocument>, bool visible_in_viewport)
{
    m_image_loader.set_visible_in_viewport(visible_in_viewport);
}

}
