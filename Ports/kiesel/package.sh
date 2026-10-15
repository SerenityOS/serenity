#!/usr/bin/env -S bash ../.port_include.sh

source ../zig/libc.sh

zig_version='0.17.0'

port='kiesel'
version='0.4.1'
workdir='kiesel'
files=(
    "https://codeberg.org/kiesel-js/kiesel/archive/${version}.tar.gz#a21430c087ff0089dc52de038e5124fbac8e981ddb11d98c1e205e70e5acf4c4"
    "https://ziglang.org/download/${zig_version}/zig-${zig_version}.tar.xz#b6c7f1728f043700d6529bac980800792f824256a9d2f1839b3d62beed0b8abd"
)
useconfigure='true'
launcher_name='Kiesel'
launcher_category='D&evelopment'
launcher_command='/usr/local/bin/kiesel'
launcher_run_in_terminal='true'

pre_patch() {
    local zig_dir="${PORT_BUILD_DIR}/zig-${zig_version}"
    local zig_patch="0001-Extend-support-for-SerenityOS-target.patch"

    # Most of our patches are upstreamed, but we still have a few so for now we
    # apply them manually and build with a custom lib dir.
    if [ ! -f "${zig_dir}/.${zig_patch}_applied" ]; then
        run_nocd patch -d "${zig_dir}" -p2 < "${PORT_META_DIR}/zig-patches/${zig_patch}"
        # Skip when applying patches to $workdir
        run_nocd touch "${zig_dir}/.${zig_patch}_applied"
    fi
}

configure() {
    if [ ! -x "$(command -v zig)" ]; then
        echo "Error: zig is not installed but is required to build kiesel" >&2
        echo "Install Zig ${zig_version} on your host and try again." >&2
        exit 1
    fi

    if [ "$(zig version)" != "${zig_version}" ]; then
        echo "Error: zig version mismatch (got $(zig version))" >&2
        echo "Install Zig ${zig_version} on your host and try again." >&2
        exit 1
    fi

    # Eventually Zig might bundle libc ABI lists for serenity,
    # until then we build with a custom libc file.
    # https://codeberg.org/ziglang/libc-abi-tools/src/branch/master/serenity
    check_gcc_crt_files
    create_libc_file "${PORT_BUILD_DIR}/libc.txt"
}

build() {
    local zig_dir="${PORT_BUILD_DIR}/zig-${zig_version}"
    local emit_dir="${PORT_BUILD_DIR}/emit-c"
    local libregexp_dir="zig-pkg/N-V-__8AANeKMgAJMmyOafYQo7YrC99SZlSbL6khjXSwI6Nd"
    local bdwgc_dir="zig-pkg/bdwgc-8.3.0-8obE-M8rMAALvGehRlqR0N4PeYIhmqxzjdj0_5nfCJJ5"

    run zig build --fetch=all

    # Zig 0.17.0 passes an invalid bare `__serenity__` argument without `-D` to
    # the C compiler (will be fixed in 0.17.1), so we use the C backend and
    # build with the toolchain. Rust does not know about serenity so we don't
    # even attempt building with Intl/Temporal.
    run zig build \
        --zig-lib="${zig_dir}/lib" \
        --prefix "${emit_dir}" \
        --libc "${PORT_BUILD_DIR}/libc.txt" \
        -Doptimize=fast \
        -Dtarget="${SERENITY_ARCH}-serenity-none" \
        -Dversion-string="${version}" \
        -Denable-intl=false \
        -Denable-temporal=false


    run "${CC}" -std=gnu17 -I "${zig_dir}/lib" \
        -c "${emit_dir}/kiesel.c" \
        -o kiesel.o

    run "${CC}" -std=gnu17 -I "${libregexp_dir}" -r \
        "${libregexp_dir}/cutils.c" \
        "${libregexp_dir}/libregexp.c" \
        "${libregexp_dir}/libunicode.c" \
        -o libregexp.o

    run "${CC}" -std=gnu17 -I "${bdwgc_dir}/include" \
        -DALL_INTERIOR_POINTERS \
        -DNO_EXECUTE_PERMISSION \
        -DGC_THREADS \
        -DPARALLEL_MARK \
        -DTHREAD_LOCAL_ALLOC \
        -DGC_BUILTIN_ATOMIC \
        -D_REENTRANT \
        -DHANDLE_FORK \
        -DENABLE_DISCLAIM \
        -DDYNAMIC_POINTER_MASK \
        -DGC_ATOMIC_UNCOLLECTABLE \
        -DNO_DEBUGGING \
        -DNDEBUG \
        -DUSE_MMAP \
        -DUSE_MUNMAP \
        -DLARGE_CONFIG \
        -DGC_NOT_DLL \
        -DHAVE_SYS_TYPES_H \
        -DHAVE_UNISTD_H \
        -DNO_MSGBOX_ON_ERROR \
        -DNO_GETENV \
        -c "${bdwgc_dir}/extra/gc.c" \
        -o libgc.o

    run "${CC}" kiesel.o libregexp.o libgc.o -o kiesel
}

install() {
    run mkdir -p "${SERENITY_INSTALL_ROOT}/usr/local/bin"
    run cp kiesel "${SERENITY_INSTALL_ROOT}/usr/local/bin/"
}
