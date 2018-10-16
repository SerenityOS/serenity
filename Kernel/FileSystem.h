#pragma once

#include "types.h"
#include "RefCounted.h"

namespace FileSystem {

void initialize();

class VirtualNode : public RefCounted<VirtualNode> {
public:
    DWORD saneValue = 0x850209;
    virtual ~VirtualNode();

    DWORD index() const { return m_index; }
    const String& path() const { return m_path; }

    virtual size_t size() const = 0;
    virtual uid_t uid() const = 0;
    virtual gid_t gid() const = 0;
    virtual size_t mode() const = 0;

    virtual size_t read(BYTE* outbuf, size_t start, size_t maxLength) = 0;

protected:
    VirtualNode(DWORD index, String&& path);

private:
    DWORD m_index { 0 };
    String m_path;
};

RefPtr<VirtualNode> createVirtualNode(String&& path);

}
