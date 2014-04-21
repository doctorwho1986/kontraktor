package de.ruedigermoeller.disruptorbench;

import de.ruedigermoeller.serialization.FSTConfiguration;

import java.io.IOException;
import java.util.concurrent.*;
import java.util.concurrent.locks.LockSupport;

/**
 * Created by ruedi on 21.04.14.
 */
public class UnorderedThreadPoolService implements LoadFeeder.Service {

    static FSTConfiguration conf = FSTConfiguration.getDefaultConfiguration();
    private LoadFeeder serv;
    private ExecutorService executor;

    private static class TestThread extends Thread {
        public TestRequest req = new TestRequest();
        public TestThread(Runnable r, String name) {
            super(r,name);
        }
    }

    ThreadLocal<LoadFeeder.Response> respTLocal = new ThreadLocal() {
        @Override
        protected LoadFeeder.Response initialValue() {
            return new LoadFeeder.Response(null,0);
        }
    };

    SharedData sharedData;

    public UnorderedThreadPoolService(LoadFeeder serv, SharedData data, int numWorkers) {
        this.serv = serv;
        init(numWorkers);
        sharedData = data;
    }

    public void init(int workers) {
        executor = (ExecutorService) createBoundedThreadExecutor(workers, "pool", 40000);
//        executor = Executors.newFixedThreadPool(workers,new ThreadFactory() {
//            @Override
//            public Thread newThread(Runnable r) {
//                return new TestThread( r, "-" );
//            }
//        });
    }

    @Override
    public void processRequest(final byte[] rawRequest) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    final TestRequest testRequest = ((TestThread)Thread.currentThread()).req;
                    testRequest.rawRequest = rawRequest;
                    testRequest.decode();
                    testRequest.process(sharedData);
                    testRequest.encode(serv);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void shutdown() {
        executor.shutdownNow();
        try {
            executor.awaitTermination(10000, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static Executor createBoundedThreadExecutor(int workers, final String name, int qsize) {
        ThreadPoolExecutor res = new ThreadPoolExecutor(workers,workers,1000, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(qsize));
//        ThreadPoolExecutor res = new ThreadPoolExecutor(1,1,1000, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(qsize));
        res.setThreadFactory(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                return new TestThread( r, name );
            }
        });
        res.setRejectedExecutionHandler(new RejectedExecutionHandler() {
            @Override
            public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                while ( !executor.isShutdown() && !executor.getQueue().offer(r)) {
                    LockSupport.parkNanos(1000);
                }
            }
        } );
        return res;
    }

    public static void main(String a[]) throws IOException, ClassNotFoundException {
        for ( int i = 0; i < 50; i++ ) {
            LoadFeeder feeder = new LoadFeeder(10000);
            UnorderedThreadPoolService service = new UnorderedThreadPoolService(feeder,new LockFreeSharedData(),4);
//            UnorderedThreadPoolService service = new UnorderedThreadPoolService(feeder,new SynchronizedSharedData(),2);
            feeder.run(service, 1000 * 1000);
        }
    }

}