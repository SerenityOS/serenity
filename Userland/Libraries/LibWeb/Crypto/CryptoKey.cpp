/*
 * Copyright (c) 2023, stelar7 <dudedbz@gmail.com>
 * Copyright (c) 2024, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Memory.h>
#include <LibWeb/Crypto/CryptoKey.h>

namespace Web::Crypto {

JS_DEFINE_ALLOCATOR(CryptoKey);

JS::NonnullGCPtr<CryptoKey> CryptoKey::create(JS::Realm& realm, InternalKeyData key_data)
{
    return realm.heap().allocate<CryptoKey>(realm, realm, move(key_data));
}

CryptoKey::CryptoKey(JS::Realm& realm, InternalKeyData key_data)
    : PlatformObject(realm)
    , m_algorithm(Object::create(realm, nullptr))
    , m_usages(Object::create(realm, nullptr))
    , m_key_data(move(key_data))
{
}

CryptoKey::~CryptoKey()
{
    m_key_data.visit(
        [](ByteBuffer& data) { secure_zero(data.data(), data.size()); },
        [](auto& data) { secure_zero(reinterpret_cast<u8*>(&data), sizeof(data)); });
}

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
