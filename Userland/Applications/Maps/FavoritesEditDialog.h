/*
 * Copyright (c) 2023, Bastiaan van der Plaat <bastiaan.v.d.plaat@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Widget.h>

namespace Maps {

class FavoritesEditDialog final : public GUI::Widget {
    C_OBJECT(FavoritesEditDialog)
public:
    static ErrorOr<NonnullRefPtr<FavoritesEditDialog>> try_create();
};

}
