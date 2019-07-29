#pragma once

#include <AK/Assertions.h>

struct ASAPI_ServerMessage {
    enum class Type {
        Invalid,
        Greeting,
        FinishedPlayingBuffer,
        EnqueueBufferResponse,
        DidGetMainMixVolume,
        DidSetMainMixVolume,
    };

    Type type { Type::Invalid };
    unsigned extra_size { 0 };
    bool success { true };
    int value { 0 };

    union {
        struct {
            int server_pid;
            int your_client_id;
        } greeting;
        struct {
            int buffer_id;
        } playing_buffer;
    };
};

struct ASAPI_ClientMessage {
    enum class Type {
        Invalid,
        Greeting,
        EnqueueBuffer,
        GetMainMixVolume,
        SetMainMixVolume,
    };

    Type type { Type::Invalid };
    unsigned extra_size { 0 };
    int value { 0 };

    union {
        struct {
            int client_pid;
        } greeting;
        struct {
            int buffer_id;
        } play_buffer;
    };
};

// FIXME: Everything below this line should be generated from some kind of IPC protocol description.

namespace ASAPI_Server {
class Greeting;
class FinishedPlayingBuffer;
class EnqueueBufferResponse;
class DidGetMainMixVolume;
class DidSetMainMixVolume;
}

namespace ASAPI_Client {

template<ASAPI_ClientMessage::Type type>
class Message {
public:
    static ASAPI_ClientMessage::Type message_type() { return type; }
    operator const ASAPI_ClientMessage&() const { return m_message; }

protected:
    Message()
    {
        m_message.type = type;
    }

    Message(const ASAPI_ClientMessage& message)
        : m_message(message)
    {
        ASSERT(message.type == type);
    }

    ASAPI_ClientMessage m_message;
};

class Greeting : public Message<ASAPI_ClientMessage::Type::Greeting> {
public:
    typedef ASAPI_Server::Greeting ResponseType;
    Greeting(const ASAPI_ClientMessage& message)
        : Message(message)
    {
    }

    Greeting(int client_pid)
    {
        m_message.greeting.client_pid = client_pid;
    }

    int client_pid() const { return m_message.greeting.client_pid; }
};

class EnqueueBuffer : public Message<ASAPI_ClientMessage::Type::EnqueueBuffer> {
public:
    typedef ASAPI_Server::EnqueueBufferResponse ResponseType;

    EnqueueBuffer(const ASAPI_ClientMessage& message)
        : Message(message)
    {
    }

    EnqueueBuffer(int buffer_id)
    {
        m_message.play_buffer.buffer_id = buffer_id;
    }

    int buffer_id() const { return m_message.play_buffer.buffer_id; }
};

class GetMainMixVolume : public Message<ASAPI_ClientMessage::Type::GetMainMixVolume> {
public:
    typedef ASAPI_Server::DidGetMainMixVolume ResponseType;

    GetMainMixVolume(const ASAPI_ClientMessage& message)
        : Message(message)
    {
    }

    GetMainMixVolume()
    {
    }
};

class SetMainMixVolume : public Message<ASAPI_ClientMessage::Type::SetMainMixVolume> {
public:
    typedef ASAPI_Server::DidSetMainMixVolume ResponseType;

    SetMainMixVolume(const ASAPI_ClientMessage& message)
        : Message(message)
    {
    }

    SetMainMixVolume(int volume)
    {
        m_message.value = volume;
    }
};

}

namespace ASAPI_Server {

template<ASAPI_ServerMessage::Type type>
class Message {
public:
    static ASAPI_ServerMessage::Type message_type() { return type; }
    operator const ASAPI_ServerMessage&() const { return m_message; }

protected:
    Message()
    {
        m_message.type = type;
    }

    Message(const ASAPI_ServerMessage& message)
        : m_message(message)
    {
        ASSERT(message.type == type);
    }

    ASAPI_ServerMessage m_message;
};

class Greeting : public Message<ASAPI_ServerMessage::Type::Greeting> {
public:
    Greeting(const ASAPI_ServerMessage& message)
        : Message(message)
    {
    }

    Greeting(int server_pid, int your_client_id)
    {
        m_message.greeting.server_pid = server_pid;
        m_message.greeting.your_client_id = your_client_id;
    }

    int server_pid() const { return m_message.greeting.server_pid; }
    int your_client_id() const { return m_message.greeting.your_client_id; }
};

class FinishedPlayingBuffer : public Message<ASAPI_ServerMessage::Type::FinishedPlayingBuffer> {
public:
    FinishedPlayingBuffer(const ASAPI_ServerMessage& message)
        : Message(message)
    {
    }

    FinishedPlayingBuffer(int buffer_id)
    {
        m_message.playing_buffer.buffer_id = buffer_id;
    }

    int buffer_id() const { return m_message.playing_buffer.buffer_id; }
};

class EnqueueBufferResponse : public Message<ASAPI_ServerMessage::Type::EnqueueBufferResponse> {
public:
    EnqueueBufferResponse(const ASAPI_ServerMessage& message)
        : Message(message)
    {
    }

    EnqueueBufferResponse(bool success, int buffer_id)
    {
        m_message.success = success;
        m_message.playing_buffer.buffer_id = buffer_id;
    }

    bool success() const { return m_message.success; }
    int buffer_id() const { return m_message.playing_buffer.buffer_id; }
};

class DidGetMainMixVolume : public Message<ASAPI_ServerMessage::Type::DidGetMainMixVolume> {
public:
    DidGetMainMixVolume(const ASAPI_ServerMessage& message)
        : Message(message)
    {
    }

    DidGetMainMixVolume(int volume)
    {
        m_message.value = volume;
    }

    int volume() const { return m_message.value; }
};

class DidSetMainMixVolume : public Message<ASAPI_ServerMessage::Type::DidSetMainMixVolume> {
public:
    DidSetMainMixVolume(const ASAPI_ServerMessage& message)
        : Message(message)
    {
    }

    DidSetMainMixVolume()
    {
    }
};

}
