#pragma once

#include <AK/HashMap.h>
#include <LibDraw/GraphicsBitmap.h>

class GIconImpl : public RefCounted<GIconImpl> {
public:
    static NonnullRefPtr<GIconImpl> create() { return adopt(*new GIconImpl); }
    ~GIconImpl() {}

    const GraphicsBitmap* bitmap_for_size(int) const;
    void set_bitmap_for_size(int, RefPtr<GraphicsBitmap>&&);

private:
    GIconImpl() {}
    HashMap<int, RefPtr<GraphicsBitmap>> m_bitmaps;
};

class GIcon {
public:
    GIcon();
    explicit GIcon(RefPtr<GraphicsBitmap>&&);
    explicit GIcon(RefPtr<GraphicsBitmap>&&, RefPtr<GraphicsBitmap>&&);
    explicit GIcon(const GIconImpl&);
    GIcon(const GIcon&);
    ~GIcon() {}

    static GIcon default_icon(const StringView&);

    GIcon& operator=(const GIcon& other)
    {
        if (this != &other)
            m_impl = other.m_impl;
        return *this;
    }

    const GraphicsBitmap* bitmap_for_size(int size) const { return m_impl->bitmap_for_size(size); }
    void set_bitmap_for_size(int size, RefPtr<GraphicsBitmap>&& bitmap) { m_impl->set_bitmap_for_size(size, move(bitmap)); }

    const GIconImpl& impl() const { return *m_impl; }

private:
    NonnullRefPtr<GIconImpl> m_impl;
};
