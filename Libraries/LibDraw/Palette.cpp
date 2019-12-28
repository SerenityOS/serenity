#include <LibDraw/Palette.h>

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
