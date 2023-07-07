package simpledb;

import simpledb.common.Database;
import simpledb.common.Type;
import simpledb.execution.SeqScan;
import simpledb.storage.*;
import simpledb.transaction.TransactionId;

import java.io.File;
import java.io.IOException;

public class test {
    public static void main(String[] args) throws IOException {
        //lab1 终极实验
        Type[] types = new Type[]{Type.INT_TYPE, Type.INT_TYPE, Type.INT_TYPE};
        String[] names = new String[]{"field0", "field1", "field2"};

        TupleDesc tupleDesc = new TupleDesc(types, names);
        //问题：怎么将文件转化为二进制文件进行读取,配置运行方式
        //创建表
        HeapFile table1 = new HeapFile(new File("some_data_file.dat"), tupleDesc);
        Database.getCatalog().addTable(table1, "test");

        //进行扫描
        TransactionId tid = new TransactionId();
        SeqScan seqScan = new SeqScan(tid, table1.getId());

        try{
            seqScan.open();
            while(seqScan.hasNext()){
                Tuple t = seqScan.next();
                System.out.println(t);
            }
            seqScan.close();
            Database.getBufferPool().transactionComplete(tid);
        }catch (Exception e){
            System.out.println(e.getMessage());
        }

    }
}
