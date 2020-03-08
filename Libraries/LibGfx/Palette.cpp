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

#include <AK/Badge.h>
#include <AK/SharedBuffer.h>
#include <LibGfx/Palette.h>
#include <string.h>

namespace Gfx {

NonnullRefPtr<PaletteImpl> PaletteImpl::create_with_shared_buffer(SharedBuffer& buffer)
{
    return adopt(*new PaletteImpl(buffer));
}

PaletteImpl::PaletteImpl(SharedBuffer& buffer)
    : m_theme_buffer(buffer)
{
}

Palette::Palette(const PaletteImpl& impl)
    : m_impl(impl)
{
}

Palette::~Palette()
{
}

const SystemTheme& PaletteImpl::theme() const
{
    return *(const SystemTheme*)m_theme_buffer->data();
}

Color PaletteImpl::color(ColorRole role) const
{
    ASSERT((int)role < (int)ColorRole::__Count);
    return theme().color[(int)role];
}

NonnullRefPtr<PaletteImpl> PaletteImpl::clone() const
{
    auto new_theme_buffer = SharedBuffer::create_with_size(m_theme_buffer->size());
    memcpy(new_theme_buffer->data(), m_theme_buffer->data(), m_theme_buffer->size());
    return adopt(*new PaletteImpl(*new_theme_buffer));
}

void Palette::set_color(ColorRole role, Color color)
{
    if (m_impl->ref_count() != 1)
        m_impl = m_impl->clone();
    auto& theme = const_cast<SystemTheme&>(impl().theme());
    theme.color[(int)role] = color;
}

PaletteImpl::~PaletteImpl()
{
}

void PaletteImpl::replace_internal_buffer(Badge<GUI::Application>, SharedBuffer& buffer)
{
    m_theme_buffer = buffer;
}

}
