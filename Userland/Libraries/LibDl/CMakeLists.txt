# Provide a dummy target and a linker script that tells everything to link against LibC instead.
add_library(LibDl INTERFACE)
target_link_libraries(LibDl INTERFACE LibC)
file(WRITE "${CMAKE_STAGING_PREFIX}/${CMAKE_INSTALL_LIBDIR}/libdl.so" "INPUT(libc.so)")
