import gurobi.GRB;
import gurobi.GRBEnv;
import gurobi.GRBException;
import gurobi.GRBLinExpr;
import gurobi.GRBModel;
import gurobi.GRBVar;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Vector;
import java.util.ArrayList;
import java.util.List;


class Student {
	int[] courses;
	String name;
	public static final int MAX_COURSES_PER_SEMESTER = 2;

	Student(String[] c, String name) {
		this.name = name;
		courses = new int[c.length];
		for (int i = 0; i < c.length; i++) {
			courses[i] = Integer.parseInt(c[i]);
		}
	}

	String getName() {
		return name;
	}

	int[] getCourses() {
		return courses;
	}
}

class Semester {
	public static final int MAX_SEMESTERS = 12;

	public enum SemesterType {
		All, Spring, Summer, Fall
	}
}

class Course {
	public static final int MAX_COURSES = 18;

	public static boolean isCourseAvailableAll(int c) {
		if (c == 2 || c == 3 || c == 4 || c == 6 || c == 8 || c == 9 || c == 12
				|| c == 13) {
			return true;
		}
		return false;
	}

	public static boolean isCourseAvailableInFall(int c) {
		if (isCourseAvailableAll(c)) {
			return true;
		}
		if (c == 1 || c == 7 || c == 11 || c == 15 || c == 17) {
			return true;
		}
		return false;
	}

	public static boolean isCourseAvailableInSpring(int c) {
		if (isCourseAvailableAll(c)) {
			return true;
		}
		if (c == 5 || c == 10 || c == 14 || c == 16 || c == 18) {
			return true;
		}
		return false;
	}
}

class Prerequisite {
	public static int get(int course) {
		if (course == 16) {
			return 4;
		}
		if (course == 12) {
			return 1;
		}
		if (course == 9) {
			return 13;
		}
		if (course == 3) {
			return 7;
		}
		return 0;
	}
}

class StudentFile {

	private boolean isValidLine(String s) {
		if (s.startsWith("%") || s.length() == 0) {
			return false;
		}
		return true;
	}

	private String removeAtEnd(String str, String end) {
		if (str.endsWith(end)) {
			return str.substring(0, str.length() - 1);
		}
		return str;
	}

	private String[] getCourses(String str) {
		return str.split(". +");
	}

	public List<Student> parse(String fileName) {
		BufferedReader br = null;
		List<Student> students = new ArrayList<Student>();
		try {
			String line;
			br = new BufferedReader(new FileReader(fileName));
			int i = 1;
			while ((line = br.readLine()) != null) {
				line = line.trim();
				if (!isValidLine(line)) {
					continue;
				}
				line = removeAtEnd(line, ".");
				students.add(new Student(getCourses(line), Integer.toString(i)));
				i++;
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (br != null)
					br.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		return students;
	}
}

public class Project1Scheduler implements Scheduler {

	private void createConstraints(GRBVar[][][] Yijk, List<Student> students,
			GRBModel model) throws GRBException {
		// students taking courses
		for (int i = 0; i < students.size(); i++) {
			Student student = students.get(i);
			int[] courses = student.getCourses();
			for (int k = 0; k < Semester.MAX_SEMESTERS; k++) {
				GRBLinExpr coursesConstraint = new GRBLinExpr();
				coursesConstraint.addTerm(1, Yijk[i][courses[k] - 1][k]);
				String name = "Course_Student" + student.getName()
						+ "_Semester" + k + "Course" + courses[k];
				model.addConstr(coursesConstraint, GRB.EQUAL, 1, name);
			}
		}

		// TODO: students can only take a class if the prerequiste is taken

		// TODO: course availablity

		// students can only take 2 classes per semester
		for (int i = 0; i < students.size(); i++) {
			Student student = students.get(i);
			for (int k = 0; k < Semester.MAX_SEMESTERS; k++) {
				GRBLinExpr maxCoursesConstraint = new GRBLinExpr();
				for (int j = 0; j < Course.MAX_COURSES; j++) {
					maxCoursesConstraint.addTerm(1, Yijk[i][j][k]);
				}
				String courseName = "MAXCOURSE_Student" + student.getName()
						+ "_Semester" + k;
				model.addConstr(maxCoursesConstraint, GRB.LESS_EQUAL,
						Student.MAX_COURSES_PER_SEMESTER, courseName);
			}
		}
		return;
	}

	private void createObjective(GRBVar[][][] Yijk, List<Student> students,
			GRBModel model) throws GRBException {
		GRBLinExpr expr = new GRBLinExpr();
		for (int i = 0; i < students.size(); i++) {
			for (int j = 0; j < Course.MAX_COURSES; j++) {
				for (int k = 0; k < Semester.MAX_SEMESTERS; k++) {
					expr.addTerm(1, Yijk[i][j][k]);
				}
			}
		}
		model.setObjective(expr, GRB.MAXIMIZE);
		return;
	}

	private GRBVar[][][] createModel(List<Student> students, GRBModel model)
			throws GRBException {
		GRBVar[][][] Yijk = new GRBVar[students.size()][Course.MAX_COURSES][Semester.MAX_SEMESTERS];
		for (int i = 0; i < students.size(); i++) {
			for (int j = 0; j < Course.MAX_COURSES; j++) {
				for (int k = 0; k < Semester.MAX_SEMESTERS; k++) {
					Yijk[i][j][k] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY,
							students.get(i).getName());
				}
			}
		}
		model.update();
		return Yijk;
	}

	private void project1(String fileName) {
		try {
			StudentFile studentFile = new StudentFile();
			List<Student> students = studentFile.parse(fileName);

			//System.out.println("# of students:" + students.size());

			GRBEnv env = new GRBEnv("project1.log");
			GRBModel model = new GRBModel(env);

			GRBVar[][][] Yijk = createModel(students, model);
			createObjective(Yijk, students, model);
			createConstraints(Yijk, students, model);
			
			model.optimize();
			System.out.println("X=" + model.get(GRB.DoubleAttr.ObjVal));
			model.dispose();
			env.dispose();

		} catch (GRBException e) {
			System.out.println("Error code: " + e.getErrorCode() + ". "
					+ e.getMessage());
		}
	}

	public void calculateSchedule(String dataFolder) {
		project1(dataFolder);
	}

	public double getObjectiveValue() {
		// TODO: You will need to implement this
		return 0;
	}

	public Vector<String> getCoursesForStudentSemester(String student,
			String semester) {
		// TODO: You will need to implement this
		return null;
	}

	public Vector<String> getStudentsForCourseSemester(String course,
			String semester) {
		// TODO: You will need to implement this
		return null;
	}
}
