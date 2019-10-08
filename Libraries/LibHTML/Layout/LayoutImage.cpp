#include <LibDraw/Font.h>
#include <LibDraw/StylePainter.h>
#include <LibGUI/GPainter.h>
#include <LibHTML/Layout/LayoutImage.h>

LayoutImage::LayoutImage(const HTMLImageElement& element, NonnullRefPtr<StyleProperties> style)
    : LayoutReplaced(element, move(style))
{
}

LayoutImage::~LayoutImage()
{
}

void LayoutImage::layout()
{
    if (node().preferred_width() && node().preferred_height()) {
        rect().set_width(node().preferred_width());
        rect().set_height(node().preferred_height());
    } else if (renders_as_alt_text()) {
        auto& font = Font::default_font();
        auto alt = node().alt();
        if (alt.is_empty())
            alt = node().src();
        rect().set_width(font.width(alt) + 16);
        rect().set_height(font.glyph_height() + 16);
    } else {
        rect().set_width(16);
        rect().set_height(16);
    }

    LayoutReplaced::layout();
}

void LayoutImage::render(RenderingContext& context)
{
    if (renders_as_alt_text()) {
        context.painter().set_font(Font::default_font());
        StylePainter::paint_frame(context.painter(), rect(), FrameShape::Container, FrameShadow::Sunken, 2);
        auto alt = node().alt();
        if (alt.is_empty())
            alt = node().src();
        context.painter().draw_text(rect(), alt, TextAlignment::Center, style().color_or_fallback(CSS::PropertyID::Color, document(), Color::Black), TextElision::Right);
    } else {
        context.painter().draw_scaled_bitmap(rect(), *node().bitmap(), node().bitmap()->rect());
    }
    LayoutReplaced::render(context);
}

bool LayoutImage::renders_as_alt_text() const
{
    return !node().bitmap();
}
