package csci201finalproject;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.concurrent.ConcurrentHashMap;

public class SchedulingThread extends Thread {

	// mark's version
	private ArrayList<AddClass> totalClasses;
	private ArrayList<Constraint> constraints;
	private ConcurrentHashMap<String, ArrayList<ArrayList<Section>>> concurrentSchedules;
	static ConcurrentHashMap<String, ArrayList<ArrayList<Section>>> generalHashMap = new ConcurrentHashMap<String, ArrayList<ArrayList<Section>>>();

	// luz's version
	private Hashtable<String, ArrayList<ArrayList<Section>>> schedules;
	private Hashtable<String, ArrayList<ArrayList<Section>>> general;
	private String addClassKey;

	// will have to create a new instance of the thread with each time the algorithm is called
	public SchedulingThread(ArrayList<AddClass> totalClasses, ArrayList<Constraint> constraints,
	                        ConcurrentHashMap<String, ArrayList<ArrayList<Section>>> schedules) {
		this.totalClasses = totalClasses;
		this.constraints = constraints;
		this.concurrentSchedules = schedules;

	}

	// will have to create a new instance of the thread with each time the algorithm is called
	// key must be passed in w/ constructor, run method cannot take parameters
	public SchedulingThread(ArrayList<AddClass> totalClasses, ArrayList<Constraint> constraints, Hashtable<String, ArrayList<ArrayList<Section>>> schedules,
	                        Hashtable<String, ArrayList<ArrayList<Section>>> general, String addClassKey) {
		this.totalClasses = totalClasses;
		this.constraints = constraints;
		this.schedules = schedules;
		this.addClassKey = addClassKey;
		this.general = general;
	}

    /*
    the process for call from the servlet
    front end get call for CSCI103
    we are going to return the schedule Hashtable["csci103"]
    however, the key does not exists
    so condition lock,
    as long as we compute the result,
    signalAll() or notifyAll()
    release the lock()
    */


	// skeleton
	public void run_2() {
        /*calculate the permutation
        get the result set
        suppose we would like to add the class csci 103
        check if the general hashtable has the CSCI103
        if it does not, do the permutation and add to the general hashtable
        check permutation code on test.java
        copy the CSCI103 in the general hashtable and check conflicts with the constraints
        save all non-conflicting result in to the shcedule hashtable


        suppose we would like to add the class CSCI103&CSCI104
        check if the general hashtable has the CSCI103&CSCI104
        if it does not, do the permutation and add to the general hashtable
        add all non-conflicting result to the schedules hashtable*/

		// this step cannot be optimized with executors
		for (AddClass singleAddClass : totalClasses) {
			String className = singleAddClass.getClassName();
			if (!SchedulingThread.generalHashMap.containsKey(className)) {
				ArrayList<ArrayList<Section>> permutations = singleAddClass.generatePermutations();
				SchedulingThread.generalHashMap.put(className, permutations);
			}
		}
		for (AddClass singleAddClass : totalClasses) {
			String className = singleAddClass.getClassName();
			if (!concurrentSchedules.containsKey(className)) {
				ArrayList<ArrayList<Section>> validPermutationWithConstrains = getPermuationWithConstraints(SchedulingThread.generalHashMap.get(className), constraints);
				concurrentSchedules.put(className, validPermutationWithConstrains);
			}

		}
		//todo may change somehow about how to update the general schedule
		String intendedClassName = getIntendedClassName(totalClasses);
		ArrayList<AddClass> subsetClassList = subsetClassList(intendedClassName);
		concurrentSchedules.put(intendedClassName, concatPermuations(subsetClassList));

	}

	/**
	 *
	 * @param validPermutations permutations (ex: lec, lab, quiz) of the class
	 * @param constraints list of Constraint object
	 * @return valid Permutation With Constrains
	 */
	public static ArrayList<ArrayList<Section>> getPermuationWithConstraints
			(ArrayList<ArrayList<Section>> validPermutations, ArrayList<Constraint> constraints) {
		ArrayList<ArrayList<Section>> validPermutationWithConstrains = new ArrayList<ArrayList<Section>>();

		for (ArrayList<Section> singleValidPermutation : validPermutations) {
			/* Extract a single permutation (ex: lec, lab, quiz) of the class we just added */
			boolean stillValid = true;
			/* these for loops check whether each aspect of the permutation is valid against all the constraints.
			 * If even one aspect (a lec, lab, or quiz) does not comply with constraints, we flag that and break
			 * out of our loops accordingly */
			for (Section aSinglePermutation : singleValidPermutation) {
				if(constraints!=null){
					for (Constraint singleConstraint : constraints) {
						if (aSinglePermutation.doesConflict(singleConstraint.getStartTime(), singleConstraint.getEndTime(), singleConstraint.getDays())) {
							stillValid = false;
							break;
						}
					}
				}
				if (!stillValid) {
					break;
				}
			}
			if (stillValid) {
				validPermutationWithConstrains.add(singleValidPermutation);
			}
		}

		return validPermutationWithConstrains;
	}

	// todo think about two same permutations
	public static ArrayList<ArrayList<Section>> concatTwoPermutations
			(ArrayList<ArrayList<Section>> firstPermutations, ArrayList<ArrayList<Section>> secondPermutations) {
		ArrayList<ArrayList<Section>> setIntersection =null;

		/* if prevClass is still an empty string, that means the class we are adding if the first
		 * class added (there would be no "previous" set of classes to create an intersection of).
		 * TODO: write code to handle that case. Should be simple enough. */
		if(firstPermutations==null){
			if(secondPermutations==null){
				return setIntersection;
			}else {
				return secondPermutations;
			}
		}else {
			if(secondPermutations==null){
				return firstPermutations;
			}else {
				setIntersection = new ArrayList<ArrayList<Section>>();
				/* extracts the schedule developed for the previous classes (ex: "103 104")*/
				/* we iterate through the valid single course schedule for the class we just added.
				 * ex: this would be the valid schedule that fits all the constraints for a single course
				 * called CSCI201 */
				for (ArrayList<Section> firstSinglePermutation : firstPermutations) {
					/* this array list will add as the container for the new single schedule developed.
					 * For example, when one schedule for the single course CS201 is compared and has no
					 * conflict with a schedule developed for CS103 and CS104, this is the container that
					 * holds that new resulting schedule. NOTE: this also means that this arraylist has a
					 * variety of sections from DIFFERENT courses. Some parsing will be required to organize it
					 * by course again. */
					ArrayList<Section> validSingleSetInersection = new ArrayList<Section>();

					boolean stillValid = true;

					for (ArrayList<Section> secondSinglePermutation : secondPermutations) {
						/* extracts a single schedule for previous classes. ex: one mixture of lec, lab, quiz for
						 * CS103 and CS104 that do not conflict with each other. */
						/* these for loops breakdown the aforementioned schedules back into comparable sections. */
						for (Section singlefirstSection : firstSinglePermutation) {
							for (Section singleSecondSection : secondSinglePermutation) {
								if (singlefirstSection.doesConflict(singleSecondSection.getStartTime(), singleSecondSection.getEndTime(), singleSecondSection.getDays())) {
									/* if even one section conflicts, then it would not be a valid schedule */
									stillValid = false;
									break;
								}
								// else {
								// 	/* otherwise, everything is fine for now and we want to push the section that
								// 	 * works into the data set */
								// 	validSingleSetInersection.add(singleSecondSection);
								// }
							}
							if (stillValid) {
								// /* if there were no conflicts at by the end, we can go ahead and push this section too. */
								// validSingleSetInersection.add(singlefirstSection);
							} else {
								break;
							}
						}

						if (stillValid) {
							/* if all the sections from both schedules are compatabile, then the resulting
							 * schedule is part of the set intersection. */
							ArrayList<Section> singlePermutationCombination= new ArrayList<Section>();
							singlePermutationCombination.addAll(firstSinglePermutation);
							singlePermutationCombination.addAll(secondSinglePermutation);
							setIntersection.add(singlePermutationCombination);

						} else {
							break;
						}
					}
				}
			}
		}

		return setIntersection;
	}

	public static ArrayList<ArrayList<Section>> concatPermuations
			(ArrayList<AddClass> subsetClassList) {
		ArrayList<ArrayList<Section>> result = null;
		return result;
	}

	public static String concatClassNames(String firstGroup, String secondGroup) {
		// e.g. first group= "CSCI103 CSCI270", secondGroup="CSCI104"
		// intended result= "CSCI103 CSCI104 CSCI 270" in alphabetical order
		String result = null;
		return result;
	}

	/**
	 *
	 * @param totalClasses
	 * @param addClassKey the new key inserted into the totalClasses
	 * @return prevClass names with " " between, no " " before or after
	 */
	public static String concatPrevClassNames(ArrayList<AddClass> totalClasses, String addClassKey){
		StringBuilder prevClass= new StringBuilder();
		for (AddClass totalClass : totalClasses) {
			if (!totalClass.getClassName().equals(addClassKey)) {
				prevClass.append(totalClass.getClassName()).append(" ");
			}
		}
		return prevClass.toString().trim();
	}

	public static String getIntendedClassName(ArrayList<AddClass> totalClasses) {
		String intendedClassName = null;
		return intendedClassName;
	}

	/**
	 * @param intendedClassName the class name I would like to have ,such as "CSCI103 CSCI104 CSCI 270"
	 * @return a list of class that is in the schedule table, which can compose the intendedClassName
	 */
	public static ArrayList<AddClass> subsetClassList(String intendedClassName) {
		// minimize size subset with given sum
		// https://leetcode.com/problems/minimum-size-subarray-sum/
		ArrayList<AddClass> subsetClassNameList = null;
		return subsetClassNameList;
	}

	public ArrayList<AddClass> getTotalClasses() {
		return totalClasses;
	}

	public void setTotalClasses(ArrayList<AddClass> totalClasses) {
		this.totalClasses = totalClasses;
	}

	public ArrayList<Constraint> getConstraints() {
		return constraints;
	}

	public void setConstraints(ArrayList<Constraint> constraints) {
		this.constraints = constraints;
	}

	public ConcurrentHashMap<String, ArrayList<ArrayList<Section>>> getConcurrentSchedules() {
		return concurrentSchedules;
	}

	public void setConcurrentSchedules(ConcurrentHashMap<String, ArrayList<ArrayList<Section>>> concurrentSchedules) {
		this.concurrentSchedules = concurrentSchedules;
	}

	public static ConcurrentHashMap<String, ArrayList<ArrayList<Section>>> getGeneralHashMap() {
		return generalHashMap;
	}

	public static void setGeneralHashMap(ConcurrentHashMap<String, ArrayList<ArrayList<Section>>> generalHashMap) {
		SchedulingThread.generalHashMap = generalHashMap;
	}

	public Hashtable<String, ArrayList<ArrayList<Section>>> getSchedules() {
		return schedules;
	}

	public void setSchedules(Hashtable<String, ArrayList<ArrayList<Section>>> schedules) {
		this.schedules = schedules;
	}

	public Hashtable<String, ArrayList<ArrayList<Section>>> getGeneral() {
		return general;
	}

	public void setGeneral(Hashtable<String, ArrayList<ArrayList<Section>>> general) {
		this.general = general;
	}

	public String getAddClassKey() {
		return addClassKey;
	}

	public void setAddClassKey(String addClassKey) {
		this.addClassKey = addClassKey;
	}

	/* TODO: get rid of this line and investigate these warnings */
	// @SuppressWarnings("unchecked")
	public void run() {

        /* Check if the class is in our general hash table; if not, create all valid permutations,
        where a valid permutation is defined as a set of classes (ex: lecture, lab, quiz) that do not conflict with each other. */


        if (!general.containsKey(addClassKey)) {
	        for (AddClass totalClass : totalClasses) {
		        if (totalClass.getClassName().equals(addClassKey)) {
			        general.put(addClassKey, totalClass.generatePermutations());
			        break;
		        }
	        }
		}


		ArrayList<ArrayList<Section>> validPermutations = general.get(addClassKey);
		/* validAgainstContraints will be the things inserted into our schedule hashtable. This is defined as a
		 * list of permutations that do not conflict with any constraint. Thus, they are considered a valid
		 * schedule for that particular class.  */
		ArrayList<ArrayList<Section>> validAgainstContraints = new ArrayList<ArrayList<Section>>();

		// for (int i = 0; i < validPermutations.size(); i++) {
		// 	/* Extract a single permutation (ex: lec, lab, quiz) of the class we just added */
		// 	ArrayList<Section> singlePermutation = validPermutations.get(i);
		// 	boolean stillValid = true;
		// 	/* these for loops check whether each aspect of the permutation is valid against all the constraints.
		// 	 * If even one aspect (a lec, lab, or quiz) does not comply with constraints, we flag that and break
		// 	 * out of our loops accordingly */
		// 	for (int j = 0; j < singlePermutation.size(); j++) {
		// 		for (int k = 0; k < constraints.size(); k++) {
		// 			Constraint singleConstraint = constraints.get(k);
		// 			if (singlePermutation.get(j).doesConflict(singleConstraint.getStartTime(), singleConstraint.getEndTime(), singleConstraint.getDays())) {
		// 				stillValid = false;
		// 				break;
		// 			}
		// 		}
		// 		if (!stillValid) {
		// 			break;
		// 		}
		// 	}
		// 	if (stillValid) {
		// 		validAgainstContraints.add(singlePermutation);
		// 	}
		// }
		validAgainstContraints=getPermuationWithConstraints(validPermutations,constraints);

		schedules.put(addClassKey, validAgainstContraints);

		/* Code to generate the key to find the set which we are finding the intersection of */
		String prevClass = "";
		prevClass=concatPrevClassNames(totalClasses,addClassKey);
		// /* the string key generated should be concatenation of all the classes inserted into table before addClass */
		// /* EX: CS103 was added to our class schedule. Then we add CS104 and that goes into our schedules hashtable. If
		//  * we want to insert CS201 next, we need to find the set intersection of the key CS201 with the set "CS103 CS104"*/
		// /* This method of key generation will have to changed when it comes to removal. We take advantage of the order of
		//  * and arraylist to generate this key */
		//
		// ArrayList<ArrayList<Section>> prevClassesSet = null;
		// ArrayList<ArrayList<Section>> setIntersection = new ArrayList<ArrayList<Section>>();
		//
		// /* if prevClass is still an empty string, that means the class we are adding if the first
		//  * class added (there would be no "previous" set of classes to create an intersection of).
		//  * TODO: write code to handle that case. Should be simple enough. */
		// if (schedules.containsKey(prevClass) && prevClass != "") {
		// 	/* extracts the schedule developed for the previous classes (ex: "103 104")*/
		// 	prevClassesSet = schedules.get(prevClass);
		// 	/* we iterate through the valid single course schedule for the class we just added.
		// 	 * ex: this would be the valid schedule that fits all the constraints for a single course
		// 	 * called CSCI201 */
		// 	for (int i = 0; i < validAgainstContraints.size(); i++) {
		// 		/* this array list will add as the container for the new single schedule developed.
		// 		 * For example, when one schedule for the single course CS201 is compared and has no
		// 		 * conflict with a schedule developed for CS103 and CS104, this is the container that
		// 		 * holds that new resulting schedule. NOTE: this also means that this arraylist has a
		// 		 * variety of sections from DIFFERENT courses. Some parsing will be required to organize it
		// 		 * by course again. */
		// 		ArrayList<Section> validSingleSetInersection = new ArrayList<Section>();
		//
		// 		ArrayList<Section> singleCourseSchedule = validAgainstContraints.get(i);
		// 		boolean stillValid = true;
		//
		// 		for (int k = 0; k < prevClassesSet.size(); k++) {
		// 			/* extracts a single schedule for previous classes. ex: one mixture of lec, lab, quiz for
		// 			 * CS103 and CS104 that do not conflict with each other. */
		// 			ArrayList<Section> singlePrevClassSchedule = prevClassesSet.get(k);
		//
		// 			/* these for loops breakdown the aforementioned schedules back into comparable sections. */
		// 			for (int oneSec = 0; oneSec < singleCourseSchedule.size(); oneSec++) {
		// 				Section singleCourse = singleCourseSchedule.get(oneSec);
		// 				for (int onePrev = 0; onePrev < singlePrevClassSchedule.size(); onePrev++) {
		// 					Section singlePrev = singlePrevClassSchedule.get(onePrev);
		// 					if (singleCourse.doesConflict(singlePrev.getStartTime(), singlePrev.getEndTime(), singlePrev.getDays())) {
		// 						/* if even one section conflicts, then it would not be a valid schedule */
		// 						stillValid = false;
		// 						break;
		// 					} else {
		// 						/* otherwise, everything is fine for now and we want to push the section that
		// 						 * works into the data set */
		// 						validSingleSetInersection.add(singlePrev);
		// 					}
		// 				}
		// 				if (stillValid) {
		// 					/* if there were no conflicts at by the end, we can go ahead and push this section too. */
		// 					validSingleSetInersection.add(singleCourse);
		// 				} else {
		// 					break;
		// 				}
		// 			}
		// 			if (stillValid) {
		// 				/* if all the sections from both schedules are compatabile, then the resulting
		// 				 * schedule is part of the set intersection. */
		// 				setIntersection.add(validSingleSetInersection);
		// 			} else {
		// 				break;
		// 			}
		// 		}
		// 	}
		// 	String newIntersectionKey = prevClass + " " + addClassKey;
		// 	schedules.put(newIntersectionKey, setIntersection);
		// 	/* this should be the end of the operation. to extract the new schedule, you just need the
		// 	 * key, which is a combination of the names of all the courses. */
		// } else {
		// 	/* Develop this code to handle not finding the previous classes intersection; the problem
		// 	 * will most likely be that we are not generating the right key for it to begin with. */
		// 	System.out.println("Schedule for previous classes not found.");
		// }

		ArrayList<ArrayList<Section>> setIntersection=concatTwoPermutations(schedules.get(prevClass.trim()),validAgainstContraints);
		if(setIntersection!=null){
			String newIntersectionKey = prevClass+" "+ addClassKey;
			schedules.put(newIntersectionKey.trim(), setIntersection);
		}
	}

	public static void printPrettyPermutations(ArrayList<ArrayList<Section>> permutations){
		if(permutations==null){
			System.out.println("Empty list");
			return;
		}
		for (ArrayList<Section> permutation:permutations		     ) {
			for (Section section:permutation){
				System.out.print(section.toString()+"\t");
			}
			System.out.println();
		}
	}

	public static void test(){
		ArrayList<AddClass> totalClasses;
		ArrayList<Constraint> constraints;
		Hashtable<String, ArrayList<ArrayList<Section>>> schedule;
		Hashtable<String, ArrayList<ArrayList<Section>>> general;
		String addClassKey;

		// single class case
		String className1="CSCI103";
		AddClass addClass1=new AddClass("CSCI", className1);
		totalClasses=new ArrayList<AddClass>();
		totalClasses.add(addClass1);

		Constraint constraint=null;
		constraints=null;

		schedule=new Hashtable<String, ArrayList<ArrayList<Section>>>();
		general=new Hashtable<String, ArrayList<ArrayList<Section>>> ();
		addClassKey="CSCI103";

		SchedulingThread test1=new SchedulingThread(totalClasses,constraints,schedule,general,addClassKey);
		test1.run();
		System.out.println(concatPrevClassNames(test1.getTotalClasses(),""));
		System.out.println("Result size: "+test1.schedules.get(concatPrevClassNames(test1.getTotalClasses(),"")).size());
		printPrettyPermutations(test1.schedules.get(className1));


		// two classes case
		System.out.println();
		String className2="CSCI201";
		AddClass addClass2=new AddClass("CSCI", className2);
		totalClasses.add(addClass2);
		addClassKey="CSCI201";

		SchedulingThread test2=new SchedulingThread(totalClasses,constraints,schedule,general,addClassKey);
		test2.run();
		System.out.println(concatPrevClassNames(test2.getTotalClasses(),""));
		System.out.println("Result size: "+test2.schedules.get(concatPrevClassNames(test2.getTotalClasses(),"")).size());
		printPrettyPermutations(test2.schedules.get(concatPrevClassNames(test2.getTotalClasses(),"")));


		// three classes case
		System.out.println();
		String className3="CSCI104";
		AddClass addClass3=new AddClass("CSCI", className3);
		totalClasses.add(addClass3);
		addClassKey="CSCI104";
		SchedulingThread test3=new SchedulingThread(totalClasses,constraints,schedule,general,addClassKey);
		test3.run();
		System.out.println(concatPrevClassNames(test3.getTotalClasses(),""));
		System.out.println("Result size: "+test3.schedules.get(concatPrevClassNames(test3.getTotalClasses(),"")).size());
		printPrettyPermutations(test3.schedules.get(concatPrevClassNames(test3.getTotalClasses(),"")));

	}


	public static void main(String[] args) {
		new SchedulingThread(null, null, null);
		new SchedulingThread(null, null, null, null, null);
		test();
	}


//        suppose we would like to add the class csci 103
//        check if the general hashtable has the CSCI103
//        if it does not, do the permutation and add to the general hashtable
//        // check permutation code on test.java
//        copy the CSCI103 in the general hashtable and check conflicts with the constraints
//        save all non-conflicting result in to the shcedule hashtable
//
//
//        suppose we would like to add the class CSCI103&CSCI104
//        check if the general hashtable has the CSCI103&CSCI104
//        if it does not, do the permutation and add to the general hashtable
//        add all non-conflicting result to the schedules hashtable


}
