/*
 * Copyright (c) 2023, stelar7 <dudedbz@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Crypto/CryptoKey.h>

namespace Web::Crypto {

JS_DEFINE_ALLOCATOR(CryptoKey);

JS::NonnullGCPtr<CryptoKey> CryptoKey::create(JS::Realm& realm)
{
    return realm.heap().allocate<CryptoKey>(realm, realm);
}

CryptoKey::CryptoKey(JS::Realm& realm)
    : PlatformObject(realm)
    , m_algorithm(Object::create(realm, nullptr))
    , m_usages(Object::create(realm, nullptr))
{
}

CryptoKey::~CryptoKey() = default;

void CryptoKey::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::CryptoKeyPrototype>(realm, "CryptoKey"_fly_string));
}

void CryptoKey::visit_edges(Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_algorithm);
    visitor.visit(m_usages);
}

}
