/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

namespace Core {

class AnonymousBuffer;
class ArgsParser;
class ChildEvent;
class ConfigFile;
class CustomEvent;
class DateTime;
class DirIterator;
class DeferredInvocationContext;
class ElapsedTimer;
class Event;
class EventLoop;
class File;
class IODevice;
class LocalServer;
class LocalSocket;
class MimeData;
class NetworkJob;
class NetworkResponse;
class Notifier;
class Object;
class ObjectClassRegistration;
class ProcessStatisticsReader;
class Socket;
class SocketAddress;
class TCPServer;
class TCPSocket;
class Timer;
class TimerEvent;
class UDPServer;
class UDPSocket;

enum class TimerShouldFireWhenNotVisible;

}
