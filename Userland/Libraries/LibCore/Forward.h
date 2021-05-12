/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

namespace Core {

class AbstractNotifier;
class AnonymousBuffer;
class ArgsParser;
template<typename T>
class BufferingIODevice;
class ChildEvent;
class ConfigFile;
class CustomEvent;
class DateTime;
class DirIterator;
class ElapsedTimer;
class Event;
class EventLoop;
class File;
class FileLikeIODevice;
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
class SocketLikeIODevice;
class TCPServer;
class TCPSocket;
class Timer;
class TimerEvent;
class UDPServer;
class UDPSocket;

enum class TimerShouldFireWhenNotVisible;

}
