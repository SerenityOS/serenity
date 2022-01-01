# Transferring files from QEMU to your host machine

## Method 1: WebServer
Serenity has a built-in web server which extends to your host machine.

Open a new terminal and use the following command to start a WebServer instance for the current working directory:

```console
ws .
```

Then we just open `localhost:8000` on our host machine :^)

![](WebServer_localhost.png)

**NOTE:** Due to the fact that same browsers download unrecognized files as plain text, you may want to use something like `wget` to download the file **as is** instead. Otherwise the file may appear corrupted when the system tries to load it.

## Method 2: Mount disk_image

Another way is to mount Serenity's disk_image to your host machine by using the following command on *nix systems:

```console
cd "Build/${SERENITY_ARCH}"
mkdir mnt
sudo mount -t ext2 _disk_image mnt
```
