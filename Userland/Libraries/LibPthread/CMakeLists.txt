# Provide a dummy target and a linker script that tells everything to link against LibC instead.
add_library(LibPthread INTERFACE)
target_link_libraries(LibPthread INTERFACE LibC)
file(WRITE "${CMAKE_STAGING_PREFIX}/${CMAKE_INSTALL_LIBDIR}/libpthread.so" "INPUT(libc.so)")
