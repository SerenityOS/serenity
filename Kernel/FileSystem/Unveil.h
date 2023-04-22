/*
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/FileSystem/UnveilData.h>

namespace Kernel {

ErrorOr<void> prepare_parameters_for_new_unveiled_path(StringView unveiled_path, StringView permissions, OwnPtr<KString>& new_unveiled_path, UnveilAccess& new_permissions);
ErrorOr<void> prepare_parameters_for_new_jail_unveiled_path(StringView unveiled_path, StringView permissions, UnveilAccess& new_permissions);

ErrorOr<void> update_unveil_data(UnveilData& locked_unveil_data, StringView unveiled_path, UnveilAccess permissions);

}
