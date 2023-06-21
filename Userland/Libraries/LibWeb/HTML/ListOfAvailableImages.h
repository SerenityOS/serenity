/*
 * Copyright (c) 2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <AK/URL.h>
#include <LibJS/Heap/Cell.h>
#include <LibWeb/Forward.h>
#include <LibWeb/HTML/CORSSettingAttribute.h>
#include <LibWeb/HTML/Origin.h>

namespace Web::HTML {

// https://html.spec.whatwg.org/multipage/images.html#list-of-available-images
class ListOfAvailableImages {
public:
    struct Key {
        AK::URL url;
        HTML::CORSSettingAttribute mode;
        Optional<HTML::Origin> origin;

        [[nodiscard]] bool operator==(Key const& other) const;
        [[nodiscard]] u32 hash() const;

    private:
        mutable Optional<u32> cached_hash;
    };

    struct Entry final : public RefCounted<Entry> {
        static ErrorOr<NonnullRefPtr<Entry>> create(NonnullRefPtr<DecodedImageData>, bool ignore_higher_layer_caching);
        ~Entry();

        bool ignore_higher_layer_caching { false };
        NonnullRefPtr<DecodedImageData> image_data;

    private:
        Entry(NonnullRefPtr<DecodedImageData>, bool ignore_higher_layer_caching);
    };

    ListOfAvailableImages();
    ~ListOfAvailableImages();

    ErrorOr<void> add(Key const&, NonnullRefPtr<DecodedImageData>, bool ignore_higher_layer_caching);
    void remove(Key const&);
    [[nodiscard]] RefPtr<Entry> get(Key const&) const;

private:
    HashMap<Key, NonnullRefPtr<Entry>> m_images;
};

}

namespace AK {

template<>
struct Traits<Web::HTML::ListOfAvailableImages::Key> : public GenericTraits<Web::HTML::ListOfAvailableImages::Key> {
    static unsigned hash(Web::HTML::ListOfAvailableImages::Key const& key)
    {
        return key.hash();
    }
    static bool equals(Web::HTML::ListOfAvailableImages::Key const& a, Web::HTML::ListOfAvailableImages::Key const& b)
    {
        return a == b;
    }
};

}
