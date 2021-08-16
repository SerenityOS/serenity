/*
 * Copyright (c) 2003, 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package java.lang.management;

/**
 * The management interface for a memory pool.  A memory pool
 * represents the memory resource managed by the Java virtual machine
 * and is managed by one or more {@link MemoryManagerMXBean memory managers}.
 *
 * <p> A Java virtual machine has one or more instances of the
 * implementation class of this interface.  An instance
 * implementing this interface is
 * an <a href="ManagementFactory.html#MXBean">MXBean</a>
 * that can be obtained by calling
 * the {@link ManagementFactory#getMemoryPoolMXBeans} method or
 * from the {@link ManagementFactory#getPlatformMBeanServer
 * platform MBeanServer} method.
 *
 * <p>The {@code ObjectName} for uniquely identifying the MXBean for
 * a memory pool within an {@code MBeanServer} is:
 * <blockquote>
 *    {@link ManagementFactory#MEMORY_POOL_MXBEAN_DOMAIN_TYPE
 *    java.lang:type=MemoryPool}{@code ,name=}<i>pool's name</i>
 * </blockquote>
 *
 * It can be obtained by calling the
 * {@link PlatformManagedObject#getObjectName} method.
 *
 * <h2>Memory Type</h2>
 * <p>The Java virtual machine has a heap for object allocation and also
 * maintains non-heap memory for the method area and the Java virtual
 * machine execution.  The Java virtual machine can have one or more
 * memory pools.  Each memory pool represents a memory area
 * of one of the following types:
 * <ul>
 *   <li>{@link MemoryType#HEAP heap}</li>
 *   <li>{@link MemoryType#NON_HEAP non-heap}</li>
 * </ul>
 *
 * <h2>Memory Usage Monitoring</h2>
 *
 * A memory pool has the following attributes:
 * <ul>
 *   <li><a href="#Usage">Memory usage</a></li>
 *   <li><a href="#PeakUsage">Peak memory usage</a></li>
 *   <li><a href="#UsageThreshold">Usage Threshold</a></li>
 *   <li><a href="#CollectionThreshold">Collection Usage Threshold</a>
 *       (only supported by some <em>garbage-collected</em> memory pools)</li>
 * </ul>
 *
 * <h3><a id="Usage">1. Memory Usage</a></h3>
 *
 * The {@link #getUsage} method provides an estimate
 * of the current usage of a memory pool.
 * For a garbage-collected memory pool, the amount of used memory
 * includes the memory occupied by all objects in the pool
 * including both <em>reachable</em> and <em>unreachable</em> objects.
 *
 * <p>In general, this method is a lightweight operation for getting
 * an approximate memory usage.  For some memory pools, for example,
 * when objects are not packed contiguously, this method may be
 * an expensive operation that requires some computation to determine
 * the current memory usage.  An implementation should document when
 * this is the case.
 *
 * <h3><a id="PeakUsage">2. Peak Memory Usage</a></h3>
 *
 * The Java virtual machine maintains the peak memory usage of a memory
 * pool since the virtual machine was started or the peak was reset.
 * The peak memory usage is returned by the {@link #getPeakUsage} method
 * and reset by calling the {@link #resetPeakUsage} method.
 *
 * <h3><a id="UsageThreshold">3. Usage Threshold</a></h3>
 *
 * Each memory pool has a manageable attribute
 * called the <i>usage threshold</i> which has a default value supplied
 * by the Java virtual machine.  The default value is platform-dependent.
 * The usage threshold can be set via the
 * {@link #setUsageThreshold setUsageThreshold} method.
 * If the threshold is set to a positive value, the usage threshold crossing
 * checking is enabled in this memory pool.
 * If the usage threshold is set to zero, usage
 * threshold crossing checking on this memory pool is disabled.
 * The {@link MemoryPoolMXBean#isUsageThresholdSupported} method can
 * be used to determine if this functionality is supported.
 * <p>
 * A Java virtual machine performs usage threshold crossing checking on a
 * memory pool basis at its best appropriate time, typically,
 * at garbage collection time.
 * Each memory pool maintains a {@link #getUsageThresholdCount
 * usage threshold count} that will get incremented
 * every time when the Java virtual machine
 * detects that the memory pool usage is crossing the threshold.
 * <p>
 * This manageable usage threshold attribute is designed for monitoring the
 * increasing trend of memory usage with low overhead.
 * Usage threshold may not be appropriate for some memory pools.
 * For example, a generational garbage collector, a common garbage collection
 * algorithm used in many Java virtual machine implementations,
 * manages two or more generations segregating objects by age.
 * Most of the objects are allocated in
 * the <em>youngest generation</em> (say a nursery memory pool).
 * The nursery memory pool is designed to be filled up and
 * collecting the nursery memory pool will free most of its memory space
 * since it is expected to contain mostly short-lived objects
 * and mostly are unreachable at garbage collection time.
 * In this case, it is more appropriate for the nursery memory pool
 * not to support a usage threshold.  In addition,
 * if the cost of an object allocation
 * in one memory pool is very low (for example, just atomic pointer exchange),
 * the Java virtual machine would probably not support the usage threshold
 * for that memory pool since the overhead in comparing the usage with
 * the threshold is higher than the cost of object allocation.
 *
 * <p>
 * The memory usage of the system can be monitored using
 * <a href="#Polling">polling</a> or
 * <a href="#ThresholdNotification">threshold notification</a> mechanisms.
 *
 * <ol type="a">
 *   <li><a id="Polling"><b>Polling</b></a>
 *       <p>
 *       An application can continuously monitor its memory usage
 *       by calling either the {@link #getUsage} method for all
 *       memory pools or the {@link #isUsageThresholdExceeded} method
 *       for those memory pools that support a usage threshold.
 *       Below is example code that has a thread dedicated for
 *       task distribution and processing.  At every interval,
 *       it will determine if it should receive and process new tasks based
 *       on its memory usage.  If the memory usage exceeds its usage threshold,
 *       it will redistribute all outstanding tasks to other VMs and
 *       stop receiving new tasks until the memory usage returns
 *       below its usage threshold.
 *
 *       <pre>
 *       // Assume the usage threshold is supported for this pool.
 *       // Set the threshold to myThreshold above which no new tasks
 *       // should be taken.
 *       pool.setUsageThreshold(myThreshold);
 *       ....
 *
 *       boolean lowMemory = false;
 *       while (true) {
 *          if (pool.isUsageThresholdExceeded()) {
 *              // potential low memory, so redistribute tasks to other VMs
 *              lowMemory = true;
 *              redistributeTasks();
 *              // stop receiving new tasks
 *              stopReceivingTasks();
 *          } else {
 *              if (lowMemory) {
 *                  // resume receiving tasks
 *                  lowMemory = false;
 *                  resumeReceivingTasks();
 *              }
 *              // processing outstanding task
 *              ...
 *          }
 *          // sleep for sometime
 *          try {
 *              Thread.sleep(sometime);
 *          } catch (InterruptedException e) {
 *              ...
 *          }
 *       }
 *       </pre>
 *
 * <hr>
 *       The above example does not differentiate the case where
 *       the memory usage has temporarily dropped below the usage threshold
 *       from the case where the memory usage remains above the threshold
 *       between two iterations.  The usage threshold count returned by
 *       the {@link #getUsageThresholdCount} method
 *       can be used to determine
 *       if the memory usage has returned below the threshold
 *       between two polls.
 *       <p>
 *       Below shows another example that takes some action if a
 *       memory pool is under low memory and ignores the memory usage
 *       changes during the action processing time.
 *
 *       <pre>
 *       // Assume the usage threshold is supported for this pool.
 *       // Set the threshold to myThreshold which determines if
 *       // the application will take some action under low memory condition.
 *       pool.setUsageThreshold(myThreshold);
 *
 *       int prevCrossingCount = 0;
 *       while (true) {
 *           // A busy loop to detect when the memory usage
 *           // has exceeded the threshold.
 *           while (!pool.isUsageThresholdExceeded() ||
 *                  pool.getUsageThresholdCount() == prevCrossingCount) {
 *               try {
 *                   Thread.sleep(sometime)
 *               } catch (InterruptException e) {
 *                   ....
 *               }
 *           }
 *
 *           // Do some processing such as check for memory usage
 *           // and issue a warning
 *           ....
 *
 *           // Gets the current threshold count. The busy loop will then
 *           // ignore any crossing of threshold happens during the processing.
 *           prevCrossingCount = pool.getUsageThresholdCount();
 *       }
 *       </pre><hr>
 *   </li>
 *   <li><a id="ThresholdNotification"><b>Usage Threshold Notifications</b></a>
 *       <p>
 *       Usage threshold notification will be emitted by {@link MemoryMXBean}.
 *       When the Java virtual machine detects that the memory usage of
 *       a memory pool has reached or exceeded the usage threshold
 *       the virtual machine will trigger the {@code MemoryMXBean} to emit an
 *       {@link MemoryNotificationInfo#MEMORY_THRESHOLD_EXCEEDED
 *       usage threshold exceeded notification}.
 *       Another usage threshold exceeded notification will not be
 *       generated until the usage has fallen below the threshold and
 *       then exceeded it again.
 *       <p>
 *       Below is an example code implementing the same logic as the
 *       first example above but using the usage threshold notification
 *       mechanism to detect low memory conditions instead of polling.
 *       In this example code, upon receiving notification, the notification
 *       listener notifies another thread to perform the actual action
 *       such as to redistribute outstanding tasks, stop receiving tasks,
 *       or resume receiving tasks.
 *       The {@code handleNotification} method should be designed to
 *       do a very minimal amount of work and return without delay to avoid
 *       causing delay in delivering subsequent notifications.  Time-consuming
 *       actions should be performed by a separate thread.
 *       The notification listener may be invoked by multiple threads
 *       concurrently; so the tasks performed by the listener
 *       should be properly synchronized.
 *
 *       <pre>
 *       class MyListener implements javax.management.NotificationListener {
 *            public void handleNotification(Notification notification, Object handback)  {
 *                String notifType = notification.getType();
 *                if (notifType.equals(MemoryNotificationInfo.MEMORY_THRESHOLD_EXCEEDED)) {
 *                    // potential low memory, notify another thread
 *                    // to redistribute outstanding tasks to other VMs
 *                    // and stop receiving new tasks.
 *                    lowMemory = true;
 *                    notifyAnotherThread(lowMemory);
 *                }
 *            }
 *       }
 *
 *       // Register MyListener with MemoryMXBean
 *       MemoryMXBean mbean = ManagementFactory.getMemoryMXBean();
 *       NotificationEmitter emitter = (NotificationEmitter) mbean;
 *       MyListener listener = new MyListener();
 *       emitter.addNotificationListener(listener, null, null);
 *
 *       // Assume this pool supports a usage threshold.
 *       // Set the threshold to myThreshold above which no new tasks
 *       // should be taken.
 *       pool.setUsageThreshold(myThreshold);
 *
 *       // Usage threshold detection is enabled and notification will be
 *       // handled by MyListener.  Continue for other processing.
 *       ....
 *
 *       </pre>
 * <hr>
 *       <p>
 *       There is no guarantee about when the {@code MemoryMXBean} will emit
 *       a threshold notification and when the notification will be delivered.
 *       When a notification listener is invoked, the memory usage of
 *       the memory pool may have crossed the usage threshold more
 *       than once.
 *       The {@link MemoryNotificationInfo#getCount} method returns the number
 *       of times that the memory usage has crossed the usage threshold
 *       at the point in time when the notification was constructed.
 *       It can be compared with the current usage threshold count returned
 *       by the {@link #getUsageThresholdCount} method to determine if
 *       such situation has occurred.
 *   </li>
 * </ol>
 *
 * <h3><a id="CollectionThreshold">4. Collection Usage Threshold</a></h3>
 *
 * Collection usage threshold is a manageable attribute only applicable
 * to some garbage-collected memory pools.
 * After a Java virtual machine has expended effort in reclaiming memory
 * space by recycling unused objects in a memory pool at garbage collection
 * time, some number of bytes in the memory pools that are garbaged
 * collected will still be in use.  The collection usage threshold
 * allows a value to be set for this number of bytes such
 * that if the threshold is exceeded,
 * a {@link MemoryNotificationInfo#MEMORY_THRESHOLD_EXCEEDED
 * collection usage threshold exceeded notification}
 * will be emitted by the {@link MemoryMXBean}.
 * In addition, the {@link #getCollectionUsageThresholdCount
 * collection usage threshold count} will then be incremented.
 *
 * <p>
 * The {@link MemoryPoolMXBean#isCollectionUsageThresholdSupported} method can
 * be used to determine if this functionality is supported.
 *
 * <p>
 * A Java virtual machine performs collection usage threshold checking
 * on a memory pool basis.  This checking is enabled if the collection
 * usage threshold is set to a positive value.
 * If the collection usage threshold is set to zero, this checking
 * is disabled on this memory pool.  Default value is zero.
 * The Java virtual machine performs the collection usage threshold
 * checking at garbage collection time.
 *
 * <p>
 * Some garbage-collected memory pools may
 * choose not to support the collection usage threshold.  For example,
 * a memory pool is only managed by a continuous concurrent garbage
 * collector.  Objects can be allocated in this memory pool by some thread
 * while the unused objects are reclaimed by the concurrent garbage
 * collector simultaneously.  Unless there is a well-defined
 * garbage collection time which is the best appropriate time
 * to check the memory usage, the collection usage threshold should not
 * be supported.
 *
 * <p>
 * The collection usage threshold is designed for monitoring the memory usage
 * after the Java virtual machine has expended effort in reclaiming
 * memory space.  The collection usage could also be monitored
 * by the polling and threshold notification mechanism
 * described above for the <a href="#UsageThreshold">usage threshold</a>
 * in a similar fashion.
 *
 * @see ManagementFactory#getPlatformMXBeans(Class)
 * @see <a href="../../../javax/management/package-summary.html">
 *      JMX Specification.</a>
 * @see <a href="package-summary.html#examples">
 *      Ways to Access MXBeans</a>
 *
 * @author  Mandy Chung
 * @since   1.5
 */
public interface MemoryPoolMXBean extends PlatformManagedObject {
    /**
     * Returns the name representing this memory pool.
     *
     * @return the name of this memory pool.
     */
    public String getName();

    /**
     * Returns the type of this memory pool.
     *
     * <p>
     * <b>MBeanServer access</b>:<br>
     * The mapped type of {@code MemoryType} is {@code String}
     * and the value is the name of the {@code MemoryType}.
     *
     * @return the type of this memory pool.
     */
    public MemoryType getType();

    /**
     * Returns an estimate of the memory usage of this memory pool.
     * This method returns {@code null}
     * if this memory pool is not valid (i.e. no longer exists).
     *
     * <p>
     * This method requests the Java virtual machine to make
     * a best-effort estimate of the current memory usage of this
     * memory pool. For some memory pools, this method may be an
     * expensive operation that requires some computation to determine
     * the estimate.  An implementation should document when
     * this is the case.
     *
     * <p>This method is designed for use in monitoring system
     * memory usage and detecting low memory condition.
     *
     * <p>
     * <b>MBeanServer access</b>:<br>
     * The mapped type of {@code MemoryUsage} is
     * {@code CompositeData} with attributes as specified in
     * {@link MemoryUsage#from MemoryUsage}.
     *
     * @return a {@link MemoryUsage} object; or {@code null} if
     * this pool not valid.
     */
    public MemoryUsage getUsage();

    /**
     * Returns the peak memory usage of this memory pool since the
     * Java virtual machine was started or since the peak was reset.
     * This method returns {@code null}
     * if this memory pool is not valid (i.e. no longer exists).
     *
     * <p>
     * <b>MBeanServer access</b>:<br>
     * The mapped type of {@code MemoryUsage} is
     * {@code CompositeData} with attributes as specified in
     * {@link MemoryUsage#from MemoryUsage}.
     *
     * @return a {@link MemoryUsage} object representing the peak
     * memory usage; or {@code null} if this pool is not valid.
     *
     */
    public MemoryUsage getPeakUsage();

    /**
     * Resets the peak memory usage statistic of this memory pool
     * to the current memory usage.
     *
     * @throws java.lang.SecurityException if a security manager
     *         exists and the caller does not have
     *         ManagementPermission("control").
     */
    public void resetPeakUsage();

    /**
     * Tests if this memory pool is valid in the Java virtual
     * machine.  A memory pool becomes invalid once the Java virtual
     * machine removes it from the memory system.
     *
     * @return {@code true} if the memory pool is valid in the running
     *              Java virtual machine;
     *         {@code false} otherwise.
     */
    public boolean isValid();

    /**
     * Returns the name of memory managers that manages this memory pool.
     * Each memory pool will be managed by at least one memory manager.
     *
     * @return an array of {@code String} objects, each is the name of
     * a memory manager managing this memory pool.
     */
    public String[] getMemoryManagerNames();

    /**
     * Returns the usage threshold value of this memory pool in bytes.
     * Each memory pool has a platform-dependent default threshold value.
     * The current usage threshold can be changed via the
     * {@link #setUsageThreshold setUsageThreshold} method.
     *
     * @return the usage threshold value of this memory pool in bytes.
     *
     * @throws UnsupportedOperationException if this memory pool
     *         does not support a usage threshold.
     *
     * @see #isUsageThresholdSupported
     */
    public long getUsageThreshold();

    /**
     * Sets the threshold of this memory pool to the given {@code threshold}
     * value if this memory pool supports the usage threshold.
     * The usage threshold crossing checking is enabled in this memory pool
     * if the threshold is set to a positive value.
     * The usage threshold crossing checking is disabled
     * if it is set to zero.
     *
     * @param threshold the new threshold value in bytes. Must be non-negative.
     *
     * @throws IllegalArgumentException if {@code threshold} is negative
     *         or greater than the maximum amount of memory for
     *         this memory pool if defined.
     *
     * @throws UnsupportedOperationException if this memory pool
     *         does not support a usage threshold.
     *
     * @throws java.lang.SecurityException if a security manager
     *         exists and the caller does not have
     *         ManagementPermission("control").
     *
     * @see #isUsageThresholdSupported
     * @see <a href="#UsageThreshold">Usage threshold</a>
     */
    public void setUsageThreshold(long threshold);

    /**
     * Tests if the memory usage of this memory pool
     * reaches or exceeds its usage threshold value.
     *
     * @return {@code true} if the memory usage of
     * this memory pool reaches or exceeds the threshold value;
     * {@code false} otherwise.
     *
     * @throws UnsupportedOperationException if this memory pool
     *         does not support a usage threshold.
     */
    public boolean isUsageThresholdExceeded();

    /**
     * Returns the number of times that the memory usage has crossed
     * the usage threshold.
     *
     * @return the number of times that the memory usage
     * has crossed its usage threshold value.
     *
     * @throws UnsupportedOperationException if this memory pool
     * does not support a usage threshold.
     */
    public long getUsageThresholdCount();

    /**
     * Tests if this memory pool supports usage threshold.
     *
     * @return {@code true} if this memory pool supports usage threshold;
     * {@code false} otherwise.
     */
    public boolean isUsageThresholdSupported();

    /**
     * Returns the collection usage threshold value of this memory pool
     * in bytes.  The default value is zero. The collection usage
     * threshold can be changed via the
     * {@link #setCollectionUsageThreshold setCollectionUsageThreshold} method.
     *
     * @return the collection usage threshold of this memory pool in bytes.
     *
     * @throws UnsupportedOperationException if this memory pool
     *         does not support a collection usage threshold.
     *
     * @see #isCollectionUsageThresholdSupported
     */
    public long getCollectionUsageThreshold();

    /**
     * Sets the collection usage threshold of this memory pool to
     * the given {@code threshold} value.
     * When this threshold is set to positive, the Java virtual machine
     * will check the memory usage at its best appropriate time after it has
     * expended effort in recycling unused objects in this memory pool.
     * <p>
     * The collection usage threshold crossing checking is enabled
     * in this memory pool if the threshold is set to a positive value.
     * The collection usage threshold crossing checking is disabled
     * if it is set to zero.
     *
     * @param threshold the new collection usage threshold value in bytes.
     *              Must be non-negative.
     *
     * @throws IllegalArgumentException if {@code threshold} is negative
     *         or greater than the maximum amount of memory for
     *         this memory pool if defined.
     *
     * @throws UnsupportedOperationException if this memory pool
     *         does not support a collection usage threshold.
     *
     * @throws java.lang.SecurityException if a security manager
     *         exists and the caller does not have
     *         ManagementPermission("control").
     *
     * @see #isCollectionUsageThresholdSupported
     * @see <a href="#CollectionThreshold">Collection usage threshold</a>
     */
    public void setCollectionUsageThreshold(long threshold);

    /**
     * Tests if the memory usage of this memory pool after
     * the most recent collection on which the Java virtual
     * machine has expended effort has reached or
     * exceeded its collection usage threshold.
     * This method does not request the Java virtual
     * machine to perform any garbage collection other than its normal
     * automatic memory management.
     *
     * @return {@code true} if the memory usage of this memory pool
     * reaches or exceeds the collection usage threshold value
     * in the most recent collection;
     * {@code false} otherwise.
     *
     * @throws UnsupportedOperationException if this memory pool
     *         does not support a usage threshold.
     */
    public boolean isCollectionUsageThresholdExceeded();

    /**
     * Returns the number of times that the Java virtual machine
     * has detected that the memory usage has reached or
     * exceeded the collection usage threshold.
     *
     * @return the number of times that the memory
     * usage has reached or exceeded the collection usage threshold.
     *
     * @throws UnsupportedOperationException if this memory pool
     *         does not support a collection usage threshold.
     *
     * @see #isCollectionUsageThresholdSupported
     */
    public long getCollectionUsageThresholdCount();

    /**
     * Returns the memory usage after the Java virtual machine
     * most recently expended effort in recycling unused objects
     * in this memory pool.
     * This method does not request the Java virtual
     * machine to perform any garbage collection other than its normal
     * automatic memory management.
     * This method returns {@code null} if the Java virtual
     * machine does not support this method.
     *
     * <p>
     * <b>MBeanServer access</b>:<br>
     * The mapped type of {@code MemoryUsage} is
     * {@code CompositeData} with attributes as specified in
     * {@link MemoryUsage#from MemoryUsage}.
     *
     * @return a {@link MemoryUsage} representing the memory usage of
     * this memory pool after the Java virtual machine most recently
     * expended effort in recycling unused objects;
     * {@code null} if this method is not supported.
     */
    public MemoryUsage getCollectionUsage();

    /**
     * Tests if this memory pool supports a collection usage threshold.
     *
     * @return {@code true} if this memory pool supports the
     * collection usage threshold; {@code false} otherwise.
     */
    public boolean isCollectionUsageThresholdSupported();
}
