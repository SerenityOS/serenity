#pragma once

#include <AK/Badge.h>
#include <LibCore/CSocket.h>

class CTCPServer;

class CTCPSocket final : public CSocket {
    C_OBJECT(CTCPSocket)
public:
    explicit CTCPSocket(CObject* parent = nullptr);
    CTCPSocket(Badge<CTCPServer>, int fd, CObject* parent = nullptr);
    virtual ~CTCPSocket() override;
};
