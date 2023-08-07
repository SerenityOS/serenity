/*
 * Copyright (c) 2022, Andrew Kaster <akaster@serenityos.org>
 * Copyright (c) 2023, Luke Wilde <lukew@serenityos.org>
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
    static WebIDL::ExceptionOr<JS::NonnullGCPtr<FileList>> create(JS::Realm&, Vector<JS::NonnullGCPtr<File>>&&);
    virtual ~FileList() override;

    // https://w3c.github.io/FileAPI/#dfn-length
    unsigned long length() const { return m_files.size(); }

    // https://w3c.github.io/FileAPI/#dfn-item
    File* item(size_t index)
    {
        return index < m_files.size() ? m_files[index].ptr() : nullptr;
    }

    // https://w3c.github.io/FileAPI/#dfn-item
    File const* item(size_t index) const
    {
        return index < m_files.size() ? m_files[index].ptr() : nullptr;
    }

    virtual bool is_supported_property_index(u32 index) const override;
    virtual WebIDL::ExceptionOr<JS::Value> item_value(size_t index) const override;

private:
    FileList(JS::Realm&, Vector<JS::NonnullGCPtr<File>>&&);

    virtual void initialize(JS::Realm&) override;
    virtual void visit_edges(Cell::Visitor&) override;

    // ^Bindings::LegacyPlatformObject
    virtual bool supports_indexed_properties() const override { return true; }
    virtual bool supports_named_properties() const override { return false; }
    virtual bool has_indexed_property_setter() const override { return false; }
    virtual bool has_named_property_setter() const override { return false; }
    virtual bool has_named_property_deleter() const override { return false; }
    virtual bool has_legacy_override_built_ins_interface_extended_attribute() const override { return false; }
    virtual bool has_legacy_unenumerable_named_properties_interface_extended_attribute() const override { return false; }
    virtual bool has_global_interface_extended_attribute() const override { return false; }
    virtual bool indexed_property_setter_has_identifier() const override { return false; }
    virtual bool named_property_setter_has_identifier() const override { return false; }
    virtual bool named_property_deleter_has_identifier() const override { return false; }

    Vector<JS::NonnullGCPtr<File>> m_files;
};

}
