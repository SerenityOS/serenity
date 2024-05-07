/*
 * Copyright (c) 2024, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Bindings/PlatformObject.h>
#include <LibWeb/CSS/FontFace.h>

namespace Web::CSS {

class FontFaceSet final : public Bindings::PlatformObject {
    WEB_PLATFORM_OBJECT(FontFace, Bindings::PlatformObject);
    JS_DECLARE_ALLOCATOR(FontFaceSet);

public:
    [[nodiscard]] static JS::NonnullGCPtr<FontFaceSet> construct_impl(JS::Realm&, Vector<JS::Handle<FontFace>> initial_faces);
    [[nodiscard]] static JS::NonnullGCPtr<FontFaceSet> create(JS::Realm&);
    virtual ~FontFaceSet() override = default;

    JS::NonnullGCPtr<FontFaceSet> add(JS::Handle<FontFace> face);
    JS::ThrowCompletionOr<JS::NonnullGCPtr<JS::Promise>> load(String const& font, String const& text);

private:
    FontFaceSet(JS::Realm&, Vector<JS::Handle<FontFace>> initial_faces);

    virtual void initialize(JS::Realm&) override;
};

}
