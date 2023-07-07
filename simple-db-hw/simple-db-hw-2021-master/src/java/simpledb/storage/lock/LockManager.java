package simpledb.storage.lock;

import simpledb.storage.PageId;
import simpledb.transaction.TransactionAbortedException;
import simpledb.transaction.TransactionId;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class LockManager {
    private Map<PageId, Map<TransactionId, PageLock>> pageLockMap;

    public LockManager() {
        pageLockMap = new ConcurrentHashMap<>();
    }


    /**
     * 实现的功能申请锁，释放锁，查看指定页的指定事务是否持有锁
     * 思路：
     * 锁管理器中没有任何锁或者该页面没有被任何事务加锁，可以直接加读/写锁；
     * 如果t在页面有锁，分情况讨论
     * 1.加的读锁，直接加锁
     * 2.加的写锁，如果锁数量为1，进行锁升级，锁量大于1，会死锁，异常中断
     * t在页面无锁，分情况讨论
     * 3.加的读锁，锁数量为1，这个锁是读锁则可以加，是写锁就wait；锁数量大于1，说明很多读锁，直接加
     * 4.加的是写锁，不管多个读锁还是一个写锁，都wait
     */

    public synchronized boolean acquireLock(PageId pageId, TransactionId tid, int acquireType) throws TransactionAbortedException, InterruptedException {
        final String lockType = acquireType == 0 ? "read lock" : "write lock";
        final String threadName = Thread.currentThread().getName();

        Map<TransactionId, PageLock> lockMap = pageLockMap.get(pageId);
        if (lockMap == null || lockMap.size() == 0) {
            PageLock pageLock = new PageLock(acquireType, tid);
            lockMap = new ConcurrentHashMap<>();
            lockMap.put(tid, pageLock);
            pageLockMap.put(pageId, lockMap);
            System.out.println(threadName + ": the" + pageId + "have no lock, transaction " + tid + " require" + lockType + " success");
            return true;
        }
        PageLock lock = lockMap.get(tid);
        if (lock != null) {
            if (acquireType == PageLock.SHARE) {
                System.out.println(threadName + ": the" + pageId + "have read lock with the same tid, transaction " + tid + " require" + lockType + " success");
                return true;
            }
            if (acquireType == PageLock.EXCLUSIVE) {
                if (lockMap.size() > 1) {
                    System.out.println(threadName + ": the" + pageId + "have many read locks, transaction " + tid + " require" + lockType + " fail");
                    throw new TransactionAbortedException();
                }
                if (lockMap.size() == 1 && lock.getType() == PageLock.EXCLUSIVE) {
                    System.out.println(threadName + ": the" + pageId + "have write lock with the same tid, transaction " + tid + " require" + lockType + " success");
                    return true;
                }
                if (lockMap.size() == 1 && lock.getType() == PageLock.SHARE) {
                    lock.setType(PageLock.EXCLUSIVE);
                    lockMap.put(tid, lock);
                    pageLockMap.put(pageId, lockMap);
                    System.out.println(threadName + ": the" + pageId + "have read lock with the same tid, transaction " + tid + " require" + lockType + " success and upgrade");
                    return true;
                }
            }
        }
        if (lock == null) {
            if (acquireType == PageLock.SHARE) {
                if (lockMap.size() > 1) {
                    PageLock pageLock = new PageLock(acquireType, tid);
                    lockMap.put(tid, pageLock);
                    pageLockMap.put(pageId, lockMap);
                    System.out.println(threadName + ": the" + pageId + "have many read locks, transaction " + tid + " require" + lockType + " success");
                    return true;
                }

                PageLock lo = null;
                for (PageLock p : lockMap.values()) {
                    lo = p;
                }
                if (lockMap.size() == 1 && lo.getType() == PageLock.SHARE) {
                    PageLock pageLock = new PageLock(acquireType, tid);
                    lockMap.put(tid, pageLock);
                    pageLockMap.put(pageId, lockMap);
                    System.out.println(threadName + ": the" + pageId + "have one lock with different tid, transaction " + tid + " require" + lockType + " success");
                    return true;
                }
                if (lockMap.size() == 1 && lo.getType() == PageLock.EXCLUSIVE) {
                    wait(50);
                    return false;
                }
            }
            if (acquireType == PageLock.EXCLUSIVE) {
                wait(10);
                return false;
            }
        }
        return true;

    }

    /**
     * 释放指定页面的指定事务的锁
     */
    public synchronized void releaseLock(PageId pageId, TransactionId tid) {
        final String threadName = Thread.currentThread().getName();

        Map<TransactionId, PageLock> lockMap = pageLockMap.get(pageId);
        if (lockMap == null) {
            return;
        }
        if (tid == null) {
            return;
        }
        PageLock pageLock = lockMap.get(tid);
        if (pageLock == null) return;
        final String lockType = lockMap.get(tid).getType() == 0 ? "读锁" : "写锁";

        lockMap.remove(tid);
        System.out.println(threadName + " release " + lockType + " in " + pageId + ", the tid lock size is " + lockMap.size());
        if (lockMap.size() == 0) {
            pageLockMap.remove(pageId);
            System.out.println(threadName + "release last lock, the page " + pageId + " have no lock, the page locks size is " + pageLockMap.size());
        }
        this.notifyAll();
    }

    /**
     * 判断事务是否持有对应页的锁
     */
    public synchronized boolean isHoldLock(PageId pageId, TransactionId tid) {
        Map<TransactionId, PageLock> lockMap = pageLockMap.get(pageId);
        if (lockMap == null) {
            return false;
        }
        return lockMap.get(tid) != null;
    }

    /**
     * 释放事务对所有页面的锁
     */
    public synchronized void completeTransaction(TransactionId tid) {
        Set<PageId> set = pageLockMap.keySet();
        for (PageId p : set) {
            releaseLock(p, tid);
        }
    }


}
