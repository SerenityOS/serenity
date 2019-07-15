#pragma once

#include <AK/RefCounted.h>
#include <AK/ByteBuffer.h>
#include <LibCore/CFile.h>
#include <LibCore/CLock.h>
#include <LibAudio/ABuffer.h>
#include <AK/NonnullRefPtrVector.h>

class ASClientConnection;

class ASMixer : public RefCounted<ASMixer> {
public:
    ASMixer();

    void queue(ASClientConnection&, const ABuffer&);

private:
    struct ASMixerBuffer {
        ASMixerBuffer(const NonnullRefPtr<ABuffer>& buf)
            : buffer(buf)
        {}
        NonnullRefPtr<ABuffer> buffer;
        int pos { 0 };
        bool done { false };
    };

    Vector<ASMixerBuffer> m_pending_mixing;
    CFile m_device;
    CLock m_lock;

    void mix();
};
