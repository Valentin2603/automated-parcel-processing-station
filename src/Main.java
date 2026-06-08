import java.util.*;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static java.lang.Math.max;
import static java.lang.Thread.currentThread;
import static java.lang.Thread.sleep;

public class Main {
    private static final Scanner scanner = new Scanner(System.in);
    private volatile static boolean running = true;

    public static void main(String[] args) throws InterruptedException {

        final int storageSize = readInt("Введите ёмкость хранилища: ");

        final int directions = readInt("Введите количество направлений обработки: ");;

        final int workers = readInt("Введите количество рабочих устройств в каждом направлении: ");

        final int timeWorking = readInt("Введите время работы программы: ");

        Storage storage = new Storage(storageSize, directions);

        Device[][] devices = new Device[directions + 1][workers + 1];

        for (int direction = 1; direction <= directions; direction++) {
            for (int worker = 1; worker <= workers; worker++) {
                devices[direction][worker] = new Device(direction, worker, storage);
            }
        }

        Thread generatorThread = new Thread(() -> {
            while (running) {
                try {
                    storage.addPackage();
                    sleep(ThreadLocalRandom.current().nextInt(100, 500));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }, "generatorThread");

        List<Thread> workerThreads = createWorkersThreads(directions, workers, devices);

        Thread monitorThread = new Thread(() -> {
            System.out.println();

            while (running) {
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (final InterruptedException interruptedException) {
                    currentThread().interrupt();
                    return;
                }

                StorageSnapshot storageSnapshot = storage.getStorageSnapshot();

                List<DeviceSnapshot> deviceSnapshots = Arrays.stream(devices)
                        .flatMap(Arrays::stream)
                        .filter(Objects::nonNull)
                        .map(Device::getDeviceSnapshot)
                        .filter(Objects::nonNull)
                        .toList();

                System.out.println("Storage: size = " + storageSnapshot.size() + " | size of each priority: first - " +
                        storageSnapshot.firstPriority() + " second - " + storageSnapshot.secondPriority() + " third - " + storageSnapshot.thirdPriority());


                for (DeviceSnapshot snapshot : deviceSnapshots) {
                    if (!snapshot.isWorking()) {
                        System.out.println("Device_" + snapshot.direction() + "_" + snapshot.number() + " | isn't working");
                    } else {
                        System.out.println("Device_" + snapshot.direction() + "_" + snapshot.number() + " | is working | direction - "
                                + snapshot.direction()  + " | priority = " + snapshot.priority() + " | remaining time = " +
                                max(0.0, snapshot.remainingTime() / 1000));
                    }
                }

                System.out.println("-------------------------------------------------------------------------------");

            }
        }, "MonitorThread");



        generatorThread.start();
        startThreads(workerThreads);
        monitorThread.start();

        TimeUnit.SECONDS.sleep(timeWorking);

        finishWorking(workerThreads, generatorThread, monitorThread);

        System.out.println("Process was finished");
    }



    public static void finishWorking(List<Thread> workerThreads, Thread generatorThread, Thread monitorThread) throws InterruptedException{
        running = false;

        for (Thread workerThread : workerThreads) {
            workerThread.interrupt();
            workerThread.join();
        }

        generatorThread.interrupt();
        monitorThread.interrupt();

        generatorThread.join();
        monitorThread.join();

    }


    public static void startThreads(List<Thread> threads) {
        IntStream.range(0, threads.size()).forEach(ind -> threads.get(ind).start());
    }


    public static List<Thread> createWorkersThreads(int directionCount, int workersCount, Device[][] devices){
        List<Thread> workerThreads = new ArrayList<>();


        for (int direction = 1; direction <= directionCount; direction++) {
            for (int worker = 1; worker <= workersCount; worker++) {

                int finalDirection = direction;
                int finalWorker = worker;
                workerThreads.add(new Thread(() -> {
                    Device device = devices[finalDirection][finalWorker];

                    while (running) {
                        try {
                            int processingTime = ThreadLocalRandom.current().nextInt(3000, 7000);
                            device.setProcessingTime(processingTime);

                            device.setIsWorking(true);

                            device.acquirePack();

                            sleep(processingTime);

                        } catch (InterruptedException e) {
                            currentThread().interrupt();
                            return;
                        } finally {
                            device.setPack(null);
                            device.setIsWorking(false);
                        }
                    }

                }, "Worker_" + finalDirection + "_" + worker));
            }
        }

        return workerThreads;
    }


    public static int readInt(String message) {
        System.out.println(message);
        int num;
        num = scanner.nextInt();

        while (num < 2 || num >= 100) {
            System.out.println("Введите число от 2 до 100");
            num = scanner.nextInt();
        }

        return num;
    }


}