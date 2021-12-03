package eu.chargetime.simulator.software.ocpp;
/*
    ChargeTime.eu - Charge Point Simulator
    
    MIT License

    Copyright (C) 2016 Thomas Volden <tv@chargetime.eu>

    Permission is hereby granted, free of charge, to any person obtaining a copy
    of this software and associated documentation files (the "Software"), to deal
    in the Software without restriction, including without limitation the rights
    to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
    copies of the Software, and to permit persons to whom the Software is
    furnished to do so, subject to the following conditions:

    The above copyright notice and this permission notice shall be included in all
    copies or substantial portions of the Software.

    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
    FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
    AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
    LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
    OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
    SOFTWARE.
 */

import eu.chargetime.ocpp.ClientEvents;
import eu.chargetime.ocpp.JSONClient;
import eu.chargetime.ocpp.OccurenceConstraintException;
import eu.chargetime.ocpp.UnsupportedFeatureException;
import eu.chargetime.ocpp.feature.profile.ClientCoreProfile;
import eu.chargetime.ocpp.model.Confirmation;
import eu.chargetime.ocpp.model.core.BootNotificationConfirmation;
import eu.chargetime.ocpp.model.core.HeartbeatConfirmation;

import java.util.concurrent.CompletionStage;

public class OCPPClient {

    private final HeartbeatIntervalChange heartbeatIntervalCallback;
    private final ClientCoreProfile coreProfile;
    private JSONClient client;

    public OCPPClient(String uri, String identiy, CoreEventHandler handler, HeartbeatIntervalChange heartbeatIntervalCallback) {
        this.heartbeatIntervalCallback = heartbeatIntervalCallback;
        this.coreProfile = new ClientCoreProfile(handler);
        this.client = new JSONClient(coreProfile, identiy);

        this.client.connect(uri, new ClientEvents() {
            @Override
            public void connectionOpened() {
                System.out.println(identiy + " Connected!");
                try {
                    CompletionStage<Confirmation> confirmation = client.send(coreProfile.createBootNotificationRequest("ChargeTimeEU", "Simulator"));
                    confirmation.whenComplete((confirmationResult, throwable) -> handleBootNotificationResponse(identiy, confirmationResult, throwable));
                } catch (UnsupportedFeatureException e) {
                    e.printStackTrace();
                } catch (OccurenceConstraintException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void connectionClosed() {
                System.out.println(identiy + " Connection closed!");
            }
        });
    }

    public void sendHeartbeat(String identiy) {
//        System.out.println("Sende heartbeat...");
        try {
            CompletionStage<Confirmation> confirmation = client.send(coreProfile.createHeartbeatRequest());
            confirmation.whenComplete((confirmationResult, throwable) -> handleHeartbeatResponse(identiy, confirmationResult, throwable));
        } catch (OccurenceConstraintException e) {
            e.printStackTrace();
        } catch (UnsupportedFeatureException e) {
            e.printStackTrace();
        }
    }

    public void disconnect() {
        this.client.disconnect();
    }

    private void handleBootNotificationResponse(String identiy, Confirmation confirmationResult, Throwable throwable) {
        if (throwable != null) {
            System.err.println(identiy + " BootNotificationResponse mit Throwable: " + throwable);
        } else {
            if (confirmationResult instanceof BootNotificationConfirmation) {
                final BootNotificationConfirmation bnc = (BootNotificationConfirmation)confirmationResult;
                if (bnc.validate()) {
                    System.out.println(identiy + " BootNotificationConfirmation erhalten: " + confirmationResult.toString());
                    heartbeatIntervalCallback.setInterval(bnc.getInterval().intValue());
                } else {
                    System.err.println(identiy + " Invalide BootNotificationConfirmation erhalten: " + confirmationResult.toString());
                }
            } else {
                System.out.println(identiy + " Unerwartete Confirmation erhalten: " + confirmationResult.toString());
            }
        }
    }

    private void handleHeartbeatResponse(String identiy, Confirmation confirmationResult, Throwable throwable) {
        if (throwable != null) {
            System.err.println(identiy + " HeartbeatResponse mit Throwable: " + throwable);
        } else {
            if (confirmationResult instanceof HeartbeatConfirmation) {
                final HeartbeatConfirmation hbc = (HeartbeatConfirmation)confirmationResult;
                if (hbc.validate()) {
                    System.out.println(identiy + " HeartbeatConfirmation erhalten: " + confirmationResult.toString());
                } else {
                    System.err.println(identiy + " Invalide HeartbeatConfirmation erhalten: " + confirmationResult.toString());
                }
            } else {
                System.out.println(identiy + " Unerwartete Confirmation erhalten: " + confirmationResult.toString());
            }
        }
    }
}
