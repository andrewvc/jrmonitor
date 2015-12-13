package com.purbon.jruby.monitor;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.*;

/**
 * Created by purbon on 12/12/15.
 */
public class HotThreads {

    public class ThreadReport {

        private static final String CPU_TIME = "cpu.time";
        private static final String BLOCKED_COUNT = "blocked.count";
        private static final String BLOCKED_TIME = "blocked.time";
        private static final String WAITED_COUNT = "waited.count";
        private static final String WAITED_TIME = "waited.time";
        private static final String THREAD_NAME = "thread.name";
        private static final String THREAD_STATE = "thread.state";

        private Map<String, Object> map = new HashMap<String, Object>();

        public ThreadReport(ThreadInfo info, long cpuTime) {
            map.put(CPU_TIME, cpuTime);
            map.put(BLOCKED_COUNT, info.getBlockedCount());
            map.put(BLOCKED_TIME, info.getBlockedTime());
            map.put(WAITED_COUNT, info.getWaitedCount());
            map.put(WAITED_TIME, info.getWaitedTime());
            map.put(THREAD_NAME, info.getThreadName());
            map.put(THREAD_STATE, info.getThreadState().name());
        }

        public Map<String, Object> toHash() {
            return map;
        }

        public Long getCpuTime() {
            return (Long)map.get(CPU_TIME);
        }

        public String getThreadState() {
            return (String)map.get(THREAD_STATE);
        }

        public String getThreadName() {
            return (String)map.get(THREAD_NAME);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("name");
            sb.append(",");
            sb.append(map.get(THREAD_NAME));
            sb.append(",");
            sb.append("cpuTime");
            sb.append(",");
            sb.append(map.get(CPU_TIME));
            sb.append(",");
            sb.append("state");
            sb.append(",");
            sb.append(map.get(THREAD_STATE));
            sb.append(",");
            return sb.toString();
        }
    }

    public List<ThreadReport> detect() {
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        enableCpuTime(threadMXBean);

        Map<Long, ThreadReport> reports = new HashMap<Long, ThreadReport>();

        for(long threadId : threadMXBean.getAllThreadIds()) {
            if (Thread.currentThread().getId() == threadId) {
                continue;
            }

            long cpuTime = threadMXBean.getThreadCpuTime(threadId);
            if (cpuTime == -1) {
                continue;
            }

            ThreadInfo info = threadMXBean.getThreadInfo(threadId, 0);
            reports.put(threadId, new ThreadReport(info, cpuTime));
        }
        List<ThreadReport> list = Arrays.asList(reports.values().toArray(new ThreadReport[reports.size()]));
        return sort(list);
    }

    public List<ThreadReport> sort(List<ThreadReport> reports) {
        Collections.sort(reports, new Comparator<ThreadReport>() {
            public int compare(ThreadReport a, ThreadReport b) {

                if (a.getCpuTime() > b.getCpuTime()) {
                    return -1;
                } else if (a.getCpuTime() < b.getCpuTime()) {
                    return 1;
                } else {
                    return a.getThreadState().compareTo(b.getThreadState());
                }
            }
        });
        return reports;
    }

    public String buildReport(Map<Long, ThreadReport> reports) {
        StringBuilder sb = new StringBuilder();

        for(ThreadReport report : reports.values()) {
            sb.append(report);
            sb.append("\n");
        }

        return sb.toString();
    }

    private void enableCpuTime(ThreadMXBean threadMXBean) {
        try {
            if (threadMXBean.isThreadCpuTimeSupported()) {
                if (!threadMXBean.isThreadCpuTimeEnabled()) {
                    threadMXBean.setThreadCpuTimeEnabled(true);
                }
            }
        } catch (SecurityException ex) {

        }
    }
}