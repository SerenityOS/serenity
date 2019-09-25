#include <LibHTML/Layout/ComputedStyle.h>

ComputedStyle::ComputedStyle()
{
}

ComputedStyle::~ComputedStyle()
{
}

ComputedStyle::PixelBox ComputedStyle::full_margin() const
{
    return {
        m_margin.top.to_px() + m_border.top.to_px() + m_padding.top.to_px(),
        m_margin.right.to_px() + m_border.right.to_px() + m_padding.right.to_px(),
        m_margin.bottom.to_px() + m_border.bottom.to_px() + m_padding.bottom.to_px(),
        m_margin.left.to_px() + m_border.left.to_px() + m_padding.left.to_px(),
    };
}
