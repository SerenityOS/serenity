/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/Value.h>
#include <LibWeb/Bindings/Wrappable.h>
#include <LibWeb/Crypto/SubtleCrypto.h>
#include <LibWeb/DOM/ExceptionOr.h>

namespace Web::Crypto {

class Crypto : public Bindings::Wrappable
    , public RefCounted<Crypto>
    , public Weakable<Crypto> {
public:
    using WrapperType = Bindings::CryptoWrapper;

    static NonnullRefPtr<Crypto> create()
    {
        return adopt_ref(*new Crypto());
    }

    NonnullRefPtr<SubtleCrypto> subtle() const { return m_subtle; }

    DOM::ExceptionOr<JS::Value> get_random_values(JS::Value array) const;

private:
    Crypto();

    NonnullRefPtr<SubtleCrypto> m_subtle;
};

}

namespace Web::Bindings {

CryptoWrapper* wrap(JS::GlobalObject&, Crypto::Crypto&);

}
