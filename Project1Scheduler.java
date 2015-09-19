import java.util.Vector;

import gurobi.GRB;
import gurobi.GRBEnv;
import gurobi.GRBException;
import gurobi.GRBLinExpr;
import gurobi.GRBModel;
import gurobi.GRBVar;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class Student {
    int[] courses;
    String name;

    Student(String[] c, String name){
       this.name = name;
        courses = new int[c.length];
        for(int i=0; i < c.length; i++){
            courses[i] = Integer.parseInt(c[i]);
        }
    }

   String getName(){
      return name;
   }

   int[] getCourses(){
      return courses;
   }
}

class Semester {

    public static final int MAX_SEMESTERS = 12;	
   //12 semesters available
   //fall, spring, summer
   // no student can take more then 2 courses a semester
}

class Course {
    public static final int MAX_COURSES = 18;	
   //18 courses
   //2,3,4,6,8,9,12,13 offered every semester
   //1,7,11,15,17 fall only
   //5,10,14,16.18 spring only
}

class Prerequisite {
   //4 -> 16
   //12 -> 1
   //9 -> 13
   //3 -> 7
}

// boolean variables Yijk (i = # students, j = # courses, k = # of semesters)
// constraints 1) max number of courses taken by a student at any semester: Yijk < Nmax
//             2) capacity limits for courses: Yijk < Ac,jk

public class Project1Scheduler implements Scheduler {

   private static final int MAX_COURSES_PER_SEMESTER = 2;

    private static List<Student> readStudentsFile(String fileName){
        BufferedReader br = null;
        List<Student> students = new ArrayList<Student>();
		try {
			String sCurrentLine;
			br = new BufferedReader(new FileReader(fileName));
         int i=1;
			while ((sCurrentLine = br.readLine()) != null) {
                sCurrentLine = sCurrentLine.trim();
                if(sCurrentLine.startsWith("%") || sCurrentLine.length() == 0){
                   continue;
                }
                if(sCurrentLine.endsWith(".")){
                    sCurrentLine = sCurrentLine.substring(0, sCurrentLine.length()-1);
                }
				students.add(new Student(sCurrentLine.split(". +"), Integer.toString(i)));
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

    private void project1(String fileName){
       try{
         List<Student> students = readStudentsFile(fileName);

         System.out.println("# of students:"+ students.size());

         GRBEnv env = new GRBEnv("project1.log");
         GRBModel model = new GRBModel(env);
         GRBVar[][][] Yijk = new GRBVar[students.size()][Course.MAX_COURSES][Semester.MAX_SEMESTERS];

         for(int i = 0; i < students.size(); i++){
            for(int j = 0; j < Course.MAX_COURSES; j++){
               for(int k = 0; k < Semester.MAX_SEMESTERS; k++){
                  Yijk[i][j][k] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, students.get(i).getName());
               }
            }
         }
         model.update();

         //TODO: objective
         GRBLinExpr expr = new GRBLinExpr();
         for(int i = 0; i < students.size(); i++){
            for(int j = 0; j < Course.MAX_COURSES; j++){
               for(int k = 0; k < Semester.MAX_SEMESTERS; k++){
                  expr.addTerm(1, Yijk[i][j][k]);
               }
            }
         }
         model.setObjective(expr, GRB.MAXIMIZE);


         //constraints

         //students taking courses
         for(int i = 0; i < students.size(); i++){
            Student student = students.get(i);
            int[] courses = student.getCourses();
            for(int k = 0; k < Semester.MAX_SEMESTERS; k++){
               GRBLinExpr coursesConstraint = new GRBLinExpr();
               coursesConstraint.addTerm(1, Yijk[i][courses[k]-1][k]);
               String name = "Course_Student" + student.getName() + "_Semester" + k + "Course" + courses[k];
               model.addConstr(coursesConstraint, GRB.EQUAL, 1, name);
            }
         }

         //TODO: students can only take a class if the prerequiste is taken

         //TODO: course availablity

         //students can only take 2 classes per semester
         for(int i = 0; i < students.size(); i++){
            Student student = students.get(i);
            for(int k = 0; k < Semester.MAX_SEMESTERS; k++){
               GRBLinExpr maxCoursesConstraint = new GRBLinExpr();
               for(int j = 0; j < Course.MAX_COURSES; j++){
                  maxCoursesConstraint.addTerm(1, Yijk[i][j][k]);
               }
               String courseName = "MAXCOURSE_Student" + student.getName() + "_Semester" + k;
               model.addConstr(maxCoursesConstraint, GRB.LESS_EQUAL, 2, courseName);
            }
         }

         model.optimize();
         model.dispose();
         env.dispose();

      } catch (GRBException e) {
         System.out.println("Error code: " + e.getErrorCode() + ". " + e.getMessage());
      }
    }

    private static final int MAX_CLASS_SIZE = 3;	// Note: Play with this and see 
    							// what happens to the results

	public void mip1(){
    try {
      GRBEnv    env   = new GRBEnv("mip1.log");
      GRBModel  model = new GRBModel(env);

      // Create variables

      GRBVar x = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "x");
      GRBVar y = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "y");
      GRBVar z = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "z");

      // Integrate new variables

      model.update();

      // Set objective: maximize x + y + 2 z

      GRBLinExpr expr = new GRBLinExpr();
      expr.addTerm(1.0, x); expr.addTerm(1.0, y); expr.addTerm(2.0, z);
      model.setObjective(expr, GRB.MAXIMIZE);

      // Add constraint: x + 2 y + 3 z <= 4

      expr = new GRBLinExpr();
      expr.addTerm(1.0, x); expr.addTerm(2.0, y); expr.addTerm(3.0, z);
      model.addConstr(expr, GRB.LESS_EQUAL, 4.0, "c0");

     // Add constraint: x + y >= 1

      expr = new GRBLinExpr();
      expr.addTerm(1.0, x); expr.addTerm(1.0, y);
      model.addConstr(expr, GRB.GREATER_EQUAL, 1.0, "c1");

      // Optimize model

      model.optimize();

      System.out.println(x.get(GRB.StringAttr.VarName)
                         + " " +x.get(GRB.DoubleAttr.X));
      System.out.println(y.get(GRB.StringAttr.VarName)
                         + " " +y.get(GRB.DoubleAttr.X));
      System.out.println(z.get(GRB.StringAttr.VarName)
                         + " " +z.get(GRB.DoubleAttr.X));

      System.out.println("Obj: " + model.get(GRB.DoubleAttr.ObjVal));

      // Dispose of model and environment

      model.dispose();
      env.dispose();

    } catch (GRBException e) {
      System.out.println("Error code: " + e.getErrorCode() + ". " +
                         e.getMessage());
    }
	}

	public void studentExample(){
		// TODO Read the test data from the provided folder

		// The following code is an example of how to use the Gurobi solver.
		// Replace the variables, objective, and constraints with those
		// needed to calculate the schedule for the provided data.
		
		// This example has three students and two classes.  Each class is
		// limited to two students. The objective is to maximize the total 
		// number of student-classes taken. It do not deal with semesters
		
        GRBEnv env;
		try {
			env = new GRBEnv("mip1.log");
			GRBModel model = new GRBModel(env);
		
			// Create the variables
			GRBVar gvarJoeCS6300 = model.addVar(0, 1, 0.0, GRB.BINARY, "Joe_CS6300");
			GRBVar gvarJoeCS6310 = model.addVar(0, 1, 0.0, GRB.BINARY, "Joe_CS6310");
			GRBVar gvarJaneCS6300 = model.addVar(0, 1, 0.0, GRB.BINARY, "Jane_CS6300");
			GRBVar gvarJaneCS6310 = model.addVar(0, 1, 0.0, GRB.BINARY, "Jane_CS6310");
			GRBVar gvarMaryCS6300 = model.addVar(0, 1, 0.0, GRB.BINARY, "Mary_CS6300");
			GRBVar gvarMaryCS6310 = model.addVar(0, 1, 0.0, GRB.BINARY, "Mary_CS6310");

			// Integrate new variables
            model.update();
			
            // Set the objective as the sum of all student-courses
            GRBLinExpr expr = new GRBLinExpr();
            expr.addTerm( 1, gvarJoeCS6300 );
            expr.addTerm( 1, gvarJoeCS6310 );
            expr.addTerm( 1, gvarJaneCS6300 );
            expr.addTerm( 1, gvarJaneCS6310 );
            expr.addTerm( 1, gvarMaryCS6300 );
            expr.addTerm( 1, gvarMaryCS6310 );
            
            model.setObjective(expr, GRB.MAXIMIZE);

			// Add Constraints for each class so that the sum of students taking
            // the course is less than or equal to MAX_CLASS_SIZE
            expr = new GRBLinExpr();
            expr.addTerm( 1, gvarJoeCS6300 );
            expr.addTerm( 1, gvarJaneCS6300 );
            expr.addTerm( 1, gvarMaryCS6300 );
            model.addConstr(expr, GRB.LESS_EQUAL, MAX_CLASS_SIZE, "CS6300" );

            expr = new GRBLinExpr();
            expr.addTerm( 1, gvarJoeCS6310 );
            expr.addTerm( 1, gvarJaneCS6310 );
            expr.addTerm( 1, gvarMaryCS6310 );
            model.addConstr(expr, GRB.LESS_EQUAL, MAX_CLASS_SIZE, "CS6310" );

            // Optimize the model
            model.optimize();

            // Display our results
            double objectiveValue = model.get(GRB.DoubleAttr.ObjVal);            
            System.out.printf( "Ojective value = %f\n", objectiveValue );
            
            if( gvarJoeCS6300.get(GRB.DoubleAttr.X) == 1 )
                System.out.printf( "Joe is taking CS6300\n" );            	
            if( gvarJoeCS6310.get(GRB.DoubleAttr.X) == 1 )
                System.out.printf( "Joe is taking CS6310\n" );            	
            if( gvarJaneCS6300.get(GRB.DoubleAttr.X) == 1 )
                System.out.printf( "Jane is taking CS6300\n" );            	
            if( gvarJaneCS6310.get(GRB.DoubleAttr.X) == 1 )
                System.out.printf( "Jane is taking CS6310\n" );            	
            if( gvarMaryCS6300.get(GRB.DoubleAttr.X) == 1 )
                System.out.printf( "Mary is taking CS6300\n" );            	
            if( gvarMaryCS6310.get(GRB.DoubleAttr.X) == 1 )
                System.out.printf( "Mary is taking CS6310\n" );            	
                        
            
		} catch (GRBException e) {
			e.printStackTrace();
		}


	}

	public void calculateSchedule(String dataFolder){

		//mip1();
		//studentExmple();
        project1(dataFolder);
	}

	public double getObjectiveValue() {
		// TODO: You will need to implement this
		return 0;
	}

	public Vector<String> getCoursesForStudentSemester(String student, String semester) {
		// TODO: You will need to implement this
		return null;
	}

	public Vector<String> getStudentsForCourseSemester(String course, String semester) {
		// TODO: You will need to implement this
		return null;
	}

}
/*import java.util.Vector;

import gurobi.GRB;
import gurobi.GRBEnv;
import gurobi.GRBException;
import gurobi.GRBLinExpr;
import gurobi.GRBModel;
import gurobi.GRBVar;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class Student {
    int[] classes;
    String name;

    Student(String[] c, String name){
       this.name = name;
        classes = new int[c.length];
        for(int i=0; i < c.length; i++){
            classes[i] = Integer.parseInt(c[i]);
        }
    }

   String getName(){
      return name;
   }

}

class Semester {

    public static final int MAX_SEMESTERS = 12;	
   //12 semesters available
   //fall, spring, summer
   // no student can take more then 2 courses a semester
}

class Course {
    public static final int MAX_COURSES = 18;	
   //18 courses
   //2,3,4,6,8,9,12,13 offered every semester
   //1,7,11,15,17 fall only
   //5,10,14,16.18 spring only
}

class Prerequisite {
   //4 -> 16
   //12 -> 1
   //9 -> 13
   //3 -> 7
}

// boolean variables Yijk (i = # students, j = # courses, k = # of semesters)
// constraints 1) max number of courses taken by a student at any semester: Yijk < Nmax
//             2) capacity limits for courses: Yijk < Ac,jk

public class Project1Scheduler implements Scheduler {

    private static List<Student> readStudentsFile(String fileName){
        BufferedReader br = null;
        List<Student> students = new ArrayList<Student>();
		try {
			String sCurrentLine;
			br = new BufferedReader(new FileReader(fileName));
         int i=1;
			while ((sCurrentLine = br.readLine()) != null) {
                sCurrentLine = sCurrentLine.trim();
                if(sCurrentLine.startsWith("%") || sCurrentLine.length() == 0){
                   continue;
                }
                if(sCurrentLine.endsWith(".")){
                    sCurrentLine = sCurrentLine.substring(0, sCurrentLine.length()-1);
                }
				students.add(new Student(sCurrentLine.split(". +"), Integer.toString(i)));
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

    private void project1(String fileName){
       try{
       List<Student> students = readStudentsFile(fileName);

       System.out.println("# of students:"+ students.size());

       GRBEnv env = new GRBEnv("project1.log");
       GRBModel model = new GRBModel(env);

      for(int i = 0; i < students.size(); i++){
         for(int j = 0; j < Course.MAX_COURSES; j++){
            for(int k = 0; k < Semester.MAX_SEMESTERS; k++){
               model.addVar(0.0, 1.0, 0.0, GRB.BINARY, students.get(i).getName());
            }
         }
      }
      model.update();
    } catch (GRBException e) {
      System.out.println("Error code: " + e.getErrorCode() + ". " +
                         e.getMessage());
    }
    }

    private static final int MAX_CLASS_SIZE = 3;	// Note: Play with this and see 
    							// what happens to the results

	public void mip1(){
    try {
      GRBEnv    env   = new GRBEnv("mip1.log");
      GRBModel  model = new GRBModel(env);

      // Create variables

      GRBVar x = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "x");
      GRBVar y = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "y");
      GRBVar z = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "z");

      // Integrate new variables

      model.update();

      // Set objective: maximize x + y + 2 z

      GRBLinExpr expr = new GRBLinExpr();
      expr.addTerm(1.0, x); expr.addTerm(1.0, y); expr.addTerm(2.0, z);
      model.setObjective(expr, GRB.MAXIMIZE);

      // Add constraint: x + 2 y + 3 z <= 4

      expr = new GRBLinExpr();
      expr.addTerm(1.0, x); expr.addTerm(2.0, y); expr.addTerm(3.0, z);
      model.addConstr(expr, GRB.LESS_EQUAL, 4.0, "c0");

     // Add constraint: x + y >= 1

      expr = new GRBLinExpr();
      expr.addTerm(1.0, x); expr.addTerm(1.0, y);
      model.addConstr(expr, GRB.GREATER_EQUAL, 1.0, "c1");

      // Optimize model

      model.optimize();

      System.out.println(x.get(GRB.StringAttr.VarName)
                         + " " +x.get(GRB.DoubleAttr.X));
      System.out.println(y.get(GRB.StringAttr.VarName)
                         + " " +y.get(GRB.DoubleAttr.X));
      System.out.println(z.get(GRB.StringAttr.VarName)
                         + " " +z.get(GRB.DoubleAttr.X));

      System.out.println("Obj: " + model.get(GRB.DoubleAttr.ObjVal));

      // Dispose of model and environment

      model.dispose();
      env.dispose();

    } catch (GRBException e) {
      System.out.println("Error code: " + e.getErrorCode() + ". " +
                         e.getMessage());
    }
	}

	public void studentExample(){
		// TODO Read the test data from the provided folder

		// The following code is an example of how to use the Gurobi solver.
		// Replace the variables, objective, and constraints with those
		// needed to calculate the schedule for the provided data.
		
		// This example has three students and two classes.  Each class is
		// limited to two students. The objective is to maximize the total 
		// number of student-classes taken. It do not deal with semesters
		
        GRBEnv env;
		try {
			env = new GRBEnv("mip1.log");
			GRBModel model = new GRBModel(env);
		
			// Create the variables
			GRBVar gvarJoeCS6300 = model.addVar(0, 1, 0.0, GRB.BINARY, "Joe_CS6300");
			GRBVar gvarJoeCS6310 = model.addVar(0, 1, 0.0, GRB.BINARY, "Joe_CS6310");
			GRBVar gvarJaneCS6300 = model.addVar(0, 1, 0.0, GRB.BINARY, "Jane_CS6300");
			GRBVar gvarJaneCS6310 = model.addVar(0, 1, 0.0, GRB.BINARY, "Jane_CS6310");
			GRBVar gvarMaryCS6300 = model.addVar(0, 1, 0.0, GRB.BINARY, "Mary_CS6300");
			GRBVar gvarMaryCS6310 = model.addVar(0, 1, 0.0, GRB.BINARY, "Mary_CS6310");

			// Integrate new variables
            model.update();
			
            // Set the objective as the sum of all student-courses
            GRBLinExpr expr = new GRBLinExpr();
            expr.addTerm( 1, gvarJoeCS6300 );
            expr.addTerm( 1, gvarJoeCS6310 );
            expr.addTerm( 1, gvarJaneCS6300 );
            expr.addTerm( 1, gvarJaneCS6310 );
            expr.addTerm( 1, gvarMaryCS6300 );
            expr.addTerm( 1, gvarMaryCS6310 );
            
            model.setObjective(expr, GRB.MAXIMIZE);

			// Add Constraints for each class so that the sum of students taking
            // the course is less than or equal to MAX_CLASS_SIZE
            expr = new GRBLinExpr();
            expr.addTerm( 1, gvarJoeCS6300 );
            expr.addTerm( 1, gvarJaneCS6300 );
            expr.addTerm( 1, gvarMaryCS6300 );
            model.addConstr(expr, GRB.LESS_EQUAL, MAX_CLASS_SIZE, "CS6300" );

            expr = new GRBLinExpr();
            expr.addTerm( 1, gvarJoeCS6310 );
            expr.addTerm( 1, gvarJaneCS6310 );
            expr.addTerm( 1, gvarMaryCS6310 );
            model.addConstr(expr, GRB.LESS_EQUAL, MAX_CLASS_SIZE, "CS6310" );

            // Optimize the model
            model.optimize();

            // Display our results
            double objectiveValue = model.get(GRB.DoubleAttr.ObjVal);            
            System.out.printf( "Ojective value = %f\n", objectiveValue );
            
            if( gvarJoeCS6300.get(GRB.DoubleAttr.X) == 1 )
                System.out.printf( "Joe is taking CS6300\n" );            	
            if( gvarJoeCS6310.get(GRB.DoubleAttr.X) == 1 )
                System.out.printf( "Joe is taking CS6310\n" );            	
            if( gvarJaneCS6300.get(GRB.DoubleAttr.X) == 1 )
                System.out.printf( "Jane is taking CS6300\n" );            	
            if( gvarJaneCS6310.get(GRB.DoubleAttr.X) == 1 )
                System.out.printf( "Jane is taking CS6310\n" );            	
            if( gvarMaryCS6300.get(GRB.DoubleAttr.X) == 1 )
                System.out.printf( "Mary is taking CS6300\n" );            	
            if( gvarMaryCS6310.get(GRB.DoubleAttr.X) == 1 )
                System.out.printf( "Mary is taking CS6310\n" );            	
                        
            
		} catch (GRBException e) {
			e.printStackTrace();
		}


	}

	public void calculateSchedule(String dataFolder){

		//mip1();
		//studentExmple();
        project1(dataFolder);
	}

	public double getObjectiveValue() {
		// TODO: You will need to implement this
		return 0;
	}

	public Vector<String> getCoursesForStudentSemester(String student, String semester) {
		// TODO: You will need to implement this
		return null;
	}

	public Vector<String> getStudentsForCourseSemester(String course, String semester) {
		// TODO: You will need to implement this
		return null;
	}

}*/
