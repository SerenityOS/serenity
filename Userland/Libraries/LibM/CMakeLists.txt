# Provide a dummy target and a linker script that tells everything to link against LibC instead.
add_library(LibM INTERFACE)
target_link_libraries(LibM INTERFACE LibC)
file(WRITE "${CMAKE_STAGING_PREFIX}/${CMAKE_INSTALL_LIBDIR}/libm.so" "INPUT(libc.so)")
