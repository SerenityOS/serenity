#include <LibGUI/GIcon.h>

GIcon::GIcon()
    : m_impl(GIconImpl::create())
{
}

GIcon::GIcon(const GIconImpl& impl)
    : m_impl(const_cast<GIconImpl&>(impl))
{
}

GIcon::GIcon(const GIcon& other)
    : m_impl(other.m_impl)
{
}

GIcon::GIcon(RefPtr<GraphicsBitmap>&& bitmap)
    : GIcon()
{
    if (bitmap) {
        ASSERT(bitmap->width() == bitmap->height());
        int size = bitmap->width();
        set_bitmap_for_size(size, move(bitmap));
    }
}

GIcon::GIcon(RefPtr<GraphicsBitmap>&& bitmap1, RefPtr<GraphicsBitmap>&& bitmap2)
    : GIcon(move(bitmap1))
{
    if (bitmap2) {
        ASSERT(bitmap2->width() == bitmap2->height());
        int size = bitmap2->width();
        set_bitmap_for_size(size, move(bitmap2));
    }
}

const GraphicsBitmap* GIconImpl::bitmap_for_size(int size) const
{
    auto it = m_bitmaps.find(size);
    if (it != m_bitmaps.end())
        return it->value.ptr();

    int best_diff_so_far = INT32_MAX;
    const GraphicsBitmap* best_fit = nullptr;
    for (auto& it : m_bitmaps) {
        int abs_diff = abs(it.key - size);
        if (abs_diff < best_diff_so_far) {
            best_diff_so_far = abs_diff;
            best_fit = it.value.ptr();
        }
    }
    return best_fit;
}

void GIconImpl::set_bitmap_for_size(int size, RefPtr<GraphicsBitmap>&& bitmap)
{
    if (!bitmap) {
        m_bitmaps.remove(size);
        return;
    }
    m_bitmaps.set(size, move(bitmap));
}

GIcon GIcon::default_icon(const StringView& name)
{
    auto bitmap16 = GraphicsBitmap::load_from_file(String::format("/res/icons/16x16/%s.png", String(name).characters()));
    auto bitmap32 = GraphicsBitmap::load_from_file(String::format("/res/icons/32x32/%s.png", String(name).characters()));
    return GIcon(move(bitmap16), move(bitmap32));
}
