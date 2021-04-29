/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Badge.h>
#include <LibGfx/Palette.h>
#include <string.h>

namespace Gfx {

NonnullRefPtr<PaletteImpl> PaletteImpl::create_with_anonymous_buffer(Core::AnonymousBuffer buffer)
{
    return adopt(*new PaletteImpl(move(buffer)));
}

PaletteImpl::PaletteImpl(Core::AnonymousBuffer buffer)
    : m_theme_buffer(move(buffer))
{
}

Palette::Palette(const PaletteImpl& impl)
    : m_impl(impl)
{
}

Palette::~Palette()
{
}

int PaletteImpl::metric(MetricRole role) const
{
    VERIFY((int)role < (int)MetricRole::__Count);
    return theme().metric[(int)role];
}

String PaletteImpl::path(PathRole role) const
{
    VERIFY((int)role < (int)PathRole::__Count);
    return theme().path[(int)role];
}

NonnullRefPtr<PaletteImpl> PaletteImpl::clone() const
{
    auto new_theme_buffer = Core::AnonymousBuffer::create_with_size(m_theme_buffer.size());
    memcpy(new_theme_buffer.data<SystemTheme>(), &theme(), m_theme_buffer.size());
    return adopt(*new PaletteImpl(move(new_theme_buffer)));
}

void Palette::set_color(ColorRole role, Color color)
{
    if (m_impl->ref_count() != 1)
        m_impl = m_impl->clone();
    auto& theme = const_cast<SystemTheme&>(impl().theme());
    theme.color[(int)role] = color.value();
}

void Palette::set_metric(MetricRole role, int value)
{
    if (m_impl->ref_count() != 1)
        m_impl = m_impl->clone();
    auto& theme = const_cast<SystemTheme&>(impl().theme());
    theme.metric[(int)role] = value;
}

void Palette::set_path(PathRole role, String path)
{
    if (m_impl->ref_count() != 1)
        m_impl = m_impl->clone();
    auto& theme = const_cast<SystemTheme&>(impl().theme());
    memcpy(theme.path[(int)role], path.characters(), min(path.length() + 1, sizeof(theme.path[(int)role])));
    theme.path[(int)role][sizeof(theme.path[(int)role]) - 1] = '\0';
}

PaletteImpl::~PaletteImpl()
{
}

void PaletteImpl::replace_internal_buffer(Badge<GUI::Application>, Core::AnonymousBuffer buffer)
{
    m_theme_buffer = move(buffer);
}

}
