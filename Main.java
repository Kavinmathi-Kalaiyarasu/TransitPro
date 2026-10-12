import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.Scanner;

// ----------------------------------------------------
// STOP CLASS
// ----------------------------------------------------
class Stop {
    private int stopId;
    private String stopName;

    public Stop(int stopId, String stopName) {
        this.stopId = stopId;
        this.stopName = stopName;
    }

    public int getStopId() {
        return stopId;
    }

    public String getStopName() {
        return stopName;
    }

    @Override
    public String toString() {
        return stopId + " - " + stopName;
    }
}

// ----------------------------------------------------
// BUS CLASS
// ----------------------------------------------------
class Bus {
    private int busId;
    private int capacity;
    private boolean available;

    public Bus(int busId, int capacity) {
        this.busId = busId;
        this.capacity = capacity;
        this.available = true;
    }

    public int getBusId() {
        return busId;
    }

    public int getCapacity() {
        return capacity;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    // Method for polymorphism
    public String getBusType() {
        return "Bus";
    }

    // Check whether the bus can carry the passengers
    public boolean canBoard(int passengers) {
        return passengers <= capacity;
    }

    @Override
    public String toString() {
        return "Bus ID: " + busId +
               ", Type: " + getBusType() +
               ", Capacity: " + capacity +
               ", Available: " + available;
    }
}

// ----------------------------------------------------
// MINIBUS - INHERITANCE
// ----------------------------------------------------
class MiniBus extends Bus {

    public MiniBus(int busId, int capacity) {
        super(busId, capacity);
    }

    @Override
    public String getBusType() {
        return "MiniBus";
    }
}

// ----------------------------------------------------
// STANDARD BUS - INHERITANCE
// ----------------------------------------------------
class StandardBus extends Bus {

    public StandardBus(int busId, int capacity) {
        super(busId, capacity);
    }

    @Override
    public String getBusType() {
        return "StandardBus";
    }
}

// ----------------------------------------------------
// DEMAND REQUEST CLASS
// ----------------------------------------------------
class DemandRequest {
    private int requestId;
    private Stop pickupStop;
    private Stop destination;
    private int passengers;
    private int arrivalOrder;

    public DemandRequest(int requestId,
                         Stop pickupStop,
                         Stop destination,
                         int passengers,
                         int arrivalOrder) {

        this.requestId = requestId;
        this.pickupStop = pickupStop;
        this.destination = destination;
        this.passengers = passengers;
        this.arrivalOrder = arrivalOrder;
    }

    public int getRequestId() {
        return requestId;
    }

    public Stop getPickupStop() {
        return pickupStop;
    }

    public Stop getDestination() {
        return destination;
    }

    public int getPassengers() {
        return passengers;
    }

    public int getArrivalOrder() {
        return arrivalOrder;
    }

    public void reducePassengers(int number) {
        passengers = passengers - number;
    }

    @Override
    public String toString() {
        return "Request ID: " + requestId +
               ", From: " + pickupStop.getStopName() +
               ", To: " + destination.getStopName() +
               ", Passengers: " + passengers;
    }
}

// ----------------------------------------------------
// TRANSPORT SERVICE CLASS
// ----------------------------------------------------
class TransportService {

    // HashMaps for storing objects
    private HashMap<Integer, Stop> stops;
    private HashMap<Integer, Bus> buses;
    private HashMap<Integer, DemandRequest> demands;

    // PriorityQueue:
    // Higher passenger demand gets higher priority.
    // If passengers are equal, earlier request gets priority.
    private PriorityQueue<DemandRequest> demandQueue;

    private int nextArrivalOrder = 1;

    public TransportService() {

        stops = new HashMap<>();
        buses = new HashMap<>();
        demands = new HashMap<>();

        demandQueue = new PriorityQueue<>(
            (d1, d2) -> {

                // Higher passenger count first
                if (d1.getPassengers() != d2.getPassengers()) {
                    return Integer.compare(
                        d2.getPassengers(),
                        d1.getPassengers()
                    );
                }

                // Earlier arrival first
                return Integer.compare(
                    d1.getArrivalOrder(),
                    d2.getArrivalOrder()
                );
            }
        );
    }

    // ------------------------------------------------
    // ADD STOP
    // ------------------------------------------------
    public void addStop(Stop stop) {
        addStopInternal(stop);
        DatabaseManager.saveStop(stop);
    }

    public void addStopInternal(Stop stop) {
        stops.put(stop.getStopId(), stop);
    }

    // ------------------------------------------------
    // DISPLAY STOPS
    // ------------------------------------------------
    public void displayStops() {

        System.out.println("\n--- STOPS ---");

        for (Stop stop : stops.values()) {
            System.out.println(stop);
        }
    }

    // ------------------------------------------------
    // ADD BUS
    // ------------------------------------------------
    public void addBus(Bus bus) {
        addBusInternal(bus);
        DatabaseManager.saveBus(bus);
    }

    public void addBusInternal(Bus bus) {
        buses.put(bus.getBusId(), bus);
    }

    // ------------------------------------------------
    // DISPLAY BUSES
    // ------------------------------------------------
    public void displayBuses() {

        System.out.println("\n--- BUSES ---");

        for (Bus bus : buses.values()) {
            System.out.println(bus);
        }
    }

    // ------------------------------------------------
    // ADD DEMAND
    // ------------------------------------------------
    public void addDemand(DemandRequest demand) {
        addDemandInternal(demand);
        DatabaseManager.saveDemand(demand);

        System.out.println(
            "Demand request added successfully."
        );
    }

    public void addDemandInternal(DemandRequest demand) {
        demands.put(
            demand.getRequestId(),
            demand
        );

        demandQueue.add(demand);
    }

    public void loadFromDatabase() {
        DatabaseManager.loadIntoService(this);
    }

    // ------------------------------------------------
    // DISPLAY WAITING DEMANDS
    // ------------------------------------------------
    public void displayWaitingDemands() {

        System.out.println("\n--- WAITING DEMANDS ---");

        if (demandQueue.isEmpty()) {
            System.out.println("No waiting demands.");
            return;
        }

        // Temporary queue so original queue is not destroyed
        PriorityQueue<DemandRequest> temp =
            new PriorityQueue<>(demandQueue);

        while (!temp.isEmpty()) {
            System.out.println(temp.poll());
        }
    }

    // ------------------------------------------------
    // FIND AVAILABLE BUS
    // ------------------------------------------------
    private Bus findAvailableBus() {

        for (Bus bus : buses.values()) {

            if (bus.isAvailable()) {
                return bus;
            }
        }

        return null;
    }

    // ------------------------------------------------
    // ALLOCATE BUS
    // ------------------------------------------------
    public void allocateBus() {

        if (demandQueue.isEmpty()) {

            System.out.println(
                "No waiting demand."
            );

            return;
        }

        Bus bus = findAvailableBus();

        if (bus == null) {

            System.out.println(
                "No available bus."
            );

            return;
        }

        DemandRequest demand = demandQueue.poll();

        int passengers = demand.getPassengers();

        System.out.println(
            "\nAllocating Bus " +
            bus.getBusId() +
            " to Request " +
            demand.getRequestId()
        );

        // Check capacity
        if (passengers <= bus.getCapacity()) {

            System.out.println(
                "Passengers served: " +
                passengers
            );

            System.out.println(
                "Bus capacity: " +
                bus.getCapacity()
            );

            demand.reducePassengers(passengers);

            bus.setAvailable(false);

            System.out.println(
                "Bus allocated successfully."
            );

        } else {

            // Only capacity number of passengers can board
            int served = bus.getCapacity();

            int remaining =
                passengers - served;

            System.out.println(
                "Demand exceeds bus capacity."
            );

            System.out.println(
                "Passengers served: " +
                served
            );

            System.out.println(
                "Passengers remaining: " +
                remaining
            );

            demand.reducePassengers(served);

            // Put remaining demand back into queue
            demandQueue.add(demand);

            bus.setAvailable(false);

            System.out.println(
                "Remaining passengers kept in waiting queue."
            );
        }
    }

    // ------------------------------------------------
    // COMPLETE BUS TRIP
    // ------------------------------------------------
    public void completeBusTrip(int busId) {

        Bus bus = buses.get(busId);

        if (bus == null) {

            System.out.println(
                "Bus not found."
            );

            return;
        }

        if (bus.isAvailable()) {

            System.out.println(
                "Bus is already available."
            );

            return;
        }

        bus.setAvailable(true);

        System.out.println(
            "Bus " + busId +
            " is now available."
        );
    }

    // ------------------------------------------------
    // GET STOP
    // ------------------------------------------------
    public Stop getStop(int id) {
        return stops.get(id);
    }

    // ------------------------------------------------
    // GETTERS FOR GUI ACCESS (read-only)
    // ------------------------------------------------
    public HashMap<Integer, Stop> getStops() {
        return stops;
    }

    public HashMap<Integer, Bus> getBuses() {
        return buses;
    }

    public HashMap<Integer, DemandRequest> getDemands() {
        return demands;
    }

    public PriorityQueue<DemandRequest> getDemandQueue() {
        return demandQueue;
    }

    public int getNextArrivalOrder() {
        return nextArrivalOrder;
    }

    public void incrementArrivalOrder() {
        nextArrivalOrder++;
    }
}

// ----------------------------------------------------
// MAIN CLASS
// ----------------------------------------------------
public class Main {

    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);

        TransportService service =
            new TransportService();

        service.loadFromDatabase();

        if (service.getStops().isEmpty() && service.getBuses().isEmpty()) {
            // ------------------------------------------------
            // SAMPLE STOPS
            // ------------------------------------------------

            service.addStop(
                new Stop(1, "Central Bus Stand")
            );

            service.addStop(
                new Stop(2, "Market")
            );

            service.addStop(
                new Stop(3, "College")
            );

            service.addStop(
                new Stop(4, "Hospital")
            );

            service.addStop(
                new Stop(5, "Railway Station")
            );

            // ------------------------------------------------
            // SAMPLE BUSES
            // ------------------------------------------------

            service.addBus(
                new MiniBus(101, 15)
            );

            service.addBus(
                new StandardBus(102, 30)
            );

            service.addBus(
                new StandardBus(103, 40)
            );

            // ------------------------------------------------
            // SAMPLE DEMANDS
            // ------------------------------------------------

            service.addDemand(
                new DemandRequest(
                    1,
                    service.getStop(1),
                    service.getStop(3),
                    20,
                    1
                )
            );

            service.addDemand(
                new DemandRequest(
                    2,
                    service.getStop(2),
                    service.getStop(5),
                    10,
                    2
                )
            );
        }

        // ------------------------------------------------
        // MENU
        // ------------------------------------------------

        while (true) {

            System.out.println(
                "\n================================"
            );

            System.out.println(
                " SMART PUBLIC TRANSPORTATION"
            );

            System.out.println(
                " ROUTE OPTIMIZATION - BASIC"
            );

            System.out.println(
                "================================"
            );

            System.out.println(
                "1. Display Stops"
            );

            System.out.println(
                "2. Display Buses"
            );

            System.out.println(
                "3. Display Waiting Demands"
            );

            System.out.println(
                "4. Add New Demand"
            );

            System.out.println(
                "5. Allocate Bus"
            );

            System.out.println(
                "6. Complete Bus Trip"
            );

            System.out.println(
                "7. Exit"
            );

            System.out.print(
                "Enter your choice: "
            );

            int choice = sc.nextInt();

            // --------------------------------------------
            // DISPLAY STOPS
            // --------------------------------------------

            if (choice == 1) {

                service.displayStops();
            }

            // --------------------------------------------
            // DISPLAY BUSES
            // --------------------------------------------

            else if (choice == 2) {

                service.displayBuses();
            }

            // --------------------------------------------
            // DISPLAY DEMANDS
            // --------------------------------------------

            else if (choice == 3) {

                service.displayWaitingDemands();
            }

            // --------------------------------------------
            // ADD DEMAND
            // --------------------------------------------

            else if (choice == 4) {

                System.out.print(
                    "Enter request ID: "
                );

                int requestId = sc.nextInt();

                System.out.print(
                    "Enter pickup stop ID: "
                );

                int pickupId = sc.nextInt();

                System.out.print(
                    "Enter destination stop ID: "
                );

                int destinationId = sc.nextInt();

                System.out.print(
                    "Enter number of passengers: "
                );

                int passengers = sc.nextInt();

                Stop pickup =
                    service.getStop(pickupId);

                Stop destination =
                    service.getStop(destinationId);

                if (pickup == null ||
                    destination == null) {

                    System.out.println(
                        "Invalid stop ID."
                    );

                } else if (passengers <= 0) {

                    System.out.println(
                        "Passenger count must be positive."
                    );

                } else {

                    DemandRequest demand =
                        new DemandRequest(
                            requestId,
                            pickup,
                            destination,
                            passengers,
                            100 + requestId
                        );

                    service.addDemand(demand);
                }
            }

            // --------------------------------------------
            // ALLOCATE BUS
            // --------------------------------------------

            else if (choice == 5) {

                service.allocateBus();
            }

            // --------------------------------------------
            // COMPLETE BUS TRIP
            // --------------------------------------------

            else if (choice == 6) {

                System.out.print(
                    "Enter bus ID: "
                );

                int busId = sc.nextInt();

                service.completeBusTrip(busId);
            }

            // --------------------------------------------
            // EXIT
            // --------------------------------------------

            else if (choice == 7) {

                System.out.println(
                    "Program ended."
                );

                break;
            }

            else {

                System.out.println(
                    "Invalid choice."
                );
            }
        }

        sc.close();
    }
}