#!/usr/bin/env bash
set -eu

SCRIPT="$(realpath $(dirname "${BASH_SOURCE[0]}"))"

if [ -z "${SERENITY_STRIPPED_ENV:-}" ]; then
    exec "${SCRIPT}/.strip_env.sh" "${@}"
fi
unset SERENITY_STRIPPED_ENV

export MAKEJOBS="${MAKEJOBS:-$(nproc)}"
export CMAKE_BUILD_PARALLEL_LEVEL="$MAKEJOBS"

buildstep() {
    local buildstep_name=$1
    shift
    if [ "$#" -eq '0' ]; then
        "${buildstep_name}"
    else
        "$@"
    fi 2>&1 | sed $'s|^|\x1b[34m['"${port}/${buildstep_name}"$']\x1b[39m |'
    local return_code=${PIPESTATUS[0]}
    if [ ${return_code} != 0 ]; then
        echo -e "\x1b[1;31mError in step ${port}/${buildstep_name} (status=${return_code})\x1b[0m"
    fi
    return ${return_code}
}

buildstep_intro() {
    echo -e "\x1b[1;32m=> $@\x1b[0m"
}

target_env() {
    if [ -f "${SCRIPT}/.hosted_defs.sh" ]; then
        . "${SCRIPT}/.hosted_defs.sh"
    elif [ "$(uname -s)" = "SerenityOS" ]; then
        export SERENITY_ARCH="$(uname -m)"
        export SERENITY_INSTALL_ROOT=""
    else
        >&2 echo "Error: .hosted_defs.sh is missing and we are not running on Serenity."
        exit 1
    fi
}

target_env

DESTDIR="${SERENITY_INSTALL_ROOT}"

enable_ccache() {
    if [ "${USE_CCACHE:-true}" = "true" ] && command -v ccache &>/dev/null; then
        ccache_tooldir="${SERENITY_BUILD_DIR}/ccache"
        mkdir -p "$ccache_tooldir"
        for tool in cc clang gcc c++ clang++ g++; do
            name="${SERENITY_ARCH}-pc-serenity-${tool}"
            if ! command -v "${name}" >/dev/null; then
                continue
            fi
            ln -sf "$(command -v ccache)" "${ccache_tooldir}/${name}"
        done
        export PATH="${ccache_tooldir}:$PATH"
    fi
}

enable_ccache

host_env() {
    export CC="${HOST_CC}"
    export CXX="${HOST_CXX}"
    export LD="${HOST_LD}"
    export AR="${HOST_AR}"
    export RANLIB="${HOST_RANLIB}"
    export PATH="${HOST_PATH}"
    export READELF="${HOST_READELF}"
    export OBJCOPY="${HOST_OBJCOPY}"
    export STRIP="${HOST_STRIP}"
    export CXXFILT="${HOST_CXXFILT}"
    export PKG_CONFIG_DIR="${HOST_PKG_CONFIG_DIR}"
    export PKG_CONFIG_SYSROOT_DIR="${HOST_PKG_CONFIG_SYSROOT_DIR}"
    export PKG_CONFIG_LIBDIR="${HOST_PKG_CONFIG_LIBDIR}"
    enable_ccache
}

packagesdb="${DESTDIR}/usr/Ports/packages.db"

makeopts=("-j${MAKEJOBS}")
installopts=()
configscript=configure
configopts=()
useconfigure=false
config_sub_paths=("config.sub")
config_guess_paths=("config.guess")
use_fresh_config_sub=false
use_fresh_config_guess=false
depends=()
patchlevel=1
auth_type=
auth_import_key=
auth_opts=()
launcher_name=
launcher_category=
launcher_command=
launcher_workdir=
launcher_run_in_terminal=false
icon_file=

. "$@"
shift

: "${workdir:=$port-$version}"

PORT_META_DIR="$(pwd)"
if [[ -z ${SERENITY_BUILD_DIR:-} ]]; then
    PORT_BUILD_DIR="${PORT_META_DIR}"
else
    PORT_BUILD_DIR="${SERENITY_BUILD_DIR}/Ports/${port}"
fi

mkdir -p "${PORT_BUILD_DIR}"
cd "${PORT_BUILD_DIR}"

cleanup_git() {
    echo "WARNING: Reverting changes to $workdir as we are in dev mode!"
    run git clean -xffd >/dev/null 2>&1
}

# Make sure to clean up the git repository of the port afterwards.
if [ -n "${IN_SERENITY_PORT_DEV:-}" ]; then
    echo "WARNING: All changes to the workdir in the current state (inside ./package.sh dev) are temporary!"
    echo "         They will be reverted once the command exits!"
    trap "run cleanup_git" EXIT
fi

run_nocd() {
    echo "+ $@ (nocd)" >&2
    ("$@")
}

run() {
    echo "+ $@"
    (cd "$workdir" && "$@")
}

run_replace_in_file() {
    if [ "$(uname -s)" = "SerenityOS" ]; then
        run sed -i "$1" $2
    else
        run perl -p -i -e "$1" $2
    fi
}

sed_in_place() {
    if [ "$(uname -s)" = "Darwin" ]; then
        sed -i '' "${@}"
    else
        sed -i "${@}"
    fi
}

get_new_config_sub() {
    config_sub="${1:-config.sub}"
    if [ ! -f "$workdir/$config_sub" ]; then
        >&2 echo "Error: Downloaded $config_sub does not replace an existing file!"
        exit 1
    fi
    if ! run grep -q serenity "$config_sub"; then
        run do_download_file "https://git.savannah.gnu.org/gitweb/?p=config.git;a=blob_plain;f=config.sub" "${config_sub}" false
    fi
}

get_new_config_guess() {
    config_guess="${1:-config.guess}"
    if [ ! -f "$workdir/$config_guess" ]; then
        >&2 echo "Error: Downloaded $config_guess does not replace an existing file!"
        exit 1
    fi
    if ! run grep -q SerenityOS "$config_guess"; then
        run do_download_file "https://git.savannah.gnu.org/gitweb/?p=config.git;a=blob_plain;f=config.guess" "${config_guess}" false
    fi
}

ensure_new_config_sub() {
    for path in "${config_sub_paths[@]}"; do
        get_new_config_sub "${path}"
    done
}

ensure_new_config_guess() {
    for path in "${config_guess_paths[@]}"; do
        get_new_config_guess "${path}"
    done
}

ensure_build() {
    # Sanity check.
    if [ ! -f "${DESTDIR}/usr/lib/libc.so" ]; then
        echo "libc.so could not be found. This likely means that SerenityOS:"
        echo "- has not been built and/or installed yet"
        echo "- has been installed in an unexpected location"
        echo "The currently configured build directory is ${SERENITY_BUILD_DIR}. Resolve this issue and try again."
        exit 1
    fi
}

install_main_icon() {
    if [ -n "$icon_file" ] && [ -n "$launcher_command" ]; then
        local launcher_binary="${launcher_command%% *}"
        install_icon "$icon_file" "${launcher_binary}"
    fi
}

install_icon() {
    if [ "$#" -lt 2 ]; then
        echo "Syntax: install_icon <icon> <launcher>"
        exit 1
    fi
    local icon="$1"
    local launcher="$2"

    command -v convert >/dev/null || true
    local convert_exists=$?
    command -v identify >/dev/null || true
    local identify_exists=$?

    if [ "${convert_exists}" != "0" ] || [ "${identify_exists}" != 0 ]; then
        echo 'Unable to install icon: missing convert or identify, did you install ImageMagick?'
        return
    fi

    for icon_size in "16x16" "32x32"; do
        index=$(run identify "$icon" | grep "$icon_size" | grep -oE "\[[0-9]+\]" | tr -d "[]" | head -n1)
        if [ -n "$index" ]; then
            run convert "${icon}[${index}]" "app-${icon_size}.png"
        else
            run convert "$icon[0]" -resize $icon_size "app-${icon_size}.png"
        fi
    done
    run $OBJCOPY --add-section serenity_icon_s="app-16x16.png" "${DESTDIR}${launcher}"
    run $OBJCOPY --add-section serenity_icon_m="app-32x32.png" "${DESTDIR}${launcher}"
}

install_main_launcher() {
    if [ -n "$launcher_name" ] && [ -n "$launcher_category" ] && [ -n "$launcher_command" ]; then
        install_launcher "$launcher_name" "$launcher_category" "$launcher_command" "$launcher_workdir"
    fi
}

install_launcher() {
    if [ "$#" -lt 4 ]; then
        echo "Syntax: install_launcher <name> <category> <command> <workdir>"
        exit 1
    fi
    local launcher_name="$1"
    local launcher_category="$2"
    local launcher_command="$3"
    local launcher_workdir="$4"
    local launcher_filename="${launcher_name,,}"
    launcher_filename="${launcher_filename// /}"
    local icon_override=""
    case "$launcher_command" in
        *\ *)
            mkdir -p $DESTDIR/usr/local/libexec
            launcher_executable="/usr/local/libexec/$launcher_filename"
            cat >"$DESTDIR/$launcher_executable" <<SCRIPT
#!/bin/sh
exec $(printf '%q ' $launcher_command)
SCRIPT
            chmod +x "$DESTDIR/$launcher_executable"
            icon_override="IconPath=${launcher_command%% *}"
            ;;
        *)
            launcher_executable="$launcher_command"
            ;;
    esac
    mkdir -p $DESTDIR/res/apps
    cat >$DESTDIR/res/apps/$launcher_filename.af <<CONFIG
[App]
Name=$launcher_name
Executable=$launcher_executable
Category=$launcher_category
WorkingDirectory=$launcher_workdir
RunInTerminal=$launcher_run_in_terminal
${icon_override}
CONFIG
}
# Checks if a function is defined. In this case, if the function is not defined in the port's script, then we will use our defaults. This way, ports don't need to include these functions every time, but they can override our defaults if needed.
func_defined() {
    PATH= command -V "$1"  > /dev/null 2>&1
}

func_defined pre_fetch || pre_fetch() {
    :
}
func_defined post_fetch || post_fetch() {
    :
}

do_download_file() {
    local url="$1"
    local filename="$2"
    local accept_existing="${3:-true}"

    echo "Downloading URL: ${url}"

    # FIXME: Serenity's curl port does not support https, even with openssl installed.
    if which curl >/dev/null 2>&1 && ! curl https://example.com -so /dev/null; then
        url=$(echo "$url" | sed "s/^https:\/\//http:\/\//")
    fi

    # download files
    if $accept_existing && [ -f "$filename" ]; then
        echo "$filename already exists"
    else
        if which curl; then
            run_nocd curl ${curlopts:-} "$url" -L -o "$filename"
        else
            run_nocd pro "$url" > "$filename"
        fi
    fi
}

fetch() {
    pre_fetch

    if [ "$auth_type" = "sig" ] && [ ! -z "${auth_import_key}" ]; then
        # import gpg key if not existing locally
        # The default keyserver keys.openpgp.org prints "new key but contains no user ID - skipped"
        # and fails. Use a different key server.
        gpg --list-keys $auth_import_key || gpg --keyserver hkps://keyserver.ubuntu.com --recv-key $auth_import_key
    fi

    tried_download_again=0

    while true; do
        OLDIFS=$IFS
        IFS=$'\n'
        for f in $files; do
            IFS=$OLDIFS
            read url filename auth_sum<<< $(echo "$f")
            do_download_file "$url" "${PORT_META_DIR}/${filename}"
        done

        verification_failed=0

        OLDIFS=$IFS
        IFS=$'\n'
        for f in $files; do
            IFS=$OLDIFS
            read url filename auth_sum<<< $(echo "$f")

            # check sha256sum if given
            if [ "$auth_type" = "sha256" ]; then
                echo "Expecting ${auth_type}sum: $auth_sum"
                calc_sum="$(sha256sum "${PORT_META_DIR}/${filename}" | cut -f1 -d' ')"
                echo "${auth_type}sum($filename) = '$calc_sum'"
                if [ "$calc_sum" != "$auth_sum" ]; then
                    # remove downloaded file to re-download on next run
                    rm -f "${PORT_META_DIR}/${filename}"
                    echo "${auth_type}sums mismatching, removed erroneous download."
                    if [ $tried_download_again -eq 1 ]; then
                        echo "Please run script again."
                        exit 1
                    fi
                    echo "Trying to download the files again."
                    tried_download_again=1
                    verification_failed=1
                fi
            fi
        done

        # check signature
        if [ "$auth_type" = "sig" ]; then
            if $NO_GPG; then
                echo "WARNING: gpg signature check was disabled by --no-gpg-verification"
            else
                if $(cd "${PORT_META_DIR}" && gpg --verify "${auth_opts[@]}"); then
                    echo "- Signature check OK."
                else
                    echo "- Signature check NOT OK"
                    for f in $files; do
                        rm -f $f
                    done
                    rm -rf "$workdir"
                    echo "  Signature mismatching, removed erronous download."
                    if [ $tried_download_again -eq 1 ]; then
                        echo "Please run script again."
                        exit 1
                    fi
                    echo "Trying to download the files again."
                    tried_download_again=1
                    verification_failed=1
                fi
            fi
        fi

        if [ $verification_failed -ne 1 ]; then
            break
        fi
    done

    # extract
    OLDIFS=$IFS
    IFS=$'\n'
    for f in $files; do
        IFS=$OLDIFS
        read url filename auth_sum<<< $(echo "$f")

        if [ ! -f "$workdir"/.${filename}_extracted ]; then
            case "$filename" in
                *.tar.gz|*.tgz)
                    run_nocd tar -xzf "${PORT_META_DIR}/${filename}"
                    run touch .${filename}_extracted
                    ;;
                *.tar.gz|*.tar.bz|*.tar.bz2|*.tar.xz|*.tar.lz|*.tar.zst|.tbz*|*.txz|*.tgz)
                    run_nocd tar -xf "${PORT_META_DIR}/${filename}"
                    run touch .${filename}_extracted
                    ;;
                *.gz)
                    run_nocd gunzip "${PORT_META_DIR}/${filename}"
                    run touch .${filename}_extracted
                    ;;
                *.zip)
                    run_nocd bsdtar xf "${PORT_META_DIR}/${filename}" || run_nocd unzip -qo "${PORT_META_DIR}/${filename}"
                    run touch .${filename}_extracted
                    ;;
                *.asc)
                    run_nocd gpg --import "${PORT_META_DIR}/${filename}" || true
                    ;;
                *)
                    echo "Note: no case for file $filename."
                    cp "${PORT_META_DIR}/${filename}" ./
                    ;;
            esac
        fi
    done

    post_fetch
}

func_defined pre_install || pre_install() {
    :
}

func_defined pre_patch || pre_patch() {
    :
}

func_defined patch_internal || patch_internal() {
    # patch if it was not yet patched (applying patches multiple times doesn't work!)
    if [ -z "${IN_SERENITY_PORT_DEV:-}" ] && [ -d "${PORT_META_DIR}/patches" ]; then
        for filepath in "${PORT_META_DIR}"/patches/*.patch; do
            filename=$(basename $filepath)
            if [ ! -f "$workdir"/.${filename}_applied ]; then
                run patch -p"$patchlevel" < "$filepath"
                run touch .${filename}_applied
            fi
        done
    fi
}
func_defined pre_configure || pre_configure() {
    :
}
func_defined configure || configure() {
    chmod +x "${workdir}"/"$configscript"
    if [[ -n "${SERENITY_SOURCE_DIR:-}" ]]; then
        run ./"$configscript" --host="${SERENITY_ARCH}-pc-serenity" "${configopts[@]}"
    else
        run ./"$configscript" --build="${SERENITY_ARCH}-pc-serenity" "${configopts[@]}"
    fi
}
func_defined post_configure || post_configure() {
    :
}
func_defined build || build() {
    run make "${makeopts[@]}"
}
func_defined install || install() {
    run make DESTDIR=$DESTDIR "${installopts[@]}" install
}
func_defined post_install || post_install() {
    :
}
clean() {
    rm -rf "${PORT_BUILD_DIR}/"*
}
clean_dist() {
    OLDIFS=$IFS
    IFS=$'\n'
    for f in $files; do
        IFS=$OLDIFS
        read url filename hash <<< $(echo "$f")
        rm -f "${PORT_META_DIR}/${filename}"
    done
}
clean_all() {
    clean
    clean_dist
}
addtodb() {
    buildstep_intro "Adding $port $version to database of installed ports..."
    if [ -n "$(package_install_state $port $version)" ]; then
        echo "Note: Skipped because $port $version is already installed."
        return
    fi
    if [ "${1:-}" = "--auto" ]; then
        echo "auto $port $version" >> "$packagesdb"
    else
        echo "manual $port $version" >> "$packagesdb"
    fi
    if [ "${#depends[@]}" -gt 0 ]; then
        echo "dependency $port ${depends[@]}" >> "$packagesdb"
    fi
    echo "Successfully installed $port $version."
}
ensure_packagesdb() {
    if [ ! -f "$packagesdb" ]; then
        mkdir -p "$(dirname $packagesdb)"
        touch "$packagesdb"
    fi
}
package_install_state() {
    local port=$1
    local version=${2:-}

    ensure_packagesdb
    grep -E "^(auto|manual) $port $version" "$packagesdb" | cut -d' ' -f1
}
installdepends() {
    for depend in "${depends[@]}"; do
        if [ -n "$(package_install_state $depend)" ]; then
            continue
        fi

        # Split colon separated string into a list
        IFS=':' read -ra port_directories <<< "$SERENITY_PORT_DIRS"
        for port_dir in "${port_directories[@]}"; do
            if [ -d "${port_dir}/$depend" ]; then
                (cd "${port_dir}/$depend" && ./package.sh --auto)
                continue 2
            fi
        done

        >&2 echo "Error: Dependency $depend could not be found."
        exit 1
    done
}
uninstall() {
    if [ "$(package_install_state $port)" != "manual" ]; then
        >&2 echo "Error: $port is not installed. Cannot uninstall."
        return
    elif [ ! -f plist ]; then
        >&2 echo "Error: This port does not have a plist yet. Cannot uninstall."
        return
    fi
    for f in `cat plist`; do
        case $f in
            */)
                run rmdir "${DESTDIR}/$f" || true
                ;;
            *)
                run rm -rf "${DESTDIR}/$f"
                ;;
        esac
    done
    # Without || true, mv will not be executed if you are uninstalling your only remaining port.
    grep -v "^manual $port " "$packagesdb" > packages.db.tmp || true
    mv packages.db.tmp "$packagesdb"
}
do_installdepends() {
    buildstep_intro "Installing dependencies of $port..."
    installdepends
}
do_fetch() {
    buildstep_intro "Fetching $port..."
    buildstep fetch
}
do_patch() {
    buildstep_intro "Patching $port..."
    buildstep pre_patch
    buildstep patch_internal
}
do_configure() {
    ensure_build
    if [ "$useconfigure" = "true" ]; then
        buildstep_intro "Configuring $port..."
        if "$use_fresh_config_sub"; then
            buildstep ensure_new_config_sub
        fi
        if "$use_fresh_config_guess"; then
            buildstep ensure_new_config_guess
        fi
        buildstep pre_configure
        buildstep configure
        buildstep post_configure
    else
        buildstep configure echo "This port does not use a configure script. Skipping configure step."
    fi
}
do_build() {
    ensure_build
    buildstep_intro "Building $port..."
    buildstep build
}
do_install() {
    ensure_build
    buildstep pre_install
    buildstep_intro "Installing $port..."
    buildstep install
    buildstep install_main_launcher
    buildstep install_main_icon
    buildstep post_install
    addtodb "${1:-}"
}
do_clean() {
    buildstep_intro "Cleaning build directory for $port..."
    buildstep clean
}
do_clean_dist() {
    buildstep_intro "Cleaning dist files for $port..."
    buildstep clean_dist
}
do_clean_all() {
    buildstep_intro "Cleaning all for $port..."
    buildstep clean_all
}
do_uninstall() {
    buildstep_intro "Uninstalling $port..."
    buildstep uninstall
}
do_showproperty() {
    while [ $# -gt 0 ]; do
        if ! declare -p "${1}" > /dev/null 2>&1; then
            echo "Property '$1' is not set." >&2
            exit 1
        fi
        property_declaration="$(declare -p "${1}")"
        if [[ "$property_declaration" =~ "declare -a" ]]; then
            prop_array="${1}[@]"
            # Some magic to avoid empty arrays being considered unset.
            echo "${!prop_array+"${!prop_array}"}"
        else
            echo ${!1}
        fi
        printf '\n'
        shift
    done
}
do_all() {
    do_installdepends
    do_fetch
    do_patch
    do_configure
    do_build
    do_install "${1:-}"
}

do_shell() {
    do_installdepends
    do_fetch
    do_patch
    cd "$workdir"
    bash
    echo "End of package shell. Back to the User shell."
}

do_generate_patch_readme() {
    if [ ! -d "${PORT_META_DIR}/patches" ]; then
        >&2 echo "Error: Port $port does not have any patches"
        exit 1
    fi

    if [ -f "${PORT_META_DIR}/patches/ReadMe.md"  ]; then
        read -N1 -rp \
            "A ReadMe.md already exists, overwrite? (N/y) " should_overwrite
        echo
        if [ "${should_overwrite,,}" != y ]; then
            >&2 echo "Not overwriting Ports/$port/patches/ReadMe.md"
            exit 0
        fi
    fi

    # An existing patches directory but no actual patches presumably means that we just deleted all patches,
    # so remove the ReadMe file accordingly.
    if [ -z "$(find -L "${PORT_META_DIR}/patches" -maxdepth 1 -name '*.patch' -print -quit)" ]; then
        >&2 echo "Port $port does not have any patches, deleting ReadMe..."
        rm -f "${PORT_META_DIR}/patches/ReadMe.md"
        exit 0
    fi

    local tempdir="$(pwd)/.patches.tmp"
    rm -fr "$tempdir"
    mkdir "$tempdir"

    pushd "${PORT_META_DIR}/patches"

    echo "# Patches for $port on SerenityOS" > ReadMe.md
    echo >> ReadMe.md

    local count=0
    for patch in *.patch; do
        git mailinfo \
            "$tempdir/$patch.msg" \
            /dev/null \
            < "$patch" \
            > "$tempdir/$patch.info" \
            2> "$tempdir/$patch.error" \
        || {
            rc=$?
            >&2 echo "Failed to extract patch info from $patch"
            >&2 echo "git returned $rc and said:"
            >&2 cat "$tempdir/$patch.error"
            exit 1
        }

        (
            grep 'Subject: ' "$tempdir/$patch.info" | sed -e 's/Subject: \(.*\)$/\1/'
            echo
            cat "$tempdir/$patch.msg"
        ) > "$tempdir/$patch.desc"


        if [ ! -s "$tempdir/$patch.desc" ]; then
            >&2 echo "WARNING: $patch does not contain a valid git patch or is missing a commit message, and is going to be skipped!"
            continue
        fi

        {
            echo "## \`$patch\`"
            echo
            sed -e '/^Co-Authored-By: /d' < "$tempdir/$patch.desc"
            echo
        } >> ReadMe.md
        count=$((count + 1))
    done

    popd

    >&2 echo "Successfully generated entries for $count patch(es) in patches/ReadMe.md."
}

launch_user_shell() {
    env \
        IN_SERENITY_PORT_DEV="$port" \
        "${SHELL:-bash}" || \
    true
}

prompt_yes_no() {
    read -N1 -rp \
        "$1 (N/y) " result
    2>&1 echo
    if [ "${result,,}" == y ]; then
        return 0
    else
        return 1
    fi
}

prompt_yes_no_default_yes() {
    read -N1 -rp \
        "$1 (Y/n) " result
    2>&1 echo
    if [ "${result,,}" == n ]; then
        return 1
    else
        return 0
    fi
}

do_dev() {
    if [ -n "${IN_SERENITY_PORT_DEV:-}"  ]; then
        >&2 echo "Error: Already in dev environment for $IN_SERENITY_PORT_DEV"
        exit 1
    fi

    if [ "${1:-}" != "--no-depends" ]; then
        do_installdepends
    fi
    if [ -d "$workdir" ] && [ ! -d "$workdir/.git" ]; then
        if prompt_yes_no "- Would you like to clean the working direcory (i.e. ./package.sh clean)?"; then
            do_clean
        fi
    fi

    local force_patch_regeneration='false'

    [ -d "$workdir" ] || {
        do_fetch
        pushd "$workdir"
        if [ ! -d ".git" ]; then
            git init .
            git config core.autocrlf false
            git add --all --force
            git commit -a -m 'Initial import'
            git tag import
        fi

        # Import patches as commits, or ask the user to commit them
        # if they're not git patches already.
        if [ -d "${PORT_META_DIR}/patches" ] && [ -n "$(find -L "${PORT_META_DIR}/patches" -maxdepth 1 -name '*.patch' -print -quit)" ]; then
            for patch in "${PORT_META_DIR}"/patches/*.patch; do
                if [ -f "$workdir/.$(basename $patch).applied" ]; then
                    continue
                fi

                echo "Importing patch $(basename "${patch}")..."
                git am --keep-cr --keep-non-patch "$patch" >/dev/null 2>&1 || {
                    git am --abort >/dev/null 2>&1 || true
                    if git apply < $patch; then
                        git add -A
                        if prompt_yes_no "- This patch does not appear to be a git patch, would you like to modify its changes before continuing?"; then
                            >&2 echo "Apply any changes you want, commit them into the current repo and quit this shell to continue."

                            launch_user_shell
                        fi
                        main_author=''
                        co_authors=()
                        while read -r line; do
                            author="$(echo "$line" | cut -f2 -d'	')"
                            if [[ -z "$main_author" ]]; then
                                main_author="$author"
                            else
                                co_authors+=("$author")
                            fi
                        done < <(git -C "${PORT_META_DIR}" shortlog -esn -- "patches/$(basename "$patch")")

                        if [[ -n "$main_author" ]]; then
                            date="$(git -C "${PORT_META_DIR}" log --format=%ad -n1 -- "patches/$(basename "$patch")")"
                            >&2 echo -n "- This patch was authored by $main_author"
                            if [[ ${#co_authors[@]} -ne 0 ]]; then
                                >&2 echo -n " (and ${co_authors[*]})"
                            fi
                            >&2 echo " at $date"
                            if prompt_yes_no_default_yes "- Would you like to preserve that information?"; then
                                trailers=()
                                for a in "${co_authors[@]}"; do
                                    trailers+=("--trailer" "Co-Authored-By: $a")
                                done
                                git commit --verbose --author "$main_author" --date "$date" "${trailers[@]}"
                            else
                                >&2 echo " Okay, using your current git identity as the author."
                                git commit --verbose
                            fi
                        else
                            git commit --verbose
                        fi
                    else
                        # The patch didn't apply, oh no!
                        # Ask the user to figure it out :shrug:
                        git am --keep-cr --keep-non-patch --3way "$patch" || true
                        >&2 echo "- This patch does not apply, you'll be dropped into a shell to investigate and fix this, quit the shell when the problem is resolved."
                        >&2 echo "Note that the patch needs to be committed into the current repository!"
                        launch_user_shell
                        force_patch_regeneration='true'
                    fi

                    if ! git diff --quiet >/dev/null 2>&1; then
                        >&2 echo "- It appears that there are uncommitted changes from applying the previous patch:"
                        for line in $(git diff --color=always); do
                            echo "|  $line"
                        done
                        if prompt_yes_no "- Would you like to drop them before moving on to the next patch?"; then
                            git clean -xf
                        else
                            >&2 echo "- The uncommitted changes will be committed with the next patch or left in the tree."
                        fi
                    fi
                }
            done
        fi

        git tag original

        popd
    }

    [ -d "$workdir/.git" ] || {
        >&2 echo "$workdir does not appear to be a git repository."
        >&2 echo "If you want to use './package.sh dev', please run './package.sh clean' first."
        exit 1
    }

    pushd "$workdir"
    launch_user_shell
    popd >/dev/null 2>&1

    local original_hash="$(git -C "$workdir" rev-parse refs/tags/original)"
    local current_hash="$(git -C "$workdir" rev-parse HEAD)"

    # If the hashes are the same, we have no changes, otherwise generate patches
    if [ "$original_hash" != "$current_hash" ] || [ "${force_patch_regeneration}" = "true" ]; then
        >&2 echo "Note: Regenerating patches as there are changed commits in the port repo (started at $original_hash, now is $current_hash)"
        rm -fr "${PORT_META_DIR}"/patches/*.patch
        git -C "$workdir" format-patch --no-numbered --zero-commit --no-signature --full-index refs/tags/import -o "$(realpath "${PORT_META_DIR}/patches")"
        do_generate_patch_readme
    fi
}

NO_GPG=false
parse_arguments() {
    if [ -z "${1:-}" ]; then
        do_all
        return
    fi
    case "$1" in
        build|clean|clean_all|clean_dist|configure|dev|fetch|generate_patch_readme|install|installdepends|patch|shell|showproperty|uninstall)
            method=$1
            shift
            do_${method} "$@"
            ;;
        --auto)
            do_all $1
            ;;
        --no-gpg-verification)
            NO_GPG=true
            shift
            parse_arguments $@
            ;;
        interactive)
            export PS1="(serenity):\w$ "
            bash --norc
            ;;
        *)
            >&2 echo "I don't understand $1! Supported arguments: build, clean, clean_all, clean_dist, configure, dev, fetch, generate_patch_readme, install, installdepends, interactive, patch, shell, showproperty, uninstall."
            exit 1
            ;;
    esac
}

parse_arguments $@
