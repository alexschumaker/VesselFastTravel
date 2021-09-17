package net.cobblers.vft;

import java.util.TimerTask;

public class VoyageTimer extends TimerTask {
    private VesselTransport sailAction;
    private Queue queue;
    private int remainingTime = 5;

    public VoyageTimer(VesselTransport sailAction, Queue queue) {
        this.sailAction = sailAction;
        this.queue = queue;
    }

    @Override
    public void run() {
        if (queue.getStatus().equals("CANCELLED")) {
            sailAction.removeDestination();
            sailAction = null;
            queue = null;
            this.cancel();

        } else if (remainingTime > 0) {
            queue.systemMsgToServer("Setting sail in " + remainingTime + "...");

        } else {
            if (sailAction.transportPlayers(queue)) {
                sailAction.status = "DESTINATION";

                if (queue.getStatus().equals("READY")) {
                    queue.complete();
                }
            } else {
                queue.resetQueue();
            }

            this.cancel();
        }

        remainingTime--;
    }
}
