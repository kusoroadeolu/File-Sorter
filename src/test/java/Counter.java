import java.util.concurrent.atomic.AtomicInteger;

public class Counter {

    public int increment(AtomicInteger count){
        return count.addAndGet(1);
    }

    public int decrement(AtomicInteger count){
        return count.addAndGet(-1);
    }




}
