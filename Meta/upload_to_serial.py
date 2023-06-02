import serial
import sys
from time import perf_counter

if len(sys.argv) != 3:
    print(f"Usage: {sys.argv[0]} PAYLOAD SERIAL_DEVICE")
    exit(1)

# Most common issue (not checked in serenity.sh) is that the serial port is invalid.
# Check this before anything else.
try:
    # FIXME: Use a higher clock rate than this.
    ser = serial.Serial(sys.argv[2], 115_200, timeout=20)
except serial.SerialException:
    print(f"'{sys.argv[2]}' could not be opened as a serial port.")
    exit(1)

with open(sys.argv[1], "rb") as f:
    content = f.read()
print(f"Payload size: {len(content)} bytes")

print("Waiting for Prekernel to be ready...")
# Bootloader header is SPK\x03 ("SerenityOS Prekernel" and a random byte to avoid accidental matches).
bootloader_header = b"SPK\x03"
bootloader_bytes = bytes()
# Allow the bootloader to send other initialization data.
# NOTE: For testing against a QEMU VM, this check needs to be removed.
while bootloader_bytes != bootloader_header:
    bootloader_bytes += ser.read(1)
    if not bootloader_header.startswith(bootloader_bytes):
        bootloader_bytes = bytes()

print("Sending payload length to Prekernel...")
ser.write(len(content).to_bytes(4, byteorder="little"))

print("Waiting for OK from Prekernel...")
bootloader_bytes = ser.read(2)
if bootloader_bytes != b"OK":
    print(f"Whoops, Prekernel said '{bootloader_bytes}'")
    exit(0)

print("Sending payload to Prekernel: \33[s", end="", flush=True)
blocksize = 2**14
current_block = 0
start_time = perf_counter()
last_write_time = perf_counter()
while current_block * blocksize < len(content):
    ser.write(content[current_block * blocksize: (current_block + 1) * blocksize])
    rate = blocksize / (perf_counter() - last_write_time)
    last_write_time = perf_counter()
    percentage = (blocksize * current_block / len(content)) * 100
    print(f"\33[u{percentage:6.2f}% ({rate:.2f} B/s)", end="", flush=True)
    current_block += 1

total_time = perf_counter() - start_time

print(
    f"\nSuccesfully send payload to Prekernel in {total_time:.1f}s. Kernel serial output below:"
)

print(
    "--------------------------------------------------------------------------------"
)

while True:
    print(ser.readline().decode("utf-8", errors="replace"), end="")
