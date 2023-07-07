package simpledb.execution;

import simpledb.common.Type;
import simpledb.storage.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Knows how to compute some aggregate over a set of StringFields.
 */
public class StringAggregator implements Aggregator {

    private static final long serialVersionUID = 1L;
    // 分组字段的序号（是一个字段，用于辨别是否是该类型）举例：group by 字段
    private int gbfield;
    private Type gbfieldtype;
    // 聚合字段的序号（是用于取新插入的值） 举例： sum(字段),min(字段)
    private int afield;
    private Op what;
    Map<Field, Integer> map;

    /**
     * Aggregate constructor
     * @param gbfield the 0-based index of the group-by field in the tuple, or NO_GROUPING if there is no grouping
     * @param gbfieldtype the type of the group by field (e.g., Type.INT_TYPE), or null if there is no grouping
     * @param afield the 0-based index of the aggregate field in the tuple
     * @param what aggregation operator to use -- only supports COUNT
     * @throws IllegalArgumentException if what != COUNT
     */

    public StringAggregator(int gbfield, Type gbfieldtype, int afield, Op what) {
        // some code goes here
        if(!what.equals(Op.COUNT)){
            throw new IllegalArgumentException("String类型只支持计数");
        }
        this.gbfield = gbfield;
        this.gbfieldtype = gbfieldtype;
        this.afield = afield;
        this.what = what;
        map = new HashMap<>();
    }

    /**
     * Merge a new tuple into the aggregate, grouping as indicated in the constructor
     * @param tup the Tuple containing an aggregate field and a group-by field
     */
    public void mergeTupleIntoGroup(Tuple tup) {
        // some code goes here
        Field field = gbfield == NO_GROUPING ? null : tup.getField(gbfield);
        if(map.containsKey(field)){
            map.put(field, map.get(field) + 1);
        }else{
            map.put(field, 1);
        }
    }

    /**
     * Create a OpIterator over group aggregate results.
     *
     * @return a OpIterator whose tuples are the pair (groupVal,
     *   aggregateVal) if using group, or a single (aggregateVal) if no
     *   grouping. The aggregateVal is determined by the type of
     *   aggregate specified in the constructor.
     */
    public OpIterator iterator() {
        // some code goes here
        //构造tuple
        Type[] types;
        String[] names;
        TupleDesc tupleDesc;
        //存储结果
        List<Tuple> tuples = new ArrayList<>();
        if(gbfield == NO_GROUPING){
            types = new Type[]{Type.INT_TYPE};
            names = new String[]{"aggregateVal"};
            tupleDesc = new TupleDesc(types, names);
            //组合成行
            Tuple tuple = new Tuple(tupleDesc);
            tuple.setField(0, new IntField(map.get(null)));
            tuples.add(tuple);
        }else{
            types = new Type[]{gbfieldtype, Type.INT_TYPE};
            names = new String[]{"groupType", "aggregateVal"};
            tupleDesc = new TupleDesc(types, names);
            for(Field t : map.keySet()){
                Tuple tuple = new Tuple(tupleDesc);
                if(gbfieldtype == Type.INT_TYPE){
                    IntField intField = (IntField)t;
                    tuple.setField(0, intField);
                }else if(gbfieldtype == Type.STRING_TYPE){
                    StringField stringField = (StringField) t;
                    tuple.setField(0, stringField);
                }
                IntField resultField = new IntField(map.get(t));
                tuple.setField(1,resultField);
                tuples.add(tuple);
            }
        }
        return new TupleIterator(tupleDesc, tuples);
    }

}
