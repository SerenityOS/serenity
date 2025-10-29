# Setting up SPICE integrations

# Ubuntu

1. Build QEMU via `Toolchain/BuildQemu.sh`
2. Install virt-viewer 8.0
    - On 23.04+ just do `sudo apt-get install virt-viewer`
    - For earlier versions you need to build it from source (see below).
3. export SERENITY_SPICE=1
4. Meta/serenity.sh run

## Building + installing `virt-viewer`

**Note:** If you installed an old version from `apt` uninstall that first (`sudo apt-get purge virt-viewer`).

Install the build dependencies:

```
sudo apt-get install libvirt-glib-1.0 libvirt-dev spice-client-gtk-3.0 spice-client-glib-2.0 intltool
```

Fetch and extract the sources:

```
wget https://releases.pagure.org/virt-viewer/virt-viewer-8.0.tar.gz
tar -xvf virt-viewer-8.0.tar.gz
```

Configure and build:

```
cd ./virt-viewer-8.0
./configure --with-spice-gtk
make
```

Install:

```
sudo make install
```
