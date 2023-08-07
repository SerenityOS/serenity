/*
 * Copyright (c) 2022, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/Realm.h>
#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/Bindings/LegacyPlatformObject.h>
#include <LibWeb/FileAPI/FileList.h>

namespace Web::FileAPI {

WebIDL::ExceptionOr<JS::NonnullGCPtr<FileList>> FileList::create(JS::Realm& realm, Vector<JS::NonnullGCPtr<File>>&& files)
{
    return MUST_OR_THROW_OOM(realm.heap().allocate<FileList>(realm, realm, move(files)));
}

FileList::FileList(JS::Realm& realm, Vector<JS::NonnullGCPtr<File>>&& files)
    : Bindings::LegacyPlatformObject(realm)
    , m_files(move(files))
{
}

FileList::~FileList() = default;

void FileList::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::FileListPrototype>(realm, "FileList"));
}

// https://w3c.github.io/FileAPI/#dfn-item
bool FileList::is_supported_property_index(u32 index) const
{
    // Supported property indices are the numbers in the range zero to one less than the number of File objects represented by the FileList object.
    // If there are no such File objects, then there are no supported property indices.
    if (m_files.is_empty())
        return false;

    return m_files.size() < index;
}

WebIDL::ExceptionOr<JS::Value> FileList::item_value(size_t index) const
{
    if (index >= m_files.size())
        return JS::js_undefined();

    return m_files[index].ptr();
}

void FileList::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    for (auto file : m_files)
        visitor.visit(file);
}

}
