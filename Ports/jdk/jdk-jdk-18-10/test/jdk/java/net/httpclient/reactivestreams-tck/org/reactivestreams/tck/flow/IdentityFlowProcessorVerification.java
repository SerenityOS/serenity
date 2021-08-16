/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
 */

package org.reactivestreams.tck.flow;

import org.reactivestreams.*;
import org.reactivestreams.tck.IdentityProcessorVerification;
import org.reactivestreams.tck.TestEnvironment;
import org.reactivestreams.tck.flow.support.SubscriberWhiteboxVerificationRules;
import org.reactivestreams.tck.flow.support.PublisherVerificationRules;

import java.util.concurrent.Flow;

public abstract class IdentityFlowProcessorVerification<T> extends IdentityProcessorVerification<T>
  implements SubscriberWhiteboxVerificationRules, PublisherVerificationRules {

  public IdentityFlowProcessorVerification(TestEnvironment env) {
    super(env);
  }

  public IdentityFlowProcessorVerification(TestEnvironment env, long publisherReferenceGCTimeoutMillis) {
    super(env, publisherReferenceGCTimeoutMillis);
  }

  public IdentityFlowProcessorVerification(TestEnvironment env, long publisherReferenceGCTimeoutMillis, int processorBufferSize) {
    super(env, publisherReferenceGCTimeoutMillis, processorBufferSize);
  }

  /**
   * By implementing this method, additional TCK tests concerning a "failed" Flow publishers will be run.
   *
   * The expected behaviour of the {@link Flow.Publisher} returned by this method is hand out a subscription,
   * followed by signalling {@code onError} on it, as specified by Rule 1.9.
   *
   * If you want to ignore these additional tests, return {@code null} from this method.
   */
  protected abstract Flow.Publisher<T> createFailedFlowPublisher();

  /**
   * This is the main method you must implement in your test incarnation.
   * It must create a {@link Flow.Processor}, which simply forwards all stream elements from its upstream
   * to its downstream. It must be able to internally buffer the given number of elements.
   *
   * @param bufferSize number of elements the processor is required to be able to buffer.
   */
  protected abstract Flow.Processor<T,T> createIdentityFlowProcessor(int bufferSize);

  @Override
  public final Processor<T, T> createIdentityProcessor(int bufferSize) {
    return FlowAdapters.toProcessor(createIdentityFlowProcessor(bufferSize));
  }

  @Override
  public final Publisher<T> createFailedPublisher() {
    Flow.Publisher<T> failed = createFailedFlowPublisher();
    if (failed == null) return null; // because `null` means "SKIP" in createFailedPublisher
    else return FlowAdapters.toPublisher(failed);
  }

}
