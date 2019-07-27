#pragma once

#include <AK/ByteBuffer.h>
#include <AK/NonnullRefPtrVector.h>
#include <AK/RefCounted.h>
#include <AK/WeakPtr.h>
#include <LibAudio/ABuffer.h>
#include <LibCore/CFile.h>
#include <LibCore/CLock.h>

class ASClientConnection;

class ASMixer : public RefCounted<ASMixer> {
public:
    ASMixer();

    void queue(ASClientConnection&, const ABuffer&);

private:
    struct ASMixerBuffer {
        ASMixerBuffer(const NonnullRefPtr<ABuffer>&, ASClientConnection&);
        NonnullRefPtr<ABuffer> buffer;
        int pos { 0 };
        bool done { false };
        WeakPtr<ASClientConnection> m_client;
    };

    Vector<ASMixerBuffer> m_pending_mixing;
    CFile m_device;
    CLock m_lock;

    void mix();
};
