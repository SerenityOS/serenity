/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 * Copyright (c) 2021, Stephan Unverwerth <s.unverwerth@serenityos.org>
 * Copyright (c) 2021, Jelle Raaijmakers <jelle@gmta.nl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGL/GL/glplatform.h>

#ifdef __cplusplus
extern "C" {
#endif

#define GL_VERSION_1_0 1
#define GL_VERSION_1_1 1
#define GL_VERSION_1_2 1
#define GL_VERSION_1_3 1
#define GL_VERSION_1_4 1
#define GL_VERSION_1_5 1
#define GL_ES_VERSION_2_0 1

// OpenGL related `defines`
#define GL_TRUE 1
#define GL_FALSE 0
#define GL_NONE 0

// Matrix Modes
#define GL_MODELVIEW 0x1700
#define GL_PROJECTION 0x1701
#define GL_TEXTURE 0x1702

// glBegin/glEnd primitive types
#define GL_POINTS 0x0000
#define GL_LINES 0x0001
#define GL_LINE_LOOP 0x0002
#define GL_LINE_STRIP 0x0003
#define GL_TRIANGLES 0x0004
#define GL_TRIANGLE_STRIP 0x0005
#define GL_TRIANGLE_FAN 0x0006
#define GL_QUADS 0x0007
#define GL_QUAD_STRIP 0x0008
#define GL_POLYGON 0x0009

// Depth buffer and alpha test compare functions
#define GL_NEVER 0x0200
#define GL_LESS 0x0201
#define GL_EQUAL 0x0202
#define GL_LEQUAL 0x0203
#define GL_GREATER 0x0204
#define GL_NOTEQUAL 0x0205
#define GL_GEQUAL 0x0206
#define GL_ALWAYS 0x0207

// Buffer bits
#define GL_DEPTH_BUFFER_BIT 0x00100
#define GL_STENCIL_BUFFER_BIT 0x00400
#define GL_COLOR_BUFFER_BIT 0x04000

// Enable capabilities
#define GL_POLYGON_MODE 0x0B40
#define GL_POLYGON_SMOOTH 0x0B41
#define GL_POLYGON_STIPPLE 0x0B42
#define GL_EDGE_FLAG 0x0B43
#define GL_CULL_FACE 0x0B44
#define GL_FOG 0x0B60
#define GL_DEPTH_TEST 0x0B71
#define GL_STENCIL_TEST 0x0B90
#define GL_DITHER 0x0BD0
#define GL_POLYGON_OFFSET_FILL 0x8037

// Alpha testing
#define GL_ALPHA_TEST 0x0BC0
#define GL_ALPHA_TEST_REF 0x0BC2
#define GL_ALPHA_TEST_FUNC 0x0BC1

// Alpha blending
#define GL_BLEND 0x0BE2
#define GL_BLEND_SRC_ALPHA 0x0302
#define GL_BLEND_DST_ALPHA 0x0304

// Attribute enum
#define GL_EVAL_BIT 0x00010000
#define GL_ALL_ATTRIB_BITS 0xFFFFFFFF

// Utility
#define GL_VENDOR 0x1F00
#define GL_RENDERER 0x1F01
#define GL_VERSION 0x1F02
#define GL_EXTENSIONS 0x1F03
#define GL_SHADING_LANGUAGE_VERSION 0x8B8C

// Get parameters
#define GL_COLOR_MATERIAL_FACE 0x0B55
#define GL_COLOR_MATERIAL_MODE 0x0B56
#define GL_COLOR_MATERIAL 0x0B57
#define GL_FOG_START 0x0B63
#define GL_FOG_END 0x0B64
#define GL_STENCIL_CLEAR_VALUE 0x0B91
#define GL_MATRIX_MODE 0x0BA0
#define GL_NORMALIZE 0x0BA1
#define GL_VIEWPORT 0x0BA2
#define GL_DOUBLEBUFFER 0x0C32
#define GL_TEXTURE_GEN_S 0x0C60
#define GL_TEXTURE_GEN_T 0x0C61
#define GL_TEXTURE_GEN_R 0x0C62
#define GL_TEXTURE_GEN_Q 0x0C63
#define GL_MAX_TEXTURE_SIZE 0x0D33
#define GL_MAX_MODELVIEW_STACK_DEPTH 0x0D36
#define GL_MAX_PROJECTION_STACK_DEPTH 0x0D38
#define GL_MAX_TEXTURE_STACK_DEPTH 0x0D39
#define GL_RED_BITS 0x0D52
#define GL_GREEN_BITS 0x0D53
#define GL_BLUE_BITS 0x0D54
#define GL_ALPHA_BITS 0x0D55
#define GL_DEPTH_BITS 0x0D56
#define GL_STENCIL_BITS 0x0D57
#define GL_MAX_TEXTURE_UNITS 0x84E2
#define GL_MAX_LIGHTS 0x0D31
#define GL_AUTO_NORMAL 0x0D80
#define GL_MAP1_COLOR_4 0x0D90
#define GL_MAP1_INDEX 0x0D91
#define GL_MAP1_NORMAL 0x0D92
#define GL_MAP1_TEXTURE_COORD_1 0x0D93
#define GL_MAP1_TEXTURE_COORD_2 0x0D94
#define GL_MAP1_TEXTURE_COORD_3 0x0D95
#define GL_MAP1_TEXTURE_COORD_4 0x0D96
#define GL_MAP1_VERTEX_3 0x0D97
#define GL_MAP1_VERTEX_4 0x0D98
#define GL_MAP2_COLOR_4 0x0DB0
#define GL_MAP2_INDEX 0x0DB1
#define GL_MAP2_NORMAL 0x0DB2
#define GL_MAP2_TEXTURE_COORD_1 0x0DB3
#define GL_MAP2_TEXTURE_COORD_2 0x0DB4
#define GL_MAP2_TEXTURE_COORD_3 0x0DB5
#define GL_MAP2_TEXTURE_COORD_4 0x0DB6
#define GL_MAP2_VERTEX_3 0x0DB7
#define GL_MAP2_VERTEX_4 0x0DB8
#define GL_NORMAL_ARRAY 0x8075

// Blend factors
#define GL_ZERO 0
#define GL_ONE 1
#define GL_SRC_COLOR 0x0300
#define GL_ONE_MINUS_SRC_COLOR 0x0301
#define GL_SRC_ALPHA 0x0302
#define GL_ONE_MINUS_SRC_ALPHA 0x0303
#define GL_DST_ALPHA 0x0304
#define GL_ONE_MINUS_DST_ALPHA 0x0305
#define GL_DST_COLOR 0x0306
#define GL_ONE_MINUS_DST_COLOR 0x0307
#define GL_SRC_ALPHA_SATURATE 0x0308

// Sides
#define GL_FRONT_LEFT 0x0400
#define GL_FRONT_RIGHT 0x0401
#define GL_BACK_LEFT 0x0402
#define GL_BACK_RIGHT 0x0403
#define GL_FRONT 0x0404
#define GL_BACK 0x0405
#define GL_LEFT 0x0406
#define GL_RIGHT 0x0407
#define GL_FRONT_AND_BACK 0x0408

// Error codes
#define GL_NO_ERROR 0x0000
#define GL_INVALID_ENUM 0x0500
#define GL_INVALID_VALUE 0x0501
#define GL_INVALID_OPERATION 0x0502
#define GL_STACK_OVERFLOW 0x0503
#define GL_STACK_UNDERFLOW 0x0504
#define GL_OUT_OF_MEMORY 0x0505
#define GL_INVALID_FRAMEBUFFER_OPERATION 0x0506

// Triangle winding order
#define GL_CW 0x0900
#define GL_CCW 0x0901

// Hint enums
#define GL_DONT_CARE 0x1100
#define GL_FASTEST 0x1101
#define GL_NICEST 0x1102

#define GL_PERSPECTIVE_CORRECTION_HINT 0x0C50
#define GL_POINT_SMOOTH_HINT 0x0C51
#define GL_LINE_SMOOTH_HINT 0x0C52
#define GL_POLYGON_SMOOTH_HINT 0x0C53
#define GL_FOG_HINT 0x0C54
#define GL_GENERATE_MIPMAP_HINT 0x8192
#define GL_TEXTURE_COMPRESSION_HINT 0x84EF

// Reading pixels & unpacking texture patterns
#define GL_UNPACK_SWAP_BYTES 0x0CF0
#define GL_UNPACK_LSB_FIRST 0x0CF1
#define GL_UNPACK_ROW_LENGTH 0x0CF2
#define GL_UNPACK_SKIP_ROWS 0x0CF3
#define GL_UNPACK_SKIP_PIXELS 0x0CF4
#define GL_UNPACK_ALIGNMENT 0x0CF5
#define GL_UNPACK_SKIP_IMAGES 0x806D
#define GL_UNPACK_IMAGE_HEIGHT 0x806E

#define GL_PACK_SWAP_BYTES 0x0D00
#define GL_PACK_LSB_FIRST 0x0D01
#define GL_PACK_ROW_LENGTH 0x0D02
#define GL_PACK_SKIP_ROWS 0x0D03
#define GL_PACK_SKIP_PIXELS 0x0D04
#define GL_PACK_ALIGNMENT 0x0D05
#define GL_PACK_SKIP_IMAGES 0x806B
#define GL_PACK_IMAGE_HEIGHT 0x806C

// Listing enums
#define GL_COMPILE 0x1300
#define GL_COMPILE_AND_EXECUTE 0x1301

// Type enums
#define GL_BYTE 0x1400
#define GL_UNSIGNED_BYTE 0x1401
#define GL_SHORT 0x1402
#define GL_UNSIGNED_SHORT 0x1403
#define GL_INT 0x1404
#define GL_UNSIGNED_INT 0x1405
#define GL_FLOAT 0x1406
#define GL_2_BYTES 0x1407
#define GL_3_BYTES 0x1408
#define GL_4_BYTES 0x1409
#define GL_DOUBLE 0x140A
#define GL_BOOL 0x8B56

// Format enums
#define GL_COLOR_INDEX 0x1900
#define GL_COLOR_INDEX8_EXT 0x80E5
#define GL_STENCIL_INDEX 0x1901
#define GL_DEPTH_COMPONENT 0x1902
#define GL_RED 0x1903
#define GL_GREEN 0x1904
#define GL_BLUE 0x1905
#define GL_ALPHA 0x1906
#define GL_RGB 0x1907
#define GL_RGBA 0x1908
#define GL_LUMINANCE 0x1909
#define GL_LUMINANCE8 0x8040
#define GL_LUMINANCE_ALPHA 0x190A
#define GL_LUMINANCE8_ALPHA8 0x8045
#define GL_BGR 0x80E0
#define GL_BGRA 0x80E1
#define GL_BITMAP 0x1A00

#define GL_LUMINANCE8 0x8040
#define GL_INTENSITY8 0x804B
#define GL_R3_G3_B2 0x2A10
#define GL_RGB4 0x804F
#define GL_RGB5 0x8050
#define GL_RGB8 0x8051
#define GL_RGBA2 0x8055
#define GL_RGBA4 0x8056
#define GL_RGB5_A1 0x8057
#define GL_RGBA8 0x8058

// Lighting related defines
#define GL_LIGHTING 0x0B50
#define GL_LIGHT_MODEL_LOCAL_VIEWER 0x0B51
#define GL_LIGHT_MODEL_TWO_SIDE 0x0B52
#define GL_LIGHT_MODEL_AMBIENT 0x0B53

#define GL_FLAT 0x1D00
#define GL_SMOOTH 0x1D01
#define GL_AMBIENT 0x1200
#define GL_DIFFUSE 0x1201
#define GL_SPECULAR 0x1202
#define GL_POSITION 0x1203
#define GL_SPOT_DIRECTION 0x1204
#define GL_SPOT_EXPONENT 0x1205
#define GL_SPOT_CUTOFF 0x1206
#define GL_CONSTANT_ATTENUATION 0x1207
#define GL_LINEAR_ATTENUATION 0x1208
#define GL_QUADRATIC_ATTENUATION 0x1209
#define GL_EMISSION 0x1600
#define GL_SHININESS 0x1601
#define GL_AMBIENT_AND_DIFFUSE 0x1602
#define GL_COLOR_INDEXES 0x1603

#define GL_LIGHT0 0x4000
#define GL_LIGHT1 0x4001
#define GL_LIGHT2 0x4002
#define GL_LIGHT3 0x4003
#define GL_LIGHT4 0x4004
#define GL_LIGHT5 0x4005
#define GL_LIGHT6 0x4006
#define GL_LIGHT7 0x4007

// More blend factors
#define GL_CONSTANT_COLOR 0x8001
#define GL_ONE_MINUS_CONSTANT_COLOR 0x8002
#define GL_CONSTANT_ALPHA 0x8003
#define GL_ONE_MINUS_CONSTANT_ALPHA 0x8004

// Points
#define GL_POINT_SMOOTH 0x0B10
#define GL_POINT_SIZE_MIN_EXT 0x8126
#define GL_POINT_SIZE_MAX_EXT 0x8127
#define GL_DISTANCE_ATTENUATION_EXT 0x8129

// Polygon modes
#define GL_POINT 0x1B00
#define GL_LINE 0x1B01
#define GL_FILL 0x1B02

// Source pixel data format
#define GL_UNSIGNED_BYTE 0x1401
#define GL_UNSIGNED_BYTE_3_3_2 0x8032
#define GL_UNSIGNED_SHORT_4_4_4_4 0x8033
#define GL_UNSIGNED_SHORT_5_5_5_1 0x8034
#define GL_UNSIGNED_INT_8_8_8_8 0x8035
#define GL_UNSIGNED_INT_10_10_10_2 0x8036
#define GL_UNSIGNED_BYTE_2_3_3_REV 0x8362
#define GL_UNSIGNED_SHORT_5_6_5 0x8363
#define GL_UNSIGNED_SHORT_5_6_5_REV 0x8364
#define GL_UNSIGNED_SHORT_4_4_4_4_REV 0x8365
#define GL_UNSIGNED_SHORT_1_5_5_5_REV 0x8366
#define GL_UNSIGNED_INT_8_8_8_8_REV 0x8367
#define GL_UNSIGNED_INT_2_10_10_10_REV 0x8368

// Stencil buffer operations
#define GL_KEEP 0x1E00
#define GL_REPLACE 0x1E01
#define GL_INCR 0x1E02
#define GL_INCR_WRAP 0x8507
#define GL_DECR 0x1E03
#define GL_DECR_WRAP 0x8508
#define GL_INVERT 0x150A

// Texture targets
#define GL_TEXTURE_1D 0x0DE0
#define GL_TEXTURE_2D 0x0DE1
#define GL_PROXY_TEXTURE_1D 0x8063
#define GL_PROXY_TEXTURE_2D 0x8064
#define GL_TEXTURE_3D 0x806F
#define GL_PROXY_TEXTURE_3D 0x8070
#define GL_TEXTURE_CUBE_MAP 0x8513

// Texture parameters
#define GL_TEXTURE_WIDTH 0x1000
#define GL_TEXTURE_HEIGHT 0x1001

// Texture Unit indices
#define GL_TEXTURE0 0x84C0
#define GL_TEXTURE1 0x84C1
#define GL_TEXTURE2 0x84C2
#define GL_TEXTURE3 0x84C3
#define GL_TEXTURE4 0x84C4
#define GL_TEXTURE5 0x84C5
#define GL_TEXTURE6 0x84C6
#define GL_TEXTURE7 0x84C7
#define GL_TEXTURE8 0x84C8
#define GL_TEXTURE9 0x84C9
#define GL_TEXTURE10 0x84CA
#define GL_TEXTURE11 0x84CB
#define GL_TEXTURE12 0x84CC
#define GL_TEXTURE13 0x84CD
#define GL_TEXTURE14 0x84CE
#define GL_TEXTURE15 0x84CF
#define GL_TEXTURE16 0x84D0
#define GL_TEXTURE17 0x84D1
#define GL_TEXTURE18 0x84D2
#define GL_TEXTURE19 0x84D3
#define GL_TEXTURE20 0x84D4
#define GL_TEXTURE21 0x84D5
#define GL_TEXTURE22 0x84D6
#define GL_TEXTURE23 0x84D7
#define GL_TEXTURE24 0x84D8
#define GL_TEXTURE25 0x84D9
#define GL_TEXTURE26 0x84DA
#define GL_TEXTURE27 0x84DB
#define GL_TEXTURE28 0x84DC
#define GL_TEXTURE29 0x84DD
#define GL_TEXTURE30 0x84DE
#define GL_TEXTURE31 0x84DF

#define GL_TEXTURE0_ARB GL_TEXTURE0
#define GL_TEXTURE1_ARB GL_TEXTURE1
#define GL_TEXTURE2_ARB GL_TEXTURE2
#define GL_TEXTURE3_ARB GL_TEXTURE3
#define GL_TEXTURE4_ARB GL_TEXTURE4
#define GL_TEXTURE5_ARB GL_TEXTURE5
#define GL_TEXTURE6_ARB GL_TEXTURE6
#define GL_TEXTURE7_ARB GL_TEXTURE7
#define GL_TEXTURE8_ARB GL_TEXTURE8
#define GL_TEXTURE9_ARB GL_TEXTURE9
#define GL_TEXTURE10_ARB GL_TEXTURE10
#define GL_TEXTURE11_ARB GL_TEXTURE11
#define GL_TEXTURE12_ARB GL_TEXTURE12
#define GL_TEXTURE13_ARB GL_TEXTURE13
#define GL_TEXTURE14_ARB GL_TEXTURE14
#define GL_TEXTURE15_ARB GL_TEXTURE15
#define GL_TEXTURE16_ARB GL_TEXTURE16
#define GL_TEXTURE17_ARB GL_TEXTURE17
#define GL_TEXTURE18_ARB GL_TEXTURE18
#define GL_TEXTURE19_ARB GL_TEXTURE19
#define GL_TEXTURE20_ARB GL_TEXTURE20
#define GL_TEXTURE21_ARB GL_TEXTURE21
#define GL_TEXTURE22_ARB GL_TEXTURE22
#define GL_TEXTURE23_ARB GL_TEXTURE23
#define GL_TEXTURE24_ARB GL_TEXTURE24
#define GL_TEXTURE25_ARB GL_TEXTURE25
#define GL_TEXTURE26_ARB GL_TEXTURE26
#define GL_TEXTURE27_ARB GL_TEXTURE27
#define GL_TEXTURE28_ARB GL_TEXTURE28
#define GL_TEXTURE29_ARB GL_TEXTURE29
#define GL_TEXTURE30_ARB GL_TEXTURE30
#define GL_TEXTURE31_ARB GL_TEXTURE31

// Texture coord names
#define GL_S 0x2000
#define GL_T 0x2001
#define GL_R 0x2002
#define GL_Q 0x2003

// Texture Environment and Parameters
#define GL_MODULATE 0x2100
#define GL_TEXTURE_ENV_MODE 0x2200
#define GL_DECAL 0x2101
#define GL_TEXTURE_ENV 0x2300
#define GL_NEAREST 0x2600
#define GL_LINEAR 0x2601
#define GL_NEAREST_MIPMAP_NEAREST 0x2700
#define GL_LINEAR_MIPMAP_NEAREST 0x2701
#define GL_NEAREST_MIPMAP_LINEAR 0x2702
#define GL_LINEAR_MIPMAP_LINEAR 0x2703
#define GL_TEXTURE_MAG_FILTER 0x2800
#define GL_TEXTURE_MIN_FILTER 0x2801
#define GL_TEXTURE_WRAP_S 0x2802
#define GL_TEXTURE_WRAP_T 0x2803
#define GL_CLAMP 0x2900
#define GL_REPEAT 0x2901
#define GL_MIRRORED_REPEAT 0x8370
#define GL_CLAMP_TO_BORDER 0x812D
#define GL_CLAMP_TO_EDGE 0x812F

// Texture gen modes
#define GL_EYE_LINEAR 0x2400
#define GL_OBJECT_LINEAR 0x2401
#define GL_SPHERE_MAP 0x2402
#define GL_NORMAL_MAP 0x8511
#define GL_REFLECTION_MAP 0x8512

// Texture gen parameters
#define GL_TEXTURE_GEN_MODE 0x2500
#define GL_OBJECT_PLANE 0x2501
#define GL_EYE_PLANE 0x2502

// Client state capabilities
#define GL_VERTEX_ARRAY 0x8074
#define GL_COLOR_ARRAY 0x8076
#define GL_TEXTURE_COORD_ARRAY 0x8078

// Fog parameters
#define GL_EXP 0x0800
#define GL_EXP2 0x0801
#define GL_FOG_MODE 0x0B65
#define GL_FOG_COLOR 0x0B66
#define GL_FOG_DENSITY 0x0B62

// Scissor enums
#define GL_SCISSOR_BOX 0x0C10
#define GL_SCISSOR_TEST 0x0C11

// OpenGL State & GLGet
#define GL_MODELVIEW_MATRIX 0x0BA6
#define GL_PROJECTION_MATRIX 0x0BA7

GLAPI void glBegin(GLenum mode);
GLAPI void glClear(GLbitfield mask);
GLAPI void glClearColor(GLclampf red, GLclampf green, GLclampf blue, GLclampf alpha);
GLAPI void glClearDepth(GLdouble depth);
GLAPI void glClearStencil(GLint s);
GLAPI void glColor3d(GLdouble r, GLdouble g, GLdouble b);
GLAPI void glColor3dv(GLdouble const* v);
GLAPI void glColor3f(GLfloat r, GLfloat g, GLfloat b);
GLAPI void glColor3fv(const GLfloat* v);
GLAPI void glColor3ub(GLubyte r, GLubyte g, GLubyte b);
GLAPI void glColor3ubv(GLubyte const* v);
GLAPI void glColor4b(GLbyte r, GLbyte g, GLbyte b, GLbyte a);
GLAPI void glColor4dv(GLdouble const* v);
GLAPI void glColor4f(GLfloat r, GLfloat g, GLfloat b, GLfloat a);
GLAPI void glColor4fv(const GLfloat* v);
GLAPI void glColor4ub(GLubyte r, GLubyte g, GLubyte b, GLubyte a);
GLAPI void glColor4ubv(const GLubyte* v);
GLAPI void glColorMask(GLboolean red, GLboolean green, GLboolean blue, GLboolean alpha);
GLAPI void glColorMaterial(GLenum face, GLenum mode);
GLAPI void glDeleteTextures(GLsizei n, const GLuint* textures);
GLAPI void glEnd();
GLAPI void glFrustum(GLdouble left, GLdouble right, GLdouble bottom, GLdouble top, GLdouble nearVal, GLdouble farVal);
GLAPI void glGenTextures(GLsizei n, GLuint* textures);
GLAPI GLenum glGetError();
GLAPI GLubyte* glGetString(GLenum name);
GLAPI void glLoadIdentity();
GLAPI void glLoadMatrixd(const GLdouble* matrix);
GLAPI void glLoadMatrixf(const GLfloat* matrix);
GLAPI void glMatrixMode(GLenum mode);
GLAPI void glOrtho(GLdouble left, GLdouble right, GLdouble bottom, GLdouble top, GLdouble nearVal, GLdouble farVal);
GLAPI void glPushMatrix();
GLAPI void glPopMatrix();
GLAPI void glMultMatrixd(GLdouble const* matrix);
GLAPI void glMultMatrixf(GLfloat const* matrix);
GLAPI void glRotated(GLdouble angle, GLdouble x, GLdouble y, GLdouble z);
GLAPI void glRotatef(GLfloat angle, GLfloat x, GLfloat y, GLfloat z);
GLAPI void glScaled(GLdouble x, GLdouble y, GLdouble z);
GLAPI void glScalef(GLfloat x, GLfloat y, GLfloat z);
GLAPI void glTranslated(GLdouble x, GLdouble y, GLdouble z);
GLAPI void glTranslatef(GLfloat x, GLfloat y, GLfloat z);
GLAPI void glVertex2d(GLdouble x, GLdouble y);
GLAPI void glVertex2dv(const GLdouble* v);
GLAPI void glVertex2f(GLfloat x, GLfloat y);
GLAPI void glVertex2fv(const GLfloat* v);
GLAPI void glVertex2i(GLint x, GLint y);
GLAPI void glVertex2iv(const GLint* v);
GLAPI void glVertex2s(GLshort x, GLshort y);
GLAPI void glVertex2sv(const GLshort* v);
GLAPI void glVertex3d(GLdouble x, GLdouble y, GLdouble z);
GLAPI void glVertex3dv(const GLdouble* v);
GLAPI void glVertex3f(GLfloat x, GLfloat y, GLfloat z);
GLAPI void glVertex3fv(const GLfloat* v);
GLAPI void glVertex3i(GLint x, GLint y, GLint z);
GLAPI void glVertex3iv(const GLint* v);
GLAPI void glVertex3s(GLshort x, GLshort y, GLshort z);
GLAPI void glVertex3sv(const GLshort* v);
GLAPI void glVertex4d(GLdouble x, GLdouble y, GLdouble z, GLdouble w);
GLAPI void glVertex4dv(const GLdouble* v);
GLAPI void glVertex4f(GLfloat x, GLfloat y, GLfloat z, GLfloat w);
GLAPI void glVertex4fv(const GLfloat* v);
GLAPI void glVertex4i(GLint x, GLint y, GLint z, GLint w);
GLAPI void glVertex4iv(const GLint* v);
GLAPI void glVertex4s(GLshort x, GLshort y, GLshort z, GLshort w);
GLAPI void glVertex4sv(const GLshort* v);
GLAPI void glViewport(GLint x, GLint y, GLsizei width, GLsizei height);
GLAPI void glEnable(GLenum cap);
GLAPI void glDisable(GLenum cap);
GLAPI GLboolean glIsEnabled(GLenum cap);
GLAPI void glCullFace(GLenum mode);
GLAPI void glFrontFace(GLenum mode);
GLAPI GLuint glGenLists(GLsizei range);
GLAPI void glCallList(GLuint list);
GLAPI void glCallLists(GLsizei n, GLenum type, void const* lists);
GLAPI void glDeleteLists(GLuint list, GLsizei range);
GLAPI void glListBase(GLuint base);
GLAPI void glEndList(void);
GLAPI void glNewList(GLuint list, GLenum mode);
GLAPI GLboolean glIsList(GLuint list);
GLAPI void glFlush();
GLAPI void glFinish();
GLAPI void glBlendFunc(GLenum sfactor, GLenum dfactor);
GLAPI void glShadeModel(GLenum mode);
GLAPI void glAlphaFunc(GLenum func, GLclampf ref);
GLAPI void glHint(GLenum target, GLenum mode);
GLAPI void glReadBuffer(GLenum mode);
GLAPI void glDrawBuffer(GLenum buffer);
GLAPI void glReadPixels(GLint x, GLint y, GLsizei width, GLsizei height, GLenum format, GLenum type, GLvoid* pixels);
GLAPI void glTexImage1D(GLenum target, GLint level, GLint internalFormat, GLsizei width, GLint border, GLenum format, GLenum type, GLvoid const* data);
GLAPI void glTexImage2D(GLenum target, GLint level, GLint internalFormat, GLsizei width, GLsizei height, GLint border, GLenum format, GLenum type, GLvoid const* data);
GLAPI void glTexImage3D(GLenum target, GLint level, GLint internalFormat, GLsizei width, GLsizei height, GLsizei depth, GLint border, GLenum format, GLenum type, GLvoid const* data);
GLAPI void glTexSubImage2D(GLenum target, GLint level, GLint xoffset, GLint yoffset, GLsizei width, GLsizei height, GLenum format, GLenum type, const GLvoid* data);
GLAPI void glTexCoord1f(GLfloat s);
GLAPI void glTexCoord1fv(GLfloat const* v);
GLAPI void glTexCoord2d(GLdouble s, GLdouble t);
GLAPI void glTexCoord2dv(GLdouble const* v);
GLAPI void glTexCoord2f(GLfloat s, GLfloat t);
GLAPI void glTexCoord2fv(GLfloat const* v);
GLAPI void glTexCoord2i(GLint s, GLint t);
GLAPI void glTexCoord3f(GLfloat s, GLfloat t, GLfloat r);
GLAPI void glTexCoord3fv(GLfloat const* v);
GLAPI void glTexCoord4f(GLfloat s, GLfloat t, GLfloat r, GLfloat q);
GLAPI void glTexCoord4fv(const GLfloat* v);
GLAPI void glMultiTexCoord2fARB(GLenum target, GLfloat s, GLfloat t);
GLAPI void glMultiTexCoord2f(GLenum target, GLfloat s, GLfloat t);
GLAPI void glTexParameteri(GLenum target, GLenum pname, GLint param);
GLAPI void glTexParameterf(GLenum target, GLenum pname, GLfloat param);
GLAPI void glTexEnvf(GLenum target, GLenum pname, GLfloat param);
GLAPI void glTexEnvi(GLenum target, GLenum pname, GLint param);
GLAPI void glBindTexture(GLenum target, GLuint texture);
GLAPI GLboolean glIsTexture(GLuint texture);
GLAPI void glActiveTextureARB(GLenum texture);
GLAPI void glActiveTexture(GLenum texture);
GLAPI void glGetBooleanv(GLenum pname, GLboolean* data);
GLAPI void glGetDoublev(GLenum pname, GLdouble* params);
GLAPI void glGetFloatv(GLenum pname, GLfloat* params);
GLAPI void glGetIntegerv(GLenum pname, GLint* data);
GLAPI void glGetLightfv(GLenum light, GLenum pname, GLfloat* params);
GLAPI void glGetLightiv(GLenum light, GLenum pname, GLint* params);
GLAPI void glGetMaterialfv(GLenum face, GLenum pname, GLfloat* params);
GLAPI void glGetMaterialiv(GLenum face, GLenum pname, GLint* params);
GLAPI void glDepthMask(GLboolean flag);
GLAPI void glEnableClientState(GLenum cap);
GLAPI void glDisableClientState(GLenum cap);
GLAPI void glClientActiveTextureARB(GLenum target);
GLAPI void glClientActiveTexture(GLenum target);

GLAPI void glVertexPointer(GLint size, GLenum type, GLsizei stride, const void* pointer);
GLAPI void glColorPointer(GLint size, GLenum type, GLsizei stride, const void* pointer);
GLAPI void glTexCoordPointer(GLint size, GLenum type, GLsizei stride, const void* pointer);
GLAPI void glDrawArrays(GLenum mode, GLint first, GLsizei count);
GLAPI void glDrawElements(GLenum mode, GLsizei count, GLenum type, const void* indices);
GLAPI void glDrawPixels(GLsizei width, GLsizei height, GLenum format, GLenum type, const void* data);
GLAPI void glDepthRange(GLdouble nearVal, GLdouble farVal);
GLAPI void glDepthFunc(GLenum func);
GLAPI void glPolygonMode(GLenum face, GLenum mode);
GLAPI void glPolygonOffset(GLfloat factor, GLfloat units);
GLAPI void glFogfv(GLenum mode, GLfloat* params);
GLAPI void glFogf(GLenum pname, GLfloat param);
GLAPI void glFogi(GLenum pname, GLint param);
GLAPI void glPixelStorei(GLenum pname, GLint param);
GLAPI void glScissor(GLint x, GLint y, GLsizei width, GLsizei height);
GLAPI void glLightf(GLenum light, GLenum pname, GLfloat param);
GLAPI void glLightfv(GLenum light, GLenum pname, GLfloat const* params);
GLAPI void glLighti(GLenum light, GLenum pname, GLint param);
GLAPI void glLightiv(GLenum light, GLenum pname, GLint const* params);
GLAPI void glLightModelf(GLenum pname, GLfloat param);
GLAPI void glLightModelfv(GLenum pname, GLfloat const* params);
GLAPI void glLightModeli(GLenum pname, GLint param);
GLAPI void glLightModeliv(GLenum pname, GLint const* params);
GLAPI void glStencilFunc(GLenum func, GLint ref, GLuint mask);
GLAPI void glStencilFuncSeparate(GLenum face, GLenum func, GLint ref, GLuint mask);
GLAPI void glStencilMask(GLuint mask);
GLAPI void glStencilMaskSeparate(GLenum face, GLuint mask);
GLAPI void glStencilOp(GLenum sfail, GLenum dpfail, GLenum dppass);
GLAPI void glStencilOpSeparate(GLenum face, GLenum sfail, GLenum dpfail, GLenum dppass);
GLAPI void glNormal3f(GLfloat nx, GLfloat ny, GLfloat nz);
GLAPI void glNormal3fv(GLfloat const* v);
GLAPI void glNormalPointer(GLenum type, GLsizei stride, void const* pointer);
GLAPI void glRasterPos2i(GLint x, GLint y);
GLAPI void glMaterialf(GLenum face, GLenum pname, GLfloat param);
GLAPI void glMaterialfv(GLenum face, GLenum pname, GLfloat const* params);
GLAPI void glMateriali(GLenum face, GLenum pname, GLint param);
GLAPI void glMaterialiv(GLenum face, GLenum pname, GLint const* params);
GLAPI void glLineWidth(GLfloat width);
GLAPI void glPushAttrib(GLbitfield mask);
GLAPI void glPopAttrib();
GLAPI void glBitmap(GLsizei width, GLsizei height, GLfloat xorig, GLfloat yorig, GLfloat xmove, GLfloat ymove, GLubyte const* bitmap);
GLAPI void glCopyTexImage2D(GLenum target, GLint level, GLenum internalformat, GLint x, GLint y, GLsizei width, GLsizei height, GLint border);
GLAPI void glMap1d(GLenum target, GLdouble u1, GLdouble u2, GLint stride, GLint order, GLdouble const* points);
GLAPI void glMap1f(GLenum target, GLfloat u1, GLfloat u2, GLint stride, GLint order, GLfloat const* points);
GLAPI void glMap2d(GLenum target, GLdouble u1, GLdouble u2, GLint ustride, GLint uorder, GLdouble v1, GLdouble v2, GLint vstride, GLint vorder, GLdouble const* points);
GLAPI void glMap2f(GLenum target, GLfloat u1, GLfloat u2, GLint ustride, GLint uorder, GLfloat v1, GLfloat v2, GLint vstride, GLint vorder, GLfloat const* points);
GLAPI void glMapGrid1d(GLint un, GLdouble u1, GLdouble u2);
GLAPI void glMapGrid1f(GLint un, GLfloat u1, GLfloat u2);
GLAPI void glMapGrid2d(GLint un, GLdouble u1, GLdouble u2, GLint vn, GLdouble v1, GLdouble v2);
GLAPI void glMapGrid2f(GLint un, GLfloat u1, GLfloat u2, GLint vn, GLfloat v1, GLfloat v2);
GLAPI void glEvalCoord1d(GLdouble u);
GLAPI void glEvalCoord1f(GLfloat u);
GLAPI void glEvalCoord2d(GLdouble u, GLdouble v);
GLAPI void glEvalCoord2f(GLfloat u, GLfloat v);
GLAPI void glEvalMesh1(GLenum mode, GLint i1, GLint i2);
GLAPI void glEvalMesh2(GLenum mode, GLint i1, GLint i2, GLint j1, GLint j2);
GLAPI void glEvalPoint1(GLint i);
GLAPI void glEvalPoint2(GLint i, GLint j);
GLAPI void glTexGend(GLenum coord, GLenum pname, GLdouble param);
GLAPI void glTexGenf(GLenum coord, GLenum pname, GLfloat param);
GLAPI void glTexGenfv(GLenum coord, GLenum pname, GLfloat const* params);
GLAPI void glTexGeni(GLenum coord, GLenum pname, GLint param);
GLAPI void glRectf(GLfloat x1, GLfloat y1, GLfloat x2, GLfloat y2);
GLAPI void glRecti(GLint x1, GLint y1, GLint x2, GLint y2);
GLAPI void glGetTexLevelParameteriv(GLenum target, GLint level, GLenum pname, GLint* params);
GLAPI void glPointSize(GLfloat size);

#ifdef __cplusplus
}
#endif
