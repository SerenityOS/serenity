/*
 * Copyright (c) 2026, Bastiaan van der Plaat <bastiaan.v.d.plaat@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Widget.h>

namespace UsersSettings {

class ChangePasswordDialog final : public GUI::Widget {
    C_OBJECT(ChangePasswordDialog)
public:
    static ErrorOr<NonnullRefPtr<ChangePasswordDialog>> try_create();

private:
    ChangePasswordDialog() = default;
};

}
