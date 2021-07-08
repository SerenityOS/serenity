# Troubleshooting

In case of an error, you might find an answer of how to deal it here.

## I build everything, the VM starts and then I see...

### "Error: Kernel Image too big for memory slot. Halting!"

This means the kernel is too large again. Contact us on the discord server or open a GitHub Issue about it.
You might want to revert latest changes in tree to see if that solves the problem temporarily.

### "Your computer does not support long mode (64-bit mode). Halting!"

Either your machine (if you try to boot on bare metal) is very old, thus it's not supporting x86_64 extensions, or you try to use VirtualBox without using a x64 virtualization mode or you try to use `qemu-system-i386` which doesn't support x86_64 extensions too.

### "Your computer does not support PAE. Halting!"
- If booting on bare metal, your CPU is too old to boot Serenity.
- If you're using VirtualBox, you need to enable PAE/NX. Check the instructions [here.](VirtualBox.md)
- If you're using QEMU, the [CPU model configuration](https://qemu-project.gitlab.io/qemu/system/qemu-cpu-models.html) is not exposing PAE.
