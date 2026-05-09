#!/usr/bin/env -S bash ../.port_include.sh
port='llvm'
useconfigure='true'
version='9edf0e73b59540ccec25b355268ebff6d3bba4ef'
workdir="llvm-project-${version}"
configopts=(
    "-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt"
)
files=(
    "https://github.com/llvm/llvm-project/archive/$version.zip#374416b18414e8bf966623ae89a8a46e6753c28d3e23e699ab485bb6e31e86cd"
)
depends=(
    "ncurses"
    "zlib"
    "zstd"
)

configure() {
    # The cross compilers will be picked up from the CMake toolchain file.
    # CC/CXX must point to the host compiler to let it build host tools (tblgen).
    host_env

    if [ "$SERENITY_TOOLCHAIN" = "Clang" ]; then
        linker="lld"
        objcopy="llvm-objcopy"
        rtlib="compiler-rt"
        stdlib="libc++"
        unwindlib="libunwind"
        use_llvm_unwinder="ON"
        exclude_atomic_builtin="OFF"
    else
        linker="ld"
        objcopy="objcopy"
        rtlib="libgcc"
        stdlib="libstdc++"
        unwindlib="libgcc"
        use_llvm_unwinder="OFF"
        # Atomic builtins can't be cross-compiled with GCC. Use the libatomic port
        # if the program you're building has references to symbols like __atomic_load.
        exclude_atomic_builtin="ON"
    fi

    mkdir -p llvm-build
    cmake ${workdir}/llvm \
        -G Ninja \
        -B llvm-build "${configopts[@]}" \
        -DCLANG_DEFAULT_OBJCOPY=$objcopy \
        -DCLANG_DEFAULT_RTLIB=$rtlib \
        -DCLANG_DEFAULT_CXX_STDLIB=$stdlib \
        -DCLANG_DEFAULT_UNWINDLIB=$unwindlib \
        -DCMAKE_BUILD_TYPE=MinSizeRel \
        -DCMAKE_FIND_ROOT_PATH="$SERENITY_BUILD_DIR"/Root \
        -DCOMPILER_RT_BUILD_CRT=ON \
        -DCOMPILER_RT_BUILD_ORC=OFF \
        -DCOMPILER_RT_EXCLUDE_ATOMIC_BUILTIN=$exclude_atomic_builtin \
        -DCOMPILER_RT_USE_LLVM_UNWINDER=$use_llvm_unwinder \
        -DCOMPILER_RT_OS_DIR=serenity \
        -DCROSS_TOOLCHAIN_FLAGS_NATIVE="-DCMAKE_C_COMPILER=$CC;-DCMAKE_CXX_COMPILER=$CXX" \
        -DHAVE_LIBRT=OFF \
        -DLLVM_APPEND_VC_REV=OFF \
        -DLLVM_DEFAULT_TARGET_TRIPLE=$SERENITY_ARCH-serenity \
        -DLLVM_ENABLE_PROJECTS="clang;lld;compiler-rt" \
        -DLLVM_HAVE_LIBXAR=OFF \
        -DLLVM_INCLUDE_BENCHMARKS=OFF \
        -DLLVM_INCLUDE_TESTS=OFF \
        -DLLVM_INSTALL_TOOLCHAIN_ONLY=ON \
        -DLLVM_OCAML_INSTALL_PATH="${SERENITY_INSTALL_ROOT}/usr/local/ocaml" \
        -DLLVM_PTHREAD_LIB=pthread \
        -DLLVM_TARGETS_TO_BUILD="X86;AArch64;RISCV"
}

build() {
    ninja -j${MAKEJOBS} -C llvm-build
}

install() {
    ninja -C llvm-build install
}
