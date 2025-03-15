#!/usr/bin/env python3

# Copyright (c) 2018-2023, the SerenityOS developers.
# Copyright (c) 2023, kleines Filmröllchen <filmroellchen@serenityos.org>
#
# SPDX-License-Identifier: BSD-2-Clause

from __future__ import annotations

import os
import re
import sys
from dataclasses import dataclass, field
from enum import Enum, unique
from itertools import chain, repeat
from os import access, environ
from pathlib import Path
from shutil import which
from subprocess import run
from typing import Any, Callable, Literal
import shlex

QEMU_MINIMUM_REQUIRED_MAJOR_VERSION = 6
QEMU_MINIMUM_REQUIRED_MINOR_VERSION = 2

BUILD_DIRECTORY = Path(environ.get("SERENITY_BUILD_DIR") or Path.cwd())


class RunError(Exception):
    pass


@unique
class Arch(Enum):
    """SerenityOS architecture, not host architecture."""

    Aarch64 = "aarch64"
    RISCV64 = "riscv64"
    x86_64 = "x86_64"


def host_arch_matches(arch: Arch) -> bool:
    machine = os.uname().machine
    if arch == Arch.Aarch64:
        return machine == Arch.Aarch64.value or machine == "arm64"
    elif arch == Arch.RISCV64:
        return machine == Arch.RISCV64.value
    elif arch == Arch.x86_64:
        return machine == Arch.x86_64.value or machine == "amd64" or machine == "x64"


@unique
class QEMUKind(Enum):
    """VM distinctions determining which hardware acceleration technology *might* be used."""

    # Linux and anything that may or may not have KVM, including WSL with Linux QEMU.
    Other = "kvm"
    # WSL with native Windows QEMU (and possibly WHPX).
    NativeWindows = "whpx"
    # MacOS with HVF.
    MacOS = "hvf"
    # Serenity on Serenity with ported QEMU
    SerenityOS = "tcg"


@unique
class MachineType(Enum):
    Default = ""
    QEMUTap = "qtap"
    QEMUWithoutNetwork = "qn"
    QEMUExtLinux = "qextlinux"
    QEMU35 = "q35"
    QEMU35Grub = "q35grub"
    QEMUGrub = "qgrub"
    CI = "ci"
    Limine = "limine"
    RaspberryPi3B = "raspi3b"
    RaspberryPi4B = "raspi4b"

    def uses_grub(self) -> bool:
        return self in [MachineType.QEMU35Grub, MachineType.QEMUGrub]

    def is_q35(self) -> bool:
        return self in [MachineType.QEMU35Grub, MachineType.QEMU35]

    def is_raspberry_pi(self) -> bool:
        return self in [MachineType.RaspberryPi3B, MachineType.RaspberryPi4B]

    def supports_pc_speaker(self) -> bool:
        """Whether the pcspk-audiodev option is allowed for this machine type."""
        return self in [
            MachineType.Default,
            MachineType.QEMUTap,
            MachineType.QEMUWithoutNetwork,
            MachineType.QEMUExtLinux,
            MachineType.QEMU35,
            MachineType.QEMU35Grub,
            MachineType.QEMUGrub,
            MachineType.Limine,
        ]


@unique
class BootDriveType(Enum):
    AHCI = "ahci"
    NVMe = "nvme"
    PCI_SD = "pci-sd"
    USB_UHCI = "usb-uhci"
    USB_xHCI = "usb-xhci"
    USB_UAS = "usb-uas"
    VirtIOBLK = "virtio"


def arguments_generator(prefix: str) -> Any:
    """
    Construct an argument generator that returns some prefix and the member value(s) if the member value
    is not None, or returns an empty list otherwise.
    The member value is the return value of the function decorated with this decorator.
    If a default is provided, in this case we return [prefix, default] instead.
    Many of our configurable QEMU arguments work like this.
    """

    def decorate_function(member_accessor: Callable[[Configuration], str | list[str]]):
        def generate_arguments(self: Configuration) -> list[str]:
            member_value = member_accessor(self)
            if member_value is not None:
                if type(member_value) is list:
                    # apply the prefix to every element of the list
                    return list(chain(*zip(repeat(prefix), member_value)))
                # NOTE: the typechecker gets confused and can't figure out that
                # type(member_value) is *always* str here.
                elif type(member_value) is str:
                    return [prefix, member_value]
            return []

        return generate_arguments

    return decorate_function


@dataclass
class Configuration:
    """Run configuration, populated from command-line or environment variable data."""

    # ## Programs and environmental configuration
    virtualization_support: bool = False
    qemu_binary: Path | None = None
    qemu_kind: QEMUKind | None = None
    kvm_usable: bool | None = None
    architecture: Arch | None = None
    serenity_src: Path | None = None

    # ## High-level run configuration
    machine_type: MachineType = MachineType.Default
    enable_gdb: bool = False
    enable_gl: bool = False
    screen_count: int = 1
    host_ip: str = "127.0.0.1"
    ethernet_device_type: str = "e1000"
    disk_image: Path = Path("_disk_image")
    boot_drive_type: BootDriveType = BootDriveType.NVMe

    # ## Low-level QEMU configuration
    # QEMU -append
    kernel_cmdline: list[str] = field(default_factory=lambda: ["hello"])
    # QEMU -m
    ram_size: str | None = "2G"
    # QEMU -cpu
    qemu_cpu: str | None = "max"
    # QEMU -smp
    cpu_count: int | None = 2
    # QEMU -machine
    qemu_machine: str | None = None
    # QEMU -usb
    enable_usb: bool = False
    # QEMU -audiodev
    audio_backend: str | None = "none,id=snd0"
    # Each is a QEMU -device related to audio.
    audio_devices: list[str] = field(default_factory=list)
    # QEMU -vga
    vga_type: str | None = "none"
    # QEMU -display; if None, will omit the option and let QEMU figure out which backend to use on its own.
    display_backend: str | None = None
    # A QEMU -device for the graphics card.
    display_device: str | None = "virtio-gpu-pci"
    # QEMU -netdev
    network_backend: str | None = None
    # A QEMU -device for networking.
    # Note that often, there are other network devices in the generic device list, added by specific machine types.
    network_default_device: str | None = None
    # QEMU -drive
    boot_drive: str | None = None
    # Each is a QEMU -chardev
    character_devices: list[str] = field(default_factory=list)
    # Each is a QEMU -device
    devices: list[str] = field(default_factory=list)

    # ## Argument lists and methods generating them

    # Argument list pertaining to Kernel and Prekernel image(s)
    kernel_and_initrd_arguments: list[str] = field(default_factory=list)
    # Argument list provided by the user for performing packet logging
    packet_logging_arguments: list[str] = field(default_factory=list)
    # Various arguments relating to SPICE setup
    spice_arguments: list[str] = field(default_factory=list)
    # Arbitrary extra arguments
    extra_arguments: list[str] = field(default_factory=list)

    @property
    @arguments_generator(prefix="-accel")
    def accelerator_arguments(self) -> str | None:
        return self.qemu_kind.value if self.virtualization_support and (self.qemu_kind is not None) else "tcg"

    @property
    def kernel_cmdline_arguments(self) -> list[str]:
        return ["-append", " ".join(self.kernel_cmdline)] if len(self.kernel_cmdline) != 0 else []

    @property
    @arguments_generator(prefix="-m")
    def ram_arguments(self) -> str | None:
        return self.ram_size

    @property
    @arguments_generator(prefix="-cpu")
    def cpu_arguments(self) -> str | None:
        return self.qemu_cpu

    @property
    @arguments_generator(prefix="-smp")
    def smp_arguments(self) -> str | None:
        return str(self.cpu_count) if self.cpu_count is not None else None

    @property
    @arguments_generator(prefix="-machine")
    def machine_arguments(self) -> str | None:
        return self.qemu_machine

    @property
    def usb_arguments(self) -> list[str]:
        return ["-usb"] if self.enable_usb else []

    @property
    @arguments_generator(prefix="-audiodev")
    def audio_backend_arguments(self) -> str | None:
        return self.audio_backend

    @property
    @arguments_generator(prefix="-device")
    def audio_devices_arguments(self) -> list[str] | None:
        return self.audio_devices

    @property
    @arguments_generator(prefix="-vga")
    def vga_arguments(self) -> str | None:
        return self.vga_type

    @property
    @arguments_generator(prefix="-display")
    def display_backend_arguments(self) -> str | None:
        return self.display_backend

    @property
    @arguments_generator(prefix="-device")
    def display_device_arguments(self) -> str | None:
        return self.display_device

    @property
    def network_backend_arguments(self) -> list[str]:
        return ["-netdev", self.network_backend] if self.network_backend is not None else ["-nic", "none"]

    @property
    @arguments_generator(prefix="-device")
    def network_default_arguments(self) -> str | None:
        return self.network_default_device

    @property
    @arguments_generator(prefix="-drive")
    def boot_drive_arguments(self) -> str | None:
        return self.boot_drive

    @property
    @arguments_generator(prefix="-chardev")
    def character_device_arguments(self) -> list[str]:
        return self.character_devices

    @property
    @arguments_generator(prefix="-device")
    def device_arguments(self) -> list[str]:
        return self.devices

    def add_device(self, device: str):
        self.devices.append(device)

    def add_devices(self, devices: list[str]):
        self.devices.extend(devices)


def kvm_usable() -> bool:
    return access("/dev/kvm", os.R_OK | os.W_OK)


def determine_qemu_kind() -> QEMUKind:
    if which("wslpath") is not None and environ.get("SERENITY_NATIVE_WINDOWS_QEMU", "1") == "1":
        # Assume native Windows QEMU for now,
        # we might discard that assuption later when we properly
        # look for the binary.
        return QEMUKind.NativeWindows
    if sys.platform == "darwin":
        return QEMUKind.MacOS
    if os.uname().sysname == "SerenityOS":
        return QEMUKind.SerenityOS
    return QEMUKind.Other


def determine_serenity_arch() -> Arch:
    arch = environ.get("SERENITY_ARCH")
    if arch == "aarch64":
        return Arch.Aarch64
    if arch == "riscv64":
        return Arch.RISCV64
    if arch == "x86_64":
        return Arch.x86_64
    raise RunError("Please specify a valid SerenityOS architecture")


def determine_machine_type() -> MachineType:
    provided_machine_type = environ.get("SERENITY_RUN")
    if provided_machine_type is not None:
        try:
            value = MachineType(provided_machine_type)
        except ValueError:
            raise RunError(f"{provided_machine_type} is not a valid SerenityOS machine type")
        return value
    return MachineType.Default


def determine_boot_drive_type() -> BootDriveType:
    provided_boot_drive_type = environ.get("SERENITY_BOOT_DRIVE")
    if provided_boot_drive_type is not None:
        try:
            value = BootDriveType(provided_boot_drive_type)
        except ValueError:
            raise RunError(f"{provided_boot_drive_type} is not a valid SerenityOS boot drive type")
        return value
    return BootDriveType.NVMe


def detect_ram_size() -> str | None:
    return environ.get("SERENITY_RAM_SIZE", "2G")


def set_up_qemu_binary(config: Configuration):
    qemu_binary_basename: str | None = None
    if "SERENITY_QEMU_BIN" in environ:
        qemu_binary_basename = environ.get("SERENITY_QEMU_BIN")
    else:
        if config.architecture == Arch.Aarch64:
            qemu_binary_basename = "qemu-system-aarch64"
        elif config.architecture == Arch.RISCV64:
            qemu_binary_basename = "qemu-system-riscv64"
        elif config.architecture == Arch.x86_64:
            qemu_binary_basename = "qemu-system-x86_64"
    if qemu_binary_basename is None:
        raise RunError("QEMU binary could not be determined")

    # Try finding native Windows QEMU first
    if config.qemu_kind == QEMUKind.NativeWindows:
        # FIXME: Consider using the wslwinreg module instead to access the registry more conveniently.
        # Some Windows systems don't have reg.exe's directory on the PATH by default.
        environ["PATH"] = environ["PATH"] + ":/mnt/c/Windows/System32"
        try:
            qemu_install_dir_result = run(
                ["reg.exe", "query", r"HKLM\Software\QEMU", "/v", "Install_Dir", "/t", "REG_SZ"],
                capture_output=True,
            )
            if qemu_install_dir_result.returncode == 0:
                registry_regex = re.compile(rb"Install_Dir\s+REG_SZ\s+(.*)$", flags=re.MULTILINE)
                qemu_install_dir_match = registry_regex.search(qemu_install_dir_result.stdout)
                if qemu_install_dir_match is not None:
                    # If Windows prints non-ASCII characters, those will most likely not be UTF-8.
                    # Therefore, don't decode sooner. Also, remove trailing '\r'
                    qemu_install_dir = Path(qemu_install_dir_match.group(1).decode("utf-8").strip())
                    config.qemu_binary = Path(
                        run(
                            ["wslpath", "--", Path(qemu_install_dir, qemu_binary_basename)],
                            encoding="utf-8",
                            capture_output=True,
                        ).stdout.strip()
                    ).with_suffix(".exe")
            # No native Windows QEMU, reconfigure to Linux QEMU without KVM
            else:
                config.virtualization_support = False
                config.qemu_kind = QEMUKind.Other
        except Exception:
            # reg.exe not found; errors in reg.exe itself do not throw an error.
            config.qemu_kind = QEMUKind.Other

    if config.qemu_binary is None:
        # Set up full path for the binary if possible (otherwise trust system PATH)
        local_qemu_bin = Path(str(config.serenity_src), "Toolchain/Local/qemu/bin/", qemu_binary_basename)
        old_local_qemu_bin = Path(str(config.serenity_src), "Toolchain/Local/x86_64/bin/", qemu_binary_basename)
        if local_qemu_bin.exists():
            config.qemu_binary = local_qemu_bin
        elif old_local_qemu_bin.exists():
            config.qemu_binary = old_local_qemu_bin
        else:
            config.qemu_binary = Path(qemu_binary_basename)


def check_qemu_version(config: Configuration):
    if config.qemu_binary is None:
        raise RunError(
            f"Please install QEMU version {QEMU_MINIMUM_REQUIRED_MAJOR_VERSION}.{QEMU_MINIMUM_REQUIRED_MINOR_VERSION} or newer or use the Toolchain/BuildQemu.sh script."  # noqa: E501
        )
    version_information = run([config.qemu_binary, "-version"], capture_output=True, encoding="utf-8").stdout
    qemu_version_regex = re.compile(r"QEMU emulator version ([1-9][0-9]*|0)\.([1-9][0-9]*|0)")
    version_groups = qemu_version_regex.search(version_information)
    if version_groups is None:
        raise RunError(f'QEMU seems to be defective, its version information is "{version_information}"')
    major = int(version_groups.group(1))
    minor = int(version_groups.group(2))

    if major < QEMU_MINIMUM_REQUIRED_MAJOR_VERSION or (
        major == QEMU_MINIMUM_REQUIRED_MAJOR_VERSION and minor < QEMU_MINIMUM_REQUIRED_MINOR_VERSION
    ):
        raise RunError(
            f"Required QEMU >= {QEMU_MINIMUM_REQUIRED_MAJOR_VERSION}.{QEMU_MINIMUM_REQUIRED_MINOR_VERSION}!\
                Found {major}.{minor}. Please install a newer version of QEMU or use the Toolchain/BuildQemu.sh script."
        )


def set_up_virtualization_support(config: Configuration):
    provided_virtualization_enable = environ.get("SERENITY_VIRTUALIZATION_SUPPORT")
    # The user config always forces the platform-appropriate virtualizer to be used,
    # even if we couldn't detect it otherwise; this is intended behavior.
    if provided_virtualization_enable is not None:
        config.virtualization_support = provided_virtualization_enable == "1"
    elif host_arch_matches(config.architecture) and not config.machine_type.is_raspberry_pi():
        config.virtualization_support = (config.qemu_kind in [QEMUKind.NativeWindows, QEMUKind.MacOS]
                                         or kvm_usable())

    # FIXME: Booting with KVM on aarch64 is broken, so disable it for now.
    # FIXME: QEMU on Windows on ARM does not support WHPX yet
    if config.virtualization_support and config.qemu_kind != QEMUKind.MacOS and config.architecture == Arch.Aarch64:
        config.virtualization_support = False

    if config.virtualization_support and config.qemu_kind in [QEMUKind.NativeWindows, QEMUKind.MacOS]:
        available_accelerators = run(
            [str(config.qemu_binary), "-accel", "help"],
            capture_output=True,
        ).stdout
        # Check if HVF is actually available if we're on MacOS
        if config.qemu_kind == QEMUKind.MacOS and (b"hvf" not in available_accelerators):
            config.virtualization_support = False
        # Check if WHPX is actually available if we're on Windows
        if config.qemu_kind == QEMUKind.NativeWindows and (b"whpx" not in available_accelerators):
            config.virtualization_support = False


def set_up_basic_kernel_cmdline(config: Configuration):
    provided_cmdline = environ.get("SERENITY_KERNEL_CMDLINE")
    if provided_cmdline is not None:
        # Split environment variable at spaces, since we don't pass arguments like shell scripts do.
        config.kernel_cmdline.extend(provided_cmdline.split(sep=None))


def set_up_disk_image_path(config: Configuration):
    provided_disk_image = environ.get("SERENITY_DISK_IMAGE")
    if provided_disk_image is not None:
        config.disk_image = Path(provided_disk_image)
    else:
        if config.machine_type.uses_grub():
            config.disk_image = Path("grub_disk_image")
        elif config.machine_type == MachineType.Limine:
            config.disk_image = Path("limine_disk_image")
        elif config.machine_type == MachineType.QEMUExtLinux:
            config.disk_image = Path("extlinux_disk_image")

    if config.qemu_kind == QEMUKind.NativeWindows:
        config.disk_image = Path(
            run(["wslpath", "-w", config.disk_image], capture_output=True, encoding="utf-8").stdout.strip()
        )


def set_up_cpu(config: Configuration):
    if config.qemu_kind == QEMUKind.NativeWindows:
        # Setting -cpu to "max" breaks on QEMU 9 with WHPX whereas the default cpu is more broadly compatible
        config.qemu_cpu = None
    else:
        provided_cpu = environ.get("SERENITY_QEMU_CPU")
        if provided_cpu is not None:
            config.qemu_cpu = provided_cpu


def set_up_cpu_count(config: Configuration):
    if config.architecture != Arch.x86_64:
        return

    provided_cpu_count = environ.get("SERENITY_CPUS")
    if provided_cpu_count is not None:
        try:
            config.cpu_count = int(provided_cpu_count)
        except ValueError:
            raise RunError(f"Non-integer CPU count {provided_cpu_count}")

    if config.cpu_count is not None and config.qemu_cpu is not None and config.cpu_count <= 8:
        # -x2apic is not a flag, but disables x2APIC for easier testing on lower CPU counts.
        config.qemu_cpu += ",-x2apic"


def set_up_spice(config: Configuration):
    # Only the default machine has virtio-serial (required for the spice agent).
    if config.machine_type != MachineType.Default:
        return
    use_non_qemu_spice = environ.get("SERENITY_SPICE") == "1"
    chardev_info = run(
        [str(config.qemu_binary), "-chardev", "help"],
        capture_output=True,
        encoding="utf-8",
    ).stdout.lower()
    if use_non_qemu_spice and "spicevmc" in chardev_info:
        config.spice_arguments = ["-chardev", "spicevmc,id=vdagent,name=vdagent"]
    elif "qemu-vdagent" in chardev_info:
        config.extra_arguments.extend([
            "-chardev",
            "qemu-vdagent,clipboard=on,mouse=off,id=vdagent,name=vdagent",
        ])

    if use_non_qemu_spice and "spice" in chardev_info:
        config.spice_arguments.extend(["-spice", "port=5930,agent-mouse=off,disable-ticketing=on"])
    if "spice" in chardev_info or "vdagent" in chardev_info:
        config.extra_arguments.extend(["-device", "virtserialport,chardev=vdagent,nr=1"])


def set_up_audio_backend(config: Configuration):
    if config.qemu_kind == QEMUKind.MacOS:
        config.audio_backend = "coreaudio"
    elif config.qemu_kind == QEMUKind.NativeWindows:
        config.audio_backend = "dsound,timer-period=2000"
    elif config.machine_type != MachineType.CI:
        # FIXME: Use "-audiodev help" once that contains information on all our supported versions,
        # "-audio-help" is marked as deprecated.
        qemu_audio_help = run(
            [str(config.qemu_binary), "-audio-help"],
            capture_output=True,
            encoding="utf-8",
        ).stdout
        if qemu_audio_help == "":
            qemu_audio_help = run(
                [str(config.qemu_binary), "-audiodev", "help"],
                capture_output=True,
                encoding="utf-8",
            ).stdout
        if "sdl" in qemu_audio_help:
            config.audio_backend = "sdl"
        elif "pa" in qemu_audio_help:
            config.audio_backend = "pa,timer-period=2000"

    if config.audio_backend is not None:
        config.audio_backend += ",id=snd0"


def set_up_audio_hardware(config: Configuration):
    provided_audio_hardware = environ.get("SERENITY_AUDIO_HARDWARE", "intelhda")
    if provided_audio_hardware == "ac97":
        config.audio_devices = ["ac97,audiodev=snd0"]
    elif provided_audio_hardware == "intelhda":
        config.audio_devices = ["ich9-intel-hda", "hda-output,audiodev=snd0"]
    else:
        raise RunError(f"Unknown audio hardware {provided_audio_hardware}. Supported values: ac97, intelhda")

    if config.machine_type.supports_pc_speaker() and config.architecture == Arch.x86_64:
        config.extra_arguments.extend(["-machine", "pcspk-audiodev=snd0"])


def has_virgl() -> bool:
    try:
        ldconfig_result = run(["ldconfig", "-p"], capture_output=True, encoding="utf-8").stdout.lower()
        return "virglrenderer" in ldconfig_result
    except FileNotFoundError:
        print("Warning: ldconfig not found in PATH, assuming virgl support to not be present.")
        return False


def set_up_screens(config: Configuration):
    provided_screen_count_unparsed = environ.get("SERENITY_SCREENS", "1")
    try:
        config.screen_count = int(provided_screen_count_unparsed)
    except ValueError:
        raise RunError(f"Invalid screen count {provided_screen_count_unparsed}")

    provided_display_backend = environ.get("SERENITY_QEMU_DISPLAY_BACKEND")
    if provided_display_backend is not None:
        config.display_backend = provided_display_backend
    else:
        # The `gtk,gl=on` display backend appears to be broken on systems with NVIDIA graphics.
        # Check if `nvidia-smi` is installed; if it is, assume that NVIDIA drivers would be used.
        is_linux_with_nvidia_graphics = sys.platform == "linux" and which("nvidia-smi") is not None

        qemu_display_info = run(
            [str(config.qemu_binary), "-display", "help"],
            capture_output=True,
            encoding="utf-8",
        ).stdout.lower()
        if len(config.spice_arguments) != 0:
            config.display_backend = "spice-app"
        elif config.qemu_kind == QEMUKind.NativeWindows:
            # QEMU for windows does not like gl=on, so detect if we are building in wsl, and if so, disable it
            # Also, when using the GTK backend we run into this problem:
            # https://github.com/SerenityOS/serenity/issues/7657
            config.display_backend = "sdl,gl=off"
        elif config.screen_count > 1 and "sdl" in qemu_display_info:
            config.display_backend = "sdl,gl=off"
        elif "gtk" in qemu_display_info and has_virgl() and not is_linux_with_nvidia_graphics:
            config.display_backend = "gtk,gl=on"
        elif "sdl" in qemu_display_info and has_virgl():
            config.display_backend = "sdl,gl=on"
        elif "cocoa" in qemu_display_info:
            config.display_backend = "cocoa,gl=off"
        elif config.qemu_kind == QEMUKind.SerenityOS:
            config.display_backend = "sdl,gl=off"
        else:
            config.display_backend = "gtk,gl=off"

    # Disable "Zoom To Fit" because it breaks absolute mouse input and looks ugly.
    if config.display_backend.startswith("gtk"):
        config.display_backend += ",zoom-to-fit=off"


def set_up_display_device(config: Configuration):
    config.enable_gl = environ.get("SERENITY_GL") == "1"
    provided_display_device = environ.get("SERENITY_QEMU_DISPLAY_DEVICE")
    if provided_display_device is not None:
        config.display_device = provided_display_device
    elif config.enable_gl:
        # QEMU appears to not support the GL backend for VirtIO GPU variant on macOS.
        if config.qemu_kind == QEMUKind.MacOS:
            raise RunError("SERENITY_GL is not supported since there's no GL backend on macOS")
        elif config.screen_count > 1:
            raise RunError("SERENITY_GL and multi-monitor support cannot be set up simultaneously")
        config.display_device = "virtio-vga-gl"

    elif config.screen_count > 1:
        # QEMU appears to not support the virtio-vga VirtIO GPU variant on macOS.
        # To ensure we can still boot on macOS with VirtIO GPU, use the virtio-gpu-pci
        # variant, which lacks any VGA compatibility (which is not relevant for us anyway).
        if config.qemu_kind == QEMUKind.MacOS:
            config.display_device = f"virtio-gpu-pci,max_outputs={config.screen_count}"
        else:
            config.display_device = f"virtio-vga,max_outputs={config.screen_count}"

        # QEMU appears to always relay absolute mouse coordinates relative to the screen that the mouse is
        # pointed to, without any way for us to know what screen it was. So, when dealing with multiple
        # displays force using relative coordinates only.
        config.kernel_cmdline.append("vmmouse=off")


def set_up_boot_drive(config: Configuration):
    if config.machine_type.is_raspberry_pi():
        config.boot_drive = f"file={config.disk_image},if=sd,format=raw,id=boot-drive"
        return

    config.boot_drive = f"file={config.disk_image},if=none,format=raw,id=boot-drive"

    if config.boot_drive_type == BootDriveType.AHCI:
        config.add_devices(["ahci,id=boot-drive-ahci", "ide-hd,drive=boot-drive,bus=boot-drive-ahci.0"])
        config.kernel_cmdline.append("root=ahci0:0:0")
    if config.boot_drive_type == BootDriveType.NVMe:
        if config.architecture == Arch.x86_64:
            config.add_devices([
                "i82801b11-bridge,id=bridge4",
                "nvme,serial=deadbeef,drive=boot-drive,bus=bridge4,logical_block_size=4096,physical_block_size=4096",
            ])
            config.kernel_cmdline.append("root=nvme0:1:0")
        else:
            config.add_devices(["nvme,serial=deadbeef,drive=boot-drive"])
            config.kernel_cmdline.append("root=block3:0")
    elif config.boot_drive_type == BootDriveType.PCI_SD:
        config.add_devices(["sdhci-pci", "sd-card,drive=boot-drive"])
        config.kernel_cmdline.append("root=sd0:0:0")
    elif config.boot_drive_type == BootDriveType.USB_UHCI:
        config.add_device("piix4-usb-uhci,id=boot-drive-uhci")
        config.add_device("usb-storage,bus=boot-drive-uhci.0,drive=boot-drive")
        # FIXME: Find a better way to address the usb drive
        config.kernel_cmdline.append("root=block3:0")
    elif config.boot_drive_type == BootDriveType.USB_xHCI:
        config.add_device("qemu-xhci,id=boot-drive-xhci")
        config.add_device("usb-storage,bus=boot-drive-xhci.0,drive=boot-drive")
        # FIXME: Find a better way to address the usb drive
        config.kernel_cmdline.append("root=block3:0")
    elif config.boot_drive_type == BootDriveType.USB_UAS:
        config.add_device("qemu-xhci,id=boot-drive-xhci,p3=0")
        config.add_device("usb-uas,bus=boot-drive-xhci.0,id=boot-drive-uas,pcap=log.pcap")
        config.add_device("scsi-hd,bus=boot-drive-uas.0,scsi-id=0,lun=0,drive=boot-drive")
        # FIXME: Find a better way to address the usb drive
        config.kernel_cmdline.append("root=block3:0")
    elif config.boot_drive_type == BootDriveType.VirtIOBLK:
        config.add_device("virtio-blk-pci,drive=boot-drive")
        config.kernel_cmdline.append("root=lun2:0:0")


def determine_host_address() -> str:
    return environ.get("SERENITY_HOST_IP", "127.0.0.1")


def set_up_gdb(config: Configuration):
    config.enable_gdb = environ.get("SERENITY_DISABLE_GDB_SOCKET") != "1"
    if config.qemu_kind == QEMUKind.NativeWindows or (
        config.virtualization_support and config.qemu_kind == QEMUKind.MacOS
    ):
        config.enable_gdb = False

    if config.enable_gdb:
        config.extra_arguments.extend(["-gdb", f"tcp:{config.host_ip}:1234"])


def set_up_network_hardware(config: Configuration):
    config.packet_logging_arguments = (environ.get("SERENITY_PACKET_LOGGING_ARG", "")).split()

    provided_ethernet_device_type = environ.get("SERENITY_ETHERNET_DEVICE_TYPE")
    if provided_ethernet_device_type is not None:
        config.ethernet_device_type = provided_ethernet_device_type
    elif config.architecture in [Arch.Aarch64, Arch.RISCV64] and not config.machine_type.is_raspberry_pi():
        config.ethernet_device_type = "virtio-net-pci"

    if config.machine_type.is_raspberry_pi():
        config.network_backend = None
        config.network_default_device = None
    else:
        config.network_backend = f"user,id=breh,hostfwd=tcp:{config.host_ip}:8888-10.0.2.15:8888,\
hostfwd=tcp:{config.host_ip}:8823-10.0.2.15:23,\
hostfwd=tcp:{config.host_ip}:8000-10.0.2.15:8000,\
hostfwd=tcp:{config.host_ip}:2222-10.0.2.15:22"
        config.network_default_device = f"{config.ethernet_device_type},netdev=breh"


def set_up_kernel(config: Configuration):
    if config.architecture in [Arch.Aarch64, Arch.RISCV64]:
        config.kernel_and_initrd_arguments = ["-kernel", "Kernel/Kernel.bin"]
    elif config.architecture == Arch.x86_64:
        config.kernel_and_initrd_arguments = ["-kernel", "Kernel/Kernel"]


def set_up_machine_devices(config: Configuration):
    # TODO: Maybe disable SPICE everywhere except the default machine?

    if config.qemu_kind != QEMUKind.NativeWindows:
        config.extra_arguments.extend(["-qmp", "unix:qmp-sock,server,nowait"])

    config.extra_arguments.extend(["-name", "SerenityOS", "-d", "guest_errors"])

    # Machine/Architecture specifics.
    if config.machine_type.is_raspberry_pi():
        config.qemu_machine = config.machine_type.value
        config.cpu_count = None
        config.ram_size = None
        config.vga_type = None
        config.display_device = None
        config.kernel_cmdline.append("serial_debug")
        if config.machine_type != MachineType.CI:
            # FIXME: Windows QEMU crashes when we set the same display as usual here.
            config.display_backend = None
            config.audio_devices = []

            caches_path = BUILD_DIRECTORY.parent / "caches"
            dtb_path = str(caches_path / "bcm2710-rpi-3-b.dtb")
            if config.machine_type == MachineType.RaspberryPi4B:
                dtb_path = str(caches_path / "bcm2711-rpi-4-b.dtb")

            config.extra_arguments.extend(
                [
                    "-serial", "stdio",
                    "-dtb", dtb_path
                ]
            )
            config.qemu_cpu = None
            return

    elif config.architecture == Arch.Aarch64 or config.architecture == Arch.RISCV64:
        config.qemu_machine = "virt"
        config.cpu_count = None
        config.audio_devices = []
        config.extra_arguments.extend(["-serial", "stdio"])
        config.kernel_cmdline.extend(["serial_debug"])
        config.qemu_cpu = "max" if config.architecture == Arch.Aarch64 else None
        config.add_devices(
            [
                "virtio-keyboard",
                "virtio-tablet",
                "virtio-serial,max_ports=2",
            ]
        )
        return

    # Machine specific base setups
    if config.machine_type in [MachineType.QEMU35Grub, MachineType.QEMU35]:
        config.qemu_machine = "q35"
        config.vga_type = None
        # We set up our own custom display devices.
        config.display_device = None
        config.add_devices(
            [
                "isa-debugcon,chardev=stdout",
                "vmware-svga",
                "ich9-usb-ehci1,bus=pcie.0,multifunction=on,addr=0x05.3,multifunction=on,id=ehci1",
                "ich9-usb-uhci1,bus=pcie.0,multifunction=on,addr=0x05.0,masterbus=ehci1.0,firstport=0",
                "ich9-usb-uhci2,bus=pcie.0,multifunction=on,addr=0x05.1,masterbus=ehci1.0,firstport=2",
                "ich9-usb-uhci3,bus=pcie.0,multifunction=on,addr=0x05.2,masterbus=ehci1.0,firstport=4",
                "ich9-usb-ehci2,bus=pcie.0,multifunction=on,addr=0x07.3,multifunction=on,id=ehci2",
                "ich9-usb-uhci4,bus=pcie.0,multifunction=on,addr=0x07.0,masterbus=ehci2.0,firstport=0",
                "ich9-usb-uhci5,bus=pcie.0,multifunction=on,addr=0x07.1,masterbus=ehci2.0,firstport=2",
                "ich9-usb-uhci6,bus=pcie.0,multifunction=on,addr=0x07.2,masterbus=ehci2.0,firstport=4",
                "pcie-root-port,port=0x10,chassis=1,id=pcie.1,bus=pcie.0,multifunction=on,addr=0x6",
                "pcie-root-port,port=0x11,chassis=2,id=pcie.2,bus=pcie.0,addr=0x6.0x1",
                "pcie-root-port,port=0x12,chassis=3,id=pcie.3,bus=pcie.0,addr=0x6.0x2",
                "pcie-root-port,port=0x13,chassis=4,id=pcie.4,bus=pcie.0,addr=0x6.0x3",
                "pcie-root-port,port=0x14,chassis=5,id=pcie.5,bus=pcie.0,addr=0x6.0x4",
                "pcie-root-port,port=0x15,chassis=6,id=pcie.6,bus=pcie.0,addr=0x6.0x5",
                "pcie-root-port,port=0x16,chassis=7,id=pcie.7,bus=pcie.0,addr=0x6.0x6",
                "pcie-root-port,port=0x17,chassis=8,id=pcie.8,bus=pcie.0,addr=0x6.0x7",
                "ich9-intel-hda,bus=pcie.2,addr=0x03.0x0",
                "bochs-display",
                "nec-usb-xhci,bus=pcie.2,addr=0x11.0x0",
                "pci-bridge,chassis_nr=1,id=bridge1,bus=pcie.4,addr=0x3.0x0",
            ]
        )
        config.character_devices.append("stdio,id=stdout,mux=on")
        config.enable_usb = True

    elif config.machine_type == MachineType.CI:
        config.display_backend = "none"
        config.audio_backend = None
        config.audio_devices = []
        config.extra_arguments.extend(["-serial", "stdio", "-no-reboot", "-monitor", "none"])
        config.spice_arguments = []
        if config.architecture == Arch.Aarch64:
            config.extra_arguments.extend(["-serial", "file:debug.log"])
        else:
            config.add_device("ich9-ahci")
            config.extra_arguments.extend(["-debugcon", "file:debug.log"])

    else:
        # Default machine
        config.network_default_device = f"{config.network_default_device},bus=bridge1"
        config.add_devices(
            [
                "virtio-serial,max_ports=2",
                "virtconsole,chardev=stdout",
                "isa-debugcon,chardev=stdout",
                "virtio-rng-pci",
                "pci-bridge,chassis_nr=1,id=bridge1",
                "i82801b11-bridge,bus=bridge1,id=bridge2",
                "i82801b11-bridge,id=bridge3",
                "ich9-ahci,bus=bridge3",
            ]
        )
        config.character_devices.append("stdio,id=stdout,mux=on")
        config.enable_usb = True

    # Modifications for machine types that are *mostly* like the default,
    # but not entirely (especially in terms of networking).
    if config.machine_type in [MachineType.QEMUWithoutNetwork, MachineType.QEMU35Grub]:
        config.network_backend = None
        config.network_default_device = config.ethernet_device_type
        config.packet_logging_arguments = []
    elif config.machine_type == MachineType.QEMUTap:
        config.network_backend = "tap,ifname=tap0,id=br0"
        config.network_default_device = f"{config.ethernet_device_type},netdev=br0"
    elif config.machine_type in [MachineType.QEMUGrub, MachineType.QEMUExtLinux]:
        config.kernel_cmdline = []


def assemble_arguments(config: Configuration) -> list[str | Path]:
    passed_qemu_args = shlex.split(environ.get("SERENITY_EXTRA_QEMU_ARGS", ""))

    return [
        config.qemu_binary or "",
        # Deviate from standard order here:
        # The device list contains PCI bridges which must be available for other devices.
        *config.device_arguments,
        *config.kernel_and_initrd_arguments,
        *config.packet_logging_arguments,
        *config.spice_arguments,
        *config.extra_arguments,
        *config.accelerator_arguments,
        *config.kernel_cmdline_arguments,
        *config.ram_arguments,
        *config.cpu_arguments,
        *config.smp_arguments,
        *config.machine_arguments,
        *config.usb_arguments,
        *config.audio_backend_arguments,
        *config.audio_devices_arguments,
        *config.vga_arguments,
        *config.display_backend_arguments,
        *config.display_device_arguments,
        *config.network_backend_arguments,
        *config.network_default_arguments,
        *config.boot_drive_arguments,
        *config.character_device_arguments,
        *passed_qemu_args,
    ]


class TapController:
    """Context manager for setting up and tearing down a tap device when QEMU is run with tap networking."""

    def __init__(self, machine_type: MachineType):
        self.should_enable_tap = machine_type == MachineType.QEMUTap

    def __enter__(self) -> None:
        if self.should_enable_tap:
            run(["sudo", "ip", "tuntap", "del", "dev", "tap0", "mode", "tap"])
            user = os.getuid()
            run(["sudo", "ip", "tuntap", "add", "dev", "tap0", "mode", "tap", "user", str(user)])

    def __exit__(self, exc_type: type | None, exc_value: Any | None, traceback: Any | None) -> Literal[False]:
        if self.should_enable_tap:
            run(["sudo", "ip", "tuntap", "del", "dev", "tap0", "mode", "tap"])
        # Re-raise exceptions in any case.
        return False


def configure_and_run():
    config = Configuration()
    config.kvm_usable = kvm_usable()
    config.qemu_kind = determine_qemu_kind()
    config.architecture = determine_serenity_arch()
    config.machine_type = determine_machine_type()
    config.ram_size = detect_ram_size()
    config.host_ip = determine_host_address()
    config.boot_drive_type = determine_boot_drive_type()

    serenity_src = environ.get("SERENITY_SOURCE_DIR")
    if serenity_src is None:
        raise RunError("SERENITY_SOURCE_DIR not set or empty")
    config.serenity_src = Path(serenity_src)

    set_up_qemu_binary(config)
    check_qemu_version(config)
    set_up_virtualization_support(config)
    set_up_basic_kernel_cmdline(config)
    set_up_disk_image_path(config)
    set_up_cpu(config)
    set_up_cpu_count(config)
    set_up_spice(config)
    set_up_audio_backend(config)
    set_up_audio_hardware(config)
    set_up_screens(config)
    set_up_display_device(config)
    set_up_boot_drive(config)
    set_up_gdb(config)
    set_up_network_hardware(config)
    set_up_kernel(config)
    set_up_machine_devices(config)

    arguments = assemble_arguments(config)

    os.chdir(BUILD_DIRECTORY)

    with TapController(config.machine_type):
        run(arguments)


def main():
    try:
        configure_and_run()
    except KeyboardInterrupt:
        pass
    except RunError as e:
        print(f"Error: {e}")
    except Exception as e:
        print(f"Unknown error: {e}")
        print("This is likely a bug, consider filing a bug report.")


if __name__ == "__main__":
    main()
