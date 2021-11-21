#!/usr/bin/env -S bash ../.port_include.sh
port=llvm
useconfigure=true
version=13.0.0
workdir=llvm-project-llvmorg-${version}
configopts=("-DCMAKE_TOOLCHAIN_FILE=${SERENITY_BUILD_DIR}/CMakeToolchain.txt")
files="https://github.com/llvm/llvm-project/archive/refs/tags/llvmorg-${version}.zip llvm.zip 333a4e053fb543d9efb8dc68126d9e7a948ecb246985f2804a0ecbc5ccdb9d08"
auth_type=sha256
depends=("ncurses" "zlib")

pre_patch() {
    host_env
    mkdir -p llvm-host
    cmake ${workdir}/llvm \
    -B llvm-host \
    -DLLVM_ENABLE_PROJECTS=clang
    make -C llvm-host -j $(nproc) llvm-tblgen clang-tblgen
    target_env
}

configure() {
    if [ "$SERENITY_TOOLCHAIN" = "Clang" ]; then
        stdlib=""
        unwindlib=""
    else
        stdlib="libstdc++"
        unwindlib="libgcc"
    fi
    mkdir -p llvm-build
    cmake ${workdir}/llvm \
    -G Ninja \
    -B llvm-build "${configopts[@]}" \
    -DCMAKE_BUILD_TYPE=MinSizeRel \
    -DCMAKE_FIND_ROOT_PATH="$SERENITY_BUILD_DIR"/Root \
    -DCLANG_DEFAULT_CXX_STDLIB=$stdlib \
    -DCLANG_DEFAULT_UNWINDLIB=$unwindlib \
    -DCLANG_TABLEGEN=$(pwd)/llvm-host/bin/clang-tblgen \
    -DCOMPILER_RT_BUILD_CRT=ON \
    -DCOMPILER_RT_BUILD_ORC=OFF \
    -DCOMPILER_RT_EXCLUDE_ATOMIC_BUILTIN=OFF \
    -DCOMPILER_RT_OS_DIR=serenity \
    -DHAVE_LIBRT=OFF \
    -DLLVM_DEFAULT_TARGET_TRIPLE=$SERENITY_ARCH-pc-serenity \
    -DLLVM_ENABLE_PROJECTS="clang;lld;compiler-rt" \
    -DLLVM_HAVE_LIBXAR=OFF \
    -DLLVM_INCLUDE_BENCHMARKS=OFF \
    -DLLVM_INCLUDE_TESTS=OFF \
    -DLLVM_INSTALL_TOOLCHAIN_ONLY=ON \
    -DLLVM_PTHREAD_LIB=pthread \
    -DLLVM_TABLEGEN=$(pwd)/llvm-host/bin/llvm-tblgen \
    -DLLVM_TARGETS_TO_BUILD=X86
}

build() {
    ninja -C llvm-build
}

install() {
    ninja -C llvm-build install
}
