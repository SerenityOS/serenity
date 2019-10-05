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
    LayoutReplaced::layout();
}

void LayoutImage::render(RenderingContext& context)
{
    LayoutReplaced::render(context);
}
