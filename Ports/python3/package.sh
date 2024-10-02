#!/usr/bin/env -S bash ../.port_include.sh

source version.sh

export PATH="${SERENITY_SOURCE_DIR}/Toolchain/Local/python/bin:${PATH}"

port='python3'
version="${PYTHON_VERSION}"
workdir="Python-${version}"
useconfigure='true'
use_fresh_config_sub='true'
files=(
    "${PYTHON_ARCHIVE_URL}#${PYTHON_ARCHIVE_SHA256SUM}"
    "https://raw.githubusercontent.com/python/cpython/942dd9f3f77eef08fabddbd9fb883a866ad6d4cb/PC/pycon.ico#55c1e1fcabc2f254a6d02242912359d29f141d11c4892c20375d58b6dcd89ac0"
)
launcher_name='Python'
launcher_category='D&evelopment'
launcher_command='/usr/local/bin/python3'
launcher_run_in_terminal='true'
icon_file='../pycon.ico' # This is an older icon that's downloaded separately, so we need to go outside of $workdir
depends=(
    'bzip2'
    'libffi'
    'libuuid'
    'ncurses'
    'openssl'
    'readline'
    'sqlite'
    'xz'
    'zlib'
)
configopts=(
    '--disable-ipv6'
    '--enable-shared'
    '--without-ensurepip'
    'ac_cv_file__dev_ptmx=no'
    'ac_cv_file__dev_ptc=no'
    'ac_cv_header_libintl_h=no'
)

export BLDSHARED="${CC} -shared"

configure() {
    run ./configure --host="${SERENITY_ARCH}-pc-serenity" "--with-build-python=${PYTHON_BIN}" --build="$($workdir/config.guess)" "${configopts[@]}"
}

# Note: The showproperty command is used when linting ports, we don't actually need python at this time.
if [ "$1" != "showproperty" ]; then
    PYTHON_BIN="python3"
    PYTHON_VERSION_SHORT=$(echo $PYTHON_VERSION | cut -f1-2 -d".")
    if [ -x "$(command -v python${PYTHON_VERSION_SHORT})" ]; then
        PYTHON_BIN="python${PYTHON_VERSION_SHORT}"
    elif [ -x "$(command -v ${PYTHON_BIN})" ]; then
        # Check if major and minor version of python3 are matching
        if ! ${PYTHON_BIN} -c "import sys; major, minor = map(int, '${PYTHON_VERSION_SHORT}'.split('.')); sys.exit(not (sys.version_info.major == major and sys.version_info.minor == minor))"; then
            echo "Error: python3 version does not match needed version to build ${PYTHON_VERSION}" >&2
            echo "Build this Python version on your host using Toolchain/BuildPython.sh or install it otherwise and try again." >&2
            exit 1
        fi
    else
        echo "Error: python3 is not installed but is required to build ${PYTHON_VERSION}" >&2
        echo "Build this Python version on your host using Toolchain/BuildPython.sh or install it otherwise and try again." >&2
        exit 1
    fi
fi
