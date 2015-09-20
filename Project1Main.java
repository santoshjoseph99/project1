public class Project1Main {

	public static void main(String[] args) {
		Project1Scheduler scheduler = new Project1Scheduler();
		if (args.length == 2 && args[0].equals("-i")) {
			scheduler.calculateSchedule(args[1]);
			return;
		}
		System.out.println("USAGE: -i inputfile");
	}
}
