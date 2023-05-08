/*
 * Copyright (c) 2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

namespace Web::CSS {

enum class Important {
    No,
    Yes,
};

struct StyleProperty {
    Important important { Important::No };
    CSS::PropertyID property_id;
    NonnullRefPtr<StyleValue const> value;
    DeprecatedString custom_name {};
};

}
