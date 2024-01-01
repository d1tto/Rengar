package rengar.thirdparty.hunter.cn.ac.ios.Utils.timeout;


import rengar.thirdparty.hunter.cn.ac.ios.Bean.Pair;
import rengar.thirdparty.hunter.cn.ac.ios.Bean.AttackBean;

import java.util.concurrent.*;

/**
 * @author pqc
 */
public class TimeoutTaskUtils {

    /**
     * 执行一个有时间限制的任务
     *
     * @param task 待执行的任务
     * @return
     */
    public static Pair<Boolean, Integer> execute(TimeoutTask task) {
        Pair<Boolean, Integer> result = new Pair<>(false, 0);
        ExecutorService threadPool = Executors.newSingleThreadExecutor();
        long time = System.currentTimeMillis();
        try {
            Future<Pair<Boolean, Integer>> future = threadPool.submit(task);
            result = future.get(AttackBean.TIME_OUT, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            time = System.currentTimeMillis() - time;
            result = new Pair<>(time >= AttackBean.TIME_OUT, (int) time);
            threadPool.shutdownNow();
        } catch (ExecutionException e) {
            result = new Pair<>(false, AttackBean.STACK_ERROR);
            threadPool.shutdownNow();
        } catch (InterruptedException in) {
            result = new Pair<>(false, AttackBean.INTERRUPTED);
            threadPool.shutdownNow();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            task.close();
            threadPool.shutdownNow();
        }
        return result;
    }
}