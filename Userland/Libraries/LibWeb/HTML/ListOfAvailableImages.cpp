/*
 * Copyright (c) 2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/HTML/DecodedImageData.h>
#include <LibWeb/HTML/ListOfAvailableImages.h>

namespace Web::HTML {

ListOfAvailableImages::ListOfAvailableImages() = default;
ListOfAvailableImages::~ListOfAvailableImages() = default;

bool ListOfAvailableImages::Key::operator==(Key const& other) const
{
    return url == other.url && mode == other.mode && origin == other.origin;
}

u32 ListOfAvailableImages::Key::hash() const
{
    if (!cached_hash.has_value()) {
        u32 url_hash = Traits<AK::URL>::hash(url);
        u32 mode_hash = static_cast<u32>(mode);
        u32 origin_hash = 0;
        if (origin.has_value())
            origin_hash = Traits<HTML::Origin>::hash(origin.value());
        cached_hash = pair_int_hash(url_hash, pair_int_hash(mode_hash, origin_hash));
    }
    return cached_hash.value();
}

ErrorOr<NonnullRefPtr<ListOfAvailableImages::Entry>> ListOfAvailableImages::Entry::create(NonnullRefPtr<DecodedImageData> image_data, bool ignore_higher_layer_caching)
{
    return adopt_nonnull_ref_or_enomem(new (nothrow) Entry(move(image_data), ignore_higher_layer_caching));
}

ListOfAvailableImages::Entry::Entry(NonnullRefPtr<DecodedImageData> data, bool ignore_higher_layer_caching)
    : ignore_higher_layer_caching(ignore_higher_layer_caching)
    , image_data(move(data))
{
}

ListOfAvailableImages::Entry::~Entry() = default;

ErrorOr<void> ListOfAvailableImages::add(Key const& key, NonnullRefPtr<DecodedImageData> image_data, bool ignore_higher_layer_caching)
{
    auto entry = TRY(Entry::create(move(image_data), ignore_higher_layer_caching));
    TRY(m_images.try_set(key, move(entry)));
    return {};
}

void ListOfAvailableImages::remove(Key const& key)
{
    m_images.remove(key);
}

RefPtr<ListOfAvailableImages::Entry> ListOfAvailableImages::get(Key const& key) const
{
    auto it = m_images.find(key);
    if (it == m_images.end())
        return nullptr;
    return it->value;
}

}
