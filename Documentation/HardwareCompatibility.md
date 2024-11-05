## SerenityOS Hardware Compatibility List

A list of hardware known to be at least partly working with SerenityOS.

Serenity boots to a graphical desktop on all machines unless otherwise noted.

### Network Adapters

| Model                                  | Notes                  |
| -------------------------------------- | ---------------------- |
| Intel 82545XX                          | Also known as e1000    |
| Intel 82574L                           | Also known as e1000e   |
| RTL8168/8111 (Variants B, E, E-VL & H) | Other variants are WIP |

### Desktop machines

| Make and model | Notes                   |
| -------------- | ----------------------- |
| Viglen VM3B    | Has onboard RTL8139 NIC |

### Laptops, notebooks and netbooks

| Make and model        | Notes                                                                                                                                                                                                            |
| --------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Dell Inspiron mini 10 | ICH7-M SATA controller works in IDE mode, RTL810xE NIC and Intel NM10 audio unsupported                                                                                                                          |
| HP 15-ac108na         | Pentium 3825U, Wildcat Point Chipset. AHCI works. Keyboard and trackpad work (both are PS2), trackpad scrolling/gestures don't work. RTL810xE NIC, RTL8723BE Wireless NIC, xHCI and Intel HDA audio unsupported. |
| Lenovo Ideapad 510s   | Sunrise Point Chipset. AHCI works. Keyboard and trackpad work (both are PS2), RTL810xE NIC, Intel 3165 Wireless NIC, USB and Intel HDA audio unsupported.                                                        |

### Motherboards

| Make and model                        | Notes                                                 |
| ------------------------------------- | ----------------------------------------------------- |
| Intel Desktop Board D915GAG / D915PSY | Pentium 4 HT CPU                                      |
| Intel Desktop Board D875PBZ           | Pentium 4 HT CPU                                      |
| Gigabyte G31M-ES2L                    | ICH7 2009 machine with IDE controller only            |
| Asus PRIME B360-PLUS                  | Has only one PS2 port, AHCI works                     |
| Asus H81M-K                           | w/ Intel Core i3-4170, AHCI works                     |
| Acer OEM Motherboard, H57H-AM2        | w/ Intel Core i5-760, AHCI work (graphics not tested) |
