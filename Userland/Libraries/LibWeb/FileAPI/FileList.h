/*
 * Copyright (c) 2022, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Vector.h>
#include <LibJS/Heap/GCPtr.h>
#include <LibWeb/Bindings/LegacyPlatformObject.h>
#include <LibWeb/FileAPI/File.h>

namespace Web::FileAPI {

class FileList : public Bindings::LegacyPlatformObject {
    WEB_PLATFORM_OBJECT(FileList, Bindings::LegacyPlatformObject);

public:
    static JS::NonnullGCPtr<FileList> create(JS::Realm&, Vector<JS::NonnullGCPtr<File>>&&);
    virtual ~FileList() override;

    // https://w3c.github.io/FileAPI/#dfn-length
    unsigned long length() const { return m_files.size(); }

    // https://w3c.github.io/FileAPI/#dfn-item
    File const* item(size_t index) const
    {
        return index < m_files.size() ? m_files[index].ptr() : nullptr;
    }

    virtual bool is_supported_property_index(u32 index) const override;
    virtual JS::Value item_value(size_t index) const override;

private:
    FileList(JS::Realm&, Vector<JS::NonnullGCPtr<File>>&&);

    virtual void visit_edges(Cell::Visitor&) override;

    Vector<JS::NonnullGCPtr<File>> m_files;
};

}
