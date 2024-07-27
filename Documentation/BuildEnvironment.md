# Build Environment for SerenityOS with docker

If you did not finded your OS in [build instructions for other systems](BuildInstructionsOther.md) or had problems while building Serenity;
You can use docker build environment, follow instructions bellow to run it

```sh
    cd Meta
    sudo ./BuildEnvironment.sh
```

Inside Build Environment run:
```sh
    cd /home/ubuntu/serenity
```

Then simply follow [build instructions](BuildInstructions.md#build)

**NOTE:** BuildEnvironment supports grub and extlinux only.