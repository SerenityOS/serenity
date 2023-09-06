/*
 * Copyright (c) 2023, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <android/log.h>
#include <jni.h>
#include <unistd.h>

extern "C" JNIEXPORT void JNICALL
Java_org_serenityos_ladybird_WebContentService_nativeHandleTransferSockets(JNIEnv*, jobject /* thiz */, jint ipc_socket, jint fd_passing_socket)
{
    __android_log_print(ANDROID_LOG_INFO, "WebContent", "New binding received, sockets %d and %d", ipc_socket, fd_passing_socket);
    ::close(ipc_socket);
    ::close(fd_passing_socket);
}
