/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 *
 */

#include "precompiled.hpp"

// This test performs mocking of certain JVM functionality. This works by
// including the source file under test inside an anonymous namespace (which
// prevents linking conflicts) with the mocked symbols redefined.

// The include list should mirror the one found in the included source file -
// with the ones that should pick up the mocks removed. Those should be included
// later after the mocks have been defined.

#include "logging/log.hpp"
#include "jfr/jfrEvents.hpp"
#include "jfr/metadata/jfrSerializer.hpp"
#include "jfr/periodic/jfrOSInterface.hpp"
#include "jfr/utilities/jfrTime.hpp"
#include "jfr/utilities/jfrTypes.hpp"
#include "runtime/os_perf.hpp"
#include "utilities/globalDefinitions.hpp"
#include "utilities/growableArray.hpp"

#include "unittest.hpp"

#include <vector>
#include <list>
#include <map>

namespace {

  class MockFastUnorderedElapsedCounterSource : public ::FastUnorderedElapsedCounterSource {
   public:
    static jlong current_ticks;
    static Type now() {
      return current_ticks;
    }
    static uint64_t nanoseconds(Type value) {
      return value;
    }
  };

  typedef TimeInstant<CounterRepresentation, MockFastUnorderedElapsedCounterSource> MockJfrTicks;
  typedef TimeInterval<CounterRepresentation, MockFastUnorderedElapsedCounterSource> MockJfrTickspan;

  class MockJfrCheckpointWriter {
   public:
    traceid current;
    std::map<traceid, std::string> ids;

    const JfrCheckpointContext context() const {
      return JfrCheckpointContext();
    }
    intptr_t reserve(size_t size) {
      return 0;
    }
    void write_key(traceid id) {
      current = id;
    }
    void write_type(JfrTypeId id) {}
    MockJfrCheckpointWriter() {}
    void write(const char* data) {}
    void set_context(const JfrCheckpointContext ctx) { }
    void write_count(u4 nof_entries) { }
  };

  class MockJfrSerializer {
   public:
    static bool register_serializer(JfrTypeId id, bool permit_cache, MockJfrSerializer* serializer) {
      return true;
    }
    virtual void on_rotation() {}
    virtual void serialize(MockJfrCheckpointWriter& writer) {}
  };

  struct MockNetworkInterface {
    std::string name;
    uint64_t bytes_in;
    uint64_t bytes_out;
    traceid id;
    MockNetworkInterface(std::string name, uint64_t bytes_in, uint64_t bytes_out, traceid id) :
      name(name), bytes_in(bytes_in), bytes_out(bytes_out), id(id) {}

    bool operator==(const MockNetworkInterface& rhs) const {
      return name == rhs.name;
    }
  };

  class NetworkInterface : public ::NetworkInterface {
   public:
    NetworkInterface(const char* name, uint64_t bytes_in, uint64_t bytes_out, NetworkInterface* next) :
      ::NetworkInterface(name, bytes_in, bytes_out, next) {}
    NetworkInterface* next(void) const {
      return reinterpret_cast<NetworkInterface*>(::NetworkInterface::next());
    }
  };

  class MockJfrOSInterface {
    static std::list<MockNetworkInterface> _interfaces;
   public:
    MockJfrOSInterface() {}
    static int network_utilization(NetworkInterface** network_interfaces) {
      *network_interfaces = NULL;
      for (std::list<MockNetworkInterface>::const_iterator i = _interfaces.begin();
           i != _interfaces.end();
           ++i) {
        NetworkInterface* cur = new NetworkInterface(i->name.c_str(), i->bytes_in, i->bytes_out, *network_interfaces);
        *network_interfaces = cur;
      }
      return OS_OK;
    }
    static MockNetworkInterface& add_interface(const std::string& name, traceid id) {
      MockNetworkInterface iface(name, 0, 0, id);
      _interfaces.push_front(iface);
      return _interfaces.front();
    }
    static void remove_interface(const MockNetworkInterface& iface) {
      _interfaces.remove(iface);
    }
    static void clear_interfaces() {
      _interfaces.clear();
    }
    static const MockNetworkInterface& get_interface(traceid id) {
      std::list<MockNetworkInterface>::const_iterator i = _interfaces.begin();
      for (; i != _interfaces.end(); ++i) {
        if (i->id == id) {
          break;
        }
      }
      return *i;
    }
  };

  std::list<MockNetworkInterface> MockJfrOSInterface::_interfaces;

  class MockEventNetworkUtilization : public ::EventNetworkUtilization {
   public:
    std::string iface;
    s8 readRate;
    s8 writeRate;
    static std::vector<MockEventNetworkUtilization> committed;
    MockJfrCheckpointWriter writer;

   public:
    MockEventNetworkUtilization(EventStartTime timing=TIMED) :
    ::EventNetworkUtilization(timing) {}

    void set_networkInterface(traceid new_value) {
      const MockNetworkInterface& entry  = MockJfrOSInterface::get_interface(new_value);
      iface = entry.name;
    }
    void set_readRate(s8 new_value) {
      readRate = new_value;
    }
    void set_writeRate(s8 new_value) {
      writeRate = new_value;
    }

    void commit() {
      committed.push_back(*this);
    }

    void set_starttime(const MockJfrTicks& time) {}
    void set_endtime(const MockJfrTicks& time) {}

    static const MockEventNetworkUtilization& get_committed(const std::string& name) {
      static MockEventNetworkUtilization placeholder;
      for (std::vector<MockEventNetworkUtilization>::const_iterator i = committed.begin();
           i != committed.end();
           ++i) {
        if (name == i->iface) {
          return *i;
        }
      }
      return placeholder;
    }
  };

  std::vector<MockEventNetworkUtilization> MockEventNetworkUtilization::committed;

  jlong MockFastUnorderedElapsedCounterSource::current_ticks;

// Reincluding source files in the anonymous namespace unfortunately seems to
// behave strangely with precompiled headers (only when using gcc though)
#ifndef DONT_USE_PRECOMPILED_HEADER
#define DONT_USE_PRECOMPILED_HEADER
#endif

#define EventNetworkUtilization MockEventNetworkUtilization
#define FastUnorderedElapsedCounterSource MockFastUnorderedElapsedCounterSource
#define JfrOSInterface MockJfrOSInterface
#define JfrSerializer MockJfrSerializer
#define JfrCheckpointWriter MockJfrCheckpointWriter
#define JfrTicks MockJfrTicks
#define JfrTickspan MockJfrTickspan

#include "jfr/periodic/jfrNetworkUtilization.hpp"
#include "jfr/periodic/jfrNetworkUtilization.cpp"

#undef EventNetworkUtilization
#undef FastUnorderedElapsedCounterSource
#undef JfrOSInterface
#undef JfrSerializer
#undef JfrCheckpointWriter
#undef JfrTicks
#undef JfrTickspan

} // anonymous namespace

class JfrTestNetworkUtilization : public ::testing::Test {
protected:
  void SetUp() {
    MockEventNetworkUtilization::committed.clear();
    MockJfrOSInterface::clear_interfaces();
    // Ensure that tests are separated in time
    MockFastUnorderedElapsedCounterSource::current_ticks += 1 * NANOSECS_PER_SEC;
  }

  void TearDown() {
    JfrNetworkUtilization::destroy();
  }
};

static traceid id = 0;

TEST_VM_F(JfrTestNetworkUtilization, RequestFunctionBasic) {

  MockNetworkInterface& eth0 = MockJfrOSInterface::add_interface("eth0", ++id);
  JfrNetworkUtilization::send_events();
  ASSERT_EQ(0u, MockEventNetworkUtilization::committed.size());

  eth0.bytes_in += 10;
  MockFastUnorderedElapsedCounterSource::current_ticks += 2 * NANOSECS_PER_SEC;

  JfrNetworkUtilization::send_events();
  ASSERT_EQ(1u, MockEventNetworkUtilization::committed.size());
  MockEventNetworkUtilization& e = MockEventNetworkUtilization::committed[0];
  EXPECT_EQ(40, e.readRate);
  EXPECT_EQ(0, e.writeRate);
  EXPECT_STREQ("eth0", e.iface.c_str());
}

TEST_VM_F(JfrTestNetworkUtilization, RequestFunctionMultiple) {

  MockNetworkInterface& eth0 = MockJfrOSInterface::add_interface("eth0", ++id);
  MockNetworkInterface& eth1 = MockJfrOSInterface::add_interface("eth1", ++id);
  MockNetworkInterface& ppp0 = MockJfrOSInterface::add_interface("ppp0", ++id);
  JfrNetworkUtilization::send_events();
  ASSERT_EQ(0u, MockEventNetworkUtilization::committed.size());

  eth0.bytes_in += 10;
  eth1.bytes_in += 100;
  ppp0.bytes_out += 50;
  MockFastUnorderedElapsedCounterSource::current_ticks += 2 * NANOSECS_PER_SEC;

  JfrNetworkUtilization::send_events();
  ASSERT_EQ(3u, MockEventNetworkUtilization::committed.size());
  const MockEventNetworkUtilization& eth0_event = MockEventNetworkUtilization::get_committed("eth0");
  const MockEventNetworkUtilization& eth1_event = MockEventNetworkUtilization::get_committed("eth1");
  const MockEventNetworkUtilization& ppp0_event = MockEventNetworkUtilization::get_committed("ppp0");

  EXPECT_EQ(40, eth0_event.readRate);
  EXPECT_EQ(0, eth0_event.writeRate);
  EXPECT_STREQ("eth0", eth0_event.iface.c_str());

  EXPECT_EQ(400, eth1_event.readRate);
  EXPECT_EQ(0, eth1_event.writeRate);
  EXPECT_STREQ("eth1", eth1_event.iface.c_str());

  EXPECT_EQ(0, ppp0_event.readRate);
  EXPECT_EQ(200, ppp0_event.writeRate);
  EXPECT_STREQ("ppp0", ppp0_event.iface.c_str());
}

TEST_VM_F(JfrTestNetworkUtilization, InterfaceRemoved) {
  MockNetworkInterface& eth0 = MockJfrOSInterface::add_interface("eth0", ++id);
  MockNetworkInterface& eth1 = MockJfrOSInterface::add_interface("eth1", ++id);
  JfrNetworkUtilization::send_events();
  ASSERT_EQ(0u, MockEventNetworkUtilization::committed.size());

  eth0.bytes_in += 10;
  eth1.bytes_in += 20;
  MockFastUnorderedElapsedCounterSource::current_ticks += 2 * NANOSECS_PER_SEC;

  JfrNetworkUtilization::send_events();
  ASSERT_EQ(2u, MockEventNetworkUtilization::committed.size());
  const MockEventNetworkUtilization& eth0_event = MockEventNetworkUtilization::get_committed("eth0");
  const MockEventNetworkUtilization& eth1_event = MockEventNetworkUtilization::get_committed("eth1");

  EXPECT_EQ(40, eth0_event.readRate);
  EXPECT_EQ(0, eth0_event.writeRate);
  EXPECT_STREQ("eth0", eth0_event.iface.c_str());

  EXPECT_EQ(80, eth1_event.readRate);
  EXPECT_EQ(0, eth1_event.writeRate);
  EXPECT_STREQ("eth1", eth1_event.iface.c_str());

  MockJfrOSInterface::remove_interface(eth0);
  MockEventNetworkUtilization::committed.clear();

  eth1.bytes_in += 10;
  MockFastUnorderedElapsedCounterSource::current_ticks += 2 * NANOSECS_PER_SEC;
  JfrNetworkUtilization::send_events();
  ASSERT_EQ(1u, MockEventNetworkUtilization::committed.size());
  const MockEventNetworkUtilization& eth1_event_v2 = MockEventNetworkUtilization::get_committed("eth1");

  EXPECT_EQ(40, eth1_event_v2.readRate);
  EXPECT_EQ(0, eth1_event_v2.writeRate);
  EXPECT_STREQ("eth1", eth1_event_v2.iface.c_str());
}

TEST_VM_F(JfrTestNetworkUtilization, InterfaceReset) {
  MockNetworkInterface& eth0 = MockJfrOSInterface::add_interface("eth0", ++id);
  JfrNetworkUtilization::send_events();
  ASSERT_EQ(0u, MockEventNetworkUtilization::committed.size());

  eth0.bytes_in += 10;
  MockFastUnorderedElapsedCounterSource::current_ticks += 2 * NANOSECS_PER_SEC;

  JfrNetworkUtilization::send_events();
  ASSERT_EQ(1u, MockEventNetworkUtilization::committed.size());
  const MockEventNetworkUtilization& event = MockEventNetworkUtilization::committed[0];
  EXPECT_EQ(40, event.readRate);
  EXPECT_EQ(0, event.writeRate);
  EXPECT_STREQ("eth0", event.iface.c_str());

  eth0.bytes_in = 0;
  MockFastUnorderedElapsedCounterSource::current_ticks += 2 * NANOSECS_PER_SEC;
  MockEventNetworkUtilization::committed.clear();

  JfrNetworkUtilization::send_events();
  ASSERT_EQ(0u, MockEventNetworkUtilization::committed.size());

  eth0.bytes_in = 10;
  MockFastUnorderedElapsedCounterSource::current_ticks += 2 * NANOSECS_PER_SEC;

  JfrNetworkUtilization::send_events();
  ASSERT_EQ(1u, MockEventNetworkUtilization::committed.size());
  const MockEventNetworkUtilization& event_v2 = MockEventNetworkUtilization::committed[0];
  EXPECT_EQ(40, event_v2.readRate);
  EXPECT_EQ(0, event_v2.writeRate);
  EXPECT_STREQ("eth0", event_v2.iface.c_str());
}
