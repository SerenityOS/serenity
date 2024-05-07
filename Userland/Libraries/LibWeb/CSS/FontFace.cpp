/*
 * Copyright (c) 2024, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Heap/Heap.h>
#include <LibJS/Runtime/Realm.h>
#include <LibWeb/Bindings/FontFacePrototype.h>
#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/CSS/FontFace.h>
#include <LibWeb/WebIDL/Promise.h>

namespace Web::CSS {

JS_DEFINE_ALLOCATOR(FontFace);

JS::NonnullGCPtr<FontFace> FontFace::construct_impl(JS::Realm& realm, String family, FontFaceSource source, FontFaceDescriptors const& descriptors)
{
    return realm.heap().allocate<FontFace>(realm, realm, move(family), move(source), descriptors);
}

FontFace::FontFace(JS::Realm& realm, String, FontFaceSource, FontFaceDescriptors const&)
    : Bindings::PlatformObject(realm)
{
}

void FontFace::initialize(JS::Realm& realm)
{
    Base::initialize(realm);

    WEB_SET_PROTOTYPE_FOR_INTERFACE(FontFace);
}

// https://drafts.csswg.org/css-font-loading/#dom-fontface-load
JS::ThrowCompletionOr<JS::NonnullGCPtr<JS::Promise>> FontFace::load()
{
    // FIXME: Do the steps
    auto promise = WebIDL::create_rejected_promise(realm(), WebIDL::NotSupportedError::create(realm(), "FontFace::load is not yet implemented"_fly_string));
    return verify_cast<JS::Promise>(*promise->promise());
}

}
