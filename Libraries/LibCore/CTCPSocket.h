#pragma once

#include <AK/Badge.h>
#include <LibCore/CSocket.h>

class CTCPServer;

class CTCPSocket final : public CSocket {
    C_OBJECT(CTCPSocket)
public:
    virtual ~CTCPSocket() override;

private:
    CTCPSocket(int fd, CObject* parent = nullptr);
    explicit CTCPSocket(CObject* parent = nullptr);
};
