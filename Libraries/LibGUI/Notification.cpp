#include <LibGUI/Notification.h>
#include <LibIPC/ServerConnection.h>
#include <NotificationServer/NotificationClientEndpoint.h>
#include <NotificationServer/NotificationServerEndpoint.h>

namespace GUI {

class NotificationServerConnection : public IPC::ServerConnection<NotificationClientEndpoint, NotificationServerEndpoint>
    , public NotificationClientEndpoint {
    C_OBJECT(NotificationServerConnection)
public:
    virtual void handshake() override
    {
        auto response = send_sync<Messages::NotificationServer::Greet>();
        set_my_client_id(response->client_id());
    }

private:
    NotificationServerConnection()
        : IPC::ServerConnection<NotificationClientEndpoint, NotificationServerEndpoint>(*this, "/tmp/portal/notify")
    {

    }
    virtual void handle(const Messages::NotificationClient::Dummy&) override {}
};

Notification::Notification()
{
}

Notification::~Notification()
{
}

static NotificationServerConnection& notification_server_connection()
{
    static NotificationServerConnection* connection;
    if (!connection)
        connection = &NotificationServerConnection::construct().leak_ref();
    return *connection;
}

void Notification::show()
{
    notification_server_connection().post_message(Messages::NotificationServer::ShowNotification(m_text, m_title));
}

}
