set(LOADER_SOURCES
    main.cpp
    math.cpp
    misc.cpp
)

file(GLOB AK_SOURCES "../../AK/*.cpp")
file(GLOB ELF_SOURCES "../../Libraries/LibELF/*.cpp")
set(ELF_SOURCES ${ELF_SOURCES} ../../Libraries/LibELF/Arch/i386/plt_trampoline.S)
file(GLOB LIBC_SOURCES1 "../../Libraries/LibC/*.cpp")
file(GLOB LIBC_SOURCES2 "../../Libraries/LibC/*/*.cpp")
file(GLOB LIBC_SOURCES3 "../../Libraries/LibC/*.S")

list(FILTER LIBC_SOURCES1 EXCLUDE REGEX ".+crt0.cpp")
list(FILTER LIBC_SOURCES1 EXCLUDE REGEX ".+crt0.+.cpp")

set(SOURCES ${LOADER_SOURCES} ${AK_SOURCES} ${ELF_SOURCES} ${LIBC_SOURCES1} ${LIBC_SOURCES2} ${LIBC_SOURCES3})

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -nostdlib -pie -fpic -DNO_TLS")

add_executable(Loader.so ${SOURCES})
install(TARGETS Loader.so RUNTIME DESTINATION usr/lib/)
