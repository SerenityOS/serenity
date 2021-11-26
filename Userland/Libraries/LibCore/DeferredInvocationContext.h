/*
 * Copyright (c) 2018-2020, sin-ack <sin-ack@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibCore/Object.h>

namespace Core {

class DeferredInvocationContext final : public Core::Object {
    C_OBJECT(DeferredInvocationContext)
private:
    DeferredInvocationContext() { }
};

}
