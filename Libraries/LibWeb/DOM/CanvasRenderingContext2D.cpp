#include <AK/OwnPtr.h>
#include <LibGfx/Painter.h>
#include <LibWeb/DOM/CanvasRenderingContext2D.h>
#include <LibWeb/DOM/HTMLCanvasElement.h>

namespace Web {

CanvasRenderingContext2D::CanvasRenderingContext2D(HTMLCanvasElement& element)
    : m_element(element.make_weak_ptr())
{
}

CanvasRenderingContext2D::~CanvasRenderingContext2D()
{
}

void CanvasRenderingContext2D::set_fill_style(String style)
{
    m_fill_style = Gfx::Color::from_string(style).value_or(Color::Black);
}

String CanvasRenderingContext2D::fill_style() const
{
    return m_fill_style.to_string();
}

void CanvasRenderingContext2D::fill_rect(int x, int y, int width, int height)
{
    auto painter = this->painter();
    if (!painter)
        return;

    Gfx::Rect rect(x, y, width, height);
    painter->fill_rect(rect, m_fill_style);
    did_draw(rect);
}

void CanvasRenderingContext2D::did_draw(const Gfx::Rect&)
{
    // FIXME: Make use of the rect to reduce the invalidated area when possible.
    if (!m_element)
        return;
    if (!m_element->layout_node())
        return;
    m_element->layout_node()->set_needs_display();
}

OwnPtr<Gfx::Painter> CanvasRenderingContext2D::painter()
{
    if (!m_element)
        return nullptr;

    return make<Gfx::Painter>(m_element->ensure_bitmap());
}

}
