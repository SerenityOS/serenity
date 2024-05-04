## Name

Boot Device Addressing - addressing the correct boot device to use.

## Synopsis

Serenity's kernel can select the boot device at boot time, based on the `root` boot parameter.
This functionality is used to control which boot device is selected to be used for all further boot process operations.

## Description

The kernel `root` boot parameter takes the form of **`root={value}`**, where the **`={value}`**
trailer can be set to specific prefixes to indicate the boot device preference.

### Addressing options

The user can choose to use addressing based on synthetic unix concepts:

```
block0:0
```

This is especially useful in static hardware setups, so the user can choose to use
either a raw `StorageDevice` or partition block device. The `0,0` selection is the `MAJOR,MINOR`
numbers of the device.

However, when there's knowledge of the hardware arrangement of raw `StorageDevice`s,
it could be valuable to use addressing based on hardware-relative interface-specific "location"
to address raw `StorageDevice`s:

```
ata0:0:0 [First ATA controller, ATA first primary channel, master device]
nvme0:1:0 [First NVMe Controller, First NVMe Namespace, Not Applicable]
```

When the logical arrangement is known, using (absolute) LUNs is the easiest option as it doesn't rely on
using unix device numbers or hardware-relative location:

```
lun0:0:0 - first device on the first channel of the first controller to be enumerated
```

### Note on selecting partitions from raw `StorageDevice`s

All the addressing options above support selecting a partition device, given that
the selected device is a `StorageDevice` and not a `DiskPartition` device:

```
nvme0;part0
lun0:0:0;part0
```

The only exception to this is when choosing a `BlockDevice`. As such,
trying to specify `block0:0;part0`, for example, will lead to a kernel panic,
as an invalid boot device parameter.

### Selecting a specific partition based on known GUID

For GPT partitions, passing `PARTUUID:` and the GUID of the partition can be used
to select a GPT partition. Although it could be slower to find the corresponding
partition, it is the safest option available for persistent storage.
