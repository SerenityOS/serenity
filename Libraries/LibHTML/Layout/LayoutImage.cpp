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
    if (renders_as_alt_text()) {
        auto& font = Font::default_font();
        rect().set_width(font.width(node().alt()) + 16);
        rect().set_height(font.glyph_height() + 16);
    } else {
        rect().set_width(node().bitmap()->width());
        rect().set_height(node().bitmap()->height());
    }

    LayoutReplaced::layout();
}

void LayoutImage::render(RenderingContext& context)
{
    if (renders_as_alt_text()) {
        context.painter().set_font(Font::default_font());
        StylePainter::paint_frame(context.painter(), rect(), FrameShape::Container, FrameShadow::Sunken, 2);
        context.painter().draw_text(rect(), node().alt(), TextAlignment::Center, Color::White);
    } else {
        context.painter().draw_scaled_bitmap(rect(), *node().bitmap(), node().bitmap()->rect());
    }
    LayoutReplaced::render(context);
}

bool LayoutImage::renders_as_alt_text() const
{
    return !node().bitmap();
}
