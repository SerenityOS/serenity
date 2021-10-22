/*
 * Copyright (c) 2021, Maciej Zygmanowski <sppmacd@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <AK/StringView.h>

#ifdef __serenity__
namespace Core::IPCSockets {

String system_socket_directory();
String system_socket(StringView const& basename);
String user_socket_directory();
String user_socket(StringView const& basename);

ErrorOr<void> unveil_system_socket(StringView name);
ErrorOr<void> unveil_user_socket(StringView name);

}
#endif
