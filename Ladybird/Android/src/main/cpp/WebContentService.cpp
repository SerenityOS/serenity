/*
 * Copyright (c) 2023, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/LexicalPath.h>
#include <Ladybird/FontPlugin.h>
#include <Ladybird/HelperProcess.h>
#include <Ladybird/ImageCodecPlugin.h>
#include <Ladybird/Utilities.h>
#include <LibAudio/Loader.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/EventLoop.h>
#include <LibCore/LocalServer.h>
#include <LibCore/System.h>
#include <LibIPC/ConnectionFromClient.h>
#include <LibJS/Bytecode/Interpreter.h>
#include <LibWeb/Bindings/MainThreadVM.h>
#include <LibWeb/HTML/Window.h>
#include <LibWeb/Loader/FrameLoader.h>
#include <LibWeb/Loader/ResourceLoader.h>
#include <LibWeb/Platform/AudioCodecPluginAgnostic.h>
#include <LibWeb/Platform/EventLoopPluginSerenity.h>
#include <LibWebView/RequestServerAdapter.h>
#include <LibWebView/WebSocketClientAdapter.h>
#include <WebContent/ConnectionFromClient.h>
#include <WebContent/PageHost.h>
#include <jni.h>
#include <unistd.h>

class NullResourceConnector : public Web::ResourceLoaderConnector {
    virtual void prefetch_dns(AK::URL const&) override { }
    virtual void preconnect(AK::URL const&) override { }

    virtual RefPtr<Web::ResourceLoaderConnectorRequest> start_request(DeprecatedString const&, AK::URL const&, HashMap<DeprecatedString, DeprecatedString> const&, ReadonlyBytes, Core::ProxyData const&) override
    {
        return nullptr;
    }
};

ErrorOr<int> web_content_main(int ipc_socket, int fd_passing_socket)
{
    Core::EventLoop event_loop;

    Web::Platform::EventLoopPlugin::install(*new Web::Platform::EventLoopPluginSerenity);
    Web::Platform::ImageCodecPlugin::install(*new Ladybird::ImageCodecPlugin);

    Web::Platform::AudioCodecPlugin::install_creation_hook([](auto loader) {
        (void)loader;
        return Error::from_string_literal("Don't know how to initialize audio in this configuration!");
    });

    Web::FrameLoader::set_default_favicon_path(DeprecatedString::formatted("{}/res/icons/16x16/app-browser.png", s_serenity_resource_root));

    Web::ResourceLoader::initialize(make_ref_counted<NullResourceConnector>());

    bool is_layout_test_mode = false;

    Web::HTML::Window::set_internals_object_exposed(is_layout_test_mode);
    Web::Platform::FontPlugin::install(*new Ladybird::FontPlugin(is_layout_test_mode));

    Web::FrameLoader::set_resource_directory_url(DeprecatedString::formatted("file://{}/res", s_serenity_resource_root));
    Web::FrameLoader::set_error_page_url(DeprecatedString::formatted("file://{}/res/html/error.html", s_serenity_resource_root));
    Web::FrameLoader::set_directory_page_url(DeprecatedString::formatted("file://{}/res/html/directory.html", s_serenity_resource_root));

    TRY(Web::Bindings::initialize_main_thread_vm());

    auto webcontent_socket = TRY(Core::LocalSocket::adopt_fd(ipc_socket));
    auto webcontent_client = TRY(WebContent::ConnectionFromClient::try_create(move(webcontent_socket)));
    webcontent_client->set_fd_passing_socket(TRY(Core::LocalSocket::adopt_fd(fd_passing_socket)));

    return event_loop.exec();
}

extern "C" JNIEXPORT void JNICALL
Java_org_serenityos_ladybird_WebContentService_nativeThreadLoop(JNIEnv*, jobject /* thiz */, jint ipc_socket, jint fd_passing_socket)
{
    dbgln("New binding received, sockets {} and {}", ipc_socket, fd_passing_socket);
    auto ret = web_content_main(ipc_socket, fd_passing_socket);
    if (ret.is_error()) {
        warnln("Runtime Error: {}", ret.release_error());
    } else {
        outln("Thread exited with code {}", ret.release_value());
    }
}

extern "C" JNIEXPORT void JNICALL
Java_org_serenityos_ladybird_WebContentService_initNativeCode(JNIEnv* env, jobject /* thiz */, jstring resource_dir, jstring tag_name)
{
    static Atomic<bool> s_initialized_flag { false };
    if (s_initialized_flag.exchange(true) == true) {
        // Skip initializing if someone else already started the process at some point in the past
        return;
    }

    char const* raw_resource_dir = env->GetStringUTFChars(resource_dir, nullptr);
    s_serenity_resource_root = raw_resource_dir;
    env->ReleaseStringUTFChars(resource_dir, raw_resource_dir);

    char const* raw_tag_name = env->GetStringUTFChars(tag_name, nullptr);
    AK::set_log_tag_name(raw_tag_name);
    env->ReleaseStringUTFChars(tag_name, raw_tag_name);
}
