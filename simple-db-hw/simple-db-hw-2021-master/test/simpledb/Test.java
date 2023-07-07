package simpledb;


import java.util.*;
import java.util.concurrent.*;

class Test{
        private static final ThreadPoolExecutor pool = new ThreadPoolExecutor(1,1,0l, TimeUnit.SECONDS,new ArrayBlockingQueue<>(100));
        public static void main(String[] args) throws InterruptedException {
            LinkedBlockingQueue<LuckyMoney> luckyMoneys = new LinkedBlockingQueue<>();

            List<FutureTask> futureTasks = new ArrayList<>();
            //准备抢红包
            for (int i = 0; i < 5; i++) {
                FutureTask<Object> futureTask = new FutureTask<>(new CatchLuckMoney(luckyMoneys, "name" + i), null);
                new Thread(futureTask,"name"+i).start();
                futureTasks.add(futureTask);
            }
            Thread.sleep(5);//确保抢红包线



            // 程准备就绪
            Random random = new Random();
            Integer total = 100;
            //发5个红包
            for (int i = 0; i < 5; i++) {
                int t = random.nextInt(total);
                luckyMoneys.put(new LuckyMoney("红包"+i, t+1));
                total -= t;
                if(total <= 0) break;
            }
            //等到红包抢完
            while (!luckyMoneys.isEmpty()){
                Thread.sleep(1);
            }
            //终止抢红包线程
            for (FutureTask futureTask : futureTasks) {
                futureTask.cancel(true);
            }
            new HashMap<>();


    }


   
}
class CatchLuckMoney implements Runnable {

    public CatchLuckMoney(LinkedBlockingQueue<LuckyMoney> luckyMoneys, String name) {
        this.luckyMoneys = luckyMoneys;
        this.name = name;
    }

    private LinkedBlockingQueue<LuckyMoney> luckyMoneys;
    private String name;

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            LuckyMoney redPackage = null;
            try {
                redPackage = luckyMoneys.take();
            } catch (InterruptedException e) {
//        e.printStackTrace();
                break;
            }
            System.out.println(name + "抢到了-->" + redPackage);
            luckyMoneys.remove(redPackage);
        }
        System.out.println("end>>>>>>>>>>>>>>>>>>>>>>>>>>>>"+Thread.currentThread().getName());
    }
}

class LuckyMoney {

    public LuckyMoney(String name, Integer money) {
        this.name = name;
        this.money = money;
    }

    private String name;
    private Integer money;

    @Override
    public String toString() {
        return "LuckyMoney{" +
                "name='" + name + '\'' +
                ", money=" + money +
                '}';
    }
}







