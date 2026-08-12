#!/usr/bin/env -S bash ../.port_include.sh

zig_version='0.16.0'

port='kiesel'
version='0.4.0-dev.26+ae39f076b'
_commit='ae39f076bfcd39ede51fa092b4df1b452d5bb290'
workdir='kiesel'
files=(
    "https://codeberg.org/kiesel-js/kiesel/archive/${_commit}.tar.gz#156ae8b9cda791bb29aeb420fa22a2fd87858c8f30000ac52f17ab3d252a95e9"
    "https://ziglang.org/download/${zig_version}/zig-${zig_version}.tar.xz#43186959edc87d5c7a1be7b7d2a25efffd22ce5807c7af99067f86f99641bfdf"
)
useconfigure='true'
launcher_name='Kiesel'
launcher_category='D&evelopment'
launcher_command='/usr/local/bin/kiesel'
launcher_run_in_terminal='true'

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
    run_nocd cat >"${PORT_BUILD_DIR}/libc.txt" <<EOF
include_dir=${SERENITY_INSTALL_ROOT}/usr/include
sys_include_dir=${SERENITY_INSTALL_ROOT}/usr/include
crt_dir=${SERENITY_INSTALL_ROOT}/usr/lib
msvc_lib_dir=
kernel32_lib_dir=
gcc_dir=
EOF
}

build() {
    local zig_dir="${PORT_BUILD_DIR}/zig-${zig_version}"
    local zig_patch="0005-Extend-support-for-SerenityOS-target.patch"

    # Most of our patches are upstreamed and will be in Zig 0.17,
    # until then we apply them manually and build with a custom lib dir.
    if [ ! -f "${zig_dir}/.${zig_patch}_applied" ]; then
        run_nocd patch -d "${zig_dir}" -p2 < "${PORT_META_DIR}/../zig/patches/${zig_patch}"
        run_nocd touch "${zig_dir}/.${zig_patch}_applied"
    fi

    # Rust does not know about serenity so we don't even attempt
    # building with Intl/Temporal.
    run zig build \
        --prefix "${SERENITY_INSTALL_ROOT}/usr/local" \
        --libc "${PORT_BUILD_DIR}/libc.txt" \
        --zig-lib-dir "${zig_dir}/lib" \
        -Doptimize=ReleaseFast \
        -Dtarget="${SERENITY_ARCH}-serenity-none" \
        -Dversion-string="${version}" \
        -Denable-intl=false \
        -Denable-temporal=false
}

install() {
    :
}
