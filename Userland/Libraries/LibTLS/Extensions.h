/*
 * Copyright (c) 2020, Ali Mohammad Pur <mpfard@serenityos.org>
 * Copyright (c) 2023, stelar7 <dudedbz@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

namespace TLS {

#define _ENUM_KEY(name) name,
#define _ENUM_KEY_VALUE(name, value) name = value,

// https://www.iana.org/assignments/tls-parameters/tls-parameters.xhtml#tls-parameters-5
#define __ENUM_CONTENT_TYPES                \
    _ENUM_KEY_VALUE(CHANGE_CIPHER_SPEC, 20) \
    _ENUM_KEY_VALUE(ALERT, 21)              \
    _ENUM_KEY_VALUE(HANDSHAKE, 22)          \
    _ENUM_KEY_VALUE(APPLICATION_DATA, 23)   \
    _ENUM_KEY_VALUE(HEARTBEAT, 24)          \
    _ENUM_KEY_VALUE(TLS12_CID, 25)          \
    _ENUM_KEY_VALUE(ACK, 26)

enum class ContentType : u8 {
    __ENUM_CONTENT_TYPES
};

#undef _ENUM_KEY
#undef _ENUM_KEY_VALUE

}
