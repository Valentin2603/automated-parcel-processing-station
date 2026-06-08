import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;


public class Storage {
    private final int storageSize;
    private static final int PRIORITY_COUNT = 3;
    private int packagesCount;
    private final int directionsCount;
    private final List<Package> packages;
    private final Lock lock = new ReentrantLock();
    private final Condition notFull = lock.newCondition();
    private final Condition notEmpty = lock.newCondition();

    public Storage(int storageSize, int directionsCount) {
        this.storageSize = storageSize;
        this.directionsCount = directionsCount;
        this.packages = new ArrayList<>();
        packagesCount = 0;
    }

    public void addPackage() throws InterruptedException {
        lock.lock();

        try {
            while (packages.size() >= storageSize) {
                notFull.await();
            }

            int randomDirection = ThreadLocalRandom.current().nextInt(1, directionsCount + 1);
            int randomPriority = ThreadLocalRandom.current().nextInt(1, PRIORITY_COUNT + 1);

            Package pack = new Package(randomDirection, randomPriority, ++packagesCount);
            packages.add(pack);
            notEmpty.signalAll();
        } finally {
            lock.unlock();
        }

    }

    public Package givePackageToDevice(Device device ) throws InterruptedException{
        lock.lock();

        try {
            while (packages.stream().noneMatch(pack -> pack.directionNumber() == device.getDirectionNumber())) {
                notEmpty.await();
            }

            Package currentPack = packages.stream()
                    .filter(pack -> pack.directionNumber() == device.getDirectionNumber())
                    .min(Comparator.comparingInt(Package::priority).reversed().thenComparingInt(Package::id))
                    .orElse(new Package(-1, -1, -1));

            removePackage(currentPack);
            return currentPack;

        } finally {
            lock.unlock();
        }
    }

    private void removePackage(Package pack) {
        packages.remove(pack);
        notFull.signalAll();
    }



    public StorageSnapshot getStorageSnapshot() {
        lock.lock();

        try {
            Map<Integer, Long> sizeOfEachPriority = packages.stream()
                    .collect(Collectors.groupingBy(
                            Package::priority,
                            Collectors.counting()
                    ));

            return new StorageSnapshot(packages.size(), sizeOfEachPriority.getOrDefault(1, 0L),
                    sizeOfEachPriority.getOrDefault(2, 0L), sizeOfEachPriority.getOrDefault(3, 0L));
        } finally {
            lock.unlock();
        }
    }




}
