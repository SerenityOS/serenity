#include <LibGUI/GPainter.h>
#include <LibHTML/Layout/LayoutListItemMarker.h>

LayoutListItemMarker::LayoutListItemMarker()
    : LayoutBox(nullptr, StyleProperties::create())
{
}

LayoutListItemMarker::~LayoutListItemMarker()
{
}

void LayoutListItemMarker::render(RenderingContext& context)
{
    Rect bullet_rect { 0, 0, 4, 4 };
    bullet_rect.center_within(rect());
    // FIXME: It would be nicer to not have to go via the parent here to get our inherited style.
    context.painter().fill_rect(bullet_rect, parent()->style().color_or_fallback(CSS::PropertyID::Color, document(), Color::Black));
}
