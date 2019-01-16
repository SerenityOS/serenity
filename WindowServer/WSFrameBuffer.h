#pragma once

#include "WSScreen.h"
#include <Widgets/Color.h>

class GraphicsBitmap;

class WSFrameBuffer final : public WSScreen {
public:
    WSFrameBuffer(unsigned width, unsigned height);
    WSFrameBuffer(RGBA32*, unsigned width, unsigned height);
    virtual ~WSFrameBuffer() override;

    void show();

    static WSFrameBuffer& the();

    RGBA32* scanline(int y);

    static void initialize();

private:
    RGBA32* m_data { nullptr };
};

