#pragma once

#include "Color.h"
#include "Size.h"
#include <AK/Retainable.h>
#include <AK/RetainPtr.h>

#ifdef KERNEL
#include "Process.h"
#endif

class GraphicsBitmap : public Retainable<GraphicsBitmap> {
public:
#ifdef KERNEL
    static RetainPtr<GraphicsBitmap> create(Process&, const Size&);
#endif
    static RetainPtr<GraphicsBitmap> create_wrapper(const Size&, RGBA32*);
    ~GraphicsBitmap();

    RGBA32* scanline(int y);
    const RGBA32* scanline(int y) const;

    Size size() const { return m_size; }
    int width() const { return m_size.width(); }
    int height() const { return m_size.height(); }
    size_t pitch() const { return m_pitch; }

#ifdef KERNEL
    Region* client_region() { return m_client_region; }
    Region* server_region() { return m_server_region; }
#endif

private:
#ifdef KERNEL
    GraphicsBitmap(Process&, const Size&);
#endif
    GraphicsBitmap(const Size&, RGBA32*);

    Size m_size;
    RGBA32* m_data { nullptr };
    size_t m_pitch { 0 };

#ifdef KERNEL
    Process* m_client_process { nullptr };
    Region* m_client_region { nullptr };
    Region* m_server_region { nullptr };
#endif
};

inline RGBA32* GraphicsBitmap::scanline(int y)
{
    return reinterpret_cast<RGBA32*>((((byte*)m_data) + (y * m_pitch)));
}

inline const RGBA32* GraphicsBitmap::scanline(int y) const
{
    return reinterpret_cast<const RGBA32*>((((const byte*)m_data) + (y * m_pitch)));
}
