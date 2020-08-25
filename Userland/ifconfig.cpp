/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/JsonArray.h>
#include <AK/JsonObject.h>
#include <AK/NumberFormat.h>
#include <AK/String.h>
#include <AK/Types.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <net/if.h>
#include <net/route.h>
#include <netinet/in.h>
#include <stdio.h>
#include <string.h>
#include <sys/ioctl.h>
#include <sys/socket.h>

int main(int argc, char** argv)
{
    const char* value_ipv4 = nullptr;
    const char* value_adapter = nullptr;
    const char* value_gateway = nullptr;
    const char* value_mask = nullptr;

    Core::ArgsParser args_parser;
    args_parser.add_option(value_ipv4, "Set the IP address of the selected network", "ipv4", 'i', "The new IP of the network");
    args_parser.add_option(value_adapter, "Select a specific network adapter to configure", "adapter", 'a', "The name of a network adapter");
    args_parser.add_option(value_gateway, "Set the default gateway of the selected network", "gateway", 'g', "The new IP of the gateway");
    args_parser.add_option(value_mask, "Set the network mask of the selected network", "mask", 'm', "The new network mask");
    args_parser.parse(argc, argv);

    if (!value_ipv4 && !value_adapter && !value_gateway && !value_mask) {

        auto file = Core::File::construct("/proc/net/adapters");
        if (!file->open(Core::IODevice::ReadOnly)) {
            fprintf(stderr, "Error: %s\n", file->error_string());
            return 1;
        }

        auto file_contents = file->read_all();
        auto json = JsonValue::from_string(file_contents);
        ASSERT(json.has_value());
        json.value().as_array().for_each([](auto& value) {
            auto if_object = value.as_object();

            auto name = if_object.get("name").to_string();
            auto class_name = if_object.get("class_name").to_string();
            auto mac_address = if_object.get("mac_address").to_string();
            auto ipv4_address = if_object.get("ipv4_address").to_string();
            auto gateway = if_object.get("ipv4_gateway").to_string();
            auto netmask = if_object.get("ipv4_netmask").to_string();
            auto packets_in = if_object.get("packets_in").to_u32();
            auto bytes_in = if_object.get("bytes_in").to_u32();
            auto packets_out = if_object.get("packets_out").to_u32();
            auto bytes_out = if_object.get("bytes_out").to_u32();
            auto mtu = if_object.get("mtu").to_u32();

            printf("%s:\n", name.characters());
            printf("\tmac: %s\n", mac_address.characters());
            printf("\tipv4: %s\n", ipv4_address.characters());
            printf("\tnetmask: %s\n", netmask.characters());
            printf("\tgateway: %s\n", gateway.characters());
            printf("\tclass: %s\n", class_name.characters());
            printf("\tRX: %u packets %u bytes (%s)\n", packets_in, bytes_in, human_readable_size(bytes_in).characters());
            printf("\tTX: %u packets %u bytes (%s)\n", packets_out, bytes_out, human_readable_size(bytes_out).characters());
            printf("\tMTU: %u\n", mtu);
            printf("\n");
        });
    } else {

        if (!value_adapter) {
            fprintf(stderr, "No network adapter was specified.\n");
            return 1;
        }

        String ifname = value_adapter;

        if (value_ipv4) {
            auto address = IPv4Address::from_string(value_ipv4);

            if (!address.has_value()) {
                fprintf(stderr, "Invalid IPv4 address: '%s'\n", value_ipv4);
                return 1;
            }

            int fd = socket(AF_INET, SOCK_DGRAM, IPPROTO_IP);
            if (fd < 0) {
                perror("socket");
                return 1;
            }

            struct ifreq ifr;
            memset(&ifr, 0, sizeof(ifr));

            bool fits = ifname.copy_characters_to_buffer(ifr.ifr_name, IFNAMSIZ);
            if (!fits) {
                fprintf(stderr, "Interface name '%s' is too long\n", ifname.characters());
                return 1;
            }
            ifr.ifr_addr.sa_family = AF_INET;
            ((sockaddr_in&)ifr.ifr_addr).sin_addr.s_addr = address.value().to_in_addr_t();

            int rc = ioctl(fd, SIOCSIFADDR, &ifr);
            if (rc < 0) {
                perror("ioctl(SIOCSIFADDR)");
                return 1;
            }
        }

        if (value_mask) {
            auto address = IPv4Address::from_string(value_mask);

            if (!address.has_value()) {
                fprintf(stderr, "Invalid IPv4 mask: '%s'\n", value_mask);
                return 1;
            }

            int fd = socket(AF_INET, SOCK_DGRAM, IPPROTO_IP);
            if (fd < 0) {
                perror("socket");
                return 1;
            }

            struct ifreq ifr;
            memset(&ifr, 0, sizeof(ifr));

            bool fits = ifname.copy_characters_to_buffer(ifr.ifr_name, IFNAMSIZ);
            if (!fits) {
                fprintf(stderr, "Interface name '%s' is too long\n", ifname.characters());
                return 1;
            }
            ifr.ifr_netmask.sa_family = AF_INET;
            ((sockaddr_in&)ifr.ifr_netmask).sin_addr.s_addr = address.value().to_in_addr_t();

            int rc = ioctl(fd, SIOCSIFNETMASK, &ifr);
            if (rc < 0) {
                perror("ioctl(SIOCSIFNETMASK)");
                return 1;
            }
        }

        if (value_gateway) {
            auto address = IPv4Address::from_string(value_gateway);

            int fd = socket(AF_INET, SOCK_DGRAM, IPPROTO_IP);
            if (fd < 0) {
                perror("socket");
                return 1;
            }

            struct rtentry rt;
            memset(&rt, 0, sizeof(rt));

            rt.rt_dev = const_cast<char*>(ifname.characters());
            rt.rt_gateway.sa_family = AF_INET;
            ((sockaddr_in&)rt.rt_gateway).sin_addr.s_addr = address.value().to_in_addr_t();
            rt.rt_flags = RTF_UP | RTF_GATEWAY;

            int rc = ioctl(fd, SIOCADDRT, &rt);
            if (rc < 0) {
                perror("ioctl(SIOCADDRT)");
                return 1;
            }
        }
    }
    return 0;
}
