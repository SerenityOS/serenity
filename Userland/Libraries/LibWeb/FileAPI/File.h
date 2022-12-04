/*
 * Copyright (c) 2022, Kenneth Myhra <kennethmyhra@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/FileAPI/Blob.h>

namespace Web::FileAPI {

struct FilePropertyBag : BlobPropertyBag {
    Optional<i64> last_modified;
};

class File : public Blob {
    WEB_PLATFORM_OBJECT(File, Blob);

public:
    static WebIDL::ExceptionOr<JS::NonnullGCPtr<File>> create(JS::Realm&, Vector<BlobPart> const& file_bits, DeprecatedString const& file_name, Optional<FilePropertyBag> const& options = {});
    static WebIDL::ExceptionOr<JS::NonnullGCPtr<File>> construct_impl(JS::Realm&, Vector<BlobPart> const& file_bits, DeprecatedString const& file_name, Optional<FilePropertyBag> const& options = {});

    virtual ~File() override;

    // https://w3c.github.io/FileAPI/#dfn-name
    DeprecatedString const& name() const { return m_name; }
    // https://w3c.github.io/FileAPI/#dfn-lastModified
    i64 last_modified() const { return m_last_modified; }

private:
    File(JS::Realm&, ByteBuffer, DeprecatedString file_name, DeprecatedString type, i64 last_modified);

    DeprecatedString m_name;
    i64 m_last_modified { 0 };
};

}
