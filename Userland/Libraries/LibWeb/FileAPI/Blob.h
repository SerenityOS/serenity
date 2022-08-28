/*
 * Copyright (c) 2022, Kenneth Myhra <kennethmyhra@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullRefPtr.h>
#include <AK/RefCounted.h>
#include <AK/String.h>
#include <AK/Vector.h>
#include <LibWeb/Bindings/BlobPrototype.h>
#include <LibWeb/Bindings/Wrappable.h>
#include <LibWeb/DOM/ExceptionOr.h>
#include <LibWeb/Forward.h>
#include <LibWeb/HTML/Window.h>

namespace Web::FileAPI {

using BlobPart = Variant<JS::Handle<JS::Object>, NonnullRefPtr<Blob>, String>;

struct BlobPropertyBag {
    String type = String::empty();
    Bindings::EndingType endings;
};

[[nodiscard]] ErrorOr<String> convert_line_endings_to_native(String const& string);
[[nodiscard]] ErrorOr<ByteBuffer> process_blob_parts(Vector<BlobPart> const& blob_parts, Optional<BlobPropertyBag> const& options = {});
[[nodiscard]] bool is_basic_latin(StringView view);

class Blob
    : public RefCounted<Blob>
    , public Weakable<Blob>
    , public Bindings::Wrappable {

public:
    using WrapperType = Bindings::BlobWrapper;
    Blob(ByteBuffer byte_buffer, String type);

    virtual ~Blob() override;

    static DOM::ExceptionOr<NonnullRefPtr<Blob>> create(Optional<Vector<BlobPart>> const& blob_parts = {}, Optional<BlobPropertyBag> const& options = {});
    static DOM::ExceptionOr<NonnullRefPtr<Blob>> create_with_global_object(HTML::Window&, Optional<Vector<BlobPart>> const& blob_parts = {}, Optional<BlobPropertyBag> const& options = {});

    // https://w3c.github.io/FileAPI/#dfn-size
    u64 size() const { return m_byte_buffer.size(); }
    // https://w3c.github.io/FileAPI/#dfn-type
    String const& type() const { return m_type; }

    DOM::ExceptionOr<NonnullRefPtr<Blob>> slice(Optional<i64> start = {}, Optional<i64> end = {}, Optional<String> const& content_type = {});

    JS::Promise* text();
    JS::Promise* array_buffer();

    ReadonlyBytes bytes() const { return m_byte_buffer.bytes(); }

protected:
    Blob(ByteBuffer byte_buffer);

private:
    Blob() = default;

    ByteBuffer m_byte_buffer {};
    String m_type {};
};

}
