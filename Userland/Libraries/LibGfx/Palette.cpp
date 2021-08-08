/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Badge.h>
#include <LibGfx/Palette.h>
#include <LibGfx/Remote/RemoteGfxServerConnection.h>
#include <string.h>

namespace Gfx {

NonnullRefPtr<PaletteImpl> PaletteImpl::create_with_anonymous_buffer(Core::AnonymousBuffer buffer)
{
    return adopt_ref(*new PaletteImpl(move(buffer)));
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
    if (m_remote_data)
        destroy_remote_data();
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
    auto new_theme_buffer = Core::AnonymousBuffer::create_with_size(m_theme_buffer.size()).release_value();
    memcpy(new_theme_buffer.data<SystemTheme>(), &theme(), m_theme_buffer.size());
    return adopt_ref(*new PaletteImpl(move(new_theme_buffer)));
}

void Palette::set_color(ColorRole role, Color color)
{
    if (m_impl->ref_count() != 1)
        m_impl = m_impl->clone();
    auto& theme = const_cast<SystemTheme&>(impl().theme());
    theme.color[(int)role] = color.value();
}

void Palette::set_flag(FlagRole role, bool value)
{
    if (m_impl->ref_count() != 1)
        m_impl = m_impl->clone();
    auto& theme = const_cast<SystemTheme&>(impl().theme());
    theme.flag[(int)role] = value;
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

int Palette::enable_remote_painting(bool enable)
{
    if (enable) {
#ifdef __serenity__
        if (m_remote_data) {
            VERIFY(m_remote_data->palette_id > 0);
            return 0;
        }
        auto remote_gfx_session = RemoteGfx::RemoteGfxServerConnection::the().session();
        if (!remote_gfx_session)
            return 0;
        static RemoteGfx::BitmapId s_palette_id = 0;
        if (!m_remote_data)
            m_remote_data = adopt_own(*new RemoteData { .session = remote_gfx_session->make_weak_ptr(), .palette_id = ++s_palette_id });
        auto& remote_data = *m_remote_data;

        VERIFY(remote_data.palette_id > 0);
        remote_gfx_session->connection().async_create_palette(remote_data.palette_id, *this);
        return remote_data.palette_id;
#endif
    }

    m_remote_data = nullptr;
    return 0;
}

void Palette::destroy_remote_data()
{
#ifdef __serenity__
    if (!m_remote_data)
        return;
    auto& remote_data = *m_remote_data;
    VERIFY(remote_data.palette_id > 0);
    if (auto* remote_gfx = remote_data.session.ptr())
        remote_gfx->connection().async_destroy_palette(remote_data.palette_id);
#endif
    m_remote_data = nullptr;
}

PaletteImpl::~PaletteImpl()
{
}

void PaletteImpl::replace_internal_buffer(Badge<GUI::Application>, Core::AnonymousBuffer buffer)
{
    m_theme_buffer = move(buffer);
}

}
