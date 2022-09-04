/*
 * Copyright (c) 2022, Kenneth Myhra <kennethmyhra@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullRefPtr.h>
#include <AK/String.h>
#include <AK/Vector.h>
#include <LibWeb/Bindings/BlobPrototype.h>
#include <LibWeb/Bindings/PlatformObject.h>
#include <LibWeb/DOM/ExceptionOr.h>
#include <LibWeb/Forward.h>

namespace Web::FileAPI {

using BlobPart = Variant<JS::Handle<JS::Object>, JS::Handle<Blob>, String>;

struct BlobPropertyBag {
    String type = String::empty();
    Bindings::EndingType endings;
};

[[nodiscard]] ErrorOr<String> convert_line_endings_to_native(String const& string);
[[nodiscard]] ErrorOr<ByteBuffer> process_blob_parts(Vector<BlobPart> const& blob_parts, Optional<BlobPropertyBag> const& options = {});
[[nodiscard]] bool is_basic_latin(StringView view);

class Blob : public Bindings::PlatformObject {
    WEB_PLATFORM_OBJECT(Blob, Bindings::PlatformObject);

public:
    virtual ~Blob() override;

    static DOM::ExceptionOr<JS::NonnullGCPtr<Blob>> create(HTML::Window&, Optional<Vector<BlobPart>> const& blob_parts = {}, Optional<BlobPropertyBag> const& options = {});
    static DOM::ExceptionOr<JS::NonnullGCPtr<Blob>> create(HTML::Window&, ByteBuffer, String type);
    static DOM::ExceptionOr<JS::NonnullGCPtr<Blob>> create_with_global_object(HTML::Window&, Optional<Vector<BlobPart>> const& blob_parts = {}, Optional<BlobPropertyBag> const& options = {});

    // https://w3c.github.io/FileAPI/#dfn-size
    u64 size() const { return m_byte_buffer.size(); }
    // https://w3c.github.io/FileAPI/#dfn-type
    String const& type() const { return m_type; }

    DOM::ExceptionOr<JS::NonnullGCPtr<Blob>> slice(Optional<i64> start = {}, Optional<i64> end = {}, Optional<String> const& content_type = {});

    JS::Promise* text();
    JS::Promise* array_buffer();

    ReadonlyBytes bytes() const { return m_byte_buffer.bytes(); }

protected:
    Blob(HTML::Window&, ByteBuffer, String type);
    Blob(HTML::Window&, ByteBuffer);

private:
    explicit Blob(HTML::Window&);

    ByteBuffer m_byte_buffer {};
    String m_type {};
};

}

WRAPPER_HACK(Blob, Web::FileAPI)
