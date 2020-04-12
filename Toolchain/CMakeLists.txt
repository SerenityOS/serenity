cmake_minimum_required (VERSION 3.0)
project (SerenityOSToolchain NONE)

include(ExternalProject)

include(ProcessorCount)
ProcessorCount(MAKE_JOBS)

find_program(MAKE_EXE NAMES gmake make)

set(PREFIX ${CMAKE_CURRENT_SOURCE_DIR}/Local)
set(TARGET i686-pc-serenity)
set(SYSROOT ${CMAKE_CURRENT_SOURCE_DIR}/../Root)
set(PATCHES ${CMAKE_CURRENT_SOURCE_DIR}/Patches)

set(LIBC ${CMAKE_CURRENT_SOURCE_DIR}/../Libraries/LibC)
set(LIBM ${CMAKE_CURRENT_SOURCE_DIR}/../Libraries/LibM)

ExternalProject_Add(binutils
    URL      "https://ftp.gnu.org/gnu/binutils/binutils-2.33.1.tar.gz"
    URL_HASH MD5=1a6b16bcc926e312633fcc3fae14ba0a

    PATCH_COMMAND patch -p1 -t -N < ${PATCHES}/binutils.patch

    CONFIGURE_COMMAND ../binutils/configure
        --prefix=${PREFIX}
        --target=${TARGET}
        --with-sysroot=${SYSROOT}
        --enable-shared
        --disable-nls
    BUILD_COMMAND ${MAKE_EXE} -j${MAKE_JOBS}
    INSTALL_COMMAND ${MAKE_EXE} install
)

ExternalProject_Add(gcc
    DEPENDS binutils

    URL      "https://ftp.gnu.org/gnu/gcc/gcc-9.3.0/gcc-9.3.0.tar.gz"
    URL_HASH MD5=9b7e8f6cfad96114e726c752935af58a

    PATCH_COMMAND patch -p1 -t -N < ${PATCHES}/gcc.patch

    CONFIGURE_COMMAND ../gcc/configure
        --prefix=${PREFIX}
        --target=${TARGET}
        --with-sysroot=${SYSROOT}
        --disable-nls
        --with-newlib
        --enable-shared
        --enable-languages=c,c++
    BUILD_COMMAND ${MAKE_EXE} -j${MAKE_JOBS} all-gcc all-target-libgcc
    INSTALL_COMMAND ${MAKE_EXE} install-gcc install-target-libgcc
)

ExternalProject_Add_Step(gcc LibC
    COMMENT "Building Serenity LibC"
    DEPENDEES install

    WORKING_DIRECTORY ${LIBC}
    COMMAND ${MAKE_EXE} clean
    COMMAND ${MAKE_EXE} EXTRA_LIBC_DEFINES="-DBUILDING_SERENITY_TOOLCHAIN"
    COMMAND ${MAKE_EXE} install
)

ExternalProject_Add_Step(gcc LibM
    COMMENT "Building Serenity LibM"
    DEPENDEES install

    WORKING_DIRECTORY ${LIBM}
    COMMAND ${MAKE_EXE} clean
    COMMAND ${MAKE_EXE}
    COMMAND ${MAKE_EXE} install
)

ExternalProject_Add_Step(gcc libstdc++
    DEPENDEES LibC LibM

    WORKING_DIRECTORY <BINARY_DIR>
    COMMAND ${MAKE_EXE} -j${MAKE_JOBS} all-target-libstdc++-v3
    COMMAND ${MAKE_EXE} install-target-libstdc++-v3
)

ExternalProject_Add_StepTargets(gcc LibC LibM libstdc++)

if(APPLE)
    set(QEMU_UI_LIB cocoa)
else()
    set(QEMU_UI_LIB gtk)
endif()

ExternalProject_Add(qemu
    URL      "https://download.qemu.org/qemu-4.1.0.tar.xz"
    URL_HASH MD5=cdf2b5ca52b9abac9bacb5842fa420f8

    EXCLUDE_FROM_ALL TRUE

    CONFIGURE_COMMAND ../qemu/configure
        --prefix=${PREFIX}
        --target-list=i386-softmmu
        --enable-${QEMU_UI_LIB}
)
