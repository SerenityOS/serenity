/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2023, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Bindings/PlatformObject.h>
#include <LibWeb/WebIDL/AbstractOperations.h>
#include <LibWeb/WebIDL/ExceptionOr.h>

namespace Web::Bindings {

// https://webidl.spec.whatwg.org/#dfn-legacy-platform-object
class LegacyPlatformObject : public PlatformObject {
    WEB_PLATFORM_OBJECT(LegacyPlatformObject, PlatformObject);

    virtual bool has_legacy_override_built_ins_interface_extended_attribute() const = 0;

public:
    virtual ~LegacyPlatformObject() override;

    virtual JS::ThrowCompletionOr<Optional<JS::PropertyDescriptor>> internal_get_own_property(JS::PropertyKey const&) const override;
    virtual JS::ThrowCompletionOr<bool> internal_set(JS::PropertyKey const&, JS::Value, JS::Value) override;
    virtual JS::ThrowCompletionOr<bool> internal_define_own_property(JS::PropertyKey const&, JS::PropertyDescriptor const&) override;
    virtual JS::ThrowCompletionOr<bool> internal_delete(JS::PropertyKey const&) override;
    virtual JS::ThrowCompletionOr<bool> internal_prevent_extensions() override;
    virtual JS::ThrowCompletionOr<JS::MarkedVector<JS::Value>> internal_own_property_keys() const override;

    enum class IgnoreNamedProps {
        No,
        Yes,
    };
    JS::ThrowCompletionOr<Optional<JS::PropertyDescriptor>> legacy_platform_object_get_own_property(JS::PropertyKey const&, IgnoreNamedProps ignore_named_props) const;

    virtual WebIDL::ExceptionOr<JS::Value> item_value(size_t index) const;
    virtual WebIDL::ExceptionOr<JS::Value> named_item_value(DeprecatedFlyString const& name) const;
    virtual Vector<DeprecatedString> supported_property_names() const;
    virtual bool is_supported_property_index(u32) const;

    // NOTE: These will crash if you make has_named_property_setter return true but do not override these methods.
    // NOTE: This is only used if named_property_setter_has_identifier returns false, otherwise set_value_of_named_property is used instead.
    virtual WebIDL::ExceptionOr<void> set_value_of_new_named_property(DeprecatedString const&, JS::Value) { VERIFY_NOT_REACHED(); }
    virtual WebIDL::ExceptionOr<void> set_value_of_existing_named_property(DeprecatedString const&, JS::Value) { VERIFY_NOT_REACHED(); }

    // NOTE: These will crash if you make has_named_property_setter return true but do not override these methods.
    // NOTE: This is only used if you make named_property_setter_has_identifier return true, otherwise set_value_of_{new,existing}_named_property is used instead.
    virtual WebIDL::ExceptionOr<void> set_value_of_named_property(DeprecatedString const&, JS::Value) { VERIFY_NOT_REACHED(); }

    // NOTE: These will crash if you make has_indexed_property_setter return true but do not override these methods.
    // NOTE: This is only used if indexed_property_setter_has_identifier returns false, otherwise set_value_of_indexed_property is used instead.
    virtual WebIDL::ExceptionOr<void> set_value_of_new_indexed_property(u32, JS::Value) { VERIFY_NOT_REACHED(); }
    virtual WebIDL::ExceptionOr<void> set_value_of_existing_indexed_property(u32, JS::Value) { VERIFY_NOT_REACHED(); }

    // NOTE: These will crash if you make has_named_property_setter return true but do not override these methods.
    // NOTE: This is only used if indexed_property_setter_has_identifier returns true, otherwise set_value_of_{new,existing}_indexed_property is used instead.
    virtual WebIDL::ExceptionOr<void> set_value_of_indexed_property(u32, JS::Value) { VERIFY_NOT_REACHED(); }

    enum class DidDeletionFail {
        // If the named property deleter has an identifier, but does not return a boolean.
        // This is done because we don't know the return type of the deleter outside of the IDL generator.
        NotRelevant,
        No,
        Yes,
    };

    // NOTE: This will crash if you make has_named_property_deleter return true but do not override this method.
    virtual WebIDL::ExceptionOr<DidDeletionFail> delete_value(DeprecatedString const&) { VERIFY_NOT_REACHED(); }

protected:
    explicit LegacyPlatformObject(JS::Realm& realm);

    // NOTE: These two can also be seen as "has x property getter"
    virtual bool supports_indexed_properties() const = 0;
    virtual bool supports_named_properties() const = 0;

    virtual bool has_indexed_property_setter() const = 0;
    virtual bool has_named_property_setter() const = 0;

    virtual bool has_named_property_deleter() const = 0;

    virtual bool has_legacy_unenumerable_named_properties_interface_extended_attribute() const = 0;
    virtual bool has_global_interface_extended_attribute() const = 0;

    virtual bool indexed_property_setter_has_identifier() const = 0;
    virtual bool named_property_setter_has_identifier() const = 0;
    virtual bool named_property_deleter_has_identifier() const = 0;

private:
    WebIDL::ExceptionOr<void> invoke_indexed_property_setter(JS::PropertyKey const&, JS::Value);
    WebIDL::ExceptionOr<void> invoke_named_property_setter(DeprecatedString const&, JS::Value);
};

}
