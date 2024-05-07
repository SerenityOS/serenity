/*
 * Copyright (c) 2024, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Bindings/PlatformObject.h>

namespace Web::CSS {

struct FontFaceDescriptors {
    String style = "normal"_string;
    String weight = "normal"_string;
    String stretch = "normal"_string;
    String unicode_range = "U+0-10FFFF"_string;
    String feature_settings = "normal"_string;
    String variation_settings = "normal"_string;
    String display = "auto"_string;
    String ascent_override = "normal"_string;
    String descent_override = "normal"_string;
    String line_gap_override = "normal"_string;
};

class FontFace final : public Bindings::PlatformObject {
    WEB_PLATFORM_OBJECT(FontFace, Bindings::PlatformObject);
    JS_DECLARE_ALLOCATOR(FontFace);

public:
    using FontFaceSource = Variant<String, JS::Handle<JS::ArrayBuffer>, JS::Handle<WebIDL::ArrayBufferView>>;

    [[nodiscard]] static JS::NonnullGCPtr<FontFace> construct_impl(JS::Realm&, String family, FontFaceSource source, FontFaceDescriptors const& descriptors);
    virtual ~FontFace() override = default;

    JS::ThrowCompletionOr<JS::NonnullGCPtr<JS::Promise>> load();

private:
    FontFace(JS::Realm&, String family, FontFaceSource source, FontFaceDescriptors const& descriptors);

    virtual void initialize(JS::Realm&) override;
};

}
