
Rough hints for setting up MUSCLE on Solaris:

Make sure you have libusb, usually in /usr/lib:

ls -l /usr/lib/libusb.so
lrwxrwxrwx   1 root     other         11 Jan 12 16:02 /usr/lib/libusb.so -> libusb.so.1

Get PCSC and CCID.
-rwx------   1 user staff     529540 Jun 16 18:24 ccid-1.0.1.tar.gz
-rwx------   1 user staff     842654 Jun 16 18:24 pcsc-lite-1.3.1.tar.gz

Unpack pcsc
Run ./configure --enable-libusb (??)
gnumake
Make /usr/local writeable for user
gnumake install

Unpack ccid
Run ./configure
gnumake
gnumake install

As root, go to /usr/local/sbin
Run ./pcscd --foreground

