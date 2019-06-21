#pragma once

#include <AK/HashMap.h>
#include <SharedGraphics/GraphicsBitmap.h>

class GIconImpl : public RefCounted<GIconImpl> {
public:
    static Retained<GIconImpl> create() { return adopt(*new GIconImpl); }
    ~GIconImpl() {}

    const GraphicsBitmap* bitmap_for_size(int) const;
    void set_bitmap_for_size(int, RetainPtr<GraphicsBitmap>&&);

private:
    GIconImpl() {}
    HashMap<int, RetainPtr<GraphicsBitmap>> m_bitmaps;
};

class GIcon {
public:
    GIcon();
    explicit GIcon(RetainPtr<GraphicsBitmap>&&);
    explicit GIcon(RetainPtr<GraphicsBitmap>&&, RetainPtr<GraphicsBitmap>&&);
    explicit GIcon(const GIconImpl&);
    GIcon(const GIcon&);
    ~GIcon() {}

    static GIcon default_icon(const StringView&);

    GIcon& operator=(const GIcon& other)
    {
        m_impl = other.m_impl.copy_ref();
        return *this;
    }

    const GraphicsBitmap* bitmap_for_size(int size) const { return m_impl->bitmap_for_size(size); }
    void set_bitmap_for_size(int size, RetainPtr<GraphicsBitmap>&& bitmap) { m_impl->set_bitmap_for_size(size, move(bitmap)); }

    const GIconImpl& impl() const { return *m_impl; }

private:
    Retained<GIconImpl> m_impl;
};
