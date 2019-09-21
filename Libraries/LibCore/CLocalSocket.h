#pragma once

#include <AK/Badge.h>
#include <LibCore/CSocket.h>

class CLocalServer;

class CLocalSocket final : public CSocket {
    C_OBJECT(CLocalSocket)
public:
    virtual ~CLocalSocket() override;

private:
    explicit CLocalSocket(CObject* parent = nullptr);
    CLocalSocket(int fd, CObject* parent = nullptr);
};
