/*
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

namespace JS {

enum class FunctionKind {
    Normal,
    Generator,
    Async,
    AsyncGenerator
};

}
