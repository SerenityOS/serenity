/*
 * Copyright (c) 2023, Jonah Shafran <jonahshafran@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/CSS/CSSRule.h>

namespace Web::CSS {

class CSSNamespaceRule final : public CSSRule {
    WEB_PLATFORM_OBJECT(CSSNamespaceRule, CSSRule);

public:
    static WebIDL::ExceptionOr<JS::NonnullGCPtr<CSSNamespaceRule>> create(JS::Realm&, Optional<DeprecatedString> prefix, StringView namespace_uri);

    virtual ~CSSNamespaceRule() = default;

    void set_namespace_uri(DeprecatedString value) { m_namespace_uri = move(value); }
    DeprecatedString namespace_uri() const { return m_namespace_uri; }
    void set_prefix(DeprecatedString value) { m_prefix = move(value); }
    DeprecatedString prefix() const { return m_prefix; }
    virtual Type type() const override { return Type::Namespace; }

private:
    CSSNamespaceRule(JS::Realm&, Optional<DeprecatedString> prefix, StringView namespace_uri);

    virtual void initialize(JS::Realm&) override;

    virtual DeprecatedString serialized() const override;
    DeprecatedString m_namespace_uri;
    DeprecatedString m_prefix;
};

}
