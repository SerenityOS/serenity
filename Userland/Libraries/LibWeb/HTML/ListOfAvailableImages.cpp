/*
 * Copyright (c) 2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/HTML/DecodedImageData.h>
#include <LibWeb/HTML/ListOfAvailableImages.h>

namespace Web::HTML {

JS_DEFINE_ALLOCATOR(ListOfAvailableImages);
JS_DEFINE_ALLOCATOR(ListOfAvailableImages::Entry);

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

JS::NonnullGCPtr<ListOfAvailableImages::Entry> ListOfAvailableImages::Entry::create(JS::VM& vm, JS::NonnullGCPtr<DecodedImageData> image_data, bool ignore_higher_layer_caching)
{
    return vm.heap().allocate_without_realm<Entry>(image_data, ignore_higher_layer_caching);
}

void ListOfAvailableImages::visit_edges(JS::Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    for (auto& it : m_images)
        visitor.visit(it.value);
}

ListOfAvailableImages::Entry::Entry(JS::NonnullGCPtr<DecodedImageData> data, bool ignore_higher_layer_caching)
    : ignore_higher_layer_caching(ignore_higher_layer_caching)
    , image_data(data)
{
}

ListOfAvailableImages::Entry::~Entry() = default;

ErrorOr<void> ListOfAvailableImages::add(Key const& key, JS::NonnullGCPtr<DecodedImageData> image_data, bool ignore_higher_layer_caching)
{
    auto entry = Entry::create(vm(), image_data, ignore_higher_layer_caching);
    TRY(m_images.try_set(key, entry));
    return {};
}

void ListOfAvailableImages::remove(Key const& key)
{
    m_images.remove(key);
}

JS::GCPtr<ListOfAvailableImages::Entry> ListOfAvailableImages::get(Key const& key) const
{
    auto it = m_images.find(key);
    if (it == m_images.end())
        return nullptr;
    return it->value;
}

}
