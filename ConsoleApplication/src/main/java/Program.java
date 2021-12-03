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

import eu.chargetime.simulator.ChargeBox;
import eu.chargetime.simulator.ChargeBoxFirmware;
import eu.chargetime.simulator.commands.*;
import eu.chargetime.simulator.hardware.*;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class Program {

    private String prefix = "OCCPSim";

    private ConsoleReader consoleReader;
    //private String uriOCPPServer = "http://localhost:8080/steve/websocket/CentralSystemService";
    private String uriOCPPServer = "ws://192.168.1.48:8080/steve/websocket/CentralSystemService";
    private Map<Integer, ChargeBox> chargeboxes = new HashMap<>();

    public static void main(String[] args) {
        new Program(args).startConsoleReaderThread();
    }

    public Program(String[] args) {
        if (args.length != 1 || !isAoderBoderC(args[0])) {
            System.err.println("Instanz A, B oder C muss angegeben werden");
            System.exit(-1);
        }
        prefix += args[0];

        if (args.length == 2) {
            uriOCPPServer = args[1];
        }
        System.out.println("URI OCPP Server: " + uriOCPPServer);
        CommandMap commandMap = createCommandMap();
        composeRoot(commandMap);
        startChargeBoxes("1");
    }

    private boolean isAoderBoderC(String arg) {
        if ("A".equals(arg) || "B".equals(arg) || "C".equals(arg)) {
            return true;
        }
        return false;
    }

    private ChargeBox startChargeBox(String uriOCPPServer, String identity) {
        ChargeBox cb = new ChargeBox(uriOCPPServer, identity);
        new Thread(cb).start();
        return cb;
    }

    private CommandMap createCommandMap() {
        ChargeBoxFirmware firmware = new ChargeBoxFirmware();
        ILock lock = new SimpleLock(firmware,true);
        IOutlet outlet = new OutletLockDecorator(new SimpleOutlet(firmware), lock);
        LockCommand lockCommand = new LockCommand(lock);
        UnlockCommand unlockCommand = new UnlockCommand(lock);
        StatusCommand isLockedCommand = new StatusCommand(lock, outlet);
        PluginCommand pluginCommand = new PluginCommand(outlet);
        PullPlugCommand pullPluginCommand = new PullPlugCommand(outlet);

        CommandMap commandMap = new CommandMap();

        commandMap.addCommand("lock", lockCommand);
        commandMap.addCommand("unlock", unlockCommand);
        commandMap.addCommand("status", isLockedCommand);
        commandMap.addCommand("plugin", pluginCommand);
        commandMap.addCommand("plugout", pullPluginCommand);

        commandMap.addCommand("help", new HelpCommand(commandMap));
        commandMap.addCommand("count", (String param) -> startChargeBoxes(param));
        commandMap.addCommand("quit", (String param) -> stop(param));
        return commandMap;
    }

    private void composeRoot(CommandMap commandMap) {
        IInputHandler commandDispatcher = new CommandDispatcher(commandMap, (String param) -> System.out.println("Unknown command! Try with help"));
        consoleReader = new ConsoleReader(commandDispatcher);
    }

    public void startConsoleReaderThread() {
        System.out.println("Simulator started.");
        new Thread(consoleReader).start();
    }

    private void startChargeBoxes(String param) {
        if (param == null || "".equals(param)) {
            return;
        }
        int count = Integer.valueOf(param).intValue();
        if (count < 1 || count > 9999) {
            System.err.println("count invalid: " + count);
        } else {
            if (chargeboxes.size() == count) {
                System.out.println("Anzahl unveraendert: " + count);
            } else if (chargeboxes.size() > count) {
                final int reduce = chargeboxes.size() - count;
                System.out.println("Anzahl von " + chargeboxes.size() + " wird um " + reduce + " reduziert");
                Collection<Integer> toRemove = getInteger4Remove(reduce);
                toRemove.stream().forEach(c -> { chargeboxes.remove(c).stop(); });
                System.out.println("Reduzierung done: " + chargeboxes.size());
            } else if (chargeboxes.size() < count) {
                final int increase = count - chargeboxes.size();
                System.out.println("Anzahl von " + chargeboxes.size() + " wird um " + increase + " erhoeht");
                for (int i = 1; i <= increase; i++) {
                    final Integer id = getNextFreeInteger();
                    doDelay();
                    chargeboxes.put(id, startChargeBox(uriOCPPServer, prefix + int2Str(id)));
                }
                System.out.println("Erhoehung done: " + chargeboxes.size());
            }
        }
    }

    // Etwas Zeit geben zwischen dem Erzeugen der einzelnen Chargebox Threads...
    private void doDelay() {
        long delay = ThreadLocalRandom.current().nextInt(5, 25);
        try { Thread.sleep(delay); } catch (InterruptedException e) {}
    }

    private Integer getNextFreeInteger() {
        for (int i = 1; i <= 9999; i++) {
            Integer integer = Integer.valueOf(i);
            if (!chargeboxes.containsKey(integer)) {
                return integer;
            }
        }
        throw new RuntimeException("Keine freie Zahl gefunden: " + chargeboxes.keySet().stream().sorted());
    }

    private Collection<Integer> getInteger4Remove(int reduce) {
        final ArrayList arrayList = new ArrayList();
        for (int i = 1; i <= 9999; i++) {
            Integer integer = Integer.valueOf(i);
            if (chargeboxes.containsKey(integer)) {
                arrayList.add(integer);
            }
            if (arrayList.size() == reduce) {
                break;
            }
        }
        return arrayList;
    }

    private String int2Str(Integer i) {
        String r = String.valueOf(i.intValue());
        while (r.length() < 4) {
            r = "0" + r;
        }
        return r;
    }

    private void stop(String param) {
        chargeboxes.values().stream().forEach(c -> c.stop());
        chargeboxes.clear();
        System.out.println("Goodbye!");
        System.exit(0);
    }
}
