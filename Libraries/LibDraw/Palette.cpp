#include <LibDraw/Palette.h>

NonnullRefPtr<Palette> Palette::create_with_shared_buffer(SharedBuffer& buffer)
{
    return adopt(*new Palette(buffer));
}

Palette::Palette(SharedBuffer& buffer)
    : m_theme_buffer(buffer)
{
}

Palette::~Palette()
{
}

const SystemTheme& Palette::theme() const
{
    return *(const SystemTheme*)m_theme_buffer->data();
}

Color Palette::color(ColorRole role) const
{
    ASSERT((int)role < (int)ColorRole::__Count);
    return theme().color[(int)role];
}
