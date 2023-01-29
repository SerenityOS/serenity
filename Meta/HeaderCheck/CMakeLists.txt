execute_process(COMMAND "${CMAKE_CURRENT_SOURCE_DIR}/generate_all.py" "${SERENITY_ARCH}" OUTPUT_VARIABLE SOURCES_STRING)
string(REPLACE "\n" ";" SOURCES_LIST ${SOURCES_STRING})

# TODO: LibSoftGPU does not compile with this warning enabled, so we disable it here.
# Once LibSoftGPU drops this disable, HeaderCheck should remove it, too. In particular, the following error would be generated:
# Userland/Libraries/LibSoftGPU/SIMD.h: In function 'AK::SIMD::f32x4 SoftGPU::ddx(AK::SIMD::f32x4)':
# Userland/Libraries/LibSoftGPU/SIMD.h:71:59: error: SSE vector return without SSE enabled changes the ABI [-Werror=psabi]
#    71 | ALWAYS_INLINE static AK::SIMD::f32x4 ddx(AK::SIMD::f32x4 v)
#       |                                                           ^
add_compile_options(-Wno-psabi)

add_library(HeaderCheck OBJECT ${SOURCES_LIST})
