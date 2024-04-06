import java.util.*;

public class Temperature {
    public final static int MAX_TEMP = 70;
    public final static int MIN_TEMP = -100;

    public final static int NUM_THREADS = 8;
    public final static int PAUSE_TIME = 10;

    public static ArrayList<Sensor> sensors;
    public static List<ArrayList<Integer>> memory;

    public static class Sensor extends Thread {

        public boolean hasRead;
        private int id;
        private Random rand;

        Sensor(int id) {
            this.id = id;
            this.hasRead = false;
            this.rand = new Random();
        }

        @Override
        public void run() {
            hasRead = true;
            int sensorValue = rand.nextInt(MAX_TEMP - MIN_TEMP + 1) + MIN_TEMP;
            memory.get(id).add(sensorValue);
        }
    }

    public static void main(String[] args) {
        System.out.println("Beginning temperature simulation");

        memory = Collections.synchronizedList(new ArrayList<ArrayList<Integer>>(NUM_THREADS));

        sensors = new ArrayList<>();
        for (int i = 0; i < NUM_THREADS; i++) {
            sensors.add(new Sensor(i));
            memory.add(new ArrayList<Integer>());
        }

        while (true) {
            // run sensor threads
            for (Sensor s : sensors) {
                if (!s.hasRead) {
                    s.start();
                }
                else {
                    s.run();
                }
            }

            for (Sensor s : sensors) {
                try {
                    s.join();
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            // Print output for current minute
            System.out.printf("Data for minute %-2d:", memory.get(0).size());
            for (ArrayList<Integer> list : memory) {
                System.out.printf("   %-4d", list.get((list.size() - 1)));
            }
            System.out.println();

            if (memory.get(0).size() >= 60) {
                output();
                return;
            }

            // Sleep to simulate time passage
            try {
                Thread.sleep(PAUSE_TIME);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void output() {

        // Sort to get overall min and max temps
        List<Integer> sortedReadings = new ArrayList<Integer>();
        for (List<Integer> l : memory) {
            sortedReadings.addAll(l);
        }
        Collections.sort(sortedReadings);

        // Get minute by minute min and max data to calculate 10-minute interval diffference
        int[] min = new int[60];
        int[] max = new int[60];

        int a = -1, b = -1;
        int maxDiff = Integer.MIN_VALUE;

        for (int i = 0; i < 60; i++) {
            List<Integer> temp = new ArrayList<>();
            for (int j = 0; j < NUM_THREADS; j++) {
                temp.add(memory.get(j).get(i));
            }

            min[i] = Collections.min(temp);
            max[i] = Collections.max(temp);

            if (i >= 10) {
                if ((max[i] - min[i - 10]) > maxDiff) {
                    a = i;
                    b = i - 10;
                    maxDiff = Math.abs(max[i] - min[i - 10]);
                }

                if ((max[i - 10] - min[i]) > maxDiff) {
                    a = i - 10;
                    b = i;
                    maxDiff = Math.abs(max[i - 10] - min[i]);
                }
            }
        }

        // Print output

        System.out.println("Temperature Report");
        System.out.println("--------------------------------");
        System.out.println("Lowest recorded temperatures:");

        for (int i = 0; i < 5; i++) {
            System.out.println(Integer.toString(sortedReadings.get(i)));

        }
        System.out.println("Top 5 highest temperatures:");
        for (int i = 0; i < 5; i++) {
            System.out.println(Integer.toString(sortedReadings.get(sortedReadings.size() - 1 - i)));

        }
        System.out.println("10-minute interval with largest temperature difference:");
        System.out.println(
                "Minute " + (Math.min(a, b) + 1) + " to minute " + (Math.max(a, b) + 1) + " with difference of "
                        + maxDiff + " (" + min[b] + " to " + max[a] + ")");
        System.out.println();
    }
}