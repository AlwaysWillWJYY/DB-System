package simpledb.storage.lock;
import simpledb.transaction.TransactionId;


public class PageLock {
    /**
     * 共享锁
     */
    public static final int SHARE = 0;
    /**
     * 排它锁
     */
    public static final int EXCLUSIVE = 1;
    /**
     * 锁类型
     */
    private int type;
    /**
     * 事务ID
     */
    private TransactionId transactionId;

    public PageLock(int type, TransactionId transactionId){
        this.type = type;
        this.transactionId = transactionId;
    }

    @Override
    public String toString() {
        return "PageLock{" +
                "type=" + type +
                ", transactionId=" + transactionId +
                '}';
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public TransactionId getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(TransactionId transactionId) {
        this.transactionId = transactionId;
    }

}

