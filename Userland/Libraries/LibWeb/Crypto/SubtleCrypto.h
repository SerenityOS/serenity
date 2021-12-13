/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Bindings/Wrappable.h>

namespace Web::Crypto {

class SubtleCrypto
    : public Bindings::Wrappable
    , public RefCounted<SubtleCrypto> {
public:
    using WrapperType = Bindings::SubtleCryptoWrapper;

    static NonnullRefPtr<SubtleCrypto> create()
    {
        return adopt_ref(*new SubtleCrypto());
    }

private:
    SubtleCrypto() = default;
};

}

namespace Web::Bindings {

SubtleCryptoWrapper* wrap(JS::GlobalObject&, Crypto::SubtleCrypto&);

}
