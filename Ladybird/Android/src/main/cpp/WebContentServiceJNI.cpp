/*
 * Copyright (c) 2023, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "JNIHelpers.h"
#include "LadybirdServiceBase.h"
#include <jni.h>

jobject global_instance;
jclass global_class_reference;
jmethodID bind_request_server_method;
jmethodID bind_web_socket_method;

extern "C" JNIEXPORT void JNICALL
Java_org_serenityos_ladybird_WebContentService_nativeInit(JNIEnv* env, jobject thiz)
{
    global_instance = env->NewGlobalRef(thiz);

    auto local_class = env->FindClass("org/serenityos/ladybird/WebContentService");
    if (!local_class)
        TODO();
    global_class_reference = reinterpret_cast<jclass>(env->NewGlobalRef(local_class));
    env->DeleteLocalRef(local_class);

    auto method = env->GetMethodID(global_class_reference, "bindRequestServer", "(II)V");
    if (!method)
        TODO();
    bind_request_server_method = method;

    method = env->GetMethodID(global_class_reference, "bindWebSocket", "(II)V");
    if (!method)
        TODO();
    bind_web_socket_method = method;
}

void bind_request_server_java(int ipc_socket, int fd_passing_socket)
{
    Ladybird::JavaEnvironment env(global_vm);
    env.get()->CallVoidMethod(global_instance, bind_request_server_method, ipc_socket, fd_passing_socket);
}

void bind_web_socket_java(int ipc_socket, int fd_passing_socket)
{
    Ladybird::JavaEnvironment env(global_vm);
    env.get()->CallVoidMethod(global_instance, bind_web_socket_method, ipc_socket, fd_passing_socket);
}
