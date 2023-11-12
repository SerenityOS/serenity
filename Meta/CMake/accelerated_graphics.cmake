if (NOT ENABLE_ACCELERATED_GRAPHICS OR EMSCRIPTEN)
    # FIXME: Enable GL emulation in emscripten: https://emscripten.org/docs/porting/multimedia_and_graphics/OpenGL-support.html
    set(HAS_ACCELERATED_GRAPHICS OFF CACHE BOOL "" FORCE)
    return()
endif()

find_package(OpenGL COMPONENTS OpenGL EGL)

if (OPENGL_FOUND)
    set(HAS_ACCELERATED_GRAPHICS ON CACHE BOOL "" FORCE)
    set(ACCEL_GFX_LIBS OpenGL::OpenGL OpenGL::EGL CACHE STRING "" FORCE)
else()
    set(HAS_ACCELERATED_GRAPHICS OFF CACHE BOOL "" FORCE)
endif()

if (APPLE)
    set(HAS_ACCELERATED_GRAPHICS ON CACHE BOOL "" FORCE)
    set(ACCEL_GFX_LIBS "-framework OpenGL" CACHE STRING "" FORCE)
endif()
