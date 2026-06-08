import java.util.concurrent.atomic.AtomicBoolean;


public class Device {
    private final int directionNumber;
    private final int number;
    private final AtomicBoolean isWorking;
    private Package pack;
    private final Storage storage;
    private double finishTime;
    private double processingTime;

    public Device(final int directionNumber, int number, Storage storage) {
        this.directionNumber = directionNumber;
        this.number = number;
        this.storage = storage;
        isWorking = new AtomicBoolean(false);
    }


    public void acquirePack() throws InterruptedException {
        Package currentPack = storage.givePackageToDevice(this);

        finishTime = System.currentTimeMillis() + processingTime;

        pack = currentPack;

    }

    public int getDirectionNumber() {
        return directionNumber;
    }

    public void setIsWorking(boolean isWorking) {
        this.isWorking.set(isWorking);
    }

    public void setProcessingTime(double time) {
        this.processingTime = time;
    }

    public void setPack (Package pack) {
        this.pack = pack;
    }

    public DeviceSnapshot getDeviceSnapshot() {
        if (!isWorking() || pack == null) {
            return new DeviceSnapshot(false, directionNumber, number, 0, 0);
        }

        double remainingTime = finishTime - System.currentTimeMillis();

        return new DeviceSnapshot(isWorking.get(), directionNumber, number, pack.priority(), remainingTime);

    }


    public boolean isWorking() {
        return isWorking.get();
    }
}
