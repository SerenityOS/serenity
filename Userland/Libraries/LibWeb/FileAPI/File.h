/*
 * Copyright (c) 2022, Kenneth Myhra <kennethmyhra@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Time.h>
#include <LibWeb/FileAPI/Blob.h>

namespace Web::FileAPI {

struct FilePropertyBag : BlobPropertyBag {
    Optional<i64> last_modified;
};

class File : public Blob {
    WEB_PLATFORM_OBJECT(File, Blob);

public:
    static DOM::ExceptionOr<JS::NonnullGCPtr<File>> create(HTML::Window&, Vector<BlobPart> const& file_bits, String const& file_name, Optional<FilePropertyBag> const& options = {});
    static DOM::ExceptionOr<JS::NonnullGCPtr<File>> create_with_global_object(HTML::Window&, Vector<BlobPart> const& file_bits, String const& file_name, Optional<FilePropertyBag> const& options = {});

    virtual ~File() override;

    // https://w3c.github.io/FileAPI/#dfn-name
    String const& name() const { return m_name; }
    // https://w3c.github.io/FileAPI/#dfn-lastModified
    i64 last_modified() const { return m_last_modified; }

private:
    File(HTML::Window&, ByteBuffer, String file_name, String type, i64 last_modified);

    String m_name;
    i64 m_last_modified { 0 };
};

}

WRAPPER_HACK(File, Web::FileAPI)
