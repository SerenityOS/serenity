#pragma once

#include <AK/Badge.h>
#include <LibCore/CSocket.h>

class CLocalServer;

class CLocalSocket final : public CSocket {
    C_OBJECT(CLocalSocket)
public:
    explicit CLocalSocket(CObject* parent = nullptr);
    CLocalSocket(Badge<CLocalServer>, int fd, CObject* parent = nullptr);
    virtual ~CLocalSocket() override;
};
