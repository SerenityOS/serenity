/*
 * Copyright (c) 2023, Jonah Shafran <jonahshafran@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Heap/Heap.h>
#include <LibJS/Runtime/Realm.h>
#include <LibWeb/Bindings/CSSNamespaceRulePrototype.h>
#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/CSS/CSSNamespaceRule.h>
#include <LibWeb/WebIDL/ExceptionOr.h>

namespace Web::CSS {

CSSNamespaceRule::CSSNamespaceRule(JS::Realm& realm, Optional<StringView> prefix, StringView namespace_uri)
    : CSSRule(realm)
    , m_namespace_uri(namespace_uri)
    , m_prefix(prefix.has_value() ? prefix.value() : ""sv)
{
}

WebIDL::ExceptionOr<JS::NonnullGCPtr<CSSNamespaceRule>> CSSNamespaceRule::create(JS::Realm& realm, Optional<AK::StringView> prefix, AK::StringView namespace_uri)
{
    return MUST_OR_THROW_OOM(realm.heap().allocate<CSSNamespaceRule>(realm, realm, prefix, namespace_uri));
}

JS::ThrowCompletionOr<void> CSSNamespaceRule::initialize(JS::Realm& realm)
{
    MUST_OR_THROW_OOM(Base::initialize(realm));
    set_prototype(&Bindings::ensure_web_prototype<Bindings::CSSNamespaceRulePrototype>(realm, "CSSNamespaceRule"));

    return {};
}

// https://www.w3.org/TR/cssom/#serialize-a-css-rule
DeprecatedString CSSNamespaceRule::serialized() const
{
    StringBuilder builder;
    // The literal string "@namespace", followed by a single SPACE (U+0020),
    builder.append("@namespace "sv);

    // followed by the serialization as an identifier of the prefix attribute (if any),
    if (!m_prefix.is_empty() && !m_prefix.is_null()) {
        builder.append(m_prefix);
        // followed by a single SPACE (U+0020) if there is a prefix,
        builder.append(" "sv);
    }

    //  followed by the serialization as URL of the namespaceURI attribute,
    builder.append(m_namespace_uri);

    // followed the character ";" (U+003B).
    builder.append(";"sv);

    return builder.to_deprecated_string();
}

}
