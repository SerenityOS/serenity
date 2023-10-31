/*
 * Copyright (c) 2023, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullRefPtr.h>

template<typename Client>
ErrorOr<NonnullRefPtr<Client>> bind_service(void (*bind_method)(int, int));

void bind_request_server_java(int ipc_socket, int fd_passing_socket);
void bind_web_socket_java(int ipc_socket, int fd_passing_socket);
void bind_image_decoder_java(int ipc_socket, int fd_passing_socket);
