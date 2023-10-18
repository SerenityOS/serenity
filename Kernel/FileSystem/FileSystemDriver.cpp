/*
 * Copyright (c) 2023, Leon Albrecht <leon.a@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/FileSystemDriver.h>

namespace Kernel::FS {

Singleton<Vector<NonnullLockRefPtr<Driver>>> file_system_drivers;

}
