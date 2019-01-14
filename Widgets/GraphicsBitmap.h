#pragma once

#include "Color.h"
#include "Size.h"
#include <AK/Retainable.h>
#include <AK/RetainPtr.h>
#include "Process.h"

class GraphicsBitmap : public Retainable<GraphicsBitmap> {
public:
    static RetainPtr<GraphicsBitmap> create(Process&, const Size&);
    static RetainPtr<GraphicsBitmap> create_wrapper(const Size&, RGBA32*);
    ~GraphicsBitmap();

    RGBA32* scanline(int y);
    const RGBA32* scanline(int y) const;

    Size size() const { return m_size; }
    int width() const { return m_size.width(); }
    int height() const { return m_size.height(); }
    size_t pitch() const { return m_pitch; }

    Region* client_region() { return m_client_region; }
    Region* server_region() { return m_server_region; }

private:
    GraphicsBitmap(Process&, const Size&);
    GraphicsBitmap(const Size&, RGBA32*);

    Size m_size;
    RGBA32* m_data { nullptr };
    size_t m_pitch { 0 };
    Process* m_client_process { nullptr };
    Region* m_client_region { nullptr };
    Region* m_server_region { nullptr };
};
