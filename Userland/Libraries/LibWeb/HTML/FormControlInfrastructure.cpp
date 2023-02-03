/*
 * Copyright (c) 2023, Kenneth Myhra <kennethmyhra@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/HTML/FormControlInfrastructure.h>
#include <LibWeb/Infra/Strings.h>

namespace Web::HTML {

// https://html.spec.whatwg.org/multipage/form-control-infrastructure.html#create-an-entry
WebIDL::ExceptionOr<Entry> create_entry(JS::Realm& realm, String const& name, Variant<JS::NonnullGCPtr<FileAPI::Blob>, String> const& value, Optional<String> const& filename)
{
    auto& vm = realm.vm();

    // 1. Set name to the result of converting name into a scalar value string.
    auto entry_name = TRY_OR_THROW_OOM(realm.vm(), Infra::convert_to_scalar_value_string(name));

    auto entry_value = TRY(value.visit(
        // 2. If value is a string, then set value to the result of converting value into a scalar value string.
        [&](String const& string) -> WebIDL::ExceptionOr<Variant<JS::NonnullGCPtr<FileAPI::File>, String>> {
            return TRY_OR_THROW_OOM(vm, Infra::convert_to_scalar_value_string(string));
        },
        // 3. Otherwise:
        [&](JS::NonnullGCPtr<FileAPI::Blob> const& blob) -> WebIDL::ExceptionOr<Variant<JS::NonnullGCPtr<FileAPI::File>, String>> {
            // 1. If value is not a File object, then set value to a new File object, representing the same bytes, whose name attribute value is "blob".
            // 2. If filename is given, then set value to a new File object, representing the same bytes, whose name attribute is filename.
            String name_attribute;
            if (filename.has_value())
                name_attribute = filename.value();
            else
                name_attribute = TRY_OR_THROW_OOM(vm, String::from_utf8("blob"sv));
            return TRY(FileAPI::File::create(realm, { JS::make_handle(*blob) }, name_attribute.to_deprecated_string(), {}));
        }));

    // 4. Return an entry whose name is name and whose value is value.
    return Entry {
        .name = move(entry_name),
        .value = move(entry_value),
    };
}

}
