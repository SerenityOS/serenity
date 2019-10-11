#include <LibGUI/GPainter.h>
#include <LibHTML/Layout/LayoutListItemMarker.h>

LayoutListItemMarker::LayoutListItemMarker()
    : LayoutNode(nullptr)
{
}

LayoutListItemMarker::~LayoutListItemMarker()
{
}

void LayoutListItemMarker::render(RenderingContext& context)
{
    Rect bullet_rect { 0, 0, 4, 4 };
    bullet_rect.center_within(rect());
    context.painter().fill_rect(bullet_rect, style().color_or_fallback(CSS::PropertyID::Color, document(), Color::Black));
}
