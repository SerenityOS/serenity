/*
 * Copyright (c) 2024, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/API/Ioctl.h>
#include <Kernel/Devices/DeviceManagement.h>
#include <Kernel/Devices/Generic/NetworkDeviceControlDevice.h>
#include <Kernel/Net/ARP.h>
#include <Kernel/Net/ICMP.h>
#include <Kernel/Net/NetworkAdapter.h>
#include <Kernel/Net/NetworkingManagement.h>
#include <Kernel/Net/Routing.h>

namespace Kernel {

UNMAP_AFTER_INIT NonnullLockRefPtr<NetworkDeviceControlDevice> NetworkDeviceControlDevice::must_create()
{
    auto network_device_control_device_or_error = DeviceManagement::try_create_device<NetworkDeviceControlDevice>();
    // FIXME: Find a way to propagate errors
    VERIFY(!network_device_control_device_or_error.is_error());
    return network_device_control_device_or_error.release_value();
}

bool NetworkDeviceControlDevice::can_read(OpenFileDescription const&, u64) const
{
    return true;
}

UNMAP_AFTER_INIT NetworkDeviceControlDevice::NetworkDeviceControlDevice()
    : CharacterDevice(2, 11)
{
}

UNMAP_AFTER_INIT NetworkDeviceControlDevice::~NetworkDeviceControlDevice() = default;

ErrorOr<size_t> NetworkDeviceControlDevice::read(OpenFileDescription&, u64, UserOrKernelBuffer&, size_t)
{
    return 0;
}

ErrorOr<void> NetworkDeviceControlDevice::ioctl(OpenFileDescription&, unsigned request, Userspace<void*> arg)
{
    auto ioctl_route = [request, arg]() -> ErrorOr<void> {
        auto user_route = static_ptr_cast<rtentry*>(arg);
        rtentry route;
        TRY(copy_from_user(&route, user_route));

        Userspace<char const*> user_rt_dev((FlatPtr)route.rt_dev);
        auto ifname = TRY(Process::get_syscall_name_string_fixed_buffer<IFNAMSIZ>(user_rt_dev));
        auto adapter = TRY(NetworkingManagement::the().lookup_by_name(ifname.representable_view()));

        switch (request) {
        case SIOCADDRT: {
            auto current_process_credentials = Process::current().credentials();
            if (!current_process_credentials->is_superuser())
                return EPERM;
            if (route.rt_gateway.sa_family != AF_INET)
                return EAFNOSUPPORT;
            if (!(route.rt_flags & RTF_UP))
                return EINVAL; // FIXME: Find the correct value to return

            auto destination = IPv4Address(((sockaddr_in&)route.rt_dst).sin_addr.s_addr);
            auto gateway = IPv4Address(((sockaddr_in&)route.rt_gateway).sin_addr.s_addr);
            auto genmask = IPv4Address(((sockaddr_in&)route.rt_genmask).sin_addr.s_addr);

            return update_routing_table(destination, gateway, genmask, route.rt_flags, adapter, UpdateTable::Set);
        }
        case SIOCDELRT:
            auto current_process_credentials = Process::current().credentials();
            if (!current_process_credentials->is_superuser())
                return EPERM;
            if (route.rt_gateway.sa_family != AF_INET)
                return EAFNOSUPPORT;

            auto destination = IPv4Address(((sockaddr_in&)route.rt_dst).sin_addr.s_addr);
            auto gateway = IPv4Address(((sockaddr_in&)route.rt_gateway).sin_addr.s_addr);
            auto genmask = IPv4Address(((sockaddr_in&)route.rt_genmask).sin_addr.s_addr);

            return update_routing_table(destination, gateway, genmask, route.rt_flags, adapter, UpdateTable::Delete);
        }

        return EINVAL;
    };

    auto ioctl_arp = [request, arg]() -> ErrorOr<void> {
        auto user_req = static_ptr_cast<arpreq*>(arg);
        arpreq arp_req;
        TRY(copy_from_user(&arp_req, user_req));

        auto current_process_credentials = Process::current().credentials();

        switch (request) {
        case SIOCSARP:
            if (!current_process_credentials->is_superuser())
                return EPERM;
            if (arp_req.arp_pa.sa_family != AF_INET)
                return EAFNOSUPPORT;
            update_arp_table(IPv4Address(((sockaddr_in&)arp_req.arp_pa).sin_addr.s_addr), *(MACAddress*)&arp_req.arp_ha.sa_data[0], UpdateTable::Set);
            return {};

        case SIOCDARP:
            if (!current_process_credentials->is_superuser())
                return EPERM;
            if (arp_req.arp_pa.sa_family != AF_INET)
                return EAFNOSUPPORT;
            update_arp_table(IPv4Address(((sockaddr_in&)arp_req.arp_pa).sin_addr.s_addr), *(MACAddress*)&arp_req.arp_ha.sa_data[0], UpdateTable::Delete);
            return {};
        }

        return EINVAL;
    };

    auto ioctl_interface = [request, arg]() -> ErrorOr<void> {
        auto user_ifr = static_ptr_cast<ifreq*>(arg);
        ifreq ifr;
        TRY(copy_from_user(&ifr, user_ifr));

        if (request == SIOCGIFNAME) {
            if (ifr.ifr_index == 0)
                return EINVAL;

            auto adapter = TRY(NetworkingManagement::the().lookup_by_index(ifr.ifr_index));
            auto succ = adapter->name().copy_characters_to_buffer(ifr.ifr_name, IFNAMSIZ);
            if (!succ) {
                return EFAULT;
            }
            return copy_to_user(user_ifr, &ifr);
        }

        char namebuf[IFNAMSIZ + 1];
        memcpy(namebuf, ifr.ifr_name, IFNAMSIZ);
        namebuf[sizeof(namebuf) - 1] = '\0';

        if (request == SIOCGIFINDEX) {
            auto adapter = TRY(NetworkingManagement::the().lookup_by_name({ namebuf, strlen(namebuf) }));
            ifr.ifr_index = adapter->index().value();
            return copy_to_user(user_ifr, &ifr);
        }

        auto adapter = TRY(NetworkingManagement::the().lookup_by_name({ namebuf, strlen(namebuf) }));

        auto current_process_credentials = Process::current().credentials();

        switch (request) {
        case SIOCSIFADDR:
            if (!current_process_credentials->is_superuser())
                return EPERM;
            if (ifr.ifr_addr.sa_family != AF_INET)
                return EAFNOSUPPORT;
            adapter->set_ipv4_address(IPv4Address(((sockaddr_in&)ifr.ifr_addr).sin_addr.s_addr));
            return {};

        case SIOCSIFNETMASK:
            if (!current_process_credentials->is_superuser())
                return EPERM;
            if (ifr.ifr_addr.sa_family != AF_INET)
                return EAFNOSUPPORT;
            adapter->set_ipv4_netmask(IPv4Address(((sockaddr_in&)ifr.ifr_netmask).sin_addr.s_addr));
            return {};

        case SIOCGIFADDR: {
            auto ip4_addr = adapter->ipv4_address().to_u32();
            auto& socket_address_in = reinterpret_cast<sockaddr_in&>(ifr.ifr_addr);
            socket_address_in.sin_family = AF_INET;
            socket_address_in.sin_addr.s_addr = ip4_addr;
            return copy_to_user(user_ifr, &ifr);
        }

        case SIOCGIFNETMASK: {
            auto ip4_netmask = adapter->ipv4_netmask().to_u32();
            auto& socket_address_in = reinterpret_cast<sockaddr_in&>(ifr.ifr_addr);
            socket_address_in.sin_family = AF_INET;
            // NOTE: NOT ifr_netmask.
            socket_address_in.sin_addr.s_addr = ip4_netmask;

            return copy_to_user(user_ifr, &ifr);
        }

        case SIOCGIFHWADDR: {
            auto mac_address = adapter->mac_address();
            switch (adapter->adapter_type()) {
            case NetworkAdapter::Type::Loopback:
                ifr.ifr_hwaddr.sa_family = ARPHRD_LOOPBACK;
                break;
            case NetworkAdapter::Type::Ethernet:
                ifr.ifr_hwaddr.sa_family = ARPHRD_ETHER;
                break;
            default:
                VERIFY_NOT_REACHED();
            }
            mac_address.copy_to(Bytes { ifr.ifr_hwaddr.sa_data, sizeof(ifr.ifr_hwaddr.sa_data) });
            return copy_to_user(user_ifr, &ifr);
        }

        case SIOCGIFBRDADDR: {
            // Broadcast address is basically the reverse of the netmask, i.e.
            // instead of zeroing out the end, you OR with 1 instead.
            auto ip4_netmask = adapter->ipv4_netmask().to_u32();
            auto broadcast_addr = adapter->ipv4_address().to_u32() | ~ip4_netmask;
            auto& socket_address_in = reinterpret_cast<sockaddr_in&>(ifr.ifr_addr);
            socket_address_in.sin_family = AF_INET;
            socket_address_in.sin_addr.s_addr = broadcast_addr;
            return copy_to_user(user_ifr, &ifr);
        }

        case SIOCGIFMTU: {
            auto ip4_metric = adapter->mtu();

            ifr.ifr_addr.sa_family = AF_INET;
            ifr.ifr_metric = ip4_metric;
            return copy_to_user(user_ifr, &ifr);
        }

        case SIOCGIFFLAGS: {
            // FIXME: stub!
            constexpr short flags = 1;
            ifr.ifr_addr.sa_family = AF_INET;
            ifr.ifr_flags = flags;
            return copy_to_user(user_ifr, &ifr);
        }

        case SIOCGIFCONF: {
            // FIXME: stub!
            return EINVAL;
        }
        }

        return EINVAL;
    };

    switch (request) {

    case SIOCSIFADDR:
    case SIOCSIFNETMASK:
    case SIOCGIFADDR:
    case SIOCGIFHWADDR:
    case SIOCGIFNETMASK:
    case SIOCGIFBRDADDR:
    case SIOCGIFMTU:
    case SIOCGIFFLAGS:
    case SIOCGIFCONF:
    case SIOCGIFNAME:
    case SIOCGIFINDEX:
        return ioctl_interface();

    case SIOCADDRT:
    case SIOCDELRT:
        return ioctl_route();

    case SIOCSARP:
    case SIOCDARP:
        return ioctl_arp();

    default:
        return Error::from_errno(EINVAL);
    };
}

}
