/*
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

namespace WebView {

struct ModelIndex {
    bool is_valid() const { return row != -1 && column != -1; }

    int row { -1 };
    int column { -1 };
    void const* internal_data { nullptr };
};

}
