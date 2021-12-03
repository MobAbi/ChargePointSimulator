package eu.chargetime.simulator;

import eu.chargetime.simulator.software.ocpp.HeartbeatIntervalChange;

import java.util.concurrent.ThreadLocalRandom;

class HeartbeatThread implements Runnable, HeartbeatIntervalChange {
    private final SendHeartbeatCallback sendHeartbeatCallback;
    private int heartbeatIntervalSeconds;
    private boolean run;
    private long lastCheck;

    HeartbeatThread(SendHeartbeatCallback sendHeartbeatCallback) {
        this.sendHeartbeatCallback = sendHeartbeatCallback;
        this.heartbeatIntervalSeconds = 180;
        this.run = true;
        this.lastCheck = System.currentTimeMillis();
    }

    @Override
    public void run() {
        while (run) {
            final long current = System.currentTimeMillis();
            if (current >= lastCheck + (heartbeatIntervalSeconds*1000)) {
                doDelay();
                sendHeartbeat();
                lastCheck = current;
            }
            try { Thread.sleep(100); } catch (InterruptedException e) {}
            //System.out.println("run()... " + (current - lastCheck)/1000);
        }
    }

    private void doDelay() {
        long delay = ThreadLocalRandom.current().nextInt(250, 750);
        try { Thread.sleep(delay); } catch (InterruptedException e) {}
    }

    private void sendHeartbeat() {
        sendHeartbeatCallback.sendHeartbeat();
    }

    public void stop() {
        System.out.println(sendHeartbeatCallback.getIdentiy() + " HeartbeatThread stopped");
        run = false;
    }

    @Override
    public void setInterval(int seconds) {
//        System.out.println("Setze Interval: " + seconds);
        this.heartbeatIntervalSeconds = seconds;
    }
}
