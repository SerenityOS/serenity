#!/usr/bin/env -S bash ../.port_include.sh
port=mesa
version=git
commit=a6a651b96ac987a50dba3b62bd08d21d7e3e7d73
workdir=mesa-$commit
depends=("zlib" "llvm")
useconfigure=true
configopts=(-Dprefix="/usr/local" \
            --errorlogs \
            "--cross-file" "../cross_file-$SERENITY_ARCH.txt" \
            "--cross-file" "../cross_file_hack_llvm_config.txt" \
            "-Dllvm=enabled" \
            "-Dosmesa=true" \
            "-Dopengl=true" \
            "-Ddri-drivers=" \
            "-Dgallium-drivers=swrast" \
            "-Dcpp_rtti=true" \
            "-Dvulkan-drivers=swrast" \
            )
files="https://gitlab.freedesktop.org/mesa/mesa/-/archive/$commit/mesa-$commit.tar.gz mesa-git.tar.gz 9f3d81682dc2c2c0c3e5c9335194e8b01317ab96067cfa1d729b678fe39c3b78"
auth_type=sha256

configure() {
  # HACK: The meson seem to have a problem with defining the 'llvm-config' using relative path.
  #       Let's pre-normalize this path instead.
  printf "[binaries]\nllvm-config = '%s'\n" "$(realpath ./llvm-config)" > cross_file_hack_llvm_config.txt

  run meson setup _build "${configopts[@]}"
}

build() {
  run ninja -C _build
}

install() {
  export DESTDIR=$SERENITY_BUILD_DIR/Root
  run meson install -C _build
}

post_install() {
  MESA_LIBGL_DIR="/usr/local/lib/mesa_libgl"
  INSTALL_MESA_LIBGL_DIR="${DESTDIR}/${MESA_LIBGL_DIR}"

  mkdir -p "${INSTALL_MESA_LIBGL_DIR}"
  ln -sf "../libOSMesa.so.8.0.0" "${INSTALL_MESA_LIBGL_DIR}/libgl.so"
  ln -sf "../libOSMesa.so.8.0.0" "${INSTALL_MESA_LIBGL_DIR}/libgl.so.serenity"

  echo "LibGL with OSMesa backend will be in ${MESA_LIBGL_DIR}/"
  echo "Put it in the LD_LIBRARY_PATH when staring the app when you want to use it"
}
