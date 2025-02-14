/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.zookeeper.server;

import java.util.concurrent.atomic.AtomicLong;
import org.apache.zookeeper.common.Time;
import org.apache.zookeeper.server.metric.AvgMinMaxCounter;
import org.apache.zookeeper.server.quorum.BufferStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Basic Server Statistics
 *
 * 运行时的统计器，包含了最基本的运行时信息
 */
public class ServerStats {

    private static final Logger LOG = LoggerFactory.getLogger(ServerStats.class);

    /**
     * 从 ZooKeeper 启动开始，或是最近一次重置服务端统计信息之后
     * 服务端向客户端发送的响应包次数
     */
    private final AtomicLong packetsSent = new AtomicLong();
    /**
     * 从 ZooKeeper 启动开始，或是最近一次重置服务端统计信息之后
     * 服务端接收到的来自客户端的请求包次数
     */
    private final AtomicLong packetsReceived = new AtomicLong();

    private final AvgMinMaxCounter requestLatency = new AvgMinMaxCounter("request_latency");

    private final AtomicLong fsyncThresholdExceedCount = new AtomicLong(0);

    private final BufferStats clientResponseStats = new BufferStats();

    private AtomicLong nonMTLSRemoteConnCntr = new AtomicLong(0);

    private AtomicLong nonMTLSLocalConnCntr = new AtomicLong(0);

    private AtomicLong authFailedCntr = new AtomicLong(0);

    private final Provider provider;
    private final long startTime = Time.currentElapsedTime();

    public interface Provider {

        long getOutstandingRequests();
        long getLastProcessedZxid();
        String getState();
        int getNumAliveConnections();
        long getDataDirSize();
        long getLogDirSize();

    }

    public ServerStats(Provider provider) {
        this.provider = provider;
    }

    // getters
    public long getMinLatency() {
        return requestLatency.getMin();
    }

    public double getAvgLatency() {
        return requestLatency.getAvg();
    }

    public long getMaxLatency() {
        return requestLatency.getMax();
    }

    public long getOutstandingRequests() {
        return provider.getOutstandingRequests();
    }

    public long getLastProcessedZxid() {
        return provider.getLastProcessedZxid();
    }

    public long getDataDirSize() {
        return provider.getDataDirSize();
    }

    public long getLogDirSize() {
        return provider.getLogDirSize();
    }

    public long getPacketsReceived() {
        return packetsReceived.get();
    }

    public long getPacketsSent() {
        return packetsSent.get();
    }

    public String getServerState() {
        return provider.getState();
    }

    /** The number of client connections alive to this server */
    public int getNumAliveClientConnections() {
        return provider.getNumAliveConnections();
    }

    public long getUptime() {
        return Time.currentElapsedTime() - startTime;
    }

    public boolean isProviderNull() {
        return provider == null;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Latency min/avg/max: " + getMinLatency() + "/" + getAvgLatency() + "/" + getMaxLatency() + "\n");
        sb.append("Received: " + getPacketsReceived() + "\n");
        sb.append("Sent: " + getPacketsSent() + "\n");
        sb.append("Connections: " + getNumAliveClientConnections() + "\n");

        if (provider != null) {
            sb.append("Outstanding: " + getOutstandingRequests() + "\n");
            sb.append("Zxid: 0x" + Long.toHexString(getLastProcessedZxid()) + "\n");
        }
        sb.append("Mode: " + getServerState() + "\n");
        return sb.toString();
    }

    /**
     * Update request statistic. This should only be called from a request
     * that originated from that machine.
     */
    public void updateLatency(Request request, long currentTime) {
        long latency = currentTime - request.createTime;
        if (latency < 0) {
            return;
        }
        requestLatency.addDataPoint(latency);
        if (request.getHdr() != null) {
            // Only quorum request should have header
            ServerMetrics.getMetrics().UPDATE_LATENCY.add(latency);
        } else {
            // All read request should goes here
            ServerMetrics.getMetrics().READ_LATENCY.add(latency);
        }
    }

    public void resetLatency() {
        requestLatency.reset();
    }

    public void resetMaxLatency() {
        requestLatency.resetMax();
    }

    public void incrementPacketsReceived() {
        packetsReceived.incrementAndGet();
    }

    public void incrementPacketsSent() {
        packetsSent.incrementAndGet();
    }

    public void resetRequestCounters() {
        packetsReceived.set(0);
        packetsSent.set(0);
    }

    public long getFsyncThresholdExceedCount() {
        return fsyncThresholdExceedCount.get();
    }

    public void incrementFsyncThresholdExceedCount() {
        fsyncThresholdExceedCount.incrementAndGet();
    }

    public void resetFsyncThresholdExceedCount() {
        fsyncThresholdExceedCount.set(0);
    }

    public long getNonMTLSLocalConnCount() {
        return nonMTLSLocalConnCntr.get();
    }

    public void incrementNonMTLSLocalConnCount() {
        nonMTLSLocalConnCntr.incrementAndGet();
    }

    public void resetNonMTLSLocalConnCount() {
        nonMTLSLocalConnCntr.set(0);
    }

    public long getNonMTLSRemoteConnCount() {
        return nonMTLSRemoteConnCntr.get();
    }

    public void incrementNonMTLSRemoteConnCount() {
        nonMTLSRemoteConnCntr.incrementAndGet();
    }

    public void resetNonMTLSRemoteConnCount() {
        nonMTLSRemoteConnCntr.set(0);
    }

    public long getAuthFailedCount() {
        return authFailedCntr.get();
    }

    public void incrementAuthFailedCount() {
        authFailedCntr.incrementAndGet();
    }

    public void resetAuthFailedCount() {
        authFailedCntr.set(0);
    }

    public void reset() {
        resetLatency();
        resetRequestCounters();
        clientResponseStats.reset();
        ServerMetrics.getMetrics().resetAll();
    }

    public void updateClientResponseSize(int size) {
        clientResponseStats.setLastBufferSize(size);
    }

    public BufferStats getClientResponseStats() {
        return clientResponseStats;
    }

}
