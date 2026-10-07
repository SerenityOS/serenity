_gcc_lib_dir() {
    local gcc_version
    gcc_version="$(cd "${PORT_META_DIR}/../gcc" && ./package.sh showproperty version)"
    echo "${SERENITY_INSTALL_ROOT}/usr/local/lib/gcc/${SERENITY_ARCH}-serenity/${gcc_version}"
}

check_gcc_crt_files() {
    local gcc_lib_dir
    gcc_lib_dir="$(_gcc_lib_dir)"

    if [ ! -f "${gcc_lib_dir}/crtbeginS.o" ] || [ ! -f "${gcc_lib_dir}/crtendS.o" ]; then
        echo "crtbeginS.o or crtendS.o could not be found, ensure the GCC port is installed." >&2
        exit 1
    fi
}

create_libc_file() {
    local gcc_lib_dir
    gcc_lib_dir="$(_gcc_lib_dir)"

    mkdir -p "$(dirname "$1")"

    cat > "$1" <<EOF
include_dir=${SERENITY_INSTALL_ROOT}/usr/include
sys_include_dir=${SERENITY_INSTALL_ROOT}/usr/include
cc_dir=${gcc_lib_dir}
crt_dir=${SERENITY_INSTALL_ROOT}/usr/lib
msvc_lib_dir=
kernel32_lib_dir=
# FIXME: Building on macOS validates this field when setting ZIG_LIBC,
#        regardless of the actual target (upstream bug?)
darwin_sdk_dir=$(xcrun --show-sdk-path 2>/dev/null || true)
EOF
}
