#pragma once

#include <LibCore/CObject.h>
#include <LibCore/CEvent.h>
#include <LibCore/CIODevice.h>
#include <LibCore/CNotifier.h>

struct ASAPI_ServerMessage;
struct ASAPI_ClientMessage;

class ASEvent : public CEvent {
public:
    enum Type {
        Invalid = 2000,
        WM_ClientDisconnected,
    };
    ASEvent() {}
    explicit ASEvent(Type type)
        : CEvent(type)
    {
    }
};

class ASClientDisconnectedNotification : public ASEvent {
public:
    explicit ASClientDisconnectedNotification(int client_id)
        : ASEvent(WM_ClientDisconnected)
        , m_client_id(client_id)
    {
    }

    int client_id() const { return m_client_id; }

private:
    int m_client_id { 0 };
};

class ASMixer;

class ASClientConnection : public CObject
{
public:
    ASClientConnection(int fd, int client_id, ASMixer& mixer);
    ~ASClientConnection();

    void post_message(const ASAPI_ServerMessage&, const ByteBuffer& = {});
    bool handle_message(const ASAPI_ClientMessage&, const ByteBuffer& = {});

    void drain_client();

    void did_misbehave();

    const char* class_name() const override { return "ASClientConnection"; }

protected:
    void event(CEvent& event) override;
private:
    // TODO: A way to create some kind of CIODevice with an open FD would be nice.
    class ASOpenedSocket : public CIODevice
    {
    public:
        const char* class_name() const override { return "ASOpenedSocket"; }
        ASOpenedSocket(int fd)
        {
            set_fd(fd);
            set_mode(CIODevice::OpenMode::ReadWrite);
        }

        bool open(CIODevice::OpenMode) override
        {
            ASSERT_NOT_REACHED();
            return true;
        };

        int fd() const { return CIODevice::fd(); }
    };

    ASOpenedSocket m_socket;
    CNotifier m_notifier;
    int m_client_id;
    int m_pid;
    ASMixer& m_mixer;
};

