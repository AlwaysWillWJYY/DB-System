package simpledb.storage;

import simpledb.common.Database;
import simpledb.common.DbException;
import simpledb.common.Debug;
import simpledb.common.Permissions;
import simpledb.transaction.Transaction;
import simpledb.transaction.TransactionAbortedException;
import simpledb.transaction.TransactionId;

import java.io.*;
import java.util.*;

/**
 * HeapFile is an implementation of a DbFile that stores a collection of tuples
 * in no particular order. Tuples are stored on pages, each of which is a fixed
 * size, and the file is simply a collection of those pages. HeapFile works
 * closely with HeapPage. The format of HeapPages is described in the HeapPage
 * constructor.
 * 
 * @see HeapPage#HeapPage
 * @author Sam Madden
 */
public class HeapFile implements DbFile {

    private final File file;
    private final TupleDesc td;
    /**
     * Constructs a heap file backed by the specified file.
     * 
     * @param f
     *            the file that stores the on-disk backing store for this heap
     *            file.
     */
    public HeapFile(File f, TupleDesc td) {
        // some code goes here
        this.file = f;
        this.td = td;
    }

    /**
     * Returns the File backing this HeapFile on disk.
     * 
     * @return the File backing this HeapFile on disk.
     */
    public File getFile() {
        // some code goes here
        return file;
    }

    /**
     * Returns an ID uniquely identifying this HeapFile. Implementation note:
     * you will need to generate this tableid somewhere to ensure that each
     * HeapFile has a "unique id," and that you always return the same value for
     * a particular HeapFile. We suggest hashing the absolute file name of the
     * file underlying the heapfile, i.e. f.getAbsoluteFile().hashCode().
     * 
     * @return an ID uniquely identifying this HeapFile.
     */
    public int getId() {
        // some code goes here
        // 文件的绝对路径，取hash。独一无二的id
        return file.getAbsolutePath().hashCode();
    }

    /**
     * Returns the TupleDesc of the table stored in this DbFile.
     * 
     * @return TupleDesc of this DbFile.
     */
    public TupleDesc getTupleDesc() {
        // some code goes here
        return td;
    }

    // see DbFile.java for javadocs
    public Page readPage(PageId pid) {
        // some code goes here
        int tableId = pid.getTableId();
        int pgNo = pid.getPageNumber();

        //随机访问
        RandomAccessFile f = null;
        try {
            //读取当前文件
            f = new RandomAccessFile(file, "r");

            //当前页号*每页字节大小 是否超出文件范围
            if((pgNo + 1) * BufferPool.getPageSize() > f.length()){
                f.close();
                throw new IllegalArgumentException(String.format("表 %d 页 %d 不存在", tableId, pgNo));
            }
            //用于存储
            byte[] bytes = new byte[BufferPool.getPageSize()];
            //指针偏移
            f.seek(pgNo * BufferPool.getPageSize());

            //读取(返回读取的数量)
            int read = f.read(bytes, 0, BufferPool.getPageSize());
            //如果取出少，说明不存在
            if(read != BufferPool.getPageSize()){
                throw new IllegalArgumentException(String.format("表 %d 页 %d 不存在", tableId, pgNo));
            }
            return new HeapPage(new HeapPageId(pid.getTableId(), pid.getPageNumber()), bytes);
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            try{
                // 关闭流
                f.close();
            }catch (IOException e){
                e.printStackTrace();
            }
        }
        throw new IllegalArgumentException(String.format("表 %d 页 %d 不存在", tableId, pgNo));
    }

    // see DbFile.java for javadocs
    public void writePage(Page page) throws IOException {
        // some code goes here
        // not necessary for lab1
        //获取页面序号
        int pageId = page.getId().getPageNumber();
        //不能超过最大页面数
        if(pageId > numPages()){
            throw new IllegalArgumentException();
        }
        //创建写入工具
        RandomAccessFile f = new RandomAccessFile(file, "rw");
        //跳过前面页面
        f.seek(pageId * BufferPool.getPageSize());
        //写入数据
        f.write(page.getPageData());
        f.close();
    }

    /**
     * Returns the number of pages in this HeapFile.
     */
    public int numPages() {
        // some code goes here
        return (int)(Math.floor(file.length() * 1.0 / BufferPool.getPageSize()));
    }

    // see DbFile.java for javadocs
    public List<Page> insertTuple(TransactionId tid, Tuple t)
            throws DbException, IOException, TransactionAbortedException {
        // some code goes here
        // not necessary for lab1
        ArrayList<Page> list = new ArrayList<>();
        //查询现有的页
        for(int pageNo = 0; pageNo < numPages(); pageNo++){
            //查询页
            HeapPageId heapPageId = new HeapPageId(getId(), pageNo);
            HeapPage heapPage = (HeapPage) Database.getBufferPool().getPage(tid, heapPageId, Permissions.READ_WRITE);
            //看当前页是否有空闲空间
            if(heapPage.getNumEmptySlots() == 0){
                Database.getBufferPool().unsafeReleasePage(tid, heapPage.getId());
                continue;
            }
            heapPage.insertTuple(t);
            list.add(heapPage);
            return list;

        }
        //如果所有页都写满,就要新建新的页面来加入(记得开启 append = true 也就是增量增加)
        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(file, true));
        //新建一个空页
        byte[] emptyPage = HeapPage.createEmptyPageData();
        bufferedOutputStream.write(emptyPage);
        //close前会flush刷到文件中
        bufferedOutputStream.close();
        //创建新的页面
        HeapPageId pageId = new HeapPageId(getId(), numPages() - 1);
        HeapPage page = (HeapPage) Database.getBufferPool().getPage(tid, pageId, Permissions.READ_WRITE);
        page.insertTuple(t);
        list.add(page);
        //表明这是脏页
        return list;

    }

    // see DbFile.java for javadocs
    public ArrayList<Page> deleteTuple(TransactionId tid, Tuple t) throws DbException,
            TransactionAbortedException {
        // some code goes here
        // not necessary for lab1
        ArrayList<Page> list = new ArrayList<>();
        PageId pageId = t.getRecordId().getPageId();
        //找到相应页
        HeapPage page = (HeapPage) Database.getBufferPool().getPage(tid, pageId, Permissions.READ_WRITE);
        page.deleteTuple(t);
        list.add(page);
        return list;
    }

    // see DbFile.java for javadocs
    public DbFileIterator iterator(TransactionId tid) {
        // some code goes here
        return new HeapFileItrator(this, tid);
    }
    public static final class HeapFileItrator implements DbFileIterator{
        private final HeapFile heapFile;
        private final TransactionId tid;
        //元组迭代器
        private Iterator<Tuple> iterator;
        private int whichPage;
        public HeapFileItrator(HeapFile heapFile, TransactionId tid){
            this.heapFile = heapFile;
            this.tid = tid;
        }

        @Override
        public void open() throws DbException, TransactionAbortedException {
            //获取第一页的全部元组
            whichPage = 0;
            iterator = getPageTuple(whichPage);
        }
        //获取当前页的所有行
        private Iterator<Tuple> getPageTuple(int pageNumber) throws TransactionAbortedException, DbException {
            //在文件范围内
            if(pageNumber >= 0 && pageNumber < heapFile.numPages()){
                //对应一个表id,一个heapFile
                HeapPageId pid = new HeapPageId(heapFile.getId(), pageNumber);
                //从缓存中查询相应的页面
                HeapPage page = (HeapPage)Database.getBufferPool().getPage(tid, pid, Permissions.READ_ONLY);
                return page.iterator();
            }
            throw new DbException(String.format("heapFile %d not contain page %d", pageNumber, heapFile.getId()));
        }

        @Override
        public boolean hasNext() throws DbException, TransactionAbortedException {
            //如果迭代器为空
            if(iterator == null){
                return false;
            }
            //如果遍历结束
            if(!iterator.hasNext()){
                //还有下一页
                while(whichPage < (heapFile.numPages() - 1)){
                    whichPage++;
                    //获取下一页
                    iterator = getPageTuple(whichPage);
                    if(iterator.hasNext()){
                        return iterator.hasNext();
                    }
                }
                //所有元组获取完毕
                return false;
            }
            return true;
        }

        @Override
        public Tuple next() throws DbException, TransactionAbortedException, NoSuchElementException {
            if(iterator == null || !iterator.hasNext()){
                //没有元组
                throw new NoSuchElementException();
            }
            //返回下一个元组
            return iterator.next();
        }

        @Override
        public void rewind() throws DbException, TransactionAbortedException {
            //清除上一个
            close();
            //重新开始
            open();
        }

        @Override
        public void close() {
            iterator = null;
        }
    }

}

