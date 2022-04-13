#!/bin/bash
# This script has been tested only on Ubuntu 21.10!
set -e

die() {
    >&2 echo "die: $*"
    exit 1
}

usage() {
  echo "Usage: $0 create-bridge ETH_IFACE"
  echo "       $0 delete-bridge"
}

ACTION="$1"

SERENITY_BRIDGE_NAME="${SERENITY_BRIDGE_NAME:-serenitybr0}"
SERENITY_TAP_NAME="${SERENITY_TAP_NAME:-serenitytap0}"

delete_bridge() {
  if ip link show "$SERENITY_BRIDGE_NAME" type bridge 1>/dev/null 2>&1; then
    # Bridge exists, delete it first
    sudo ip link set "$SERENITY_BRIDGE_NAME" down
    sudo ip link delete dev "$SERENITY_BRIDGE_NAME"
  fi
}

delete_tap() {
  if ip tuntap list | grep -c "^$SERENITY_TAP_NAME:" > /dev/null; then
    # TAP interface exists, delete it first
    sudo ip link set dev "$SERENITY_TAP_NAME" down
    sudo ip link delete dev "$SERENITY_TAP_NAME"
  fi
}

update_systemd_resolvd() {
  local COPY_FROM_INTERFACE="$1"
  local COPY_TO_INTERFACE="$2"
  local ACTION_BETWEEN="$3"
  local DNS_CONFIG=`systemd-resolve --status | sed -e "1,/^Link .*\($COPY_FROM_INTERFACE\)/d" | awk '/^$/{exit}1'`
  local DNS_DOMAIN=`echo "$DNS_CONFIG" | sed -n 's/DNS Domain\://p' | awk '$1=$1'`
  local DNS_SERVERS=`echo "$DNS_CONFIG" | sed -n 's/DNS Servers\://p' | awk '{for(i=1;i<=NF;i++) print $i}'`
  if [ "$ACTION_BETWEEN" = "delete-bridge" ]; then
    delete_bridge
  fi
  if [ "$DNS_DOMAIN" != "" ]; then
    if ! sudo systemd-resolve -i "$COPY_TO_INTERFACE" "--set-domain=$DNS_DOMAIN"; then
      die "systemd-resolve failed to set domain for $COPY_TO_INTERFACE to $DNS_DOMAIN"
    fi
  fi
  local DNS_ADDR_ARGS=()
  local DNS_ADDR=""
  for line in $DNS_SERVERS; do
    DNS_ADDR=`echo "$line" | cut -f1 -d'%'`
    DNS_ADDR_ARGS+=( "--set-dns=$DNS_ADDR" )
  done
  if [ "${#DNS_ADDR_ARGS[@]}" -gt 0 ]; then
    if ! sudo systemd-resolve -i "$COPY_TO_INTERFACE" "${DNS_ADDR_ARGS[@]}"; then
      die "systemd-resolve failed to set dns for $COPY_TO_INTERFACE to $DNS_ADDR"
    fi
  fi
}

if [ "$ACTION" = "create-bridge" ]; then
  SERENITY_BRIDGE="$2"
  if [ "$SERENITY_BRIDGE" != "" ]; then
    if ! ip link show "$SERENITY_BRIDGE" 1>/dev/null 2>&1; then
      die "Should specify a physical ethernet interface to bridge!"
    fi
    ETH_INET_ADDRS=`ip addr show "$SERENITY_BRIDGE" | grep "^\s*inet " | awk -F ' ' '{print $2, $4}'`
    ETH_INET_DEF_ROUTE=`route -n | grep "^0\.0\.0\.0 .* $SERENITY_BRIDGE\$" || true`

    delete_bridge
    if ! sudo ip link add "$SERENITY_BRIDGE_NAME" type bridge; then
      die "Failed to create bridge $SERENITY_BRIDGE_NAME"
    fi
    if ! sudo ip link set "$SERENITY_BRIDGE_NAME" type bridge stp_state 1; then
      die "Failed to enable stp on bridge $SERENITY_BRIDGE_NAME"
    fi
    if ! sudo ip link set "$SERENITY_BRIDGE_NAME" up; then
      die "Failed to bring up bridge $SERENITY_BRIDGE_NAME"
    fi
    if ! sudo ip addr flush dev "$SERENITY_BRIDGE"; then
      die "Failed to flush ip addr of $SERENITY_BRIDGE"
    fi
    if ! sudo ip link set "$SERENITY_BRIDGE" master "$SERENITY_BRIDGE_NAME"; then
      die "Failed to set $SERENITY_BRIDGE master of bridge $SERENITY_BRIDGE_NAME"
    fi
    echo "$ETH_INET_ADDRS" | while IFS= read -r line; do
      IP_ADDR=`echo "$line" | awk '{print $1}'`
      BROADCAST_ADDR=`echo "$line" | awk '{print $2}'`
      if ! sudo ip addr add "$IP_ADDR" broadcast "$BROADCAST_ADDR" dev "$SERENITY_BRIDGE_NAME"; then
        die "Failed to set bridge $SERENITY_BRIDGE_NAME address $IP_ADDR"
      fi
    done
    if [ "$ETH_INET_DEF_ROUTE" != "" ]; then
      ROUTE_TARGET=`echo "$ETH_INET_DEF_ROUTE" | awk '{print $2}'`
      if ! sudo ip route add default via "$ROUTE_TARGET"; then
        die "Failed to set default route to bridge $SERENITY_BRIDGE_NAME"
      fi
    fi

    delete_tap
    if ! sudo ip tuntap add dev "$SERENITY_TAP_NAME" mode tap user "$USER"; then
      die "Failed to create tap device $SERENITY_TAP_NAME"
    fi
    if ! sudo ip link set dev "$SERENITY_TAP_NAME" up; then
      die "Failed to bring up tap device $SERENITY_TAP_NAME"
    fi
    if ! sudo ip link set "$SERENITY_TAP_NAME" master "$SERENITY_BRIDGE_NAME"; then
      die "Failed to set $SERENITY_TAP_NAME master of bridge $SERENITY_BRIDGE_NAME"
    fi
  else
    die "No ethernet interface specified"
  fi

  update_systemd_resolvd "$SERENITY_BRIDGE" "$SERENITY_BRIDGE_NAME"

  echo "Bridge device $SERENITY_BRIDGE_NAME bridging $SERENITY_BRIDGE and $SERENITY_TAP_NAME was created."
  echo "To run SerenityOS with this bridged network, set the SERENITY_NETDEV_TAP environment variable:"
  echo "   export SERENITY_NETDEV_TAP=$SERENITY_TAP_NAME"
  echo "Please note that DNS resolution may be broken for up to a minute!"
elif [ "$ACTION" = "delete-bridge" ]; then
  # Delete the TAP device first, which allows us to then assume that whatever is left in the bridge
  # is the ethernet device that we need to copy the ip configuration back to
  delete_tap

  if ip link show "$SERENITY_BRIDGE_NAME" type bridge 1>/dev/null 2>&1; then
    BRIDGE_INET_ADDRS=`ip addr show "$SERENITY_BRIDGE_NAME" | grep "^\s*inet " | awk -F ' ' '{print $2, $4}'`
    BRIDGE_INET_DEF_ROUTE=`route -n | grep "^0\.0\.0\.0 .* $SERENITY_BRIDGE_NAME\$" || true`
    BRIDGED_DEVICES=`ip link show master $SERENITY_BRIDGE_NAME | grep -v "^\s" | cut -d':' -f2 | awk '$1=$1'`

    if ! sudo ip addr flush dev "$SERENITY_BRIDGE_NAME"; then
      die "Failed to flush ip addr of $SERENITY_BRIDGE_NAME"
    fi

    for BRIDGED_ETH_DEV in "$BRIDGED_DEVICES"; do
      echo "$BRIDGE_INET_ADDRS" | while IFS= read -r line; do
        IP_ADDR=`echo "$line" | awk '{print $1}'`
        BROADCAST_ADDR=`echo "$line" | awk '{print $2}'`
        if ! sudo ip addr add "$IP_ADDR" broadcast "$BROADCAST_ADDR" dev "$BRIDGED_ETH_DEV"; then
          die "Failed to set bridge $BRIDGED_ETH_DEV address $IP_ADDR"
        fi
      done
      if [ "$BRIDGE_INET_DEF_ROUTE" != "" ]; then
        ROUTE_TARGET=`echo "$BRIDGE_INET_DEF_ROUTE" | awk '{print $2}'`
        if ! sudo ip route add default via "$ROUTE_TARGET"; then
          die "Failed to set default route to bridge $SERENITY_BRIDGE_NAME"
        fi
      fi

      update_systemd_resolvd "$SERENITY_BRIDGE_NAME" "$BRIDGED_ETH_DEV" "delete-bridge"

      echo "Bridge device $SERENITY_BRIDGE_NAME was deleted."
      echo "You may have to take down and bring back up the $BRIDGED_ETH_DEV network!"
      break
    done
  fi
else
  usage
fi
