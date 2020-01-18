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

#include <LibDraw/GraphicsBitmap.h>
#include <LibDraw/PNGLoader.h>
#include <LibHTML/CSS/StyleValue.h>
#include <LibHTML/DOM/Document.h>
#include <LibHTML/Frame.h>
#include <LibHTML/ResourceLoader.h>

StyleValue::StyleValue(Type type)
    : m_type(type)
{
}

StyleValue::~StyleValue()
{
}

String IdentifierStyleValue::to_string() const
{
    switch (id()) {
    case CSS::ValueID::Invalid:
        return "(invalid)";
    case CSS::ValueID::VendorSpecificLink:
        return "-libhtml-link";
    default:
        ASSERT_NOT_REACHED();
    }
}

Color IdentifierStyleValue::to_color(const Document& document) const
{
    if (id() == CSS::ValueID::VendorSpecificLink)
        return document.link_color();
    return {};
}

ImageStyleValue::ImageStyleValue(const URL& url, Document& document)
    : StyleValue(Type::Image)
    , m_url(url)
    , m_document(document.make_weak_ptr())
{
    NonnullRefPtr<ImageStyleValue> protector(*this);
    ResourceLoader::the().load(url, [this, protector](auto& data) {
        if (!m_document)
            return;
        m_bitmap = load_png_from_memory(data.data(), data.size());
        if (!m_bitmap)
            return;
        // FIXME: Do less than a full repaint if possible?
        m_document->frame()->set_needs_display({});
    });
}
