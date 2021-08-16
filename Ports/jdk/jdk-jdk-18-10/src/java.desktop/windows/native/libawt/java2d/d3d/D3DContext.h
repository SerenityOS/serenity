/*
 * Copyright (c) 2007, 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

#ifndef D3DCONTEXT_H
#define D3DCONTEXT_H

#include "java_awt_Transparency.h"
#include "sun_java2d_pipe_BufferedContext.h"
#include "sun_java2d_d3d_D3DContext_D3DContextCaps.h"
#include "sun_java2d_d3d_D3DSurfaceData.h"

#include "ShaderList.h"
#include "D3DPipeline.h"
#include "D3DMaskCache.h"
#include "D3DVertexCacher.h"
#include "D3DResourceManager.h"

#include "j2d_md.h"

typedef enum {
    TILEFMT_UNKNOWN,
    TILEFMT_1BYTE_ALPHA,
    TILEFMT_3BYTE_RGB,
    TILEFMT_3BYTE_BGR,
    TILEFMT_4BYTE_ARGB_PRE,
} TileFormat;

typedef enum {
    CLIP_NONE,
    CLIP_RECT,
    CLIP_SHAPE,
} ClipType;

// - State switching optimizations -----------------------------------

/**
 * The goal is to reduce device state switching as much as possible.
 * This means: don't reset the texture if not needed, don't change
 * the texture stage states unless necessary.
 * For this we need to track the current device state. So each operation
 * supplies its own operation type to BeginScene, which updates the state
 * as necessary.
 *
 * Another optimization is to use a single vertex format for
 * all primitives.
 *
 * See D3DContext::UpdateState() and D3DContext::BeginScene() for
 * more information.
 */
#define STATE_CHANGE    (0 << 0)
#define STATE_RENDEROP  (1 << 0)
#define STATE_MASKOP    (1 << 1)
#define STATE_GLYPHOP   (1 << 2)
#define STATE_TEXTUREOP (1 << 3)
#define STATE_AAPGRAMOP (1 << 4)
#define STATE_OTHEROP   (1 << 5)

// The max. stage number we currently use (could not be
// larger than 7)
#define MAX_USED_TEXTURE_SAMPLER 1

// - Texture pixel format table  -------------------------------------
#define TR_OPAQUE      java_awt_Transparency_OPAQUE
#define TR_BITMASK     java_awt_Transparency_BITMASK
#define TR_TRANSLUCENT java_awt_Transparency_TRANSLUCENT

class D3DResource;
class D3DResourceManager;
class D3DMaskCache;
class D3DVertexCacher;
class D3DGlyphCache;

// - D3DContext class  -----------------------------------------------

/**
 * This class provides the following functionality:
 *  - holds the state of D3DContext java class (current pixel color,
 *    alpha compositing mode, extra alpha)
 *  - provides access to IDirect3DDevice9 interface (creation,
 *    disposal, exclusive access)
 *  - handles state changes of the direct3d device (transform,
 *    compositing mode, current texture)
 *  - provides means of creating textures, plain surfaces
 *  - holds a glyph cache texture for the associated device
 *  - implements primitives batching mechanism
 */
class D3DPIPELINE_API D3DContext {
public:
    /**
     * Releases the old device (if there was one) and all associated
     * resources, re-creates, initializes and tests the new device.
     *
     * If the device doesn't pass the test, it's released.
     *
     * Used when the context is first created, and then after a
     * display change event.
     *
     * Note that this method also does the necessary registry checks,
     * and if the registry shows that we've crashed when attempting
     * to initialize and test the device last time, it doesn't attempt
     * to create/init/test the device.
     */
    static
    HRESULT CreateInstance(IDirect3D9 *pd3d9, UINT adapter, D3DContext **ppCtx);
    // creates a new D3D windowed device with swap copy effect and default
    // present interval
    HRESULT InitContext();
    // creates or resets a D3D device given the parameters
    HRESULT ConfigureContext(D3DPRESENT_PARAMETERS *pNewParams);
    // resets existing D3D device with the current presentation parameters
    HRESULT ResetContext();
    HRESULT CheckAndResetDevice();

    void    ReleaseContextResources();
    void    ReleaseDefPoolResources();
    virtual ~D3DContext();

    // methods replicating java-level D3DContext objext
    HRESULT SetAlphaComposite(jint rule, jfloat extraAlpha, jint flags);
    HRESULT ResetComposite();

    /**
     * Glyph cache-related methods
     */
    HRESULT InitGrayscaleGlyphCache();
    HRESULT InitLCDGlyphCache();
    D3DGlyphCache* GetGrayscaleGlyphCache() { return pGrayscaleGlyphCache; }
    D3DGlyphCache* GetLCDGlyphCache() { return pLCDGlyphCache; }

    D3DResourceManager *GetResourceManager() { return pResourceMgr; }
    D3DMaskCache       *GetMaskCache() { return pMaskCache; }

    HRESULT UploadTileToTexture(D3DResource *pTextureRes, void *pixels,
                                jint dstx, jint dsty,
                                jint srcx, jint srcy,
                                jint srcWidth, jint srcHeight,
                                jint srcStride,
                                TileFormat srcFormat,
                                // out: num of pixels in first and last
                                // columns, only counted for LCD glyph uploads
                                jint *pPixelsTouchedL = NULL,
                                jint *pPixelsTouchedR = NULL);

    // returns capabilities of the Direct3D device
    D3DCAPS9 *GetDeviceCaps() { return &devCaps; }
    // returns caps in terms of the D3DContext
    int GetContextCaps() { return contextCaps; }
    D3DPRESENT_PARAMETERS *GetPresentationParams() { return &curParams; }

    IDirect3DDevice9 *Get3DDevice() { return pd3dDevice; }
    IDirect3D9 *Get3DObject() { return pd3dObject; }

    /**
     * This method only sets the texture if it's not already set.
     */
    HRESULT SetTexture(IDirect3DTexture9 *pTexture, DWORD dwSampler = 0);

    /**
     * This method only updates the texture color state if it hasn't changed.
     */
    HRESULT UpdateTextureColorState(DWORD dwState, DWORD dwSampler = 0);

    HRESULT SetRenderTarget(IDirect3DSurface9 *pSurface);
    HRESULT SetTransform(jdouble m00, jdouble m10,
                         jdouble m01, jdouble m11,
                         jdouble m02, jdouble m12);
    HRESULT ResetTransform();

    // clipping-related methods
    HRESULT SetRectClip(int x1, int y1, int x2, int y2);
    HRESULT BeginShapeClip();
    HRESULT EndShapeClip();
    HRESULT ResetClip();
    ClipType GetClipType();

    /**
     * Shader-related methods
     */
    HRESULT EnableBasicGradientProgram(jint flags);
    HRESULT EnableLinearGradientProgram(jint flags);
    HRESULT EnableRadialGradientProgram(jint flags);
    HRESULT EnableConvolveProgram(jint flags);
    HRESULT EnableRescaleProgram(jint flags);
    HRESULT EnableLookupProgram(jint flags);
    HRESULT EnableLCDTextProgram();
    HRESULT EnableAAParallelogramProgram();
    HRESULT DisableAAParallelogramProgram();

    BOOL IsTextureFilteringSupported(D3DTEXTUREFILTERTYPE fType);
    BOOL IsStretchRectFilteringSupported(D3DTEXTUREFILTERTYPE fType);
    BOOL IsPow2TexturesOnly()
        { return devCaps.TextureCaps & D3DPTEXTURECAPS_POW2; };
    BOOL IsSquareTexturesOnly()
        { return devCaps.TextureCaps & D3DPTEXTURECAPS_SQUAREONLY; }
    BOOL IsHWRasterizer() { return bIsHWRasterizer; }
    BOOL IsTextureFormatSupported(D3DFORMAT format, DWORD usage = 0);
    BOOL IsDynamicTextureSupported()
        { return devCaps.Caps2 & D3DCAPS2_DYNAMICTEXTURES; }
// REMIND: for now for performance testing
//        { return (getenv("J2D_D3D_USE_DYNAMIC_TEX") != NULL); }
    BOOL IsImmediateIntervalSupported()
        { return devCaps.PresentationIntervals & D3DPRESENT_INTERVAL_IMMEDIATE;}
    BOOL IsPixelShader20Supported()
        { return (devCaps.PixelShaderVersion >= D3DPS_VERSION(2,0)); }
    BOOL IsGradientInstructionExtensionSupported()
        { return devCaps.PS20Caps.Caps & D3DPS20CAPS_GRADIENTINSTRUCTIONS; }
    BOOL IsPixelShader30Supported()
        { return (devCaps.PixelShaderVersion >= D3DPS_VERSION(3,0)); }
    BOOL IsMultiTexturingSupported()
        { return (devCaps.MaxSimultaneousTextures > 1); }
    BOOL IsAlphaRTSurfaceSupported();
    BOOL IsAlphaRTTSupported();
    BOOL IsOpaqueRTTSupported();

    jint GetPaintState() { return paintState; }
    void SetPaintState(jint state) { this->paintState = state; }
    BOOL IsIdentityTx() { return bIsIdentityTx; }

    HRESULT FlushVertexQueue();
    D3DVertexCacher *pVCacher;
    HRESULT UpdateState(jbyte newState);

    HRESULT Sync();

    // primitives batching-related methods
    /**
     * Calls devices's BeginScene if there weren't one already pending,
     * sets the pending flag.
     */
    HRESULT BeginScene(jbyte newState);
    /**
     * Flushes the vertex queue and does end scene if
     * a BeginScene is pending
     */
    HRESULT EndScene();

    /**
     * Fields that track native-specific state.
     */
    jint       paintState;
    jboolean   useMask;
    jfloat     extraAlpha;

    /**
     * Current operation state.
     * See STATE_* macros above.
     */
    jbyte      opState;

private:

    /**
     * Glyph cache-related methods/fields...
     */
    D3DGlyphCache *pGrayscaleGlyphCache;
    D3DGlyphCache *pLCDGlyphCache;

    /**
     * The handle to the LCD text pixel shader program.
     */
    IDirect3DPixelShader9 *lcdTextProgram;

    /**
     * The handle to the AA pixel and vertex shader programs.
     */
    IDirect3DPixelShader9 *aaPgramProgram;

    IDirect3DPixelShader9 *CreateFragmentProgram(DWORD **shaders,
                                                 ShaderList *programs,
                                                 jint flags);
    HRESULT EnableFragmentProgram(DWORD **shaders,
                                  ShaderList *programList,
                                  jint flags);

    // finds appropriate to the target surface depth format,
    // creates the depth buffer and installs it onto the device
    HRESULT InitDepthStencilBuffer(D3DSURFACE_DESC *pTargetDesc);
    // returns true if the current depth buffer is compatible
    // with the new target, and the dimensions fit, false otherwise
    BOOL IsDepthStencilBufferOk(D3DSURFACE_DESC *pTargetDesc);

    D3DContext(IDirect3D9 *pd3dObject, UINT adapter);
    HRESULT InitDevice(IDirect3DDevice9 *d3dDevice);
    HRESULT InitContextCaps();
    // updates the texture transform(s) used for better texel to pixel mapping
    // for the passed in sampler;
    // if -1 is passed as the sampler, texture transforms for
    // samplers [0..MAX_USED_TEXTURE_SAMPLER] are updated
    // REMIND: see the comment in the method implementation before enabling.
#undef UPDATE_TX
#ifdef UPDATE_TX
    HRESULT UpdateTextureTransforms(DWORD dwSamplerToUpdate);
#endif // UPDATE_TX
    IDirect3DDevice9        *pd3dDevice;
    IDirect3D9              *pd3dObject;

    D3DResourceManager      *pResourceMgr;
    D3DMaskCache            *pMaskCache;

    ShaderList convolvePrograms;
    ShaderList rescalePrograms;
    ShaderList lookupPrograms;
    ShaderList basicGradPrograms;
    ShaderList linearGradPrograms;
    ShaderList radialGradPrograms;

    // array of the textures currently set to the device
    IDirect3DTexture9     *lastTexture[MAX_USED_TEXTURE_SAMPLER+1];

    DWORD lastTextureColorState[MAX_USED_TEXTURE_SAMPLER+1];

    UINT adapterOrdinal;
    D3DPRESENT_PARAMETERS   curParams;
    D3DCAPS9 devCaps;
    int contextCaps;
    BOOL bIsHWRasterizer;

    BOOL bIsIdentityTx;

    IDirect3DQuery9* pSyncQuery;
    D3DResource* pSyncRTRes;

    IDirect3DStateBlock9* pStateBlock;

    /**
     * Used to implement simple primitive batching.
     * See BeginScene/EndScene/ForceEndScene.
     */
    BOOL    bBeginScenePending;
};

// - Helper Macros ---------------------------------------------------

#define D3DC_INIT_SHADER_LIST(list, max) \
    do { \
        (list).head     = NULL; \
        (list).maxItems = (max); \
        (list).dispose  = D3DContext_DisposeShader; \
    } while (0)

/**
 * This constant determines the size of the shared tile texture used
 * by a number of image rendering methods.  For example, the blit tile texture
 * will have dimensions with width D3DC_BLIT_TILE_SIZE and height
 * D3DC_BLIT_TILE_SIZE (the tile will always be square).
 */
#define D3DC_BLIT_TILE_SIZE 256

/**
 * See BufferedContext.java for more on these flags...
 */
#define D3DC_NO_CONTEXT_FLAGS \
    sun_java2d_pipe_BufferedContext_NO_CONTEXT_FLAGS
#define D3DC_SRC_IS_OPAQUE    \
    sun_java2d_pipe_BufferedContext_SRC_IS_OPAQUE
#define D3DC_USE_MASK         \
    sun_java2d_pipe_BufferedContext_USE_MASK

#define CAPS_EMPTY          \
    sun_java2d_d3d_D3DContext_D3DContextCaps_CAPS_EMPTY
#define CAPS_RT_PLAIN_ALPHA \
    sun_java2d_d3d_D3DContext_D3DContextCaps_CAPS_RT_PLAIN_ALPHA
#define CAPS_RT_TEXTURE_ALPHA      \
    sun_java2d_d3d_D3DContext_D3DContextCaps_CAPS_RT_TEXTURE_ALPHA
#define CAPS_RT_TEXTURE_OPAQUE     \
    sun_java2d_d3d_D3DContext_D3DContextCaps_CAPS_RT_TEXTURE_OPAQUE
#define CAPS_MULTITEXTURE   \
    sun_java2d_d3d_D3DContext_D3DContextCaps_CAPS_MULTITEXTURE
#define CAPS_TEXNONPOW2     \
    sun_java2d_d3d_D3DContext_D3DContextCaps_CAPS_TEXNONPOW2
#define CAPS_TEXNONSQUARE   \
    sun_java2d_d3d_D3DContext_D3DContextCaps_CAPS_TEXNONSQUARE
#define CAPS_LCD_SHADER     \
    sun_java2d_d3d_D3DContext_D3DContextCaps_CAPS_LCD_SHADER
#define CAPS_BIOP_SHADER    \
    sun_java2d_d3d_D3DContext_D3DContextCaps_CAPS_BIOP_SHADER
#define CAPS_AA_SHADER    \
    sun_java2d_d3d_D3DContext_D3DContextCaps_CAPS_AA_SHADER
#define CAPS_DEVICE_OK      \
    sun_java2d_d3d_D3DContext_D3DContextCaps_CAPS_DEVICE_OK
#define CAPS_PS20           \
    sun_java2d_d3d_D3DContext_D3DContextCaps_CAPS_PS20
#define CAPS_PS30           \
    sun_java2d_d3d_D3DContext_D3DContextCaps_CAPS_PS30

#endif // D3DCONTEXT_H
