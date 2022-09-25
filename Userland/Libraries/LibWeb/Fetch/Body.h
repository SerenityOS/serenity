/*
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <LibJS/Forward.h>
#include <LibWeb/Forward.h>

namespace Web::Fetch {

enum class PackageDataType {
    ArrayBuffer,
    Blob,
    FormData,
    JSON,
    Text,
};

// https://fetch.spec.whatwg.org/#body-mixin
class BodyMixin {
public:
    virtual ~BodyMixin();

    virtual Optional<MimeSniff::MimeType> mime_type_impl() const = 0;
    virtual Optional<Infrastructure::Body&> body_impl() = 0;
    virtual Optional<Infrastructure::Body const&> body_impl() const = 0;

    [[nodiscard]] bool is_unusable() const;
    [[nodiscard]] JS::GCPtr<Streams::ReadableStream> body() const;
    [[nodiscard]] bool body_used() const;

    // JS API functions
    [[nodiscard]] JS::NonnullGCPtr<JS::Promise> array_buffer() const;
    [[nodiscard]] JS::NonnullGCPtr<JS::Promise> blob() const;
    [[nodiscard]] JS::NonnullGCPtr<JS::Promise> form_data() const;
    [[nodiscard]] JS::NonnullGCPtr<JS::Promise> json() const;
    [[nodiscard]] JS::NonnullGCPtr<JS::Promise> text() const;
};

[[nodiscard]] WebIDL::ExceptionOr<JS::Value> package_data(JS::Realm&, ByteBuffer, PackageDataType, Optional<MimeSniff::MimeType> const&);
[[nodiscard]] JS::NonnullGCPtr<JS::Promise> consume_body(JS::Realm&, BodyMixin const&, PackageDataType);

}
