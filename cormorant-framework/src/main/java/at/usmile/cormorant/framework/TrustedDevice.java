package at.usmile.cormorant.framework;

public class TrustedDevice {

    private String id;

    private String device;

    private double screenSize;

    public TrustedDevice(String id, String device, double screenSize) {
        this.id = id;
        this.device = device;
        this.screenSize = screenSize;
    }

    public String getDevice() {
        return device;
    }

    public String getId() {
        return id;
    }

    public double getScreenSize() {
        return screenSize;
    }
}
